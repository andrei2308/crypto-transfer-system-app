package com.example.crypto_payment_system.repositories;

import com.example.crypto_payment_system.api.FirestoreService;
import com.example.crypto_payment_system.models.User;

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
     * @param walletAddress The wallet address
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
                                document.getString("preferredCurrency"),
                                document.getLong("createdAt"),
                                document.getLong("lastLogin")
                        );
                    } else {
                        return null;
                    }
                });
    }

    /**
     * Update user's preferred currency
     * @param walletAddress The wallet address
     * @param currencies Comma-separated list of currency codes (e.g., "EUR,USD")
     */
    public CompletableFuture<Void> updatePreferredCurrency(String walletAddress, String currencies) {
        return firestoreService.updatePreferredCurrency(walletAddress, currencies);
    }
}