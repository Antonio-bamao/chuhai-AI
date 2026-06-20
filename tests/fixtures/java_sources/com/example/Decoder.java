package com.example;

public class Decoder {
    public static String x(String value) {
        char[] chars = value.toCharArray();
        String caller = Thread.currentThread().getStackTrace()[2].getClassName();
        chars[0] = (char)(chars[0] ^ caller.length());
        return new String(chars);
    }
}

class Logger {
    public static void log(String value) {
        System.out.println(value);
    }
}
