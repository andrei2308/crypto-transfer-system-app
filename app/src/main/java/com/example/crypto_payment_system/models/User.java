package com.example.crypto_payment_system.models;

/**
 * Model class to represent a user and their preferences
 */
public class User {
    private final String walletAddress;
    private final String preferredCurrency;

    public User(String walletAddress, String preferredCurrency) {
        this.walletAddress = walletAddress;
        this.preferredCurrency = preferredCurrency;
    }

    public String getWalletAddress() {
        return walletAddress;
    }

    /**
     * Get preferred currency. May contain multiple currencies as comma-separated values.
     * The first currency is considered the primary currency.
     *
     * @return String of comma-separated currency codes (e.g. "EUR,USD")
     */
    public String getPreferredCurrency() {
        return preferredCurrency;
    }

}