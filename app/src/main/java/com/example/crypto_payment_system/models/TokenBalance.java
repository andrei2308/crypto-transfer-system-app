package com.example.crypto_payment_system.models;

import androidx.annotation.NonNull;

import java.math.BigInteger;

/**
 * Model class to represent token balances
 */
public class TokenBalance {
    private final String tokenSymbol;
    private final String tokenAddress;
    private final BigInteger walletBalance;
    private final BigInteger contractBalance;

    public TokenBalance(String tokenSymbol, String tokenAddress, BigInteger walletBalance, BigInteger contractBalance) {
        this.tokenSymbol = tokenSymbol;
        this.tokenAddress = tokenAddress;
        this.walletBalance = walletBalance;
        this.contractBalance = contractBalance;
    }

    public String getTokenSymbol() {
        return tokenSymbol;
    }

    public String getTokenAddress() {
        return tokenAddress;
    }

    public BigInteger getWalletBalance() {
        return walletBalance;
    }

    public BigInteger getContractBalance() {
        return contractBalance;
    }

    @NonNull
    @Override
    public String toString() {
        return tokenSymbol + ": " + walletBalance + " (wallet) / " + contractBalance + " (contract)";
    }
}