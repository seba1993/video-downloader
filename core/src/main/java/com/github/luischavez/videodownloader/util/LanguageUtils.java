package com.github.luischavez.videodownloader.util;

public final class LanguageUtils {

    public static String code(String lang) {
        switch (lang) {
            case "Spanish":
                return "es";
            case "English":
                return "en";
            case "French":
                return "fr";
            case "Portuguese":
                return "pt";
            default:
                return lang;
        }
    }
}
