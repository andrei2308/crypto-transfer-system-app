package com.example.crypto_payment_system.view.viewmodels;

import static com.example.crypto_payment_system.config.Constants.CURRENCY_EUR;
import static com.example.crypto_payment_system.config.Constants.CURRENCY_USD;

import android.app.Application;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.crypto_payment_system.R;
import com.example.crypto_payment_system.domain.transaction.Transaction;
import com.example.crypto_payment_system.repositories.transaction.TransactionRepositoryImpl;
import com.example.crypto_payment_system.service.firebase.auth.AuthService;
import com.example.crypto_payment_system.service.firebase.auth.AuthServiceImpl;
import com.example.crypto_payment_system.service.firebase.firestore.FirestoreService;
import com.example.crypto_payment_system.service.firebase.firestore.FirestoreServiceImpl;
import com.example.crypto_payment_system.service.token.TokenContractService;
import com.example.crypto_payment_system.service.token.TokenContractServiceImpl;
import com.example.crypto_payment_system.service.web3.Web3Service;
import com.example.crypto_payment_system.config.Constants;
import com.example.crypto_payment_system.contracts.ExchangeContract;
import com.example.crypto_payment_system.contracts.ExchangeContractImpl;
import com.example.crypto_payment_system.domain.token.TokenBalance;
import com.example.crypto_payment_system.domain.account.User;
import com.example.crypto_payment_system.domain.account.WalletAccount;
import com.example.crypto_payment_system.domain.account.WalletManager;
import com.example.crypto_payment_system.repositories.exchange.ExchangeRepository;
import com.example.crypto_payment_system.repositories.exchange.ExchangeRepositoryImpl;
import com.example.crypto_payment_system.repositories.token.TokenRepositoryImpl;
import com.example.crypto_payment_system.repositories.user.UserRepository;
import com.example.crypto_payment_system.repositories.user.UserRepositoryImpl;
import com.example.crypto_payment_system.service.web3.Web3ServiceImpl;
import com.example.crypto_payment_system.utils.web3.TransactionResult;
import com.google.firebase.firestore.ListenerRegistration;
import com.example.crypto_payment_system.BuildConfig;
import org.json.JSONException;
import org.web3j.crypto.Credentials;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * ViewModel for the main activity to manage UI state and business logic
 */
public class MainViewModel extends AndroidViewModel {
    private final Web3Service web3Service;
    private final TokenRepositoryImpl tokenRepository;
    private final ExchangeRepository exchangeRepository;
    private final TransactionRepositoryImpl transactionRepository;
    private final UserRepository userRepository;
    private final WalletManager walletManager;

    private final MutableLiveData<String> connectionStatus = new MutableLiveData<>();
    private final MutableLiveData<Map<String, String>> tokenAddresses = new MutableLiveData<>();
    private final MutableLiveData<Map<String, TokenBalance>> tokenBalances = new MutableLiveData<>();
    private final MutableLiveData<TransactionResult> transactionResult = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> isNewUser = new MutableLiveData<>(false);
    private final MutableLiveData<User> currentUser = new MutableLiveData<>();
    private final MutableLiveData<String> selectedCurrency = new MutableLiveData<>();
    private final MutableLiveData<List<Transaction>> transactions = new MutableLiveData<>(new ArrayList<>());
    private ListenerRegistration transactionListener;

    public MainViewModel(@NonNull Application application) throws Exception {
        super(application);

        web3Service = new Web3ServiceImpl(application);
        AuthService authService = new AuthServiceImpl();
        FirestoreService firestoreService = new FirestoreServiceImpl(authService);
        this.transactionRepository = new TransactionRepositoryImpl(firestoreService);
        userRepository = new UserRepositoryImpl(firestoreService);
        TokenContractService tokenService = new TokenContractServiceImpl(web3Service);
        ExchangeContract exchangeContract = new ExchangeContractImpl(web3Service, tokenService);
        tokenRepository = new TokenRepositoryImpl(web3Service, tokenService);
        exchangeRepository = new ExchangeRepositoryImpl(web3Service, exchangeContract, tokenRepository, firestoreService);

        walletManager = new WalletManager(application);

        if (walletManager.getAccounts().isEmpty()) {
            walletManager.addAccount(application.getString(R.string.default_account), BuildConfig.ETHEREUM_PRIVATE_KEY);
        }

        walletManager.getActiveAccountLiveData().observeForever(account -> {
            if (account != null && web3Service.isConnected()) {
                loadUserData(account.getAddress());
            }
        });
    }

