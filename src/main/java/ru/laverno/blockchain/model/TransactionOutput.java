package ru.laverno.blockchain.model;

import ru.laverno.blockchain.utils.StringUtils;

import java.security.PublicKey;

public class TransactionOutput {

    private String id;
    private PublicKey recipient;
    private float value;
    private String parentTransactionId;

    public TransactionOutput(PublicKey recipient, float value, String parentTransactionId) {
        this.recipient = recipient;
        this.value = value;
        this.parentTransactionId = parentTransactionId;
        this.id = StringUtils.applySha256(StringUtils.getStringFromKey(recipient) + Float.toString(value) + parentTransactionId);
    }

    public String getId() {
        return this.id;
    }

    public PublicKey getRecipient() {
        return this.recipient;
    }

    public float getValue() {
        return this.value;
    }

    public boolean isMine(PublicKey publicKey) {
        return publicKey == recipient;
    }
}
