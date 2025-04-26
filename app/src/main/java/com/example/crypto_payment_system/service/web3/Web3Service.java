package com.example.crypto_payment_system.service.web3;

import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.TransactionReceipt;

public interface Web3Service {
    public String connect() throws Exception;
    public TransactionReceipt waitForTransactionReceipt(String transactionHash)
            throws Exception;
    public void shutdown();
    public Web3j getWeb3j();
    public String getContractAddress();
    public boolean isConnected();

}
