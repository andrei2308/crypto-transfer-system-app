package com.example.crypto_payment_system.utils.progress;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import com.example.crypto_payment_system.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class TransactionProgressDialog extends Dialog {

    public enum TransactionState {
        PREPARING("Preparing transaction...", 10),
        SUBMITTING("Submitting to blockchain...", 30),
        PENDING("Waiting for confirmation...", 60),
        CONFIRMING("Confirming transaction...", 80),
        CONFIRMED("Transaction confirmed!", 100),
        FAILED("Transaction failed", 0);

        private final String message;
        private final int progress;

        TransactionState(String message, int progress) {
            this.message = message;
            this.progress = progress;
        }

        public String getMessage() {
            return message;
        }

        public int getProgress() {
            return progress;
        }
    }

    private ProgressBar progressBar;
    private ProgressBar circularProgress;
    private TextView statusText;
    private TextView progressText;
    private TextView transactionHashText;
    private TransactionState currentState = TransactionState.PREPARING;
    private String transactionHash = "";
    public TransactionProgressDialog(@NonNull Context context) {
        super(context, R.style.TransactionProgressDialog);
        setCancelable(false);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_transaction_progress);

        initViews();
        updateUI();
    }

    private void initViews() {
        progressBar = findViewById(R.id.progressBar);
        circularProgress = findViewById(R.id.circularProgress);
        statusText = findViewById(R.id.statusText);
        progressText = findViewById(R.id.progressText);
        transactionHashText = findViewById(R.id.transactionHashText);

        // Add null checks for debugging
        if (statusText == null) {
            throw new RuntimeException("statusText is null - check if R.id.statusText exists in dialog_transaction_progress.xml");
        }
        if (progressText == null) {
            throw new RuntimeException("progressText is null - check if R.id.progressText exists in dialog_transaction_progress.xml");
        }
        if (progressBar == null) {
            throw new RuntimeException("progressBar is null - check if R.id.progressBar exists in dialog_transaction_progress.xml");
        }
    }

    public void updateState(TransactionState state) {
        this.currentState = state;
        updateUI();
    }

    public void setTransactionHash(String hash) {
        this.transactionHash = hash;
        updateTransactionHashUI();
    }

    private void updateUI() {
        // Add null checks before setting text
        if (statusText != null) {
            statusText.setText(currentState.getMessage());
        }
        if (progressText != null) {
            progressText.setText(currentState.getProgress() + "%");
        }
        if (progressBar != null) {
            progressBar.setProgress(currentState.getProgress());
        }

        switch (currentState) {
            case PREPARING:
            case SUBMITTING:
            case PENDING:
            case CONFIRMING:
                if (circularProgress != null) {
                    circularProgress.setVisibility(View.VISIBLE);
                }
                setCancelable(false);
                break;

            case CONFIRMED:
                if (circularProgress != null) {
                    circularProgress.setVisibility(View.GONE);
                }
                setCancelable(false);
                break;

            case FAILED:
                if (circularProgress != null) {
                    circularProgress.setVisibility(View.GONE);
                }
                setCancelable(false);
                break;
        }
    }

    private void updateTransactionHashUI() {
        if (!transactionHash.isEmpty()) {
            transactionHashText.setText("TX: " + transactionHash.substring(0, Math.min(10, transactionHash.length())) + "...");
            transactionHashText.setVisibility(View.VISIBLE);
        }
    }
}