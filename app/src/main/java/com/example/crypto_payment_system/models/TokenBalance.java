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
    private static final BigDecimal TOKEN_DECIMAL_FACTOR = new BigDecimal(1_000_000); // 10^6 for tokens
    private static final BigDecimal ETH_DECIMAL_FACTOR = new BigDecimal("1000000000000000000"); // 10^18 for ETH

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
        if ("ETH".equals(tokenSymbol)) {
            return formatEthBalance(walletBalance);
        } else {
            return formatTokenBalance(walletBalance);
        }
    }

    /**
     * Get contract balance as a human-readable string with 6 decimal places
     */
    public String getFormattedContractBalance() {
        // Use ETH formatting for ETH, token formatting for other tokens
        if ("ETH".equals(tokenSymbol)) {
            return formatEthBalance(contractBalance);
        } else {
            return formatTokenBalance(contractBalance);
        }
    }

    /**
     * Convert a raw token amount (with 6 decimals) to a human-readable format
     */
    private String formatTokenBalance(BigInteger rawBalance) {
        BigDecimal balanceWithDecimals = new BigDecimal(rawBalance).divide(TOKEN_DECIMAL_FACTOR, 6, RoundingMode.HALF_DOWN);
        return balanceWithDecimals.stripTrailingZeros().toPlainString();
    }

    /**
     * Convert a raw ETH amount (with 18 decimals) to a human-readable format
     */
    private String formatEthBalance(BigInteger rawBalance) {
        BigDecimal balanceWithDecimals = new BigDecimal(rawBalance).divide(ETH_DECIMAL_FACTOR, 6, RoundingMode.HALF_DOWN);
        return balanceWithDecimals.stripTrailingZeros().toPlainString();
    }

    @NonNull
    @Override
    public String toString() {
        return tokenSymbol + ": " + getFormattedWalletBalance() + " (wallet) / " +
                getFormattedContractBalance() + " (contract)";
    }
}