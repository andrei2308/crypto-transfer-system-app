package com.example.crypto_payment_system.utils.confirmation;

public class ConfirmationRequest {
    private final String message;
    private final Runnable onConfirm;

    public ConfirmationRequest(String message, Runnable onConfirm) {
        this.message = message;
        this.onConfirm = onConfirm;
    }

    public String getMessage() {
        return message;
    }

    public void confirm() {
        onConfirm.run();
    }

    public Runnable getOnConfirm() {
        return onConfirm;
    }

}