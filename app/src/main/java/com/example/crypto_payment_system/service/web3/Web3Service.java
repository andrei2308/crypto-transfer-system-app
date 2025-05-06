package com.example.crypto_payment_system.service.web3;

import com.example.crypto_payment_system.utils.EventParser;

import org.web3j.protocol.Web3j;

import java.util.concurrent.CompletableFuture;

public interface Web3Service {
    public String connect() throws Exception;
    public CompletableFuture<EventParser.ExchangeInfo> waitForTransactionReceipt(String transactionHash)
            throws Exception;
    public void shutdown();
    public Web3j getWeb3j();
    public String getContractAddress();
    public boolean isConnected();

}
