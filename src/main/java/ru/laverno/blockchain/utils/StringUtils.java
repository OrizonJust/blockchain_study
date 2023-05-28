package ru.laverno.blockchain.utils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class StringUtils {

    public static String applySha256(final String input) {
        try {
            final var digest = MessageDigest.getInstance("SHA-256");

            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));

            final var sb = new StringBuffer();

            for (int i = 0; i < hash.length; i++) {

                final var hex = Integer.toHexString(0xff & hash[i]);
                if (hex.length() == 1) {
                    sb.append('0');
                }
                sb.append(hex);
            }

            return sb.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new RuntimeException();
        }
    }
}
