package com.example.crypto_payment_system.repositories;

import com.example.crypto_payment_system.api.Web3Service;
import com.example.crypto_payment_system.config.Constants;
import com.example.crypto_payment_system.contracts.ExchangeContract;
import com.example.crypto_payment_system.repositories.TokenRepository.TransactionResult;

import org.web3j.protocol.core.methods.response.TransactionReceipt;

import java.math.BigInteger;
import java.util.concurrent.CompletableFuture;

/**
 * Repository class handling exchange operations
 */
public class ExchangeRepository {
    private final Web3Service web3Service;
    private final ExchangeContract exchangeContract;
    private final TokenRepository tokenRepository;

    public ExchangeRepository(Web3Service web3Service, ExchangeContract exchangeContract, TokenRepository tokenRepository) {
        this.web3Service = web3Service;
        this.exchangeContract = exchangeContract;
        this.tokenRepository = tokenRepository;
    }

    /**
     * Add liquidity to the exchange
     */
    public CompletableFuture<TransactionResult> addLiquidity(String currency) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String tokenAddress = currency.equals("USD") ?
                        tokenRepository.getUsdtAddress() : tokenRepository.getEurcAddress();

                BigInteger amount = new BigInteger(Constants.DEFAULT_EXCHANGE_AMOUNT);

                String txHash = exchangeContract.addLiquidity(tokenAddress, amount);
                TransactionReceipt receipt = web3Service.waitForTransactionReceipt(txHash);

                boolean success = receipt.isStatusOK();
                return new TransactionResult(success, txHash, success ?
                        "Liquidity added successfully" : "Adding liquidity failed");

            } catch (Exception e) {
                e.printStackTrace();
                return new TransactionResult(false, null, "Error: " + e.getMessage());
            }
        });
    }

    /**
     * Exchange EUR to USD
     */
    public CompletableFuture<TransactionResult> exchangeEurToUsd() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                BigInteger amount = new BigInteger(Constants.DEFAULT_EXCHANGE_AMOUNT);

                String txHash = exchangeContract.exchangeEurToUsd(
                        tokenRepository.getEurcAddress(), amount);

                TransactionReceipt receipt = web3Service.waitForTransactionReceipt(txHash);

                boolean success = receipt.isStatusOK();
                return new TransactionResult(success, txHash, success ?
                        "Exchange completed successfully" : "Exchange failed");

            } catch (Exception e) {
                e.printStackTrace();
                return new TransactionResult(false, null, "Error: " + e.getMessage());
            }
        });
    }
}