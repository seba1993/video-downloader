package com.github.luischavez.videodownloader.support;

import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;

public final class LocationRequestUtils {

    private static final String HEADER_FRAGMENT_PREFIX = "__headers__";

    private LocationRequestUtils() {
    }

    public static String sanitize(String location) {
        if (location == null) {
            return null;
        }

        int index = location.indexOf("#" + HEADER_FRAGMENT_PREFIX);
        if (index == -1) {
            return location;
        }

        return location.substring(0, index);
    }

    public static Map<String, String> extractHeaders(String location) {
        Map<String, String> headers = new HashMap<>();

        if (location == null) {
            return headers;
        }

        int index = location.indexOf("#" + HEADER_FRAGMENT_PREFIX);
        if (index == -1) {
            return headers;
        }

        String fragment = location.substring(index + 1 + HEADER_FRAGMENT_PREFIX.length());

        if (fragment.startsWith("&")) {
            fragment = fragment.substring(1);
        }

        if (fragment.trim().isEmpty()) {
            return headers;
        }

        String[] pairs = fragment.split("&");
        for (String pair : pairs) {
            if (pair == null || pair.trim().isEmpty()) {
                continue;
            }

            String[] keyValue = pair.split("=", 2);
            String key = decode(keyValue[0]);
            String value = keyValue.length > 1 ? decode(keyValue[1]) : "";

            if (key == null || key.trim().isEmpty() || value == null || value.trim().isEmpty()) {
                continue;
            }

            headers.put(normalizeHeaderName(key), value);
        }

        return headers;
    }

    public static Map<String, String> mergeHeaders(Map<String, String> first, Map<String, String> second) {
        Map<String, String> merged = new HashMap<>();

        if (first != null) {
            merged.putAll(first);
        }

        if (second != null) {
            merged.putAll(second);
        }

        return merged;
    }

    private static String normalizeHeaderName(String key) {
        String lower = key.trim().toLowerCase();

        if (lower.equals("referer") || lower.equals("referrer")) {
            return "Referer";
        } else if (lower.equals("origin")) {
            return "Origin";
        } else if (lower.equals("user-agent") || lower.equals("useragent") || lower.equals("ua")) {
            return "User-Agent";
        } else if (lower.equals("cookie") || lower.equals("cookies")) {
            return "Cookie";
        }

        return key.trim();
    }

    private static String decode(String value) {
        try {
            return URLDecoder.decode(value, "UTF-8");
        } catch (Exception ex) {
            return value;
        }
    }
}
