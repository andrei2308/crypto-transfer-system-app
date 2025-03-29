package com.example.crypto_payment_system.repositories;

import com.example.crypto_payment_system.api.TokenContractService;
import com.example.crypto_payment_system.api.Web3Service;
import com.example.crypto_payment_system.config.Constants;
import com.example.crypto_payment_system.models.TokenBalance;

import org.web3j.crypto.Credentials;
import org.web3j.protocol.core.methods.response.TransactionReceipt;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Repository class handling token-related operations
 */
public class TokenRepository {
    private final Web3Service web3Service;
    private final TokenContractService tokenService;
    private String eurcAddress = "";
    private String usdtAddress = "";

    public TokenRepository(Web3Service web3Service, TokenContractService tokenService) {
        this.web3Service = web3Service;
        this.tokenService = tokenService;
    }

    /**
     * Initialize token addresses
     */
    public CompletableFuture<Map<String, String>> initializeTokenAddresses() {
        return CompletableFuture.supplyAsync(() -> {
            Map<String, String> addresses = new HashMap<>();
            try {
                eurcAddress = tokenService.getTokenAddress("getEurcAddress");
                addresses.put("EURC", eurcAddress);

                try {
                    usdtAddress = tokenService.getTokenAddress("getUsdtAddress");
                    addresses.put("USDT", usdtAddress);
                } catch (Exception e) {
                    usdtAddress = "Not available";
                    addresses.put("USDT", "Not available");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return addresses;
        });
    }

    /**
     * Get all token balances
     */
    public CompletableFuture<Map<String, TokenBalance>> getAllBalances() {
        return CompletableFuture.supplyAsync(() -> {
            Map<String, TokenBalance> balances = new HashMap<>();

            try {
                Credentials credentials = Credentials.create(Constants.PRIVATE_KEY);
                String walletAddress = credentials.getAddress();

                // Get EUR token balances
                BigInteger walletEurcBalance = tokenService.getTokenBalance(walletAddress, eurcAddress);
                BigInteger contractEurcBalance = tokenService.getContractTokenBalance("getContractEurcBalance", eurcAddress);
                balances.put("EURC", new TokenBalance("EURC", eurcAddress, walletEurcBalance, contractEurcBalance));

                // Get USD token balances
                BigInteger walletUsdtBalance = tokenService.getTokenBalance(walletAddress, usdtAddress);
                BigInteger contractUsdtBalance = tokenService.getContractTokenBalance("getContractUsdtBalance", usdtAddress);
                balances.put("USDT", new TokenBalance("USDT", usdtAddress, walletUsdtBalance, contractUsdtBalance));

            } catch (Exception e) {
                e.printStackTrace();
            }

            return balances;
        });
    }

    /**
     * Mint tokens
     */
    public CompletableFuture<TransactionResult> mintTokens(String currency) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                final String tokenAddress = currency.equals("USD") ? usdtAddress : eurcAddress;
                BigInteger mintAmount = new BigInteger(Constants.DEFAULT_MINT_AMOUNT);

                String txHash = tokenService.mintTokens(tokenAddress, mintAmount);
                TransactionReceipt receipt = web3Service.waitForTransactionReceipt(txHash);

                boolean success = receipt.isStatusOK();
                return new TransactionResult(success, txHash, success ?
                        "Tokens minted successfully" : "Token minting failed");

            } catch (Exception e) {
                e.printStackTrace();
                return new TransactionResult(false, null, "Error: " + e.getMessage());
            }
        });
    }

    public String getEurcAddress() {
        return eurcAddress;
    }

    public String getUsdtAddress() {
        return usdtAddress;
    }

    /**
     * Inner class for transaction results
     */
    public static class TransactionResult {
        private final boolean success;
        private final String transactionHash;
        private final String message;

        public TransactionResult(boolean success, String transactionHash, String message) {
            this.success = success;
            this.transactionHash = transactionHash;
            this.message = message;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getTransactionHash() {
            return transactionHash;
        }

        public String getMessage() {
            return message;
        }
    }
}