package com.example.crypto_payment_system.utils.token;

import java.math.BigInteger;

public class TokenCostInfo {
    private final BigInteger tokenAmount;
    private final BigInteger requiredEth;

    public TokenCostInfo(BigInteger tokenAmount, BigInteger requiredEth) {
        this.tokenAmount = tokenAmount;
        this.requiredEth = requiredEth;
    }

    public BigInteger getTokenAmount() {
        return tokenAmount;
    }

    public BigInteger getRequiredEth() {
        return requiredEth;
    }
}