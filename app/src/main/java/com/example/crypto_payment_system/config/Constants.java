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

    // Contract configuration
    public static final String CONTRACT_JSON_FILE = "Exchange.json";

    // Gas configuration
    public static final long DEFAULT_GAS_LIMIT = 300000;
    public static final long APPROVAL_GAS_LIMIT = 100000;

    // Transaction configuration
    public static final int MAX_TRANSACTION_ATTEMPTS = 40;
    public static final long TRANSACTION_POLL_INTERVAL = 15000; // 15 seconds

    // Token amounts
    public static final String DEFAULT_MINT_AMOUNT = "1000000000";
    public static final String DEFAULT_EXCHANGE_AMOUNT = "1000000";
    public static final String DEFAULT_APPROVAL_AMOUNT = "1000000000000";
}