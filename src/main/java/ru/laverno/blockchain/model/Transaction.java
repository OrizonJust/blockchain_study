package ru.laverno.blockchain.model;

import ru.laverno.blockchain.Main;
import ru.laverno.blockchain.utils.StringUtils;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.List;

public class Transaction {

    private String id;
    private PublicKey sender;
    private PublicKey recipient;
    private float value;
    private byte[] signature;

    public List<TransactionInput> inputs = new ArrayList<>();
    public List<TransactionOutput> outputs = new ArrayList<>();

    private static int sequence = 0;

    public Transaction(PublicKey from, PublicKey to, float value, List<TransactionInput> inputs) {
        this.sender = from;
        this.recipient = to;
        this.value = value;
        this.inputs = inputs;
    }

    public String getId() {
        return this.id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public PublicKey getSender() {
        return this.sender;
    }

    public PublicKey getRecipient() {
        return this.recipient;
    }

    public float getValue() {
        return this.value;
    }

    private String calculateHash() {
        sequence++;

        return StringUtils.applySha256(StringUtils.getStringFromKey(sender) + StringUtils.getStringFromKey(recipient) + Float.toString(value) + sequence);
    }

    public void generateSignature(PrivateKey privateKey) {
        String data = StringUtils.getStringFromKey(sender) + StringUtils.getStringFromKey(recipient) + Float.toString(value);
        signature = StringUtils.applyECDSASign(privateKey, data);
    }

    public boolean verifySignature() {
        String data = StringUtils.getStringFromKey(sender) + StringUtils.getStringFromKey(recipient) + Float.toString(value);
        return StringUtils.verifyECDSASign(sender, data, signature);
    }

    public boolean processTransaction() {
        if (!verifySignature()) {
            System.out.println("Transaction Signature failed to verify");
            return false;
        }

        for (TransactionInput input : inputs) {
            input.setUTXO(Main.UTXOs.get(input.getTransactionOutputId()));
        }

        if (getInputsValue() < Main.minimumTransaction) {
            System.out.println("Transaction Inputs to small: " + getInputsValue());
            return false;
        }

        float leftOver = getInputsValue() - value;
        id = calculateHash();
        outputs.add(new TransactionOutput(this.recipient, value, id));
        outputs.add(new TransactionOutput(this.sender, leftOver, id));

        for (TransactionOutput output : outputs) {
            Main.UTXOs.put(output.getId(), output);
        }

        for (TransactionInput input : inputs) {
            if (input.getUTXO() == null) {
                continue;
            }

            Main.UTXOs.remove(input.getUTXO().getId());
        }

        return true;
    }

    public float getInputsValue() {
        float total = 0;
        for (TransactionInput input : inputs) {
            if (input.getUTXO() == null) {
                continue;
            }
            total += input.getUTXO().getValue();
        }

        return total;
    }

    public float getOutputsValue() {
        float total = 0;
        for (TransactionOutput output : outputs) {
            total += output.getValue();
        }

        return total;
    }
}
