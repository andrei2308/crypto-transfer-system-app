package com.example.crypto_payment_system.viewmodels;

import android.app.Application;
import android.content.Context;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.crypto_payment_system.api.TokenContractService;
import com.example.crypto_payment_system.api.Web3Service;
import com.example.crypto_payment_system.contracts.ExchangeContract;
import com.example.crypto_payment_system.models.TokenBalance;
import com.example.crypto_payment_system.repositories.ExchangeRepository;
import com.example.crypto_payment_system.repositories.TokenRepository;
import com.example.crypto_payment_system.repositories.TokenRepository.TransactionResult;

import java.util.Map;

/**
 * ViewModel for the main activity to manage UI state and business logic
 */
public class MainViewModel extends AndroidViewModel {
    // Services
    private final Web3Service web3Service;
    private final TokenContractService tokenService;

    // Contracts
    private final ExchangeContract exchangeContract;

    // Repositories
    private final TokenRepository tokenRepository;
    private final ExchangeRepository exchangeRepository;
    private final MutableLiveData<String> connectionStatus = new MutableLiveData<>();
    private final MutableLiveData<Map<String, String>> tokenAddresses = new MutableLiveData<>();
    private final MutableLiveData<Map<String, TokenBalance>> tokenBalances = new MutableLiveData<>();
    private final MutableLiveData<TransactionResult> transactionResult = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);

    public MainViewModel(Application application) {
        super(application);

        web3Service = new Web3Service(application);
        tokenService = new TokenContractService(web3Service);

        exchangeContract = new ExchangeContract(web3Service, tokenService);

        tokenRepository = new TokenRepository(web3Service, tokenService);
        exchangeRepository = new ExchangeRepository(web3Service, exchangeContract, tokenRepository);
    }

    /**
     * Connect to Ethereum network
     */
    public void connectToEthereum() {
        isLoading.setValue(true);

        new Thread(() -> {
            try {
                String clientVersion = web3Service.connect();
                connectionStatus.postValue("Connected to: " + clientVersion);

                tokenRepository.initializeTokenAddresses()
                        .thenAccept(addresses -> {
                            tokenAddresses.postValue(addresses);
                            isLoading.postValue(false);
                        });

            } catch (Exception e) {
                connectionStatus.postValue("Error: " + e.getMessage());
                isLoading.postValue(false);
            }
        }).start();
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
     * Clean up resources
     */
    @Override
    protected void onCleared() {
        super.onCleared();
        web3Service.shutdown();
    }

    // Getters for LiveData
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

    public void sendMoney(String address) {
        if(!web3Service.isConnected()){
            transactionResult.setValue(new TransactionResult(false,null,"Connect to Ethereum first!"));
            return;
        }

        isLoading.setValue(true);

        exchangeRepository.sendTransaction(address)
                .thenAccept(result -> {
                    transactionResult.postValue(result);
                    isLoading.postValue(false);
                });

    }
}