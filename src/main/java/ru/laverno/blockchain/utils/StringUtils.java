package ru.laverno.blockchain.utils;

import ru.laverno.blockchain.exception.CryptographicException;
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

            final var hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));

            final var sb = new StringBuilder();

            for (var item : hash) {
                final var hex = Integer.toHexString(0xff & item);

                if (hex.length() == 1) {
                    sb.append('0');
                }

                sb.append(hex);
            }

            return sb.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new CryptographicException("Can't find algorithm: SHA-256");
        }
    }

    public static byte[] applyECDSASign(final PrivateKey privateKey, final String input) {
        byte[] output;

        try {
            final var dsa = Signature.getInstance("ECDSA", "BC");
            dsa.initSign(privateKey);

            final var strByte = input.getBytes();
            dsa.update(strByte);

            output = dsa.sign();
        } catch (Exception ex) {
            throw new CryptographicException("Can't get instance Signature with parameters: algorithm{ECDSA}, provider{BC}");
        }

        return output;
    }

    public static boolean verifyECDSASign(final PublicKey publicKey, final String data, final byte[] signature) {
        try {
            final var ecdsaVerify = Signature.getInstance("ECDSA", "BC");
            ecdsaVerify.initVerify(publicKey);
            ecdsaVerify.update(data.getBytes());
            return ecdsaVerify.verify(signature);
        } catch (Exception ex) {
            throw new CryptographicException("Can't verify signature!");
        }
    }

    public static String getStringFromKey(final Key key) {
        return Base64.getEncoder().encodeToString(key.getEncoded());
    }

    public static String getMerkleRoot(final List<Transaction> transactions) {
        var count = transactions.size();
        var previousTreeLayer = new ArrayList<String>();

        for (var transaction : transactions) {
            previousTreeLayer.add(transaction.getId());
        }

        var treeLayer = previousTreeLayer;

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
