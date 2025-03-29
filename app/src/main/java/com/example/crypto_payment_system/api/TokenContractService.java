package com.example.crypto_payment_system.api;

import com.example.crypto_payment_system.config.Constants;

import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.generated.Uint256;
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
import java.util.concurrent.ExecutionException;

/**
 * Service class for token contract interactions
 */
public class TokenContractService {
    private Web3j web3j;
    private String contractAddress;
    private final Web3Service web3Service;

    public TokenContractService(Web3Service web3Service) {
        this.web3Service = web3Service;
        this.web3j = web3Service.getWeb3j();
        this.contractAddress = web3Service.getContractAddress();
    }

    public void updateReferences() {
        this.web3j = web3Service.getWeb3j();
        this.contractAddress = web3Service.getContractAddress();
    }

    /**
     * Get ERC20 token address from contract
     */
    public String getTokenAddress(String methodName) throws Exception {
        updateReferences();
        if (web3j == null || contractAddress == null) {
            throw new Exception("Not connected to Ethereum");
        }

        Function function = new Function(
                methodName,
                Collections.emptyList(),
                Arrays.asList(new TypeReference<Address>() {})
        );

        String encodedFunction = FunctionEncoder.encode(function);

        EthCall response = web3j.ethCall(
                Transaction.createEthCallTransaction(
                        Constants.WALLET_ADDRESS,
                        contractAddress,
                        encodedFunction
                ),
                DefaultBlockParameterName.LATEST
        ).sendAsync().get();

        List<Type> decodedResponse = FunctionReturnDecoder.decode(
                response.getValue(),
                function.getOutputParameters()
        );

        if (decodedResponse.isEmpty()) {
            throw new Exception("Failed to get token address from " + methodName);
        }

        return decodedResponse.get(0).getValue().toString();
    }

    /**
     * Get token balance for address
     */
    public BigInteger getTokenBalance(String address, String tokenAddress) throws Exception {
        Function function = new Function(
                "balanceOf",
                Arrays.asList(new Address(address)),
                Arrays.asList(new TypeReference<Uint256>() {})
        );

        String encodedFunction = FunctionEncoder.encode(function);

        EthCall response = web3j.ethCall(
                Transaction.createEthCallTransaction(
                        Constants.WALLET_ADDRESS,
                        tokenAddress,
                        encodedFunction
                ),
                DefaultBlockParameterName.LATEST
        ).sendAsync().get();

        List<Type> decodedResponse = FunctionReturnDecoder.decode(
                response.getValue(),
                function.getOutputParameters()
        );

        if (!decodedResponse.isEmpty()) {
            return (BigInteger) decodedResponse.get(0).getValue();
        }
        return BigInteger.ZERO;
    }

    /**
     * Get contract token balance
     */
    public BigInteger getContractTokenBalance(String methodName, String tokenAddress) throws Exception {
        Function function = new Function(
                methodName,
                Arrays.asList(new Address(tokenAddress)),
                Arrays.asList(new TypeReference<Uint256>() {})
        );

        String encodedFunction = FunctionEncoder.encode(function);

        EthCall response = web3j.ethCall(
                Transaction.createEthCallTransaction(
                        Constants.WALLET_ADDRESS,
                        contractAddress,
                        encodedFunction
                ),
                DefaultBlockParameterName.LATEST
        ).sendAsync().get();

        List<Type> decodedResponse = FunctionReturnDecoder.decode(
                response.getValue(),
                function.getOutputParameters()
        );

        if (!decodedResponse.isEmpty()) {
            return (BigInteger) decodedResponse.get(0).getValue();
        }
        return BigInteger.ZERO;
    }

    /**
     * Mint tokens
     */
    public String mintTokens(String tokenAddress, BigInteger amount) throws Exception {
        Credentials credentials = Credentials.create(Constants.PRIVATE_KEY);

        Function mintFunction = new Function(
                "mint",
                Arrays.asList(
                        new Address(credentials.getAddress()),
                        new Uint256(amount)
                ),
                Collections.emptyList()
        );

        return sendTransaction(credentials, tokenAddress, mintFunction, Constants.DEFAULT_GAS_LIMIT);
    }

    /**
     * Check and approve tokens if needed
     */
    public String checkAndApproveIfNeeded(String tokenAddress, BigInteger amount) throws Exception {
        Credentials credentials = Credentials.create(Constants.PRIVATE_KEY);

        BigInteger currentAllowance = getAllowance(credentials.getAddress(), tokenAddress);

        if (currentAllowance.compareTo(amount) < 0) {
            BigInteger approvalAmount = new BigInteger(Constants.DEFAULT_APPROVAL_AMOUNT);

            Function approveFunction = new Function(
                    "approve",
                    Arrays.asList(
                            new Address(contractAddress),
                            new Uint256(approvalAmount)
                    ),
                    Collections.emptyList()
            );

            return sendTransaction(credentials, tokenAddress, approveFunction, Constants.APPROVAL_GAS_LIMIT);
        }

        return null;
    }

    /**
     * Get current token allowance
     */
    private BigInteger getAllowance(String ownerAddress, String tokenAddress) throws Exception {
        Credentials credentials = Credentials.create(Constants.PRIVATE_KEY);

        Function allowanceFunction = new Function(
                "allowance",
                Arrays.asList(
                        new Address(ownerAddress),
                        new Address(contractAddress)
                ),
                Arrays.asList(new TypeReference<Uint256>() {})
        );

        String encodedAllowanceFunction = FunctionEncoder.encode(allowanceFunction);

        EthCall allowanceResponse = web3j.ethCall(
                Transaction.createEthCallTransaction(
                        credentials.getAddress(),
                        tokenAddress,
                        encodedAllowanceFunction
                ),
                DefaultBlockParameterName.LATEST
        ).sendAsync().get();

        List<Type> decodedAllowanceResponse = FunctionReturnDecoder.decode(
                allowanceResponse.getValue(),
                allowanceFunction.getOutputParameters()
        );

        if (!decodedAllowanceResponse.isEmpty()) {
            return (BigInteger) decodedAllowanceResponse.get(0).getValue();
        }

        return BigInteger.ZERO;
    }

    /**
     * Helper method to send a transaction
     */
    private String sendTransaction(Credentials credentials, String to, Function function, long gasLimit)
            throws InterruptedException, ExecutionException, Exception {

        String encodedFunction = FunctionEncoder.encode(function);

        BigInteger nonce = web3j.ethGetTransactionCount(
                credentials.getAddress(),
                DefaultBlockParameterName.LATEST
        ).sendAsync().get().getTransactionCount();

        BigInteger gasPrice = web3j.ethGasPrice().sendAsync().get().getGasPrice();

        RawTransaction rawTransaction = RawTransaction.createTransaction(
                nonce,
                gasPrice,
                BigInteger.valueOf(gasLimit),
                to,
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