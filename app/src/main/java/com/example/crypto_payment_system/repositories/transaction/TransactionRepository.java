package com.example.crypto_payment_system.repositories.transaction;

import com.example.crypto_payment_system.domain.transaction.Transaction;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Repository interface for transaction operations
 */
public interface TransactionRepository {

    /**
     * Interface for transaction list updates
     */
    interface TransactionListCallback {
        void onTransactionsLoaded(List<Transaction> transactions);
        void onError(Exception e);
    }

    /**
     * Interface for single transaction result
     */
    interface TransactionCallback {
        void onTransactionLoaded(Transaction transaction);
        void onError(Exception e);
    }

    /**
     * Get transactions for a specific wallet address and register a listener for updates
     * @param walletAddress The wallet address to get transactions for
     * @param callback Callback for transaction updates
     * @return ListenerRegistration that can be used to remove the listener
     */
    ListenerRegistration getTransactionsForWalletFrom(String walletAddress, TransactionListCallback callback);

    /**
     * Get transactions for a specific wallet address and register a listener for updates
     * @param walletAddress The wallet address to get transactions sent to
     * @param callback Callback for transaction updates
     * @return ListenerRegistration that can be used to remove the listener
     */
    ListenerRegistration getTransactionsForWalletTo(String walletAddress, TransactionListCallback callback);

    /**
     * Get a single transaction by hash
     * @param transactionHash The hash of the transaction to get
     * @param callback Callback for the result
     */
    void getTransactionByHash(String transactionHash, TransactionCallback callback);

    /**
     * Get transactions for a wallet as a Task
     * @param walletAddress The wallet address
     * @return Task with the list of transactions
     */
    Task<List<Transaction>> getTransactionsAsTask(String walletAddress);

    /**
     * Save a new transaction
     * @param walletAddressFrom The wallet address that performed the transaction
     * @param transactionType The type of transaction (ADD_LIQUIDITY, REMOVE_LIQUIDITY, SWAP)
     * @param tokenAddress The token address involved in the transaction
     * @param amount The amount of the transaction
     * @param transactionHash The transaction hash
     * @return A future that resolves to the transaction ID
     */
    CompletableFuture<String> saveTransaction(String walletAddressFrom, String transactionType,
                                              String tokenAddress, String amount,
                                              String transactionHash, String walletAddressTo);
}