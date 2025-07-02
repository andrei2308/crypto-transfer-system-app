package com.example.crypto_payment_system.contracts;

import com.example.crypto_payment_system.config.Constants;
import com.example.crypto_payment_system.service.token.TokenContractService;
import com.example.crypto_payment_system.service.web3.Web3Service;
import com.example.crypto_payment_system.utils.events.EventParser;

import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.abi.datatypes.generated.Uint8;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.RawTransaction;
import org.web3j.crypto.TransactionEncoder;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.EthCall;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.utils.Numeric;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Class that handles interactions with the exchange contract
 */
public class ExchangeContractImpl implements ExchangeContract {
    private final Web3Service web3Service;
    private final TokenContractService tokenService;

    public ExchangeContractImpl(Web3Service web3Service, TokenContractService tokenService) {
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
    @Override
    public String addLiquidity(String tokenAddress, BigInteger amount, Credentials credentials) throws Exception {
        String approvalTxHash = tokenService.checkAndApproveIfNeeded(tokenAddress, amount, credentials);
        if (approvalTxHash != null) {
            CompletableFuture<EventParser.ExchangeInfo> exchangeInfo = web3Service.waitForTransactionReceipt(approvalTxHash);
            if (!exchangeInfo.get().getReceipt().isStatusOK()) {
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
    @Override
    public String exchangeEurToUsd(String eurcAddress, BigInteger amount, Credentials credentials) throws Exception {
        String approvalTxHash = tokenService.checkAndApproveIfNeeded(eurcAddress, amount, credentials);
        if (approvalTxHash != null) {
            CompletableFuture<EventParser.ExchangeInfo> exchangeInfo = web3Service.waitForTransactionReceipt(approvalTxHash);
            if (!exchangeInfo.get().getReceipt().isStatusOK()) {
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
    @Override
    public String exchangeUsdToEur(String usdtAddress, BigInteger amount, Credentials credentials) throws Exception {
        String approvalTxHash = tokenService.checkAndApproveIfNeeded(usdtAddress, amount, credentials);
        if (approvalTxHash != null) {
            CompletableFuture<EventParser.ExchangeInfo> exchangeInfo = web3Service.waitForTransactionReceipt(approvalTxHash);
            if (!exchangeInfo.get().getReceipt().isStatusOK()) {
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

    @Override
    public String sendMoney(BigInteger amount, String address, int sendCurrency, int receiveCurrency, Credentials credentials) throws Exception {
        String approvalTxHash = tokenService.checkAndApproveIfNeeded(address, amount, credentials);
        if (approvalTxHash != null) {
            CompletableFuture<EventParser.ExchangeInfo> exchangeInfo = web3Service.waitForTransactionReceipt(approvalTxHash);
            if (!exchangeInfo.get().getReceipt().isStatusOK()) {
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
     * Improved transaction sending with better nonce management and gas estimation
     */
    private String sendTransaction(Credentials credentials, Function function) throws Exception {
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
                DefaultBlockParameterName.PENDING
        ).sendAsync().get().getTransactionCount();

        BigInteger gasPrice = web3j.ethGasPrice().sendAsync().get().getGasPrice();
        gasPrice = gasPrice.multiply(BigInteger.valueOf(110)).divide(BigInteger.valueOf(100));

        BigInteger gasLimit = estimateGasLimit(credentials.getAddress(), contractAddress, encodedFunction);

        System.out.println("Transaction details:");
        System.out.println("  From: " + credentials.getAddress());
        System.out.println("  To: " + contractAddress);
        System.out.println("  Nonce: " + nonce);
        System.out.println("  Gas Price: " + gasPrice);
        System.out.println("  Gas Limit: " + gasLimit);

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
            String errorMessage = ethSendTransaction.getError().getMessage();
            System.err.println("Transaction error: " + errorMessage);
            throw new Exception("Error sending transaction: " + errorMessage);
        }

        String txHash = ethSendTransaction.getTransactionHash();

        if (txHash == null || txHash.isEmpty()) {
            throw new Exception("Transaction hash is null or empty - transaction may not have been submitted");
        }

        System.out.println("Transaction submitted with hash: " + txHash);

        return txHash;
    }

    /**
     * Estimate gas limit with buffer
     */
    private BigInteger estimateGasLimit(String fromAddress, String contractAddress, String encodedFunction) {
        try {
            Web3j web3j = getWeb3j();

            BigInteger estimatedGas = web3j.ethEstimateGas(
                    Transaction.createFunctionCallTransaction(
                            fromAddress,
                            null,
                            BigInteger.ZERO,
                            BigInteger.ZERO,
                            contractAddress,
                            encodedFunction
                    )
            ).sendAsync().get().getAmountUsed();

            BigInteger gasWithBuffer = estimatedGas
                    .multiply(BigInteger.valueOf(100 + 20))
                    .divide(BigInteger.valueOf(100));

            System.out.println("Estimated gas: " + estimatedGas + ", with buffer: " + gasWithBuffer);
            return gasWithBuffer;

        } catch (Exception e) {
            System.err.println("Gas estimation failed, using default: " + e.getMessage());
            return BigInteger.valueOf(Constants.DEFAULT_GAS_LIMIT);
        }
    }

    /**
     * Calculate required ETH for minting USD tokens
     */
    @Override
    public BigInteger getRequiredEthForUsd(BigInteger usdAmount, Credentials credentials) throws Exception {
        Function getRequiredEthFunction = new Function(
                "getRequiredEthForUsd",
                Arrays.asList(new Uint256(usdAmount)),
                Arrays.asList(new TypeReference<Uint256>() {})
        );

        String encodedFunction = FunctionEncoder.encode(getRequiredEthFunction);

        Web3j web3j = getWeb3j();
        String contractAddress = getContractAddress();

        if (web3j == null) {
            throw new Exception("Web3j is not initialized");
        }

        if (contractAddress == null || contractAddress.isEmpty()) {
            throw new Exception("Contract address is not initialized");
        }

        EthCall ethCall = web3j.ethCall(
                Transaction.createEthCallTransaction(
                        credentials.getAddress(),
                        contractAddress,
                        encodedFunction
                ),
                DefaultBlockParameterName.LATEST
        ).sendAsync().get();

        if (ethCall.hasError()) {
            String errorMessage = ethCall.getError().getMessage();
            String errorData = ethCall.getValue();

            System.out.println("Error calling contract. Message: " + errorMessage);
            System.out.println("Error data: " + errorData);

            throw new Exception("Error calling contract: " + errorMessage);
        }

        List<Type> decoded = FunctionReturnDecoder.decode(
                ethCall.getValue(),
                getRequiredEthFunction.getOutputParameters()
        );

        if (decoded.isEmpty()) {
            throw new Exception("Failed to decode response");
        }

        return (BigInteger) decoded.get(0).getValue();
    }

    /**
     * Mint USD tokens (requires ETH)
     */
    @Override
    public String mintUsdTokens(BigInteger usdAmount, BigInteger ethAmount, Credentials credentials) throws Exception {
        Function mintFunction = new Function(
                "mintUsdTokens",
                Arrays.asList(new Uint256(usdAmount)),
                Collections.emptyList()
        );

        String encodedFunction = FunctionEncoder.encode(mintFunction);

        Web3j web3j = getWeb3j();
        String contractAddress = getContractAddress();

        if (web3j == null) {
            throw new Exception("Web3j is not initialized");
        }

        if (contractAddress == null || contractAddress.isEmpty()) {
            throw new Exception("Contract address is not initialized");
        }

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
                ethAmount,
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