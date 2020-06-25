package com.github.luischavez.videodownloader.util;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class CryptoUtils {

    public static String encodeUrl(String url) {
        try {
            return URLEncoder.encode(url, StandardCharsets.UTF_8.toString());
        } catch (UnsupportedEncodingException ex) {
            // IGNORE
        }

        return url;
    }

    public static String decodeUrl(String url) {
        try {
            return URLDecoder.decode(url, StandardCharsets.UTF_8.toString());
        } catch (UnsupportedEncodingException ex) {
            // IGNORE
        }

        return url;
    }

    public static String base64Encode(String data) {
        return Base64.getEncoder().encodeToString(data.getBytes());
    }

    public static String base64Decode(String data) {
        return new String(Base64.getDecoder().decode(data));
    }
}
