package com.example;

public class Sample {
    static String a = Decoder.x("\u0001");

    public Sample() {
        Decoder.x("\u0002");
    }

    public String run(String value) {
        return Decoder.x("\u0003") + Decoder.x("\u0004");
    }
}
