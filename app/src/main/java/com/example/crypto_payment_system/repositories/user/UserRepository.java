package com.example.crypto_payment_system.repositories.user;

import com.example.crypto_payment_system.domain.account.User;

import java.util.concurrent.CompletableFuture;

public interface UserRepository {
    public CompletableFuture<Boolean> handleUserConnection(String walletAddress);
    public CompletableFuture<Void> createNewUser(String walletAddress, String preferredCurrency);
    public CompletableFuture<User> getUserData(String walletAddress);
    public CompletableFuture<Void> updatePreferredCurrency(String walletAddress, String currencies);
    public CompletableFuture<Integer> getPreferredCurrency(String walletAddress, String sendCurrency);
}
