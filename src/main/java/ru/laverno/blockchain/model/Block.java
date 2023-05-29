package ru.laverno.blockchain.model;

import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.laverno.blockchain.utils.StringUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Block {

    private static final Logger logger = LoggerFactory.getLogger(Block.class);

    @Getter
    private String hash;

    @Getter
    private final String previousHash;

    private String merkleRoot;

    @Getter
    private final List<Transaction> transactions = new ArrayList<>();

    private final long timeStamp;

    private int nonce;

    public Block(final String previousHash) {
        this.previousHash = previousHash;
        this.timeStamp = new Date().getTime();
        this.hash = calculateHash();
    }

    public String calculateHash() {
        return StringUtils.applySha256(previousHash + timeStamp + nonce + merkleRoot);
    }

    public void mineBlock(final int difficulty) {
        merkleRoot = StringUtils.getMerkleRoot(transactions);
        final var target = new String(new char[difficulty]).replace('\0', '0');

        while (!hash.substring(0, difficulty).equals(target)) {
            nonce++;
            hash = calculateHash();
        }

        logger.info("Block has been mined! - {}", hash);
    }

    public boolean addTransaction(final Transaction transaction) {
        if (transaction == null) {
            return false;
        }

        if (!previousHash.equals("0") && !transaction.processTransaction()) {
            logger.info("Transaction failed to process. Discarded!");
            return false;
        }

        transactions.add(transaction);
        logger.info("Transaction successfully added to Block.");
        return true;
    }
}
