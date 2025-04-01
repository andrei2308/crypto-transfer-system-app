package com.example.crypto_payment_system.models;

/**
 * Model class to represent a user and their preferences
 */
public class User {
    private final String walletAddress;
    private final String preferredCurrency;
    private final long createdAt;
    private final long lastLogin;

    public User(String walletAddress, String preferredCurrency, long createdAt, long lastLogin) {
        this.walletAddress = walletAddress;
        this.preferredCurrency = preferredCurrency;
        this.createdAt = createdAt;
        this.lastLogin = lastLogin;
    }

    public String getWalletAddress() {
        return walletAddress;
    }

    /**
     * Get preferred currency. May contain multiple currencies as comma-separated values.
     * The first currency is considered the primary currency.
     * @return String of comma-separated currency codes (e.g. "EUR,USD")
     */
    public String getPreferredCurrency() {
        return preferredCurrency;
    }

    /**
     * Gets the primary currency (first in the list of preferred currencies)
     * @return Primary currency code
     */
    public String getPrimaryCurrency() {
        if (preferredCurrency == null || preferredCurrency.isEmpty()) {
            return "EUR"; // Default to EUR if no preference set
        }

        String[] currencies = preferredCurrency.split(",");
        return currencies[0];
    }

    /**
     * Check if the user has a specific currency as a preference
     * @param currencyCode Currency code to check (e.g. "EUR")
     * @return true if the currency is in the user's preferences
     */
    public boolean hasCurrencyPreference(String currencyCode) {
        if (preferredCurrency == null || preferredCurrency.isEmpty()) {
            return false;
        }

        String[] currencies = preferredCurrency.split(",");
        for (String currency : currencies) {
            if (currency.equals(currencyCode)) {
                return true;
            }
        }

        return false;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public long getLastLogin() {
        return lastLogin;
    }
}