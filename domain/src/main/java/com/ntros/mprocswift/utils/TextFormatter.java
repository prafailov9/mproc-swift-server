package com.ntros.mprocswift.service.payment;

public final class TextFormatter {

    public static String format(String template, Object... args) {
        return String.format(template, args);
    }

}