    /**
     * Connect to Ethereum with the active account
     */
    public void connectToEthereum() {
        isLoading.setValue(true);

        new Thread(() -> {
            try {
                String clientVersion = web3Service.connect();
                connectionStatus.postValue("Connected to: " + clientVersion);

                WalletAccount activeAccount = walletManager.getActiveAccount();
                if (activeAccount != null) {
                    loadUserData(activeAccount.getAddress());
                } else {
                    connectionStatus.postValue("Error: No active account");
                    isLoading.postValue(false);
                    return;
                }

                tokenRepository.initializeTokenAddresses()
                        .thenAccept(tokenAddresses::postValue);
                tokenRepository.getAllBalances(getActiveCredentials())
                        .thenAccept(balances -> {
                            tokenBalances.postValue(balances);
                            isLoading.postValue(false);
                        });
                loadTransactionsForWallet(activeAccount.getAddress());
            } catch (Exception e) {
                connectionStatus.postValue("Error: " + e.getMessage());
                isLoading.postValue(false);
            }
        }).start();
    }

    /**
     * Load user data for the specified wallet address
     */
    private void loadUserData(String walletAddress) {
        userRepository.handleUserConnection(walletAddress)
                .thenAccept(isNew -> {
                    isNewUser.postValue(isNew);

                    if (!isNew) {
                        // Existing user, get their data
                        userRepository.getUserData(walletAddress)
                                .thenAccept(user -> {
                                    currentUser.postValue(user);
                                    isLoading.postValue(false);
                                });
                    } else {
                        isLoading.postValue(false);
                    }
                });
    }

    /**
     * Add a new Ethereum account
     *
     * @return true if successful, false if account already exists
     */
    public boolean addAccount(String name, String privateKey) throws JSONException {
        WalletAccount newAccount = walletManager.addAccount(name, privateKey);
        return newAccount != null;
    }

    /**
     * Switch to a different account
     */
    public void switchAccount(String address) throws JSONException {
        isLoading.setValue(true);
        walletManager.switchAccount(address);
        checkAllBalances();
        loadTransactionsForWallet(address);
    }

    /**
     * Remove an account
     *
     * @return true if successful, false if account not found
     */
    public boolean removeAccount(String address) throws JSONException {
        return walletManager.removeAccount(address);
    }

    /**
     * Get the current active credentials for Ethereum transactions
     */
    public Credentials getActiveCredentials() {
        return walletManager.getActiveCredentials();
    }

    /**
     * Create new user with preferred currency
     */
    public void createUserWithPreferredCurrency(String preferredCurrency) {
        WalletAccount activeAccount = walletManager.getActiveAccount();
        if (activeAccount == null) {
            return;
        }

        String walletAddress = activeAccount.getAddress();
        isLoading.setValue(true);

        userRepository.createNewUser(walletAddress, preferredCurrency)
                .thenCompose(aVoid -> userRepository.getUserData(walletAddress))
                .thenAccept(user -> {
                    currentUser.postValue(user);
                    isNewUser.postValue(false);
                    isLoading.postValue(false);
                })
                .exceptionally(e -> {
                    connectionStatus.postValue("Error creating user: " + e.getMessage());
                    isLoading.postValue(false);
                    return null;
                });
    }

    /**
     * Update user's preferred currency or create user if they don't exist
     */
    public void updatePreferredCurrency(String currency) {
        WalletAccount activeAccount = walletManager.getActiveAccount();
        if (activeAccount == null) {
            return;
        }

        String walletAddress = activeAccount.getAddress();
        isLoading.setValue(true);

        userRepository.getUserData(walletAddress)
                .thenAccept(user -> {
                    if (user != null) {
                        userRepository.updatePreferredCurrency(walletAddress, currency)
                                .thenCompose(aVoid -> userRepository.getUserData(walletAddress))
                                .thenAccept(updatedUser -> {
                                    currentUser.postValue(updatedUser);
                                    isLoading.postValue(false);
                                })
                                .exceptionally(e -> {
                                    connectionStatus.postValue("Error updating currency: " + e.getMessage());
                                    isLoading.postValue(false);
                                    return null;
                                });
                    } else {
                        createUserWithPreferredCurrency(currency);
                    }
                })
                .exceptionally(e -> {
                    connectionStatus.postValue("Error checking user existence: " + e.getMessage());
                    isLoading.postValue(false);
                    return null;
                });
    }

