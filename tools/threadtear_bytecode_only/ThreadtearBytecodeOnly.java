import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

import me.nov.threadtear.execution.Clazz;
import me.nov.threadtear.execution.cleanup.remove.RemoveUnusedVariables;
import me.nov.threadtear.io.JarIO;

public final class ThreadtearBytecodeOnly {
  private ThreadtearBytecodeOnly() {
  }

  public static void main(String[] args) throws Exception {
    if (args.length != 2) {
      throw new IllegalArgumentException("usage: input.jar output.jar");
    }

    File input = new File(args[0]);
    File output = new File(args[1]);
    File parent = output.getAbsoluteFile().getParentFile();
    if (parent != null && !parent.isDirectory() && !parent.mkdirs()) {
      throw new IllegalStateException("failed to create output directory: " + parent);
    }
    ArrayList<Clazz> classes = JarIO.loadClasses(input);
    Map<String, Clazz> byName = new LinkedHashMap<>();
    for (Clazz clazz : classes) {
      byName.put(clazz.node.name, clazz);
    }

    RemoveUnusedVariables cleanup = new RemoveUnusedVariables();
    if (!cleanup.execute(byName, true)) {
      throw new IllegalStateException("Threadtear cleanup reported failure");
    }
    JarIO.saveAsJar(input, output, classes, true, false);
    if (!output.isFile() || output.length() == 0) {
      throw new IllegalStateException("Threadtear did not produce an output JAR");
    }
    System.out.println("classes=" + classes.size());
    System.out.println("execution=" + cleanup.name);
    System.out.println("output=" + output.getAbsolutePath());
  }
}
