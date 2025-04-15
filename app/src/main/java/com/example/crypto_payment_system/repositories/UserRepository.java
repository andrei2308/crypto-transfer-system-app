package com.example.crypto_payment_system.repositories;

import static android.content.ContentValues.TAG;

import static com.example.crypto_payment_system.config.Constants.CURRENCY_EUR;
import static com.example.crypto_payment_system.config.Constants.CURRENCY_USD;

import android.util.Log;

import com.example.crypto_payment_system.api.FirestoreService;
import com.example.crypto_payment_system.models.User;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Repository class for user management
 */
public class UserRepository {

    private final FirestoreService firestoreService;

    public UserRepository(FirestoreService firestoreService) {
        this.firestoreService = firestoreService;
    }

    /**
     * Handle user connection and return whether this is a first-time user
     */
    public CompletableFuture<Boolean> handleUserConnection(String walletAddress) {
        return firestoreService.checkUserExists(walletAddress)
                .thenCompose(exists -> {
                    if (exists) {
                        return firestoreService.updateUserLogin(walletAddress)
                                .thenApply(aVoid -> false);
                    } else {
                        return CompletableFuture.completedFuture(true);
                    }
                });
    }

    /**
     * Create a new user with preferred currency
     *
     * @param walletAddress     The wallet address
     * @param preferredCurrency Comma-separated list of currency codes (e.g., "EUR,USD")
     */
    public CompletableFuture<Void> createNewUser(String walletAddress, String preferredCurrency) {
        return firestoreService.createUser(walletAddress, preferredCurrency);
    }

    /**
     * Get user data
     */
    public CompletableFuture<User> getUserData(String walletAddress) {
        return firestoreService.getUserData(walletAddress)
                .thenApply(document -> {
                    if (document.exists()) {
                        return new User(
                                document.getString("walletAddress"),
                                document.getString("preferredCurrency")
                        );
                    } else {
                        return null;
                    }
                });
    }

    /**
     * Update user's preferred currency
     *
     * @param walletAddress The wallet address
     * @param currencies    Comma-separated list of currency codes (e.g., "EUR,USD")
     */
    public CompletableFuture<Void> updatePreferredCurrency(String walletAddress, String currencies) {
        return firestoreService.updatePreferredCurrency(walletAddress, currencies);
    }

    /**
     * Get the preferred currency of a user as an integer value compatible with the smart contract
     *
     * @param walletAddress The wallet address of the user
     * @return CompletableFuture with the preferred currency as an integer (1=EUR, 2=USD)
     * If user has multiple preferred currencies, returns the first one.
     * Defaults to EUR (1) if no preference is set or user not found.
     */
    public CompletableFuture<Integer> getPreferredCurrency(String walletAddress, String sendCurrency) {
        return getUserData(walletAddress.toLowerCase())
                .thenApply(user -> {
                    if (user == null || user.getPreferredCurrency() == null || user.getPreferredCurrency().isEmpty()) {
                        Log.w(TAG, "User not found or no preferred currency set for: " + walletAddress);
                        return CURRENCY_EUR;
                    }

                    String preferredCurrencies = user.getPreferredCurrency();
                    List<String> currencyList = Arrays.asList(preferredCurrencies.split(","));

                    if (currencyList.isEmpty()) {
                        return CURRENCY_EUR;
                    }
                    if (currencyList.contains(sendCurrency)) {
                        if (sendCurrency.equals("EUR")) {
                            return CURRENCY_EUR;
                        } else if (sendCurrency.equals("USD")) {
                            return CURRENCY_USD;
                        } else {
                            return CURRENCY_EUR;
                        }
                    } else {
                        String firstPreferredCurrency = currencyList.get(0);
                        if (firstPreferredCurrency.equals("EUR")) {
                            return CURRENCY_EUR;
                        } else if (firstPreferredCurrency.equals("USD")) {
                            return CURRENCY_USD;
                        } else {
                            return CURRENCY_EUR;
                        }
                    }
                })
                .exceptionally(e -> {
                    Log.e(TAG, "Error getting preferred currency", e);
                    return CURRENCY_EUR;
                });
    }
}