package com.example.crypto_payment_system.service.firebase.firestore;

import com.google.firebase.firestore.DocumentSnapshot;

import java.util.concurrent.CompletableFuture;

/**
 * Service interface for Firestore database operations
 */
public interface FirestoreService {

    /**
     * Check if a user exists in the database
     */
    CompletableFuture<Boolean> checkUserExists(String walletAddress);

    /**
     * Get user data from Firestore
     */
    CompletableFuture<DocumentSnapshot> getUserData(String walletAddress);

    /**
     * Create a new user in Firestore
     */
    CompletableFuture<Void> createUser(String walletAddress, String preferredCurrency);

    /**
     * Update user's last login time
     */
    CompletableFuture<Void> updateUserLogin(String walletAddress);

    /**
     * Update user's preferred currency
     */
    CompletableFuture<Void> updatePreferredCurrency(String walletAddress, String currency);

    /**
     * Save transaction data to Firestore
     */
    CompletableFuture<String> saveTransaction(String walletAddressFrom, String transactionType,
                                              String tokenAddress, String amount,
                                              String transactionHash, String walletAddressTo,
                                              String exchangeRate, int sentCurrency, int receivedCurrency);
}