package com.example.crypto_payment_system.domain.transaction;

import java.util.Objects;

public class Transaction {
    public Transaction(String amount, long timestamp, String tokenAddress, String transactionHash, String transactionType, String walletAddress, String exchangeRate, int sentCurrency, int receivedCurrency, String walletAddressTo) {
        this.amount = amount;
        this.timestamp = timestamp;
        this.tokenAddress = tokenAddress;
        this.transactionHash = transactionHash;
        this.transactionType = transactionType;
        this.walletAddress = walletAddress;
        this.exchangeRate = exchangeRate;
        this.sentCurrency = sentCurrency;
        this.receivedCurrency = receivedCurrency;
        this.walletAddressTo = walletAddressTo;
    }

    private String amount;
    private long timestamp;
    private String tokenAddress;
    private String transactionHash;
    private String transactionType;
    private String walletAddress;
    private String exchangeRate;
    private int sentCurrency;
    private int receivedCurrency;
    private String walletAddressTo;

    public String getAmount() {
        return amount;
    }

    public void setAmount(String amount) {
        this.amount = amount;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getTokenAddress() {
        return tokenAddress;
    }

    public void setTokenAddress(String tokenAddress) {
        this.tokenAddress = tokenAddress;
    }

    public String getTransactionHash() {
        return transactionHash;
    }

    public void setTransactionHash(String transactionHash) {
        this.transactionHash = transactionHash;
    }

    public String getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(String transactionType) {
        this.transactionType = transactionType;
    }

    public String getWalletAddress() {
        return walletAddress;
    }

    public void setWalletAddress(String walletAddress) {
        this.walletAddress = walletAddress;
    }

    public String getExchangeRate() {
        return exchangeRate;
    }

    public void setExchangeRate(String exchangeRate) {
        this.exchangeRate = exchangeRate;
    }

    public int getSentCurrency() {
        return sentCurrency;
    }

    public void setSentCurrency(int sentCurrency) {
        this.sentCurrency = sentCurrency;
    }

    public int getReceivedCurrency() {
        return receivedCurrency;
    }

    public void setReceivedCurrency(int receivedCurrency) {
        this.receivedCurrency = receivedCurrency;
    }

    public String getWalletAddressTo() {
        return walletAddressTo;
    }

    public void setWalletAddressTo(String walletAddressTo) {
        this.walletAddressTo = walletAddressTo;
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

    /**
     * Checks if this transaction involves the specified currency
     * @param currencyCode The currency code to check (CURRENCY_EUR or CURRENCY_USD)
     * @return true if the transaction involves the specified currency
     */
    public boolean involvesCurrency(int currencyCode) {
        return sentCurrency == currencyCode || receivedCurrency == currencyCode;
    }

    /**
     * Get the display currency for this transaction
     * This determines which currency to show in the transaction list
     * @return The display currency code (CURRENCY_EUR or CURRENCY_USD)
     */
    public int getDisplayCurrency() {
        if (sentCurrency == receivedCurrency) {
            return sentCurrency;
        }

        return receivedCurrency;
    }
}
