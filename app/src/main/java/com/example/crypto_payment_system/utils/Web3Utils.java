package com.example.crypto_payment_system.utils;

import com.example.crypto_payment_system.config.Constants;

import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.TransactionReceipt;

import java.util.Optional;
import java.util.concurrent.ExecutionException;

/**
 * Utility class for Web3j operations
 */
public class Web3Utils {

    /**
     * Wait for transaction receipt with timeout and retry mechanism
     */
    public static TransactionReceipt waitForTransactionReceipt(Web3j web3j, String transactionHash)
            throws InterruptedException, ExecutionException, Exception {

        int attempts = 0;
        int maxAttempts = Constants.MAX_TRANSACTION_ATTEMPTS;
        TransactionReceipt receipt = null;

        while (attempts < maxAttempts) {
            Optional<TransactionReceipt> receiptOptional =
                    web3j.ethGetTransactionReceipt(transactionHash).sendAsync().get().getTransactionReceipt();

            if (receiptOptional.isPresent()) {
                receipt = receiptOptional.get();
                break;
            }

            Thread.sleep(Constants.TRANSACTION_POLL_INTERVAL);
            attempts++;
        }

        if (receipt == null) {
            throw new Exception("Transaction not mined after " + maxAttempts + " attempts");
        }

        return receipt;
    }

    /**
     * Check if Web3j is properly connected
     */
    public static boolean isWeb3jConnected(Web3j web3j) {
        if (web3j == null) {
            return false;
        }

        try {
            web3j.web3ClientVersion().send();
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}