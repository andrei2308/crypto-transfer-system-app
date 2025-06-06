package com.example.crypto_payment_system.repositories.token;

import static com.example.crypto_payment_system.config.Constants.ETH;
import static com.example.crypto_payment_system.config.Constants.EURSC;
import static com.example.crypto_payment_system.config.Constants.USDT;

import com.example.crypto_payment_system.config.Constants;
import com.example.crypto_payment_system.domain.token.TokenBalance;
import com.example.crypto_payment_system.service.token.TokenContractService;
import com.example.crypto_payment_system.service.web3.Web3Service;

import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.response.EthGetBalance;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Repository class handling token-related operations
 */
public class TokenRepositoryImpl implements TokenRepository {
    private final Web3Service web3Service;
    private final TokenContractService tokenService;
    private String eurcAddress = "";
    private String usdtAddress = "";

    public TokenRepositoryImpl(Web3Service web3Service, TokenContractService tokenService) {
        this.web3Service = web3Service;
        this.tokenService = tokenService;
    }

    /**
     * Initialize token addresses
     */
    @Override
    public CompletableFuture<Map<String, String>> initializeTokenAddresses() {
        return CompletableFuture.supplyAsync(() -> {
            Map<String, String> addresses = new HashMap<>();
            try {
                eurcAddress = Constants.EUR_TOKEN_CONTRACT_ADDRESS;
                addresses.put(EURSC, eurcAddress);

                try {
                    usdtAddress = Constants.USD_TOKEN_CONTRACT_ADDRESS;
                    addresses.put(USDT, usdtAddress);
                } catch (Exception e) {
                    usdtAddress = "Not available";
                    addresses.put(USDT, "Not available");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return addresses;
        });
    }

    /**
     * Get all token balances
     */
    @Override
    public CompletableFuture<Map<String, TokenBalance>> getAllBalances(Credentials credentials) {
        return CompletableFuture.supplyAsync(() -> {
            Map<String, TokenBalance> balances = new HashMap<>();

            try {
                String walletAddress = credentials.getAddress();
                Web3j web3j = web3Service.getWeb3j();

                // Get ETH balance
                EthGetBalance ethBalance = web3j.ethGetBalance(walletAddress, DefaultBlockParameterName.LATEST).send();
                BigInteger walletEthBalance = ethBalance.getBalance();
                BigInteger contractEthBalance = BigInteger.ZERO;

                balances.put(ETH, new TokenBalance(ETH, "native", walletEthBalance, contractEthBalance));

                // Get EUR token balances
                BigInteger walletEurcBalance = tokenService.getTokenBalance(walletAddress, eurcAddress);
                BigInteger contractEurcBalance = tokenService.getContractTokenBalance("getContractEurcBalance", eurcAddress);
                balances.put(EURSC, new TokenBalance(EURSC, eurcAddress, walletEurcBalance, contractEurcBalance));

                // Get USD token balances
                BigInteger walletUsdtBalance = tokenService.getTokenBalance(walletAddress, usdtAddress);
                BigInteger contractUsdtBalance = tokenService.getContractTokenBalance("getContractUsdtBalance", usdtAddress);
                balances.put(USDT, new TokenBalance(USDT, usdtAddress, walletUsdtBalance, contractUsdtBalance));

            } catch (Exception e) {
                e.printStackTrace();
            }

            return balances;
        });
    }

    public String getEurcAddress() {
        return eurcAddress;
    }

    public String getUsdtAddress() {
        return usdtAddress;
    }


}