package ru.laverno.blockchain.model;

import lombok.Getter;
import lombok.Setter;

public class TransactionInput {

    @Getter
    private final String transactionOutputId;

    @Getter
    @Setter
    private TransactionOutput unspentTransactionOutput;

    public TransactionInput(final String transactionOutputId) {
        this.transactionOutputId = transactionOutputId;
    }
}
