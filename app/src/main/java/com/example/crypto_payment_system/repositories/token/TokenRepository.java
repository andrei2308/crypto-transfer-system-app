package com.example.crypto_payment_system.repositories.token;

import com.example.crypto_payment_system.domain.token.TokenBalance;
import com.example.crypto_payment_system.utils.web3.TransactionResult;

import org.web3j.crypto.Credentials;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

public interface TokenRepository {
    public CompletableFuture<Map<String, String>> initializeTokenAddresses();
    public CompletableFuture<Map<String, TokenBalance>> getAllBalances(Credentials credentials);

}
