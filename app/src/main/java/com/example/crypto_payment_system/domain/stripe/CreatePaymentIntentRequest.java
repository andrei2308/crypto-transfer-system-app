package com.example.crypto_payment_system.domain.stripe;

import com.google.gson.annotations.SerializedName;

public class CreatePaymentIntentRequest {
    @SerializedName("amount")
    private Double amount;

    @SerializedName("currency")
    private String currency;

    @SerializedName("recipientAddress")
    private String recipientAddress;

    public CreatePaymentIntentRequest(Double amount, String currency, String recipientAddress) {
        this.amount = amount;
        this.currency = currency;
        this.recipientAddress = recipientAddress;
    }

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getRecipientAddress() {
        return recipientAddress;
    }

    public void setRecipientAddress(String recipientAddress) {
        this.recipientAddress = recipientAddress;
    }
}
