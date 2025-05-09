package com.example.crypto_payment_system.repositories.transaction;

import android.util.Log;

import com.example.crypto_payment_system.domain.transaction.Transaction;
import com.example.crypto_payment_system.service.firebase.firestore.FirestoreService;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class TransactionRepositoryImpl implements TransactionRepository {
    private static final String TAG = "TransactionRepository";
    private final FirestoreService firestoreService;
    private final FirebaseFirestore firestore;

    @Inject
    public TransactionRepositoryImpl(FirestoreService firestoreService) {
        this.firestoreService = firestoreService;
        this.firestore = FirebaseFirestore.getInstance();
    }

    @Override
    public ListenerRegistration getTransactionsForWalletFrom(String walletAddress, TransactionListCallback callback) {
        Log.d(TAG, "Getting transactions for wallet: " + walletAddress);

        return firestore.collection("transactions")
                .whereEqualTo("walletAddressFrom", walletAddress)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(50)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Error fetching transactions", error);
                        callback.onError(error);
                        return;
                    }

                    if (snapshot != null) {
                        List<Transaction> transactions = new ArrayList<>();

                        for (DocumentSnapshot document : snapshot.getDocuments()) {
                            try {
                                String amount = document.getString("amount");
                                if (amount == null) amount = "0";

                                Long timestamp = document.getLong("timestamp");
                                if (timestamp == null) timestamp = 0L;

                                String tokenAddress = document.getString("tokenAddress");
                                if (tokenAddress == null) tokenAddress = "";

                                String transactionHash = document.getString("transactionHash");
                                if (transactionHash == null) transactionHash = "";

                                String transactionType = document.getString("transactionType");
                                if (transactionType == null) transactionType = "";

                                String docWalletAddress = document.getString("walletAddressFrom");
                                if (docWalletAddress == null) docWalletAddress = "";

                                String walletAddressTo = document.getString("walletAddressTo");
                                if (walletAddressTo == null) {
                                    walletAddressTo="";
                                }

                                String exchangeRate = document.getString("exchangeRate");
                                if(exchangeRate == null){
                                    exchangeRate = "";
                                }

                                int sentCurrency = Objects.requireNonNull(document.getLong("sentCurrency")).intValue();

                                int receivedCurrency = Objects.requireNonNull(document.getLong("receivedCurrency")).intValue();

                                Transaction transaction = new Transaction(
                                        amount,
                                        timestamp,
                                        tokenAddress,
                                        transactionHash,
                                        transactionType,
                                        docWalletAddress,
                                        exchangeRate,
                                        sentCurrency,
                                        receivedCurrency,
                                        walletAddressTo
                                );

                                transactions.add(transaction);
                            } catch (Exception e) {
                                Log.w(TAG, "Error converting transaction document", e);
                            }
                        }

                        Log.d(TAG, "Loaded " + transactions.size() + " transactions for wallet: " + walletAddress);
                        callback.onTransactionsLoaded(transactions);
                    }
                });
    }

    @Override
    public ListenerRegistration getTransactionsForWalletTo(String walletAddress, TransactionListCallback callback) {
        Log.d(TAG, "Getting transactions for wallet: " + walletAddress);

        return firestore.collection("transactions")
                .whereEqualTo("walletAddressTo", walletAddress)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(50)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Error fetching transactions", error);
                        callback.onError(error);
                        return;
                    }

                    if (snapshot != null) {
                        List<Transaction> transactions = new ArrayList<>();

                        for (DocumentSnapshot document : snapshot.getDocuments()) {
                            try {
                                String amount = document.getString("amount");
                                if (amount == null) amount = "0";

                                Long timestamp = document.getLong("timestamp");
                                if (timestamp == null) timestamp = 0L;

                                String tokenAddress = document.getString("tokenAddress");
                                if (tokenAddress == null) tokenAddress = "";

                                String transactionHash = document.getString("transactionHash");
                                if (transactionHash == null) transactionHash = "";

                                String transactionType = document.getString("transactionType");
                                if (transactionType == null) transactionType = "";

                                String docWalletAddress = document.getString("walletAddressFrom");
                                if (docWalletAddress == null) docWalletAddress = "";

                                String walletAddressTo = document.getString("walletAddressTo");
                                if (walletAddressTo == null) {
                                    walletAddressTo="";
                                }

                                String exchangeRate = document.getString("exchangeRate");
                                if(exchangeRate == null){
                                    exchangeRate = "";
                                }

                                int sentCurrency = Objects.requireNonNull(document.getLong("sentCurrency")).intValue();

                                int receivedCurrency = Objects.requireNonNull(document.getLong("receivedCurrency")).intValue();

                                Transaction transaction = new Transaction(
                                        amount,
                                        timestamp,
                                        tokenAddress,
                                        transactionHash,
                                        transactionType,
                                        docWalletAddress,
                                        exchangeRate,
                                        sentCurrency,
                                        receivedCurrency,
                                        walletAddressTo
                                );

                                transactions.add(transaction);
                            } catch (Exception e) {
                                Log.w(TAG, "Error converting transaction document", e);
                            }
                        }

                        Log.d(TAG, "Loaded " + transactions.size() + " transactions for wallet: " + walletAddress);
                        callback.onTransactionsLoaded(transactions);
                    }
                });
    }

    @Override
    public void getTransactionByHash(String transactionHash, TransactionCallback callback) {
        Log.d(TAG, "Getting transaction with hash: " + transactionHash);

        firestore.collection("transactions")
                .whereEqualTo("transactionHash", transactionHash)
                .limit(1)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        Log.d(TAG, "No transaction found with hash: " + transactionHash);
                        callback.onTransactionLoaded(null);
                    } else {
                        DocumentSnapshot doc = queryDocumentSnapshots.getDocuments().get(0);

                        String amount = doc.getString("amount");
                        if (amount == null) amount = "0";

                        Long timestamp = doc.getLong("timestamp");
                        if (timestamp == null) timestamp = 0L;

                        String tokenAddress = doc.getString("tokenAddress");
                        if (tokenAddress == null) tokenAddress = "";

                        String docTransactionHash = doc.getString("transactionHash");
                        if (docTransactionHash == null) docTransactionHash = "";

                        String transactionType = doc.getString("transactionType");
                        if (transactionType == null) transactionType = "";

                        String walletAddress = doc.getString("walletAddressFrom");
                        if (walletAddress == null) walletAddress = "";

                        String walletAddressTo = doc.getString("walletAddressTo");
                        if (walletAddressTo != null) {
                            walletAddressTo="";
                        }

                        String exchangeRate = doc.getString("exchangeRate");
                        if(exchangeRate == null){
                            exchangeRate = "";
                        }

                        int sentCurrency = Integer.parseInt(Objects.requireNonNull(doc.getString("sentCurrency")));

                        int receivedCurrency = Integer.parseInt(Objects.requireNonNull(doc.getString("receivedCurrency")));

                        Transaction transaction = new Transaction(
                                amount,
                                timestamp,
                                tokenAddress,
                                transactionHash,
                                transactionType,
                                walletAddress,
                                exchangeRate,
                                sentCurrency,
                                receivedCurrency,
                                walletAddressTo
                        );

                        Log.d(TAG, "Found transaction with hash: " + transactionHash);
                        callback.onTransactionLoaded(transaction);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting transaction by hash", e);
                    callback.onError(e);
                });
    }

    @Override
    public Task<List<Transaction>> getTransactionsAsTask(String walletAddress) {
        Log.d(TAG, "Getting transactions as task for wallet: " + walletAddress);

        return firestore.collection("transactions")
                .whereEqualTo("walletAddress", walletAddress)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(50)
                .get()
                .continueWith(task -> {
                    if (!task.isSuccessful()) {
                        Log.e(TAG, "Error in getTransactionsAsTask", task.getException());
                        throw task.getException();
                    }

                    List<Transaction> transactions = new ArrayList<>();
                    QuerySnapshot snapshot = task.getResult();

                    for (DocumentSnapshot document : snapshot.getDocuments()) {
                        try {
                            String amount = document.getString("amount");
                            if (amount == null) amount = "0";

                            Long timestamp = document.getLong("timestamp");
                            if (timestamp == null) timestamp = 0L;

                            String tokenAddress = document.getString("tokenAddress");
                            if (tokenAddress == null) tokenAddress = "";

                            String transactionHash = document.getString("transactionHash");
                            if (transactionHash == null) transactionHash = "";

                            String transactionType = document.getString("transactionType");
                            if (transactionType == null) transactionType = "";

                            String docWalletAddress = document.getString("walletAddressFrom");
                            if (docWalletAddress == null) docWalletAddress = "";

                            String walletAddressTo = document.getString("walletAddressTo");
                            if (walletAddressTo != null) {
                                walletAddressTo="";
                            }

                            String exchangeRate = document.getString("exchangeRate");
                            if(exchangeRate == null){
                                exchangeRate = "";
                            }

                            int sentCurrency = Integer.parseInt(Objects.requireNonNull(document.getString("sentCurrency")));

                            int receivedCurrency = Integer.parseInt(Objects.requireNonNull(document.getString("receivedCurrency")));

                            Transaction transaction = new Transaction(
                                    amount,
                                    timestamp,
                                    tokenAddress,
                                    transactionHash,
                                    transactionType,
                                    docWalletAddress,
                                    exchangeRate,
                                    sentCurrency,
                                    receivedCurrency,
                                    walletAddressTo
                            );

                            transactions.add(transaction);
                        } catch (Exception e) {
                            Log.w(TAG, "Error converting transaction document in task", e);
                        }
                    }

                    Log.d(TAG, "Loaded " + transactions.size() + " transactions as task for wallet: " + walletAddress);
                    return transactions;
                });
    }

    @Override
    public CompletableFuture<String> saveTransaction(String walletAddressFrom, String transactionType,
                                                     String tokenAddress, String amount,
                                                     String transactionHash, String walletAddressTo,
                                                     String exchangeRate, int sentCurrency, int receivedCurrency) {
        Log.d(TAG, "Saving transaction with hash: " + transactionHash);

        return firestoreService.saveTransaction(
                walletAddressFrom,
                transactionType,
                tokenAddress,
                amount,
                transactionHash,
                walletAddressTo,
                exchangeRate,
                sentCurrency,
                receivedCurrency
        );
    }
}