package com.example.crypto_payment_system.repositories.exchange;

import static com.example.crypto_payment_system.config.Constants.ADD_LIQUIDITY;
import static com.example.crypto_payment_system.config.Constants.CONTRACT_ADDRESS;
import static com.example.crypto_payment_system.config.Constants.EUR_TO_USD;
import static com.example.crypto_payment_system.config.Constants.EUR_TO_USD_TRANSFER;
import static com.example.crypto_payment_system.config.Constants.EUR_TRANSFER;
import static com.example.crypto_payment_system.config.Constants.USDT;
import static com.example.crypto_payment_system.config.Constants.USD_TOKEN_CONTRACT_ADDRESS;
import static com.example.crypto_payment_system.config.Constants.USD_TO_EUR;
import static com.example.crypto_payment_system.config.Constants.USD_TO_EUR_TRANSFER;
import static com.example.crypto_payment_system.config.Constants.USD_TRANSFER;

import com.example.crypto_payment_system.service.firebase.firestore.FirestoreService;
import com.example.crypto_payment_system.service.web3.Web3Service;
import com.example.crypto_payment_system.contracts.ExchangeContract;
import com.example.crypto_payment_system.repositories.token.TokenRepositoryImpl;
import com.example.crypto_payment_system.utils.events.EventParser;
import com.example.crypto_payment_system.utils.token.TokenCostInfo;
import com.example.crypto_payment_system.utils.web3.TransactionResult;

import org.web3j.crypto.Credentials;

import java.math.BigInteger;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

/**
 * Repository class handling exchange operations
 */
public class ExchangeRepositoryImpl implements ExchangeRepository{
    private final Web3Service web3Service;
    private final ExchangeContract exchangeContract;
    private final TokenRepositoryImpl tokenRepository;
    private final FirestoreService firestoreService;

    public ExchangeRepositoryImpl(Web3Service web3Service, ExchangeContract exchangeContract, TokenRepositoryImpl tokenRepository, FirestoreService firestoreService) {
        this.web3Service = web3Service;
        this.exchangeContract = exchangeContract;
        this.tokenRepository = tokenRepository;
        this.firestoreService = firestoreService;
    }

    @Override
    public CompletableFuture<TransactionResult> addLiquidity(String currency, Credentials credentials, String tokenUnitAmount) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String tokenAddress = currency.equals(USDT) ?
                        tokenRepository.getUsdtAddress() : tokenRepository.getEurcAddress();

                int currencyCode = currency.equals(USDT) ? 2 : 1;

                BigInteger amountToAdd = new BigInteger(tokenUnitAmount);

                String txHash = exchangeContract.addLiquidity(tokenAddress, amountToAdd, credentials);
                CompletableFuture<EventParser.ExchangeInfo> exchangeInfo = web3Service.waitForTransactionReceipt(txHash);

                boolean success = exchangeInfo.get().getReceipt().isStatusOK();

                if (success) {
                    firestoreService.saveTransaction(credentials.getAddress(), ADD_LIQUIDITY,
                            tokenAddress, tokenUnitAmount, txHash, CONTRACT_ADDRESS, exchangeInfo.get().getExchangeRate(), currencyCode, currencyCode);
                }

