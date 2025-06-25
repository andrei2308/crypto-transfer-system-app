package com.example.crypto_payment_system.domain.stripe;

import com.google.gson.annotations.SerializedName;

public class SetupIntentResponse {
    @SerializedName("clientSecret")
    private String clientSecret;

    @SerializedName("setupIntentId")
    private String setupIntentId;

    // Getters and setters
    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public String getSetupIntentId() {
        return setupIntentId;
    }

    public void setSetupIntentId(String setupIntentId) {
        this.setupIntentId = setupIntentId;
    }
}
