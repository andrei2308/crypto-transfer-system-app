package com.example.crypto_payment_system.models;

import androidx.annotation.NonNull;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;

/**
 * Model class to represent token balances
 */
public class TokenBalance {
    private final String tokenSymbol;
    private final String tokenAddress;
    private final BigInteger walletBalance;
    private final BigInteger contractBalance;
    private static final BigDecimal DECIMAL_FACTOR = new BigDecimal(1_000_000);

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

    /**
     * Get wallet balance as a human-readable string with 6 decimal places
     */
    public String getFormattedWalletBalance() {
        return formatBalance(walletBalance);
    }

    /**
     * Get contract balance as a human-readable string with 6 decimal places
     */
    public String getFormattedContractBalance() {
        return formatBalance(contractBalance);
    }

    /**
     * Convert a raw token amount (with 6 decimals) to a human-readable format
     */
    private String formatBalance(BigInteger rawBalance) {
        BigDecimal balanceWithDecimals = new BigDecimal(rawBalance).divide(DECIMAL_FACTOR, 6, RoundingMode.HALF_DOWN);
        String formatted = balanceWithDecimals.stripTrailingZeros().toPlainString();
        return formatted;
    }

    @NonNull
    @Override
    public String toString() {
        return tokenSymbol + ": " + getFormattedWalletBalance() + " (wallet) / " +
                getFormattedContractBalance() + " (contract)";
    }
}