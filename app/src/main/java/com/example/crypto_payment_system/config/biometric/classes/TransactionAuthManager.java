package com.example.crypto_payment_system.config.biometric.classes;

import androidx.annotation.NonNull;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import java.util.concurrent.Executor;

/**
 * Handles biometric authentication for transaction authorization only
 */
public class TransactionAuthManager {

    private final FragmentActivity activity;
    private final Executor executor;
    private BiometricPrompt biometricPrompt;

    public TransactionAuthManager(FragmentActivity activity) {
        this.activity = activity;
        this.executor = ContextCompat.getMainExecutor(activity);
    }

    /**
     * Check if biometric/PIN authentication is available
     */
    public boolean isAuthAvailable() {
        BiometricManager biometricManager = BiometricManager.from(activity);
        int authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG |
                BiometricManager.Authenticators.DEVICE_CREDENTIAL;
        return biometricManager.canAuthenticate(authenticators) == BiometricManager.BIOMETRIC_SUCCESS;
    }

    /**
     * Get authentication status message
     */
    public String getAuthStatusMessage() {
        BiometricManager biometricManager = BiometricManager.from(activity);
        int status = biometricManager.canAuthenticate(
                BiometricManager.Authenticators.BIOMETRIC_STRONG |
                        BiometricManager.Authenticators.DEVICE_CREDENTIAL
        );

        switch (status) {
            case BiometricManager.BIOMETRIC_SUCCESS:
                return "Authentication available";
            case BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE:
                return "No biometric hardware available";
            case BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE:
                return "Biometric hardware currently unavailable";
            case BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED:
                return "No biometric or PIN set up. Please set up authentication in Settings";
            default:
                return "Authentication not available";
        }
    }

    /**
     * Initialize biometric prompt
     */
    private void initializeBiometricPrompt(AuthCallback callback) {
        biometricPrompt = new BiometricPrompt(activity, executor, new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                super.onAuthenticationError(errorCode, errString);
                callback.onDenied("Authentication error: " + errString);
            }

            @Override
            public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);
                callback.onAuthorized();
            }

            @Override
            public void onAuthenticationFailed() {
                super.onAuthenticationFailed();
                callback.onDenied("Authentication failed - please try again");
            }
        });
    }

    /**
     * Authorize a generic transaction
     */
    public void authorizeTransaction(String title, String subtitle, String details, AuthCallback callback) {
        if (!isAuthAvailable()) {
            callback.onDenied("Authentication not available: " + getAuthStatusMessage());
            return;
        }

        initializeBiometricPrompt(callback);

        BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle(title)
                .setSubtitle(subtitle)
                .setDescription(details + "\n\nConfirm this transaction with your biometric or PIN")
                .setAllowedAuthenticators(
                        BiometricManager.Authenticators.BIOMETRIC_STRONG |
                                BiometricManager.Authenticators.DEVICE_CREDENTIAL
                )
                .build();

        biometricPrompt.authenticate(promptInfo);
    }

    /**
     * Authorize token transfer
     */
    public void authorizeTokenTransfer(String amount, String tokenSymbol, String toAddress, AuthCallback callback) {
        String title = "Authorize Transfer";
        String subtitle = "Send " + amount + " " + tokenSymbol;
        String details = "To: " + formatAddress(toAddress);

        authorizeTransaction(title, subtitle, details, callback);
    }

    /**
     * Authorize ETH transfer
     */
    public void authorizeLiquidityTransfer(String amount, String tokenSymbol, AuthCallback callback) {
        String title = "Authorize Transfer";
        String subtitle = "Add " + amount + " " + tokenSymbol + " as liquidity";
        String details = "Add liquidity to the contract";

        authorizeTransaction(title, subtitle, details, callback);
    }

    /**
     * Authorize token minting
     */
    public void authorizeMinting(String amount, String tokenSymbol, AuthCallback callback) {
        String title = "Authorize Minting";
        String subtitle = "Mint " + amount + " " + tokenSymbol;
        String details = "Mint new tokens to your account";

        authorizeTransaction(title, subtitle, details, callback);
    }

    /**
     * Authorize token swapping/exchange
     */
    public void authorizeTokenSwap(String fromAmount, String fromToken, AuthCallback callback) {
        String title = "Authorize Exchange";
        String subtitle = "Swap " + fromAmount + " " + fromToken;
        String details = "Exchange tokens at current market rate";

        authorizeTransaction(title, subtitle, details, callback);
    }

    /**
     * Authorize contract interaction
     */
    public void authorizeContractInteraction(String contractName, String functionName, String details, AuthCallback callback) {
        String title = "Authorize Contract Call";
        String subtitle = contractName + " - " + functionName;

        authorizeTransaction(title, subtitle, details, callback);
    }

    /**
     * Format address for display (show first 6 and last 4 characters)
     */
    private String formatAddress(String address) {
        if (address == null || address.length() < 10) {
            return address;
        }
        return address.substring(0, 6) + "..." + address.substring(address.length() - 4);
    }

    public interface AuthCallback {
        void onAuthorized();

        void onDenied(String reason);
    }
}