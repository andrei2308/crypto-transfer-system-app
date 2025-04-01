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
import com.example.crypto_payment_system.repositories.ExchangeRepository;
import com.example.crypto_payment_system.repositories.TokenRepository;
import com.example.crypto_payment_system.repositories.TokenRepository.TransactionResult;
import com.example.crypto_payment_system.repositories.UserRepository;

import org.web3j.crypto.Credentials;

import java.util.Map;

/**
 * ViewModel for the main activity to manage UI state and business logic
 */
public class MainViewModel extends AndroidViewModel {
    private final Web3Service web3Service;
    private final TokenContractService tokenService;

    private final ExchangeContract exchangeContract;

    private final TokenRepository tokenRepository;
    private final ExchangeRepository exchangeRepository;
    private final MutableLiveData<String> connectionStatus = new MutableLiveData<>();
    private final MutableLiveData<Map<String, String>> tokenAddresses = new MutableLiveData<>();
    private final MutableLiveData<Map<String, TokenBalance>> tokenBalances = new MutableLiveData<>();
    private final MutableLiveData<TransactionResult> transactionResult = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);

    private final FirestoreService firestoreService;
    private final UserRepository userRepository;

    private final MutableLiveData<Boolean> isNewUser = new MutableLiveData<>(false);
    private final MutableLiveData<User> currentUser = new MutableLiveData<>();
    private String currentWalletAddress;

    public MainViewModel(Application application) {
        super(application);

        web3Service = new Web3Service(application);
        this.firestoreService = new FirestoreService();
        this.userRepository = new UserRepository(firestoreService);
        tokenService = new TokenContractService(web3Service);

        exchangeContract = new ExchangeContract(web3Service, tokenService);

        tokenRepository = new TokenRepository(web3Service, tokenService);
        exchangeRepository = new ExchangeRepository(web3Service, exchangeContract, tokenRepository);
    }

    /**
     * Connect to Ethereum and handle user connection
     */
    public void connectToEthereum() {
        isLoading.setValue(true);

        new Thread(() -> {
            try {
                String clientVersion = web3Service.connect();
                connectionStatus.postValue("Connected to: " + clientVersion);

                Credentials credentials = Credentials.create(Constants.PRIVATE_KEY);
                currentWalletAddress = credentials.getAddress();

                userRepository.handleUserConnection(currentWalletAddress)
                        .thenAccept(isNew -> {
                            isNewUser.postValue(isNew);

                            if (!isNew) {
                                // Existing user, get their data
                                userRepository.getUserData(currentWalletAddress)
                                        .thenAccept(user -> {
                                            currentUser.postValue(user);
                                            isLoading.postValue(false);
                                        });
                            } else {
                                isLoading.postValue(false);
                            }
                        });

                tokenRepository.initializeTokenAddresses()
                        .thenAccept(addresses -> {
                            tokenAddresses.postValue(addresses);
                        });

            } catch (Exception e) {
                connectionStatus.postValue("Error: " + e.getMessage());
                isLoading.postValue(false);
            }
        }).start();
    }

    /**
     * Create new user with preferred currency
     */
    public void createUserWithPreferredCurrency(String preferredCurrency) {
        if (currentWalletAddress == null) {
            return;
        }

        isLoading.setValue(true);

        userRepository.createNewUser(currentWalletAddress, preferredCurrency)
                .thenCompose(aVoid -> userRepository.getUserData(currentWalletAddress))
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
        if (currentWalletAddress == null) {
            return;
        }

        isLoading.setValue(true);

        userRepository.getUserData(currentWalletAddress)
                .thenAccept(user -> {
                    if (user != null) {
                        userRepository.updatePreferredCurrency(currentWalletAddress, currency)
                                .thenCompose(aVoid -> userRepository.getUserData(currentWalletAddress))
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
    public void exchangeBasedOnPreference(String currencyToExchange) {
        User user = currentUser.getValue();
        if (user == null) {
            transactionResult.setValue(
                    new TransactionResult(false, null, "User data not available")
            );
            return;
        }

        if ("EUR".equals(currencyToExchange)) {
            exchangeEurToUsd();
        } else if ("USD".equals(currencyToExchange)){
            exchangeUsdToEur();
        } else {
            new TransactionResult(false,null,"Invalid currency");
        }
    }

    public LiveData<Boolean> isNewUser() {
        return isNewUser;
    }

    public LiveData<User> getCurrentUser() {
        return currentUser;
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

        tokenRepository.getAllBalances()
                .thenAccept(balances -> {
                    tokenBalances.postValue(balances);
                    isLoading.postValue(false);
                });
    }

    /**
     * Mint tokens
     */
    public void mintTokens(String currency) {
        if (!web3Service.isConnected()) {
            transactionResult.setValue(new TransactionResult(false, null, "Connect to Ethereum first"));
            return;
        }

        isLoading.setValue(true);

        tokenRepository.mintTokens(currency)
                .thenAccept(result -> {
                    transactionResult.postValue(result);
                    isLoading.postValue(false);
                });
    }

    /**
     * Add liquidity
     */
    public void addLiquidity(String currency) {
        if (!web3Service.isConnected()) {
            transactionResult.setValue(new TransactionResult(false, null, "Connect to Ethereum first"));
            return;
        }

        isLoading.setValue(true);

        exchangeRepository.addLiquidity(currency)
                .thenAccept(result -> {
                    transactionResult.postValue(result);
                    isLoading.postValue(false);
                });
    }

    /**
     * Exchange EUR to USD
     */
    public void exchangeEurToUsd() {
        if (!web3Service.isConnected()) {
            transactionResult.setValue(new TransactionResult(false, null, "Connect to Ethereum first"));
            return;
        }

        isLoading.setValue(true);

        exchangeRepository.exchangeEurToUsd()
                .thenAccept(result -> {
                    transactionResult.postValue(result);
                    isLoading.postValue(false);
                });
    }

    /**
     * Exchange USD to EUR
     */
    public void exchangeUsdToEur(){
        if(!web3Service.isConnected()){
            transactionResult.setValue(new TransactionResult(false,null,"Connect to Ethereum first"));
            return;
        }

        isLoading.setValue(true);

        exchangeRepository.exchangeUsdToEur()
                .thenAccept(result->{
                    transactionResult.postValue(result);
                    isLoading.postValue(false);
                });
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

    public void sendMoney(String recipientAddress, String sendCurrency) {
        if (!web3Service.isConnected()) {
            transactionResult.setValue(new TransactionResult(false, null, "Connect to Ethereum first!"));
            return;
        }

        if (TextUtils.isEmpty(recipientAddress)) {
            transactionResult.setValue(new TransactionResult(false, null, "Recipient address cannot be empty"));
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
                    .thenCompose(receiveCurrencyCode -> {
                        return exchangeRepository.sendTransaction(
                                recipientAddress,
                                sendCurrencyCode,
                                receiveCurrencyCode);
                    })
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

}