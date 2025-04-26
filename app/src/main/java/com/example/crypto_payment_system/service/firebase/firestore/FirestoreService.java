package com.example.crypto_payment_system.service.firebase.firestore;

import com.google.firebase.firestore.DocumentSnapshot;

import java.util.concurrent.CompletableFuture;

public interface FirestoreService {
    public CompletableFuture<Boolean> checkUserExists(String walletAddress);
    public CompletableFuture<DocumentSnapshot> getUserData(String walletAddress);
    public CompletableFuture<Void> createUser(String walletAddress, String preferredCurrency);
    public CompletableFuture<Void> updateUserLogin(String walletAddress);
    public CompletableFuture<Void> updatePreferredCurrency(String walletAddress, String currency);
    public CompletableFuture<String> saveTransaction(String walletAddress, String transactionType,
                                                     String tokenAddress, String amount,
                                                     String transactionHash);
}
