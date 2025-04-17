package com.example.crypto_payment_system.repositories;

import com.example.crypto_payment_system.api.Web3Service;
import com.example.crypto_payment_system.config.Constants;
import com.example.crypto_payment_system.contracts.ExchangeContract;
import com.example.crypto_payment_system.repositories.TokenRepository.TransactionResult;

import org.web3j.crypto.Credentials;
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

    public CompletableFuture<TransactionResult> addLiquidity(String currency, Credentials credentials, String tokenUnitAmount) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String tokenAddress = currency.equals("USD") ?
                        tokenRepository.getUsdtAddress() : tokenRepository.getEurcAddress();

                BigInteger amountToAdd = new BigInteger(tokenUnitAmount);

                String txHash = exchangeContract.addLiquidity(tokenAddress, amountToAdd, credentials);
                TransactionReceipt receipt = web3Service.waitForTransactionReceipt(txHash);

                boolean success = receipt.isStatusOK();
                return new TransactionResult(success, txHash, success ?
                        "Liquidity added successfully" : "Adding liquidity failed");

            } catch (Exception e) {
                return new TransactionResult(false, null, "Error: " + e.getMessage());
            }
        });
    }

    /**
     * Exchange EUR to USD
     */
    public CompletableFuture<TransactionResult> exchangeEurToUsd(String tokenAmount, Credentials credentials) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                BigInteger amount = new BigInteger(tokenAmount);

                String txHash = exchangeContract.exchangeEurToUsd(
                        tokenRepository.getEurcAddress(), amount, credentials);

                TransactionReceipt receipt = web3Service.waitForTransactionReceipt(txHash);

                boolean success = receipt.isStatusOK();
                return new TransactionResult(success, txHash, success ?
                        "Exchange completed successfully" : "Exchange failed");

            } catch (Exception e) {
                return new TransactionResult(false, null, "Error: " + e.getMessage());
            }
        });
    }

    /**
     * Exchange USD to EUR
     */
    public CompletableFuture<TransactionResult> exchangeUsdToEur(String tokenAmount, Credentials credentials) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                BigInteger amount = new BigInteger(tokenAmount);
                String txHash = exchangeContract.exchangeUsdToEur(tokenRepository.getUsdtAddress(), amount, credentials);
                TransactionReceipt receipt = web3Service.waitForTransactionReceipt(txHash);

                boolean success = receipt.isStatusOK();
                return new TransactionResult(success, txHash, success ?
                        "Exchange completed successfully" : "Exchange failed");
            } catch (Exception e) {
                return new TransactionResult(false, null, "Error: " + e.getMessage());
            }
        });
    }

    public CompletableFuture<TransactionResult> sendTransaction(String address, int sendCurrency, int receiveCurrency, Credentials credentials, String amount) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                BigInteger amountToSend = new BigInteger(amount);

                String txHash = exchangeContract.sendMoney(amountToSend, address, sendCurrency, receiveCurrency, credentials);

                TransactionReceipt receipt = web3Service.waitForTransactionReceipt(txHash);

                boolean success = receipt.isStatusOK();
                return new TransactionResult(success, txHash, success ?
                        "Money sent successfully" : "Exchange failed");
            } catch (Exception e) {
                return new TransactionResult(false, null, "Error: " + e.getMessage());
            }
        });
    }
}