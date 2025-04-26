package com.example.crypto_payment_system.api;

import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.concurrent.CompletableFuture;

public class AuthService {
    private static final String TAG = "AuthService";
    private final FirebaseAuth mAuth;

    public AuthService() {
        mAuth = FirebaseAuth.getInstance();
    }

    /**
     * Get the current authenticated user
     */
    public FirebaseUser getCurrentUser() {
        return mAuth.getCurrentUser();
    }

    /**
     * Check if user is signed in
     */
    public boolean isUserSignedIn() {
        return mAuth.getCurrentUser() != null;
    }

    /**
     * Sign in anonymously
     */
    public CompletableFuture<FirebaseUser> signInAnonymously() {
        CompletableFuture<FirebaseUser> future = new CompletableFuture<>();

        mAuth.signInAnonymously()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        Log.d(TAG, "signInAnonymously:success");
                        future.complete(user);
                    } else {
                        Log.w(TAG, "signInAnonymously:failure", task.getException());
                        future.completeExceptionally(task.getException());
                    }
                });

        return future;
    }

    /**
     * Sign out the current user
     */
    public void signOut() {
        mAuth.signOut();
    }
}