package com.example.crypto_payment_system.viewmodels;

import static com.example.crypto_payment_system.config.Constants.CURRENCY_EUR;
import static com.example.crypto_payment_system.config.Constants.CURRENCY_USD;

import android.app.Application;
import android.text.TextUtils;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.crypto_payment_system.api.FirestoreService;
import com.example.crypto_payment_system.api.TokenContractService;
import com.example.crypto_payment_system.api.Web3Service;
import com.example.crypto_payment_system.config.Constants;
import com.example.crypto_payment_system.contracts.ExchangeContract;
import com.example.crypto_payment_system.models.TokenBalance;
import com.example.crypto_payment_system.models.User;
import com.example.crypto_payment_system.models.WalletAccount;
import com.example.crypto_payment_system.models.WalletManager;
import com.example.crypto_payment_system.repositories.ExchangeRepository;
import com.example.crypto_payment_system.repositories.TokenRepository;
import com.example.crypto_payment_system.repositories.TokenRepository.TransactionResult;
import com.example.crypto_payment_system.repositories.UserRepository;

import org.json.JSONException;
import org.web3j.crypto.Credentials;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * ViewModel for the main activity to manage UI state and business logic
 */
public class MainViewModel extends AndroidViewModel {
    private final Web3Service web3Service;
    private final TokenRepository tokenRepository;
    private final ExchangeRepository exchangeRepository;
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

    public MainViewModel(Application application) throws JSONException {
        super(application);

        web3Service = new Web3Service(application);
        FirestoreService firestoreService = new FirestoreService();
        userRepository = new UserRepository(firestoreService);
        TokenContractService tokenService = new TokenContractService(web3Service);
        ExchangeContract exchangeContract = new ExchangeContract(web3Service, tokenService);
        tokenRepository = new TokenRepository(web3Service, tokenService);
        exchangeRepository = new ExchangeRepository(web3Service, exchangeContract, tokenRepository);

        walletManager = new WalletManager(application);

        if (walletManager.getAccounts().isEmpty()) {
            walletManager.addAccount("Default Account", Constants.PRIVATE_KEY);
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
//        checkAllBalances();
        isLoading.setValue(false);
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
                    isLoading.postValue(false);
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
                    isLoading.postValue(false);
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
                        isLoading.postValue(false);
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

    /**
     * Clean up resources
     */
    @Override
    protected void onCleared() {
        super.onCleared();
        web3Service.shutdown();
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
    }

    public LiveData<String> getSelectedCurrency() {
        return selectedCurrency;
    }
}