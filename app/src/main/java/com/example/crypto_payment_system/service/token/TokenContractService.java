package com.example.crypto_payment_system.service.token;

import org.web3j.crypto.Credentials;

import java.math.BigInteger;

public interface TokenContractService {
    public String getTokenAddress(String methodName) throws Exception;
    public BigInteger getTokenBalance(String address, String tokenAddress) throws Exception;
    public BigInteger getContractTokenBalance(String methodName, String tokenAddress) throws Exception;
    public String mintTokens(String tokenAddress, BigInteger amount, Credentials credentials) throws Exception;
    public String checkAndApproveIfNeeded(String tokenAddress, BigInteger amount, Credentials credentials) throws Exception;
}
