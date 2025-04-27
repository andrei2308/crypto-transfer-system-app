package com.example.crypto_payment_system.domain.transaction;

import java.util.Objects;

public class Transaction {
    private String amount;
    private long timestamp;
    private String tokenAddress;
    private String transactionHash;
    private String transactionType;
    private String walletAddress;

    public String getWalletAddressTo() {
        return walletAddressTo;
    }

    public void setWalletAddressTo(String walletAddressTo) {
        this.walletAddressTo = walletAddressTo;
    }

    private String walletAddressTo;

    public Transaction(String amount, long timestamp, String tokenAddress,
                       String transactionHash, String transactionType,
                       String walletAddress, String walletAddressTo) {
        this.amount = amount;
        this.timestamp = timestamp;
        this.tokenAddress = tokenAddress;
        this.transactionHash = transactionHash;
        this.transactionType = transactionType;
        this.walletAddress = walletAddress;
        this.walletAddressTo = walletAddressTo;
    }

    public String getAmount() {
        return amount;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getTokenAddress() {
        return tokenAddress;
    }

    public String getTransactionHash() {
        return transactionHash;
    }

    public String getTransactionType() {
        return transactionType;
    }

    public String getWalletAddress() {
        return walletAddress;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Transaction that = (Transaction) o;
        return timestamp == that.timestamp &&
                Objects.equals(amount, that.amount) &&
                Objects.equals(tokenAddress, that.tokenAddress) &&
                Objects.equals(transactionHash, that.transactionHash) &&
                Objects.equals(transactionType, that.transactionType) &&
                Objects.equals(walletAddress, that.walletAddress);
    }

    @Override
    public int hashCode() {
        return Objects.hash(amount, timestamp, tokenAddress, transactionHash, transactionType, walletAddress);
    }
}
