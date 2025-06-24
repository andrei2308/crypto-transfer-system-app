package com.example.crypto_payment_system.domain.stripe;

import com.google.gson.annotations.SerializedName;

public class PaymentIntentResponse {
    @SerializedName("clientSecret")
    private String clientSecret;

    @SerializedName("paymentIntentId")
    private String paymentIntentId;

    @SerializedName("amount")
    private Long amount;

    @SerializedName("currency")
    private String currency;

    @SerializedName("status")
    private String status;

    // Getters and setters
    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public String getPaymentIntentId() {
        return paymentIntentId;
    }

    public void setPaymentIntentId(String paymentIntentId) {
        this.paymentIntentId = paymentIntentId;
    }

    public Long getAmount() {
        return amount;
    }

    public void setAmount(Long amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