    /**
     * Exchange tokens based on user's preferred currency
     */
    public void exchangeBasedOnPreference(String currencyToExchange, String humanReadableAmount) {
        User user = currentUser.getValue();
        if (user == null) {
            transactionResult.setValue(
                    new TransactionResult(false, null, "User data not available")
            );
            return;
        }

        try {
            double amount = Double.parseDouble(humanReadableAmount);
            if (amount <= 0) {
                transactionResult.setValue(new TransactionResult(false, null, "Amount must be greater than zero"));
                return;
            }
            BigDecimal decimalAmount = BigDecimal.valueOf(amount);
            BigDecimal tokenUnits = decimalAmount.multiply(BigDecimal.valueOf(1_000_000));
            String tokenAmount = tokenUnits.toBigInteger().toString();

            isLoading.setValue(true);


            final String displayAmount = humanReadableAmount;
            final String displayCurrency = currencyToExchange.equals("USD") ? "USDT" : "EURC";


            if ("EUR".equals(currencyToExchange)) {
                exchangeEurToUsd(tokenAmount);
            } else if ("USD".equals(currencyToExchange)) {
                exchangeUsdToEur(tokenAmount);
            } else {
                transactionResult.setValue(
                        new TransactionResult(false, null, "Invalid currency")
                );
            }


        } catch (NumberFormatException e) {
            transactionResult.setValue(new TransactionResult(false, null, "Invalid amount format"));
            isLoading.setValue(false);
        }
    }

    /**
     * Check all token balances
     */
    public void checkAllBalances() {
        if (!web3Service.isConnected()) {
            transactionResult.setValue(new TransactionResult(false, null, "Connect to Ethereum first"));
            return;
        }

        isLoading.setValue(true);

        tokenRepository.getAllBalances(getActiveCredentials())
                .thenAccept(balances -> {
                    tokenBalances.postValue(balances);
                    isLoading.postValue(false);
                });
    }

    /**
     * Mint tokens
     */
    public void mintTokens(String currency, String humanReadableAmount) {
        if (!web3Service.isConnected()) {
            transactionResult.setValue(new TransactionResult(false, null, "Connect to Ethereum first"));
            return;
        }
        try {
            double amount = Double.parseDouble(humanReadableAmount);
            if (amount <= 0) {
                transactionResult.setValue(new TransactionResult(false, null, "Amount must be greater than zero"));
                return;
            }
            BigDecimal decimalAmount = BigDecimal.valueOf(amount);
            BigDecimal tokenUnits = decimalAmount.multiply(BigDecimal.valueOf(1_000_000));
            String tokenAmount = tokenUnits.toBigInteger().toString();

            isLoading.setValue(true);

            final String displayAmount = humanReadableAmount;
            final String displayCurrency = currency.equals("USD") ? "USDT" : "EURC";

            tokenRepository.mintTokens(currency, getActiveCredentials(), tokenAmount)
                    .thenAccept(result -> {
                        if (result.isSuccess()) {
                            result = new TransactionResult(
                                    true,
                                    result.getTransactionHash(),
                                    "Successfully added " + displayAmount + " " + displayCurrency + " as liquidity"
                            );
                        }
                        transactionResult.postValue(result);
                        isLoading.postValue(false);

                        if (result.isSuccess()) {
                            tokenRepository.getAllBalances(getActiveCredentials())
                                    .thenAccept(balances -> tokenBalances.postValue(balances));
                        }
                    });
        } catch (NumberFormatException e) {
            transactionResult.setValue(new TransactionResult(false, null, "Invalid amount format"));
            isLoading.setValue(false);
        }
    }

