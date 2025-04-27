package com.example.crypto_payment_system.config;

/**
 * Central location for all application constants
 */
public class Constants {
    // Network configuration
    public static final String LOCALCHAIN_URL = "http://10.0.2.2:8545";

    // Wallet configuration
    public static final String WALLET_ADDRESS = "0xf39Fd6e51aad88F6F4ce6aB8827279cffFb92266"; // Default Anvil first account
    public static final String PRIVATE_KEY = "0xac0974bec39a17e36ba4a6b4d238ff944bacb478cbed5efcae784d7bf4f2ff80"; // Default Anvil account private key

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
    public static final String DEFAULT_MINT_AMOUNT = "1000";
    public static final String DEFAULT_EXCHANGE_AMOUNT = "100";
    public static final String DEFAULT_SEND_AMOUNT = "300";
    public static final String DEFAULT_APPROVAL = "1000000000000000000000";

    // currency codes
    public static final int CURRENCY_EUR = 1;
    public static final int CURRENCY_USD = 2;

    public static final String EUR_TOKEN_CONTRACT_ADDRESS = "0xe7f1725e7734ce288f8367e1bb143e90bb3f0512";
    public static final String USD_TOKEN_CONTRACT_ADDRESS = "0x9fe46736679d2d9a65f0992f2272de9f3c7fa6e0";

    public static final String CONTRACT_ADDRESS = "0xCf7Ed3AccA5a467e9e704C703E8D87F634fB0Fc9";
}