package com.example.crypto_payment_system.repositories.api;

import com.example.crypto_payment_system.domain.auth.LoginResponse;

import java.util.concurrent.CompletableFuture;

public interface AuthRepository {
    CompletableFuture<LoginResponse> login(String username, String password);
}
