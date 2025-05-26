package com.example.crypto_payment_system.view.viewmodels;

import static com.example.crypto_payment_system.config.Constants.ADD_LIQUIDITY;
import static com.example.crypto_payment_system.config.Constants.CURRENCY_EUR;
import static com.example.crypto_payment_system.config.Constants.CURRENCY_USD;
import static com.example.crypto_payment_system.config.Constants.EURSC;
import static com.example.crypto_payment_system.config.Constants.EUR_TO_USD;
import static com.example.crypto_payment_system.config.Constants.EUR_TO_USD_TRANSFER;
import static com.example.crypto_payment_system.config.Constants.EUR_TRANSFER;
import static com.example.crypto_payment_system.config.Constants.MINT_USD;
import static com.example.crypto_payment_system.config.Constants.USDT;
import static com.example.crypto_payment_system.config.Constants.USD_TO_EUR;
import static com.example.crypto_payment_system.config.Constants.USD_TO_EUR_TRANSFER;
import static com.example.crypto_payment_system.config.Constants.USD_TRANSFER;

import android.app.Application;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.crypto_payment_system.BuildConfig;
import com.example.crypto_payment_system.R;
import com.example.crypto_payment_system.config.ApiConfig;
import com.example.crypto_payment_system.contracts.ExchangeContract;
import com.example.crypto_payment_system.contracts.ExchangeContractImpl;
import com.example.crypto_payment_system.domain.account.User;
import com.example.crypto_payment_system.domain.account.WalletAccount;
import com.example.crypto_payment_system.domain.account.WalletManager;
import com.example.crypto_payment_system.domain.token.TokenBalance;
import com.example.crypto_payment_system.domain.transaction.Transaction;
import com.example.crypto_payment_system.domain.exchangeRate.ExchangeRate;
import com.example.crypto_payment_system.repositories.api.ExchangeRateRepository;
import com.example.crypto_payment_system.repositories.api.ExchangeRateRepositoryImpl;
import com.example.crypto_payment_system.repositories.exchange.ExchangeRepository;
import com.example.crypto_payment_system.repositories.exchange.ExchangeRepositoryImpl;
import com.example.crypto_payment_system.repositories.token.TokenRepositoryImpl;
import com.example.crypto_payment_system.repositories.transaction.TransactionRepositoryImpl;
import com.example.crypto_payment_system.repositories.user.UserRepository;
import com.example.crypto_payment_system.repositories.user.UserRepositoryImpl;
import com.example.crypto_payment_system.service.firebase.auth.AuthService;
import com.example.crypto_payment_system.service.firebase.auth.AuthServiceImpl;
import com.example.crypto_payment_system.service.firebase.firestore.FirestoreService;
import com.example.crypto_payment_system.service.firebase.firestore.FirestoreServiceImpl;
import com.example.crypto_payment_system.service.token.TokenContractService;
import com.example.crypto_payment_system.service.token.TokenContractServiceImpl;
import com.example.crypto_payment_system.service.web3.Web3Service;
import com.example.crypto_payment_system.service.web3.Web3ServiceImpl;
import com.example.crypto_payment_system.utils.confirmation.ConfirmationRequest;
import com.example.crypto_payment_system.utils.web3.TransactionResult;
import com.google.firebase.firestore.ListenerRegistration;