                return new TransactionResult(success, txHash, success ?
                        "Liquidity added successfully" : "Adding liquidity failed");

            } catch (Exception e) {
                return new TransactionResult(false, null, "Error: " + e.getMessage());
            }
        });
    }

    /**
     * Exchange EUR to USD
     */
    @Override
    public CompletableFuture<TransactionResult> exchangeEurToUsd(String tokenAmount, Credentials credentials) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                BigInteger amount = new BigInteger(tokenAmount);
                String tokenAddress = tokenRepository.getEurcAddress();

                String txHash = exchangeContract.exchangeEurToUsd(
                        tokenAddress, amount, credentials);

                CompletableFuture<EventParser.ExchangeInfo> exchangeInfo = web3Service.waitForTransactionReceipt(txHash);
                System.out.println(exchangeInfo.get().getExchangeRate());
                System.out.println(exchangeInfo.get().getSendCurrency());
                System.out.println(exchangeInfo.get().getReceiveCurrency());
                boolean success = exchangeInfo.get().getReceipt().isStatusOK();

                if (success) {
                    firestoreService.saveTransaction(credentials.getAddress(), EUR_TO_USD,
                            tokenAddress, tokenAmount, txHash, credentials.getAddress(), exchangeInfo.get().getExchangeRate(), exchangeInfo.get().getSendCurrency(), exchangeInfo.get().getReceiveCurrency());
                }

                return new TransactionResult(success, txHash, success ?
                        "Exchange completed successfully" : "Exchange failed");

            } catch (Exception e) {
                return new TransactionResult(false, null, "Error: " + e.getMessage());
            }
        });
    }

    /**
     * Exchange USD to EUR
     */
    @Override
    public CompletableFuture<TransactionResult> exchangeUsdToEur(String tokenAmount, Credentials credentials) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                BigInteger amount = new BigInteger(tokenAmount);
                String tokenAddress = tokenRepository.getUsdtAddress();

                String txHash = exchangeContract.exchangeUsdToEur(tokenAddress, amount, credentials);
                CompletableFuture<EventParser.ExchangeInfo> exchangeInfo = web3Service.waitForTransactionReceipt(txHash);

                boolean success = exchangeInfo.get().getReceipt().isStatusOK();

                if (success) {
                    firestoreService.saveTransaction(credentials.getAddress(), USD_TO_EUR,
                            tokenAddress, tokenAmount, txHash, credentials.getAddress(), exchangeInfo.get().getExchangeRate(), exchangeInfo.get().getSendCurrency(), exchangeInfo.get().getReceiveCurrency());
                }

                return new TransactionResult(success, txHash, success ?
                        "Exchange completed successfully" : "Exchange failed");
            } catch (Exception e) {
                return new TransactionResult(false, null, "Error: " + e.getMessage());
            }
        });
    }

    @Override
    public CompletableFuture<TransactionResult> sendTransaction(String address, int sendCurrency, int receiveCurrency, Credentials credentials, String amount) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                BigInteger amountToSend = new BigInteger(amount);
                String tokenAddress;
                String transactionType;

                if (sendCurrency == 2) {
                    tokenAddress = tokenRepository.getUsdtAddress();
                    transactionType = receiveCurrency == 1 ? USD_TO_EUR_TRANSFER : USD_TRANSFER;
                } else {
                    tokenAddress = tokenRepository.getEurcAddress();
                    transactionType = receiveCurrency == 2 ? EUR_TO_USD_TRANSFER : EUR_TRANSFER;
                }

                String txHash = exchangeContract.sendMoney(amountToSend, address, sendCurrency, receiveCurrency, credentials);
                CompletableFuture<EventParser.ExchangeInfo> exchangeInfo = web3Service.waitForTransactionReceipt(txHash);
                System.out.println(exchangeInfo.get().getExchangeRate());
                System.out.println(exchangeInfo.get().getSendCurrency());
                System.out.println(exchangeInfo.get().getReceiveCurrency());
                boolean success = exchangeInfo.get().getReceipt().isStatusOK();

                if (success) {
                    firestoreService.saveTransaction(credentials.getAddress(), transactionType,
                            tokenAddress, amount, txHash, address, exchangeInfo.get().getExchangeRate(), exchangeInfo.get().getSendCurrency(), exchangeInfo.get().getReceiveCurrency());
                }

                return new TransactionResult(success, txHash, success ?
                        "Money sent successfully" : "Exchange failed");
            } catch (Exception e) {
                return new TransactionResult(false, null, "Error: " + e.getMessage());
            }
        });
    }

    /**
     * Mint tokens
     */
    @Override
    public CompletableFuture<TransactionResult> mintTokens(String currency, Credentials credentials, String amount) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                BigInteger mintAmount = new BigInteger(amount);
                String txHash = "";

                if (currency.equals(USDT)) {
                    BigInteger requiredEth = exchangeContract.getRequiredEthForUsd(mintAmount, credentials);
                    System.out.println(requiredEth);
                    BigInteger ethToSend = (requiredEth.multiply(BigInteger.valueOf(105)).divide(BigInteger.valueOf(100)));
                    System.out.println(ethToSend);
                    txHash = exchangeContract.mintUsdTokens(mintAmount, ethToSend, credentials);
                }

                CompletableFuture<EventParser.ExchangeInfo> exchangeInfo = web3Service.waitForTransactionReceipt(txHash);

                boolean success = exchangeInfo.get().getReceipt().isStatusOK();
                if(success) {
                    firestoreService.saveTransaction("0", "MINT_USD",
                            USD_TOKEN_CONTRACT_ADDRESS, amount, txHash, credentials.getAddress(), "1", -1, 1);
                }
                return new TransactionResult(success, txHash, success ?
                        "Tokens minted successfully" : "Token minting failed");

            } catch (Exception e) {
                e.printStackTrace();
                return new TransactionResult(false, null, "Error: " + e.getMessage());
            }
        });
    }

    /**
     * Get required cost for minting tokens
     */
    @Override
    public CompletableFuture<TokenCostInfo> getRequiredTokenCost(String currency, Credentials credentials, String amount) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                BigInteger mintAmount = new BigInteger(amount);

                if (currency.equals(USDT)) {
                    BigInteger requiredEth = exchangeContract.getRequiredEthForUsd(mintAmount, credentials);

                    BigInteger ethWithBuffer = requiredEth.multiply(BigInteger.valueOf(105)).divide(BigInteger.valueOf(100));
                    return new TokenCostInfo(mintAmount, ethWithBuffer);
                }
                throw new Exception("Unsupported currency: " + currency);
            } catch (Exception e) {
                e.printStackTrace();
                throw new CompletionException(e);
            }
        });
    }

}