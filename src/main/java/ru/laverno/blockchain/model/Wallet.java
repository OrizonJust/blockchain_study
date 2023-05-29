package ru.laverno.blockchain.model;

import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.laverno.blockchain.Main;
import ru.laverno.blockchain.exception.KeyPairGenerateException;

import java.security.*;
import java.security.spec.ECGenParameterSpec;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Wallet {

    private static final Logger logger = LoggerFactory.getLogger(Wallet.class);

    @Getter
    private PrivateKey privateKey;

    @Getter
    private PublicKey publicKey;

    @Getter
    private final Map<String, TransactionOutput> unspentTransactionOutputs = new HashMap<>();

    public Wallet() {
        generateKeyPair();
    }

    private void generateKeyPair() {
        try {
            final var keyGen = KeyPairGenerator.getInstance("ECDSA", "BC");
            final var random = SecureRandom.getInstance("SHA1PRNG");
            final var ecSpec = new ECGenParameterSpec("secp256r1");

            keyGen.initialize(ecSpec, random);
            final var keyPair = keyGen.generateKeyPair();

            privateKey = keyPair.getPrivate();
            publicKey = keyPair.getPublic();
        } catch (Exception ex) {
            throw new KeyPairGenerateException("Generate key pair not finished!");
        }
    }

    public float getBalance() {
        var total = 0;

        for (var item : Main.getUTXO().entrySet()) {
            final var unspentTransactionOutput = item.getValue();

            if (unspentTransactionOutput.isMine(publicKey)) {
                unspentTransactionOutputs.put(unspentTransactionOutput.getId(), unspentTransactionOutput);
                total += unspentTransactionOutput.getValue();
            }
        }

        return total;
    }

    public Transaction sendFunds(final PublicKey recipient, final float value) {
        if (getBalance() < value) {
            logger.info("Not enough funds to send transaction. Transaction discarded.");
            return null;
        }

        final var inputs = new ArrayList<TransactionInput>();

        var total = 0f;
        for (var item : unspentTransactionOutputs.entrySet()) {
            final var unspentTransactionOutput = item.getValue();
            total += unspentTransactionOutput.getValue();
            inputs.add(new TransactionInput(unspentTransactionOutput.getId()));

            if (total > value) {
                break;
            }
        }

        final var newTransaction = new Transaction(publicKey, recipient, value, inputs);
        newTransaction.generateSignature(privateKey);

        for (var input : inputs) {
            unspentTransactionOutputs.remove(input.getTransactionOutputId());
        }

        return newTransaction;
    }
}
