package com.example.crypto_payment_system.config;

/**
 * Central location for all application constants
 */
public class Constants {

    // Wallet configuration
    public static final String WALLET_ADDRESS = "0x95fd8bdd071f25a1baE9086b6f95Eeda9c3EBB78"; // Default Anvil first account

//    public static final String PRIVATE_KEY = "0x59c6995e998f97a5a0044966f0945389dc9e86dae88c7a8412f4603b6b78690d"; // anvil pkey
//
//    public static final String WALLET_ADDRESS = "0x70997970C51812dc3A010C7d01b50e0d17dc79C8"; // anvil

    // Contract configuration
    public static final String CONTRACT_JSON_FILE = "Exchange.json";

    // Gas configuration
    public static final long DEFAULT_GAS_LIMIT = 300000;
    public static final long APPROVAL_GAS_LIMIT = 100000;

    // Transaction configuration
    public static final int MAX_TRANSACTION_ATTEMPTS = 40;
    public static final long TRANSACTION_POLL_INTERVAL = 15000; // 15 seconds

    // Token amounts
    public static final String DEFAULT_APPROVAL = "1000000000000000000000";

    // currency codes
    public static final int CURRENCY_EUR = 1;
    public static final int CURRENCY_USD = 2;
    public static final String EURSC = "EURSC";
    public static final String USDT = "USDT";
    public static final String ADD_LIQUIDITY = "ADD_LIQUIDITY";
    public static final String EUR_TO_USD = "EUR_TO_USD";
    public static final String USD_TO_EUR = "USD_TO_EUR";
    public static final String USD_TO_EUR_TRANSFER = "USD_TO_EUR_TRANSFER";
    public static final String USD_TRANSFER = "USD_TRANSFER";
    public static final String EUR_TO_USD_TRANSFER = "EUR_TO_USD_TRANSFER";
    public static final String EUR_TRANSFER = "EUR_TRANSFER";
    public static final String ETH = "ETH";
    public static final String CONTRACT_CREATOR_ADDRESS = "0x95fd8bdd071f25a1baE9086b6f95Eeda9c3EBB78";
    public static final String MINT_USD = "MINT_USD";


    public static final String EUR_TOKEN_CONTRACT_ADDRESS = "0x08210F9170F89Ab7658F0B5E3fF39b0E03C594D4";
    public static final String USD_TOKEN_CONTRACT_ADDRESS = "0x5c3E80d8218f9e382CeBB017f4F3270f1AE2D8Fc";

    public static final String CONTRACT_ADDRESS = "0xb74710F2C45fB43949502cE7f702Cde35F4f1aCf";
}