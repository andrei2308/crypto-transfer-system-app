package com.example.crypto_payment_system.utils.web3;

import com.example.crypto_payment_system.config.Constants;
import com.example.crypto_payment_system.utils.events.EventParser;

import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.TransactionReceipt;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Utility class for Web3j operations
 */
public class Web3Utils {

    /**
     * Wait for transaction receipt with timeout and retry mechanism
     */
    public static TransactionReceipt waitForTransactionReceipt(Web3j web3j, String transactionHash)
            throws Exception {
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

        System.out.println("Transaction receipt: " + receipt);
        return receipt;
    }

    /**
     * Process the transaction receipt to extract exchange info
     */
    public static EventParser.ExchangeInfo processTransactionReceipt(TransactionReceipt receipt) {
        return EventParser.extractExchangeInfo(receipt);
    }

    /**
     * Wait for transaction and extract exchange info in one operation
     */
    public static EventParser.ExchangeInfo waitForTransactionAndProcess(Web3j web3j, String transactionHash)
            throws Exception {
        TransactionReceipt receipt = waitForTransactionReceipt(web3j, transactionHash);
        return processTransactionReceipt(receipt);
    }

    /**
     * Asynchronous version of waitForTransactionAndProcess
     */
    public static CompletableFuture<EventParser.ExchangeInfo> waitForTransactionAndProcessAsync(
            Web3j web3j, String transactionHash) {

        return CompletableFuture.supplyAsync(() -> {
            try {
                return waitForTransactionAndProcess(web3j, transactionHash);
            } catch (Exception e) {
                throw new RuntimeException("Failed to process transaction: " + e.getMessage(), e);
            }
        });
    }
}