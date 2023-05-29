package ru.laverno.blockchain.model;

import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.laverno.blockchain.Main;
import ru.laverno.blockchain.utils.StringUtils;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.List;

public class Transaction {

    private static final Logger logger = LoggerFactory.getLogger(Transaction.class);

    @Getter
    @Setter
    private String id;

    @Getter
    private final PublicKey sender;

    @Getter
    private final PublicKey recipient;

    @Getter
    private final float value;

    private byte[] signature;

    @Getter
    private final List<TransactionInput> inputs;

    @Getter
    private final List<TransactionOutput> outputs;

    public Transaction(final PublicKey from, final PublicKey to, final float value, final List<TransactionInput> inputs) {
        this.sender = from;
        this.recipient = to;
        this.value = value;
        this.inputs = inputs;
        this.outputs = new ArrayList<>();
    }

    private String calculateHash() {
        return StringUtils.applySha256(StringUtils.getStringFromKey(sender) + StringUtils.getStringFromKey(recipient) + value + (Math.random() * 100 + 1));
    }

    public void generateSignature(final PrivateKey privateKey) {
        final var data = StringUtils.getStringFromKey(sender) + StringUtils.getStringFromKey(recipient) + value;
        signature = StringUtils.applyECDSASign(privateKey, data);
    }

    public boolean verifySignature() {
        final var data = StringUtils.getStringFromKey(sender) + StringUtils.getStringFromKey(recipient) + value;
        return !StringUtils.verifyECDSASign(sender, data, signature);
    }

    public boolean processTransaction() {
        if (verifySignature()) {
            logger.info("Transaction signature failed to verify.");
            return false;
        }

        for (var input : inputs) {
            input.setUnspentTransactionOutput(Main.getUTXO().get(input.getTransactionOutputId()));
        }

        if (getInputsValue() < Main.MINIMUM_TRANSACTION) {
            logger.info("Transaction inputs to small: {}", getInputsValue());
            return false;
        }

        final var leftOver = getInputsValue() - value;
        id = calculateHash();
        outputs.add(new TransactionOutput(this.recipient, value, id));
        outputs.add(new TransactionOutput(this.sender, leftOver, id));

        for (var output : outputs) {
            Main.getUTXO().put(output.getId(), output);
        }

        for (var input : inputs) {
            if (input.getUnspentTransactionOutput() == null) {
                continue;
            }

            Main.getUTXO().remove(input.getUnspentTransactionOutput().getId());
        }

        return true;
    }

    public float getInputsValue() {
        var total = 0f;

        for (var input : inputs) {
            if (input.getUnspentTransactionOutput() == null) {
                continue;
            }
            total += input.getUnspentTransactionOutput().getValue();
        }

        return total;
    }

    public float getOutputsValue() {
        var total = 0;

        for (var output : outputs) {
            total += output.getValue();
        }

        return total;
    }
}
