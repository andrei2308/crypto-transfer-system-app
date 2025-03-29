package com.example.crypto_payment_system.api;

import android.util.Log;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.FirebaseFirestoreSettings;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Service class for Firestore database operations
 */
public class FirestoreService {
    private static final String TAG = "FirestoreService";
    private final FirebaseFirestore db;

    public FirestoreService() {
        // Enable offline persistence
        FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)
                .build();

        db = FirebaseFirestore.getInstance();
        db.setFirestoreSettings(settings);
    }

    /**
     * Check if a user exists in the database
     */
    public CompletableFuture<Boolean> checkUserExists(String walletAddress) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();

        db.collection("users")
                .document(walletAddress)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    boolean exists = documentSnapshot.exists();
                    Log.d(TAG, "User exists check for " + walletAddress + ": " + exists);
                    future.complete(exists);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error checking if user exists", e);

                    // When offline, assume user doesn't exist yet
                    if (e instanceof FirebaseFirestoreException &&
                            ((FirebaseFirestoreException) e).getCode() == FirebaseFirestoreException.Code.UNAVAILABLE) {
                        Log.w(TAG, "Device is offline, assuming new user");
                        future.complete(false);
                    } else {
                        future.completeExceptionally(e);
                    }
                });

        return future;
    }

    /**
     * Get user data from Firestore
     */
    public CompletableFuture<DocumentSnapshot> getUserData(String walletAddress) {
        CompletableFuture<DocumentSnapshot> future = new CompletableFuture<>();

        db.collection("users")
                .document(walletAddress)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    Log.d(TAG, "Retrieved user data for " + walletAddress);
                    future.complete(documentSnapshot);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting user data", e);
                    future.completeExceptionally(e);
                });

        return future;
    }

    /**
     * Create a new user in Firestore
     */
    public CompletableFuture<Void> createUser(String walletAddress, String preferredCurrency) {
        CompletableFuture<Void> future = new CompletableFuture<>();

        long currentTime = System.currentTimeMillis();

        Map<String, Object> userData = new HashMap<>();
        userData.put("walletAddress", walletAddress);
        userData.put("preferredCurrency", preferredCurrency);
        userData.put("createdAt", currentTime);
        userData.put("lastLogin", currentTime);

        db.collection("users")
                .document(walletAddress)
                .set(userData)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "User created successfully: " + walletAddress);
                    future.complete(null);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error creating user", e);
                    future.completeExceptionally(e);
                });

        return future;
    }

    /**
     * Update user's last login time
     */
    public CompletableFuture<Void> updateUserLogin(String walletAddress) {
        CompletableFuture<Void> future = new CompletableFuture<>();

        Map<String, Object> updates = new HashMap<>();
        updates.put("lastLogin", System.currentTimeMillis());

        db.collection("users")
                .document(walletAddress)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Updated last login for: " + walletAddress);
                    future.complete(null);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error updating last login", e);
                    future.completeExceptionally(e);
                });

        return future;
    }

    /**
     * Update user's preferred currency
     */
    public CompletableFuture<Void> updatePreferredCurrency(String walletAddress, String currency) {
        CompletableFuture<Void> future = new CompletableFuture<>();

        Map<String, Object> updates = new HashMap<>();
        updates.put("preferredCurrency", currency);

        db.collection("users")
                .document(walletAddress)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Updated preferred currency for: " + walletAddress);
                    future.complete(null);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error updating preferred currency", e);
                    future.completeExceptionally(e);
                });

        return future;
    }

//    /**
//     * Save transaction data to Firestore
//     */
//    public CompletableFuture<String> saveTransaction(String walletAddress, String transactionType,
//                                                     String tokenAddress, String amount,
//                                                     String transactionHash) {
//        CompletableFuture<String> future = new CompletableFuture<>();
//
//        Map<String, Object> transaction = new HashMap<>();
//        transaction.put("walletAddress", walletAddress);
//        transaction.put("transactionType", transactionType);
//        transaction.put("tokenAddress", tokenAddress);
//        transaction.put("amount", amount);
//        transaction.put("transactionHash", transactionHash);
//        transaction.put("timestamp", System.currentTimeMillis());
//
//        db.collection("transactions")
//                .whereEqualTo("transactionHash", transactionHash)
//                .get()
//                .addOnSuccessListener(querySnapshot -> {
//                    if (querySnapshot.isEmpty()) {
//                        db.collection("transactions")
//                                .add(transaction)
//                                .addOnSuccessListener(documentReference -> {
//                                    Log.d(TAG, "Transaction saved with ID: " + documentReference.getId());
//                                    future.complete(documentReference.getId());
//                                })
//                                .addOnFailureListener(e -> {
//                                    Log.e(TAG, "Error saving transaction", e);
//                                    future.completeExceptionally(e);
//                                });
//                    } else {
//                        String docId = querySnapshot.getDocuments().get(0).getId();
//                        Log.d(TAG, "Transaction already exists with ID: " + docId);
//                        future.complete(docId);
//                    }
//                })
//                .addOnFailureListener(e -> {
//                    Log.e(TAG, "Error checking for existing transaction", e);
//                    future.completeExceptionally(e);
//                });
//
//        return future;
//    }
}