    /**
     * Add liquidity - ViewModel Method
     */
    public void addLiquidity(String currency, String humanReadableAmount) {
        if (!web3Service.isConnected()) {
            transactionResult.setValue(new TransactionResult(false, null, "Connect to Ethereum first"));
            return;
        }
        try {
            double amount = Double.parseDouble(humanReadableAmount);
            if (amount <= 0) {
                transactionResult.setValue(new TransactionResult(false, null, "Amount must be greater than zero"));
                return;
            }
            BigDecimal decimalAmount = BigDecimal.valueOf(amount);
            BigDecimal tokenUnits = decimalAmount.multiply(BigDecimal.valueOf(1_000_000)); // 10^6
            String tokenAmount = tokenUnits.toBigInteger().toString();

            isLoading.setValue(true);

            final String displayAmount = humanReadableAmount;
            final String displayCurrency = currency.equals("USD") ? "USDT" : "EURC";

            exchangeRepository.addLiquidity(currency, getActiveCredentials(), tokenAmount)
                    .thenAccept(result -> {
                        if (result.isSuccess()) {
                            result = new TransactionResult(
                                    true,
                                    result.getTransactionHash(),
                                    "Successfully added " + displayAmount + " " + displayCurrency + " as liquidity"
                            );
                        }
                        transactionResult.postValue(result);
                        isLoading.postValue(false);

                        if (result.isSuccess()) {
                            tokenRepository.getAllBalances(getActiveCredentials())
                                    .thenAccept(balances -> tokenBalances.postValue(balances));
                        }
                    });
        } catch (NumberFormatException e) {
            transactionResult.setValue(new TransactionResult(false, null, "Invalid amount format"));
            isLoading.setValue(false);
        }
    }

    /**
     * Exchange EUR to USD
     */
    public void exchangeEurToUsd(String tokenAmount) {
        if (!web3Service.isConnected()) {
            transactionResult.setValue(new TransactionResult(false, null, "Connect to Ethereum first"));
            return;
        }

        isLoading.setValue(true);

        exchangeRepository.exchangeEurToUsd(tokenAmount, getActiveCredentials())
                .thenAccept(result -> {
                    transactionResult.postValue(result);
                    if (!result.isSuccess()) {
                        isLoading.postValue(false);
                    }
                });
    }

    /**
     * Exchange USD to EUR
     */
    public void exchangeUsdToEur(String tokenAmount) {
        if (!web3Service.isConnected()) {
            transactionResult.setValue(new TransactionResult(false, null, "Connect to Ethereum first"));
            return;
        }

        isLoading.setValue(true);

        exchangeRepository.exchangeUsdToEur(tokenAmount, getActiveCredentials())
                .thenAccept(result -> {
                    transactionResult.postValue(result);
                    if (!result.isSuccess()) {
                        isLoading.postValue(false);
                    }
                });
    }

    /**
     * Send money to another address
     */
    public void sendMoney(String recipientAddress, String sendCurrency, String amount) {
        if (!web3Service.isConnected()) {
            transactionResult.setValue(new TransactionResult(false, null, "Connect to Ethereum first!"));
            return;
        }

        if (TextUtils.isEmpty(recipientAddress)) {
            transactionResult.setValue(new TransactionResult(false, null, "Recipient address cannot be empty"));
            return;
        }

        if (TextUtils.isEmpty(amount)) {
            transactionResult.setValue(new TransactionResult(false, null, "Please introduce an amount."));
            return;
        }

        try {
            int sendCurrencyCode;
            if (sendCurrency.equals("EUR")) {
                sendCurrencyCode = CURRENCY_EUR;
            } else if (sendCurrency.equals("USD")) {
                sendCurrencyCode = CURRENCY_USD;
            } else {
                sendCurrencyCode = CURRENCY_EUR;
            }

            isLoading.setValue(true);

            userRepository.getPreferredCurrency(recipientAddress, sendCurrency)
                    .thenCompose(receiveCurrencyCode -> exchangeRepository.sendTransaction(
                            recipientAddress,
                            sendCurrencyCode,
                            receiveCurrencyCode,
                            getActiveCredentials(),
                            amount))
                    .thenAccept(result -> {
                        transactionResult.postValue(result);
                        if (!result.isSuccess()) {
                            isLoading.postValue(false);
                        }
                    })
                    .exceptionally(e -> {
                        transactionResult.postValue(new TransactionResult(false, null,
                                "Transaction failed: " + e.getMessage()));
                        isLoading.postValue(false);
                        return null;
                    });
        } catch (NumberFormatException e) {
            transactionResult.setValue(new TransactionResult(false, null, "Invalid amount format"));
            isLoading.setValue(false);
        } catch (Exception e) {
            transactionResult.setValue(new TransactionResult(false, null,
                    "Error: " + e.getMessage()));
            isLoading.setValue(false);
        }
    }

