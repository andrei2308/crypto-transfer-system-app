package com.example.crypto_payment_system.domain.stripe;

import com.google.gson.annotations.SerializedName;

public class PaymentIntentStatusResponse {
    @SerializedName("paymentIntentId")
    private String paymentIntentId;

    @SerializedName("status")
    private String status;

    @SerializedName("amount")
    private Long amount;

    @SerializedName("currency")
    private String currency;

    // Getters and setters
    public String getPaymentIntentId() {
        return paymentIntentId;
    }

    public void setPaymentIntentId(String paymentIntentId) {
        this.paymentIntentId = paymentIntentId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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
}