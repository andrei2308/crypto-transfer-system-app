package com.example.crypto_payment_system.domain.account;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class AccountNetworkInfo {
    private final String address;
    private final BigDecimal balance;
    private final long transactionCount;
    private final boolean hasActivity;
    private final String networkName;

    public AccountNetworkInfo(String address, BigDecimal balance, long transactionCount,
                              boolean hasActivity, String networkName) {
        this.address = address;
        this.balance = balance;
        this.transactionCount = transactionCount;
        this.hasActivity = hasActivity;
        this.networkName = networkName;
    }

    public String getAddress() { return address; }
    public BigDecimal getBalance() { return balance; }
    public long getTransactionCount() { return transactionCount; }
    public boolean hasActivity() { return hasActivity; }
    public String getNetworkName() { return networkName; }

    public String getFormattedBalance() {
        return balance.setScale(6, RoundingMode.HALF_UP).toString() + " ETH";
    }
}