    public void loadTransactionsForWallet(String walletAddress) {
        if (Boolean.FALSE.equals(web3Service.isConnected())) {
            return;
        }

        isLoading.postValue(true);

        if (transactionListener != null) {
            transactionListener.remove();
            transactionListener = null;
        }

        AtomicInteger pendingQueries = new AtomicInteger(2);

        Map<String, Transaction> transactionMap = Collections.synchronizedMap(new HashMap<>());

        Runnable checkComplete = () -> {
            if (pendingQueries.decrementAndGet() == 0) {
                List<Transaction> allTransactions = new ArrayList<>(transactionMap.values());
                Collections.sort(allTransactions, (t1, t2) ->
                        Long.compare(t2.getTimestamp(), t1.getTimestamp()));

                transactions.postValue(allTransactions);
                isLoading.postValue(false);
            }
        };

        ListenerRegistration fromListener = transactionRepository.getTransactionsForWalletFrom(
                walletAddress,
                new TransactionRepositoryImpl.TransactionListCallback() {
                    @Override
                    public void onTransactionsLoaded(List<Transaction> transactionList) {
                        for (Transaction transaction : transactionList) {
                            transactionMap.put(transaction.getTransactionHash(), transaction);
                        }

                        checkComplete.run();
                    }

                    @Override
                    public void onError(Exception e) {
                        checkComplete.run();
                    }
                }
        );

        ListenerRegistration toListener = transactionRepository.getTransactionsForWalletTo(
                walletAddress,
                new TransactionRepositoryImpl.TransactionListCallback() {
                    @Override
                    public void onTransactionsLoaded(List<Transaction> transactionList) {
                        for (Transaction transaction : transactionList) {
                            transactionMap.put(transaction.getTransactionHash(), transaction);
                        }

                        checkComplete.run();
                    }

                    @Override
                    public void onError(Exception e) {
                        checkComplete.run();
                    }
                }
        );

        transactionListeners = new ArrayList<>();
        transactionListeners.add(fromListener);
        transactionListeners.add(toListener);
    }

    private List<ListenerRegistration> transactionListeners = new ArrayList<>();

    @Override
    protected void onCleared() {
        super.onCleared();

        if (transactionListeners != null) {
            for (ListenerRegistration listener : transactionListeners) {
                if (listener != null) {
                    listener.remove();
                }
            }
            transactionListeners.clear();
        }
    }

        public LiveData<String> getConnectionStatus () {
            return connectionStatus;
        }

        public LiveData<Map<String, String>> getTokenAddresses () {
            return tokenAddresses;
        }

        public LiveData<Map<String, TokenBalance>> getTokenBalances () {
            return tokenBalances;
        }

        public LiveData<TransactionResult> getTransactionResult () {
            return transactionResult;
        }

        public void resetTransactionResult() {
            transactionResult.setValue(null);
        }

        public LiveData<Boolean> getIsLoading () {
            return isLoading;
        }

        public LiveData<Boolean> isNewUser () {
            return isNewUser;
        }

        public LiveData<User> getCurrentUser () {
            return currentUser;
        }

        public LiveData<List<WalletAccount>> getAccounts () {
            return walletManager.getAccountsLiveData();
        }

        public LiveData<WalletAccount> getActiveAccount () {
            return walletManager.getActiveAccountLiveData();
        }

        public void setSelectedCurrency (String currency){
            selectedCurrency.setValue(currency);
        }

        public LiveData<String> getSelectedCurrency () {
            return selectedCurrency;
        }

        public LiveData<List<Transaction>> getTransactions () {
            return transactions;
        }
    }