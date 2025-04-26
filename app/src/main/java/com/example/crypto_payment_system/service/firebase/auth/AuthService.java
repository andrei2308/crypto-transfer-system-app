package com.example.crypto_payment_system.service.firebase.auth;

import com.google.firebase.auth.FirebaseUser;

import java.util.concurrent.CompletableFuture;

public interface AuthService {
    public FirebaseUser getCurrentUser();
    public boolean isUserSignedIn();
    public CompletableFuture<FirebaseUser> signInAnonymously();
    public void signOut();
}
