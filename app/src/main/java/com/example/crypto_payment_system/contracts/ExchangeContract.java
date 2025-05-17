package com.example.crypto_payment_system.contracts;

import com.example.crypto_payment_system.utils.token.TokenCostInfo;

import org.web3j.crypto.Credentials;

import java.math.BigInteger;
import java.util.concurrent.CompletableFuture;

public interface ExchangeContract {
    public String addLiquidity(String tokenAddress, BigInteger amount, Credentials credentials) throws Exception;
    public String exchangeEurToUsd(String eurcAddress, BigInteger amount, Credentials credentials) throws Exception;
    public String exchangeUsdToEur(String usdtAddress, BigInteger amount, Credentials credentials) throws Exception;
    public String sendMoney(BigInteger amount, String address, int sendCurrency, int receiveCurrency, Credentials credentials) throws Exception;

    BigInteger getRequiredEthForUsd(BigInteger usdAmount, Credentials credentials) throws Exception;

    String mintUsdTokens(BigInteger usdAmount, BigInteger ethAmount, Credentials credentials) throws Exception;

}