import org.json.JSONException;
import org.web3j.crypto.Credentials;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
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
    private final ExchangeRateRepository exchangeRateRepository;

    private double currentExchangeRate = 0;

    private final MutableLiveData<String> connectionStatus = new MutableLiveData<>();
    private final MutableLiveData<Map<String, String>> tokenAddresses = new MutableLiveData<>();
    private final MutableLiveData<Map<String, TokenBalance>> tokenBalances = new MutableLiveData<>();
    private final MutableLiveData<TransactionResult> transactionResult = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> isNewUser = new MutableLiveData<>(false);
    private final MutableLiveData<User> currentUser = new MutableLiveData<>();
    private final MutableLiveData<String> selectedCurrency = new MutableLiveData<>();
    private final MutableLiveData<List<Transaction>> transactions = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<Transaction>> filteredTransactions = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<ConfirmationRequest> transactionConfirmation = new MutableLiveData<>();
    private final MutableLiveData<ExchangeRate> exchangeRate = new MutableLiveData<>();
    
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
        
        exchangeRateRepository = new ExchangeRateRepositoryImpl(
                ApiConfig.BASE_URL,
                ApiConfig.USERNAME,
                ApiConfig.PASSWORD
        );

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

                    tokenRepository.initializeTokenAddresses()
                            .thenCompose(addresses -> {
                                tokenAddresses.postValue(addresses);
                                return tokenRepository.getAllBalances(getActiveCredentials());
                            })
                            .thenAccept(balances -> {
                                tokenBalances.postValue(balances);
                                loadTransactionsForWallet(activeAccount.getAddress());
                            });
                } else {
                    connectionStatus.postValue("Error: No active account");
                    isLoading.postValue(false);
                    return;
                }
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
            final String displayCurrency = currencyToExchange.equals(USDT) ? USDT : EURSC;


            if (EURSC.equals(currencyToExchange)) {
                exchangeEurToUsd(tokenAmount);
            } else if (USDT.equals(currencyToExchange)) {
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
            final String displayCurrency = currency.equals(USDT) ? USDT : EURSC;

            exchangeRepository.getRequiredTokenCost(currency, getActiveCredentials(), tokenAmount)
                    .thenAccept(costInfo -> {
                        isLoading.postValue(false);

                        BigDecimal ethAmount = new BigDecimal(costInfo.getRequiredEth())
                                .divide(BigDecimal.TEN.pow(18), 6, RoundingMode.HALF_UP);

                        String confirmationMessage = "Converting " + displayAmount + " " + displayCurrency +
                                " will cost approximately " + ethAmount + " ETH. Proceed?";

                        transactionConfirmation.postValue(new ConfirmationRequest(
                                confirmationMessage,
                                () -> executeTokenMinting(currency, tokenAmount, displayAmount, displayCurrency)
                        ));
                    })
                    .exceptionally(ex -> {
                        isLoading.postValue(false);
                        transactionResult.postValue(new TransactionResult(
                                false,
                                null,
                                "Error calculating cost: " + ex.getMessage()
                        ));
                        return null;
                    });
        } catch (NumberFormatException e) {
            transactionResult.setValue(new TransactionResult(false, null, "Invalid amount format"));
            isLoading.setValue(false);
        }
    }

    /**
     * Execute the actual token minting after user confirmation
     */
    private void executeTokenMinting(String currency, String tokenAmount, String displayAmount, String displayCurrency) {
        isLoading.setValue(true);

        exchangeRepository.mintTokens(currency, getActiveCredentials(), tokenAmount)
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
            final String displayCurrency = currency.equals(USDT) ? USDT : EURSC;

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
            if (sendCurrency.equals(EURSC)) {
                sendCurrencyCode = CURRENCY_EUR;
            } else if (sendCurrency.equals(USDT)) {
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

        String currentCurrency = selectedCurrency.getValue();
        if (currentCurrency != null) {
            updateFilteredTransactions(currentCurrency);
        }

        transactionListeners = new ArrayList<>();
        transactionListeners.add(fromListener);
        transactionListeners.add(toListener);
    }

    public void updateFilteredTransactions(String currency) {
        List<Transaction> allTransactions = transactions.getValue();
        if (allTransactions == null || allTransactions.isEmpty()) {
            filteredTransactions.setValue(new ArrayList<>());
            return;
        }

        List<Transaction> filtered = new ArrayList<>();

        for (Transaction transaction : allTransactions) {
            String type = transaction.getTransactionType();

            boolean include = false;

            if (EURSC.equals(currency)) {
                if (type.equals(EUR_TRANSFER) || type.equals(EUR_TO_USD) || type.equals(EUR_TO_USD_TRANSFER)) {
                    include = true;
                } else if (type.equals(ADD_LIQUIDITY) && transaction.getSentCurrency() == 1) {
                    include = true;
                } else if (type.equals(USD_TO_EUR) || (type.equals(USD_TO_EUR_TRANSFER) && transaction.getWalletAddressTo().equals(getActiveAccount().getValue().getAddress()))){
                    include = true;
                }
            } else if (USDT.equals(currency)) {
                if (type.equals(USD_TRANSFER) || type.equals(USD_TO_EUR_TRANSFER) || type.equals(USD_TO_EUR)) {
                    include = true;
                } else if (type.equals(ADD_LIQUIDITY) && transaction.getSentCurrency() == 2) {
                    include = true;
                } else if (type.equals(EUR_TO_USD) || (type.equals(EUR_TO_USD_TRANSFER) && transaction.getWalletAddressTo().equals(getActiveAccount().getValue().getAddress()))){
                    include = true;
                } else if (type.equals(MINT_USD)) {
                    include = true;
                }
            }

            if (include) {
                filtered.add(transaction);
            }
        }

        filteredTransactions.setValue(filtered);
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

    public LiveData<String> getConnectionStatus() {
        return connectionStatus;
    }

    public LiveData<Map<String, String>> getTokenAddresses() {
        return tokenAddresses;
    }

    public LiveData<Map<String, TokenBalance>> getTokenBalances() {
        return tokenBalances;
    }

    public LiveData<TransactionResult> getTransactionResult() {
        return transactionResult;
    }

    public void resetTransactionResult() {
        transactionResult.setValue(null);
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public LiveData<Boolean> isNewUser() {
        return isNewUser;
    }

    public LiveData<User> getCurrentUser() {
        return currentUser;
    }

    public LiveData<List<WalletAccount>> getAccounts() {
        return walletManager.getAccountsLiveData();
    }

    public LiveData<WalletAccount> getActiveAccount() {
        return walletManager.getActiveAccountLiveData();
    }

    public void setSelectedCurrency(String currency) {
        selectedCurrency.setValue(currency);
        updateFilteredTransactions(currency);
    }

    public LiveData<String> getSelectedCurrency() {
        return selectedCurrency;
    }

    public LiveData<List<Transaction>> getTransactions() {
        return transactions;
    }

    /**
     * Get filtered transactions LiveData
     */
    public LiveData<List<Transaction>> getFilteredTransactions() {
        return filteredTransactions;
    }

    /**
     * Get user's preferred currencies as a list
     * This will be used by the HomeFragment to update its adapter
     */
    public List<String> getPreferredCurrencyList() {
        User user = currentUser.getValue();
        if (user != null && user.getPreferredCurrency() != null) {
            return Arrays.asList(user.getPreferredCurrency().split(","));
        }
        return new ArrayList<>();
    }

    public LiveData<ConfirmationRequest> getTransactionConfirmation() {
        return transactionConfirmation;
    }

    public void resetTransactionConfirmation() {
        transactionConfirmation.setValue(null);
    }

    /**
     * Fetch the current exchange rate from the API
     */
    public void fetchExchangeRate() {
        isLoading.setValue(true);
        
        exchangeRateRepository.getExchangeRate()
                .thenAccept(rate -> {
                    exchangeRate.postValue(rate);
                    isLoading.postValue(false);
                })
                .exceptionally(e -> {
                    Log.e("MainViewModel", "Error fetching exchange rate", e);
                    isLoading.postValue(false);
                    return null;
                });
    }

    /**
     * Get the exchange rate LiveData
     */
    public LiveData<ExchangeRate> getExchangeRateData() {
        return exchangeRate;
    }

    public double getCurrentExchangeRate() {
        return currentExchangeRate;
    }

    public void setCurrentExchangeRate(double rate) {
        this.currentExchangeRate = rate;
    }
}