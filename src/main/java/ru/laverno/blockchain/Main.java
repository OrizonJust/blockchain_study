package ru.laverno.blockchain;

import lombok.Getter;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.laverno.blockchain.exception.BlockchainValidException;
import ru.laverno.blockchain.model.*;

import java.security.Security;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Main {

    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    private static final List<Block> blockchain = new ArrayList<>();

    @Getter
    protected static final Map<String, TransactionOutput> UTXO = new HashMap<>();
    private static final int DIFFICULTY = 5;

    public static final float MINIMUM_TRANSACTION = 0.1f;
    private static Wallet walletA;
    private static Wallet walletB;
    private static Transaction genesisTransaction;

    public static void main(String[] args) {
        Security.addProvider(new BouncyCastleProvider());

        walletA = new Wallet();
        walletB = new Wallet();
        final var coinbase = new Wallet();

        genesisTransaction = new Transaction(coinbase.getPublicKey(), walletA.getPublicKey(), 100f, null);
        genesisTransaction.generateSignature(coinbase.getPrivateKey());
        genesisTransaction.setId("0");
        genesisTransaction.getOutputs().add(new TransactionOutput(genesisTransaction.getRecipient(), genesisTransaction.getValue(), genesisTransaction.getId()));
        UTXO.put(genesisTransaction.getOutputs().get(0).getId(), genesisTransaction.getOutputs().get(0));

        logger.info("Creating and Mining Genesis Block...");
        final var genesis = new Block("0");
        if (!genesis.addTransaction(genesisTransaction)) {
            return;
        }
        addBlock(genesis);

        final var block1 = new Block(genesis.getHash());
        logWalletBalance();
        logger.info("WalletA is Attempting to send funds (40) to WalletB...");
        block1.addTransaction(walletA.sendFunds(walletB.getPublicKey(), 40f));
        addBlock(block1);
        logWalletBalance();

        final var block2 = new Block(block1.getHash());
        logger.info("WalletA Attempting to send more funds (1000) than it has...");
        block2.addTransaction(walletA.sendFunds(walletB.getPublicKey(), 1000f));
        addBlock(block2);
        logWalletBalance();

        Block block3 = new Block(block2.getHash());
        logger.info("WalletB is Attempting to send funds (20) to WalletA...");
        block3.addTransaction(walletB.sendFunds(walletA.getPublicKey(), 20));
        logWalletBalance();

        if (!isChainValid()) {
            throw new BlockchainValidException("Blockchain is not valid!");
        }
    }

    public static boolean isChainValid() {
        Block currentBlock;
        Block previousBlock;

        final var hashTarget = new String(new char[DIFFICULTY]).replace('\0', '0');
        final var tempUnspentTransactionOutputs = new HashMap<String, TransactionOutput>();
        tempUnspentTransactionOutputs.put(genesisTransaction.getOutputs().get(0).getId(), genesisTransaction.getOutputs().get(0));

        try {
            for (int i = 1; i < blockchain.size(); i++) {

                currentBlock = blockchain.get(i);
                previousBlock = blockchain.get(i - 1);

                compareHashes(currentBlock);

                comparePreviousHashes(previousBlock, currentBlock);

                hashSolved(currentBlock, hashTarget);

                TransactionOutput tempOutput;
                for (var currentTransaction : currentBlock.getTransactions()) {

                    verifySignature(currentTransaction);

                    compareInputOutputValues(currentTransaction);

                    for (var input : currentTransaction.getInputs()) {
                        tempOutput = tempUnspentTransactionOutputs.get(input.getTransactionOutputId());

                        checkOutput(tempOutput, currentTransaction.getId());

                        checkValue(input, tempOutput, currentTransaction.getId());

                        tempUnspentTransactionOutputs.remove(input.getTransactionOutputId());
                    }

                    for (TransactionOutput output : currentTransaction.getOutputs()) {
                        tempUnspentTransactionOutputs.put(output.getId(), output);
                    }

                    checkRecipient(currentTransaction, currentTransaction.getId());

                    checkSender(currentTransaction, currentTransaction.getId());
                }
            }
        } catch (BlockchainValidException ex) {
            logger.error("Blockchain is not Valid!");
            logger.error(ex.getMessage());
            return false;
        }

        logger.info("Blockchain is Valid!");
        return true;
    }

    public static void addBlock(Block newBlock) {
        newBlock.mineBlock(DIFFICULTY);
        blockchain.add(newBlock);
    }

    private static void compareHashes(final Block currentBlock) {
        if (!currentBlock.getHash().equals(currentBlock.calculateHash())) {
            throw new BlockchainValidException("Current Hashes not equal");
        }
    }

    private static void comparePreviousHashes(final Block previousBlock, final Block currentBlock) {
        if (!previousBlock.getHash().equals(currentBlock.getPreviousHash())) {
            throw new BlockchainValidException("Previous Hashes not equal");
        }
    }

    private static void hashSolved(final Block currentBlock, final String hashTarget) {
        if (!currentBlock.getHash().substring(0, DIFFICULTY).equals(hashTarget)) {
            throw new BlockchainValidException("This block hasn't been mined");
        }
    }

    private static void verifySignature(final Transaction currentTransaction) {
        if (currentTransaction.verifySignature()) {
            throw new BlockchainValidException("Signature on Transaction with id: " + currentTransaction.getId() + ". is Invalid!");
        }
    }

    private static void compareInputOutputValues(final Transaction currentTransaction) {
        if (currentTransaction.getInputsValue() != currentTransaction.getOutputsValue()) {
            throw new BlockchainValidException("Inputs are not equal to outputs on Transaction with id: " + currentTransaction.getId());
        }
    }

    private static void checkOutput(final TransactionOutput tempOutput, final String id) {
        if (tempOutput == null) {
            throw new BlockchainValidException("Referenced input on Transaction with id: " + id + " is Missing!");
        }
    }

    private static void checkValue(final TransactionInput input, final TransactionOutput tempOutput, final String id) {
        if (input.getUnspentTransactionOutput().getValue() != tempOutput.getValue()) {
            throw new BlockchainValidException("Referenced input Transaction with id: " + id + " value is Invalid");
        }
    }

    private static void checkRecipient(final Transaction currentTransaction, final String id) {
        if (currentTransaction.getOutputs().get(0).getRecipient() != currentTransaction.getRecipient()) {
            throw new BlockchainValidException("Transaction with id: " + id + " output recipient is not who it should be");
        }
    }

    private static void checkSender(final Transaction currentTransaction, final String id) {
        if (currentTransaction.getOutputs().get(1).getRecipient() != currentTransaction.getSender()) {
            throw new BlockchainValidException("Transaction with id: " + id + " output 'change' is not sender");
        }
    }

    private static void logWalletBalance() {
        logger.info("WalletA's balance is: {}", walletA.getBalance());
        logger.info("WalletB's balance is: {}", walletB.getBalance());
    }
}
