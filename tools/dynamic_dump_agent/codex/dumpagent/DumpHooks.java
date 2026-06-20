package codex.dumpagent;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

public final class DumpHooks {
  private static PrintWriter writer;

  private DumpHooks() {
  }

  static synchronized void initialize(File output) throws Exception {
    File parent = output.getAbsoluteFile().getParentFile();
    if (parent != null && !parent.isDirectory() && !parent.mkdirs()) {
      throw new IllegalStateException("cannot create dump directory: " + parent);
    }
    writer = new PrintWriter(
        new BufferedWriter(
            new OutputStreamWriter(
                new FileOutputStream(output, true),
                StandardCharsets.UTF_8
            )
        ),
        true
    );
  }

  public static synchronized void record(String input, String output, String family) {
    if (writer == null) {
      return;
    }
    StackTraceElement[] stack = Thread.currentThread().getStackTrace();
    String callerClass = stack.length > 3 ? stack[3].getClassName() : "";
    String callerMethod = stack.length > 3 ? stack[3].getMethodName() : "";
    writer.println(
        "{"
            + "\"family\":\"" + escape(family) + "\","
            + "\"caller_class\":\"" + escape(callerClass) + "\","
            + "\"caller_method\":\"" + escape(callerMethod) + "\","
            + "\"input\":\"" + escape(input) + "\","
            + "\"output\":\"" + escape(output) + "\""
            + "}"
    );
  }

  private static String escape(String value) {
    if (value == null) {
      return "null";
    }
    StringBuilder out = new StringBuilder();
    for (int index = 0; index < value.length(); index++) {
      char ch = value.charAt(index);
      switch (ch) {
        case '\\':
          out.append("\\\\");
          break;
        case '"':
          out.append("\\\"");
          break;
        case '\r':
          out.append("\\r");
          break;
        case '\n':
          out.append("\\n");
          break;
        case '\t':
          out.append("\\t");
          break;
        default:
          if (ch < 0x20 || Character.isSurrogate(ch)) {
            out.append(String.format("\\u%04x", (int) ch));
          } else {
            out.append(ch);
          }
      }
    }
    return out.toString();
  }
}
