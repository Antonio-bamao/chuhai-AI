package codex.dumpagent;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.nio.charset.StandardCharsets;
import java.security.ProtectionDomain;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public final class DynamicStringDumpAgent {
  private DynamicStringDumpAgent() {
  }

  public static void premain(String agentArgs, Instrumentation instrumentation) throws Exception {
    Map<String, String> args = parseArgs(agentArgs);
    File targets = new File(required(args, "targets"));
    File output = new File(required(args, "out"));
    Set<String> priorities = parsePriorities(args.get("priorities"));
    Map<String, Map<String, String>> targetMap = loadTargets(targets, priorities);
    DumpHooks.initialize(output);
    instrumentation.addTransformer(new Transformer(targetMap), false);
    System.out.println("[codex-dump-agent] target classes=" + targetMap.size());
    System.out.println("[codex-dump-agent] output=" + output.getAbsolutePath());
  }

  private static Map<String, String> parseArgs(String value) {
    Map<String, String> args = new HashMap<>();
    if (value == null || value.trim().isEmpty()) {
      return args;
    }
    for (String item : value.split(",")) {
      int equals = item.indexOf('=');
      if (equals > 0) {
        args.put(item.substring(0, equals).trim(), item.substring(equals + 1).trim());
      }
    }
    return args;
  }

  private static String required(Map<String, String> args, String key) {
    String value = args.get(key);
    if (value == null || value.isEmpty()) {
      throw new IllegalArgumentException("missing agent argument: " + key);
    }
    return value;
  }

  private static Set<String> parsePriorities(String value) {
    Set<String> priorities = new HashSet<>();
    String source = value == null || value.isEmpty() ? "critical;high" : value;
    for (String priority : source.split(";")) {
      priorities.add(priority.trim());
    }
    return priorities;
  }

  private static Map<String, Map<String, String>> loadTargets(
      File file,
      Set<String> priorities
  ) throws Exception {
    Map<String, Map<String, String>> targets = new HashMap<>();
    try (BufferedReader reader = new BufferedReader(
        new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)
    )) {
      String line;
      boolean header = true;
      while ((line = reader.readLine()) != null) {
        if (header) {
          header = false;
          continue;
        }
        String[] columns = line.split("\\t", -1);
        if (columns.length < 4 || !priorities.contains(columns[3])) {
          continue;
        }
        targets.computeIfAbsent(columns[0], key -> new HashMap<>())
            .put(columns[1], columns[2]);
      }
    }
    return targets;
  }

  private static final class Transformer implements ClassFileTransformer {
    private final Map<String, Map<String, String>> targets;

    private Transformer(Map<String, Map<String, String>> targets) {
      this.targets = targets;
    }

    @Override
    public byte[] transform(
        ClassLoader loader,
        String className,
        Class<?> classBeingRedefined,
        ProtectionDomain protectionDomain,
        byte[] classfileBuffer
    ) {
      Map<String, String> methods = targets.get(className);
      if (methods == null) {
        return null;
      }
      try {
        ClassReader reader = new ClassReader(classfileBuffer);
        ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS);
        ClassVisitor visitor = new ClassVisitor(Opcodes.ASM7, writer) {
          @Override
          public MethodVisitor visitMethod(
              int access,
              String name,
              String descriptor,
              String signature,
              String[] exceptions
          ) {
            MethodVisitor method = super.visitMethod(
                access,
                name,
                descriptor,
                signature,
                exceptions
            );
            String family = methods.get(name);
            if (family == null
                || !descriptor.equals("(Ljava/lang/String;)Ljava/lang/String;")
                || (access & Opcodes.ACC_STATIC) == 0) {
              return method;
            }
            return new MethodVisitor(Opcodes.ASM7, method) {
              @Override
              public void visitInsn(int opcode) {
                if (opcode == Opcodes.ARETURN) {
                  super.visitInsn(Opcodes.DUP);
                  super.visitVarInsn(Opcodes.ALOAD, 0);
                  super.visitInsn(Opcodes.SWAP);
                  super.visitLdcInsn(family);
                  super.visitMethodInsn(
                      Opcodes.INVOKESTATIC,
                      "codex/dumpagent/DumpHooks",
                      "record",
                      "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V",
                      false
                  );
                }
                super.visitInsn(opcode);
              }
            };
          }
        };
        reader.accept(visitor, 0);
        System.out.println("[codex-dump-agent] instrumented " + className);
        return writer.toByteArray();
      } catch (Throwable throwable) {
        throwable.printStackTrace();
        return null;
      }
    }
  }
}
