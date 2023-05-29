package ru.laverno.blockchain.model;

import lombok.Getter;
import ru.laverno.blockchain.utils.StringUtils;

import java.security.PublicKey;

@Getter
public class TransactionOutput {

    private final String id;

    private final PublicKey recipient;

    private final float value;

    private final String parentTransactionId;

    public TransactionOutput(final PublicKey recipient, final float value, final String parentTransactionId) {
        this.recipient = recipient;
        this.value = value;
        this.parentTransactionId = parentTransactionId;
        this.id = StringUtils.applySha256(StringUtils.getStringFromKey(recipient) + value + parentTransactionId);
    }

    public boolean isMine(final PublicKey publicKey) {
        return publicKey == recipient;
    }
}
