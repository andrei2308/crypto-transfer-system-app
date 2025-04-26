package com.example.crypto_payment_system.utils.web3;

/**
 * Inner class for transaction results
 */
public class TransactionResult {
    private final boolean success;
    private final String transactionHash;
    private final String message;

    public TransactionResult(boolean success, String transactionHash, String message) {
        this.success = success;
        this.transactionHash = transactionHash;
        this.message = message;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getTransactionHash() {
        return transactionHash;
    }

    public String getMessage() {
        return message;
    }
}
