package ru.laverno.blockchain.utils;

import ru.laverno.blockchain.model.Transaction;

import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public class StringUtils {

    private StringUtils() throws IllegalAccessException {
        throw new IllegalAccessException();
    }

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

    public static byte[] applyECDSASign(PrivateKey privateKey, String input) {
        byte[] output;

        try {
            Signature dsa = Signature.getInstance("ECDSA", "BC");
            dsa.initSign(privateKey);

            byte[] strByte = input.getBytes();
            dsa.update(strByte);

            output = dsa.sign();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }

        return output;
    }

    public static boolean verifyECDSASign(PublicKey publicKey, String data, byte[] signature) {
        try {
            Signature ecdsaVerify = Signature.getInstance("ECDSA", "BC");
            ecdsaVerify.initVerify(publicKey);
            ecdsaVerify.update(data.getBytes());
            return ecdsaVerify.verify(signature);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public static String getStringFromKey(Key key) {
        return Base64.getEncoder().encodeToString(key.getEncoded());
    }

    public static String getMerkleRoot(List<Transaction> transactions) {
        int count = transactions.size();
        List<String> previousTreeLayer = new ArrayList<>();

        for (Transaction transaction : transactions) {
            previousTreeLayer.add(transaction.getId());
        }

        List<String> treeLayer = previousTreeLayer;

        while (count > 1) {
            treeLayer = new ArrayList<>();

            for (int i = 1; i < previousTreeLayer.size(); i++) {
                treeLayer.add(applySha256(previousTreeLayer.get(i - 1) + previousTreeLayer.get(i)));
            }

            count = treeLayer.size();
            previousTreeLayer = treeLayer;
        }

        return (treeLayer.size() == 1) ? treeLayer.get(0) : "";
    }
}
