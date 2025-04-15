package com.example.crypto_payment_system.contracts;

import com.example.crypto_payment_system.api.TokenContractService;
import com.example.crypto_payment_system.api.Web3Service;
import com.example.crypto_payment_system.config.Constants;

import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.abi.datatypes.generated.Uint8;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.RawTransaction;
import org.web3j.crypto.TransactionEncoder;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.utils.Numeric;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Class that handles interactions with the exchange contract
 */
public class ExchangeContract {
    private final Web3Service web3Service;
    private final TokenContractService tokenService;

    public ExchangeContract(Web3Service web3Service, TokenContractService tokenService) {
        this.web3Service = web3Service;
        this.tokenService = tokenService;
    }

    private Web3j getWeb3j() {
        return web3Service.getWeb3j();
    }

    private String getContractAddress() {
        return web3Service.getContractAddress();
    }

    /**
     * Add liquidity to the exchange contract
     */
    public String addLiquidity(String tokenAddress, BigInteger amount, Credentials credentials) throws Exception {
        String approvalTxHash = tokenService.checkAndApproveIfNeeded(tokenAddress, amount, credentials);
        if (approvalTxHash != null) {
            TransactionReceipt receipt = web3Service.waitForTransactionReceipt(approvalTxHash);
            if (!receipt.isStatusOK()) {
                throw new Exception("Token approval failed");
            }
        }

        Function function = new Function(
                "addLiquidity",
                Arrays.asList(
                        new Address(tokenAddress),
                        new Uint256(amount)
                ),
                Collections.emptyList()
        );

        return sendTransaction(credentials, function);
    }

    /**
     * Exchange EUR to USD
     */
    public String exchangeEurToUsd(String eurcAddress, BigInteger amount, Credentials credentials) throws Exception {
        String approvalTxHash = tokenService.checkAndApproveIfNeeded(eurcAddress, amount, credentials);
        if (approvalTxHash != null) {
            TransactionReceipt receipt = web3Service.waitForTransactionReceipt(approvalTxHash);
            if (!receipt.isStatusOK()) {
                throw new Exception("Token approval failed");
            }
        }

        Function function = new Function(
                "exchangeEurToUsd",
                List.of(new Uint256(amount)),
                List.of(new TypeReference<Uint256>() {
                })
        );

        return sendTransaction(credentials, function);
    }

    /**
     * Exchange USD to EUR
     */
    public String exchangeUsdToEur(String usdtAddress, BigInteger amount, Credentials credentials) throws Exception {
        String approvalTxHash = tokenService.checkAndApproveIfNeeded(usdtAddress, amount, credentials);
        if (approvalTxHash != null) {
            TransactionReceipt receipt = web3Service.waitForTransactionReceipt(approvalTxHash);
            if (!receipt.isStatusOK()) {
                throw new Exception("Token approval failed");
            }
        }

        Function function = new Function(
                "exchangeUsdToEur",
                List.of(new Uint256(amount)),
                List.of(new TypeReference<Uint256>() {
                })
        );

        return sendTransaction(credentials, function);
    }

    public String sendMoney(BigInteger amount, String address, int sendCurrency, int receiveCurrency, Credentials credentials) throws Exception {
        String approvalTxHash = tokenService.checkAndApproveIfNeeded(address, amount, credentials);
        if (approvalTxHash != null) {
            TransactionReceipt receipt = web3Service.waitForTransactionReceipt(approvalTxHash);
            if (!receipt.isStatusOK()) {
                throw new Exception("Token approval failed");
            }
        }

        Function function = new Function(
                "sendMoney",
                Arrays.asList(
                        new Uint256(amount),
                        new Address(address),
                        new Uint8(sendCurrency),
                        new Uint8(receiveCurrency)
                ),
                Collections.emptyList()
        );

        return sendTransaction(credentials, function);
    }

    /**
     * Helper method to send a transaction to the exchange contract
     */
    private String sendTransaction(Credentials credentials, Function function)
            throws Exception {

        Web3j web3j = getWeb3j();
        String contractAddress = getContractAddress();

        if (web3j == null) {
            throw new Exception("Web3j is not initialized");
        }

        if (contractAddress == null || contractAddress.isEmpty()) {
            throw new Exception("Contract address is not initialized");
        }

        String encodedFunction = FunctionEncoder.encode(function);

        BigInteger nonce = web3j.ethGetTransactionCount(
                credentials.getAddress(),
                DefaultBlockParameterName.LATEST
        ).sendAsync().get().getTransactionCount();

        BigInteger gasPrice = web3j.ethGasPrice().sendAsync().get().getGasPrice();
        BigInteger gasLimit = BigInteger.valueOf(Constants.DEFAULT_GAS_LIMIT);

        RawTransaction rawTransaction = RawTransaction.createTransaction(
                nonce,
                gasPrice,
                gasLimit,
                contractAddress,
                encodedFunction
        );

        byte[] signedMessage = TransactionEncoder.signMessage(rawTransaction, credentials);
        String hexValue = Numeric.toHexString(signedMessage);

        EthSendTransaction ethSendTransaction = web3j.ethSendRawTransaction(hexValue).sendAsync().get();

        if (ethSendTransaction.hasError()) {
            throw new Exception("Error sending transaction: " + ethSendTransaction.getError().getMessage());
        }

        return ethSendTransaction.getTransactionHash();
    }
}