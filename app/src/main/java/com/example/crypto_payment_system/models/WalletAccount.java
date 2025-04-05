package com.example.crypto_payment_system.models;

public class WalletAccount {
    private final String name;
    private final String address;
    private final String privateKey;

    public WalletAccount(String name, String address, String privateKey) {
        this.name = name;
        this.address = address;
        this.privateKey = privateKey;
    }

    public String getName() {
        return name;
    }

    public String getAddress() {
        return address;
    }

    public String getPrivateKey() {
        return privateKey;
    }
}
