package com.example.crypto_payment_system.repositories.exchange;

import com.example.crypto_payment_system.utils.token.TokenCostInfo;
import com.example.crypto_payment_system.utils.web3.TransactionResult;

import org.web3j.crypto.Credentials;

import java.util.concurrent.CompletableFuture;

public interface ExchangeRepository {
    public CompletableFuture<TransactionResult> addLiquidity(String currency, Credentials credentials, String tokenUnitAmount);
    public CompletableFuture<TransactionResult> exchangeEurToUsd(String tokenAmount, Credentials credentials);
    public CompletableFuture<TransactionResult> exchangeUsdToEur(String tokenAmount, Credentials credentials);
    public CompletableFuture<TransactionResult> sendTransaction(String address, int sendCurrency, int receiveCurrency, Credentials credentials, String amount);
    public CompletableFuture<TransactionResult> mintTokens(String currency, Credentials credentials, String amount);
    CompletableFuture<TokenCostInfo> getRequiredTokenCost(String currency, Credentials credentials, String amount);

}
