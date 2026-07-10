package com.sbf.main;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Date;

public final class M8D14ExeDiag extends OutputStream {
    private static boolean installed;

    private final PrintStream original;
    private final PrintStream file;

    private M8D14ExeDiag(PrintStream original, PrintStream file) {
        this.original = original;
        this.file = file;
    }

    public static synchronized void install() {
        if (installed) {
            return;
        }
        installed = true;
        try {
            File dir = new File("temp");
            if (!dir.exists()) {
                dir.mkdirs();
            }
            File file = new File(dir, "m8-d14-exe-diag.log");
            PrintStream originalOut = System.out;
            PrintStream originalErr = System.err;
            PrintStream fileOut = new PrintStream(new FileOutputStream(file, true), true);
            System.setOut(new PrintStream(new M8D14ExeDiag(originalOut, fileOut), true));
            System.setErr(new PrintStream(new M8D14ExeDiag(originalErr, fileOut), true));
            System.out.println(
                    "M8D14_EXE_DIAG_READY path="
                            + file.getAbsolutePath()
                            + " ts="
                            + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date()));
        } catch (Throwable t) {
            try {
                System.err.println("M8D14_EXE_DIAG_FAILED " + t);
            } catch (Throwable ignored) {
                // Keep startup alive even if diagnostics cannot be installed.
            }
        }
    }

    @Override
    public void write(int b) throws IOException {
        original.write(b);
        file.write(b);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        original.write(b, off, len);
        file.write(b, off, len);
    }

    @Override
    public void flush() throws IOException {
        original.flush();
        file.flush();
    }
}
