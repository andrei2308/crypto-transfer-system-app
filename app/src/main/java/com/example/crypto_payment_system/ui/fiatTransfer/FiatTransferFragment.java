package com.example.crypto_payment_system.ui.fiatTransfer;

import static com.example.crypto_payment_system.config.Constants.EURSC;
import static com.example.crypto_payment_system.config.Constants.USDT;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.example.crypto_payment_system.R;
import com.example.crypto_payment_system.config.ApiConfig;
import com.example.crypto_payment_system.config.biometric.classes.TransactionAuthManager;
import com.example.crypto_payment_system.domain.currency.Currency;
import com.example.crypto_payment_system.domain.stripe.CreatePaymentIntentRequest;
import com.example.crypto_payment_system.domain.stripe.PaymentIntentResponse;
import com.example.crypto_payment_system.repositories.api.StripeRepository;
import com.example.crypto_payment_system.ui.transaction.TransactionResultFragment;
import com.example.crypto_payment_system.utils.adapter.currency.CurrencyAdapter;
import com.example.crypto_payment_system.utils.confirmation.ConfirmationRequest;
import com.example.crypto_payment_system.utils.currency.CurrencyManager;
import com.example.crypto_payment_system.utils.progress.TransactionProgressDialog;
import com.example.crypto_payment_system.utils.simpleFactory.RepositoryFactory;
import com.example.crypto_payment_system.utils.validations.Validate;
import com.example.crypto_payment_system.utils.web3.TransactionResult;
import com.example.crypto_payment_system.view.viewmodels.MainViewModel;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.stripe.android.PaymentConfiguration;
import com.stripe.android.paymentsheet.PaymentSheet;
import com.stripe.android.paymentsheet.PaymentSheetResult;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Fragment for handling fiat transfers with Stripe integration
 */
public class FiatTransferFragment extends Fragment {
    private static final String TAG = "FiatTransferFragment";
    private static final long PROGRESS_UPDATE_DELAY = 1000L;
    private static final long STATUS_CHECK_INTERVAL = 2000L;
    private static final long DIALOG_DISMISS_DELAY = 2000L;

    private final AtomicBoolean isTransactionInProgress = new AtomicBoolean(false);
    private TextInputEditText amountTeit;
    private Spinner currencySpinner;
    private Button initiateTransferBtn;
    private FrameLayout buttonProgressContainer;
    private ProgressBar progressBar;

    // Data and Logic Components
    private MainViewModel viewModel;
    private StripeRepository stripeRepository;
    private CurrencyAdapter currencyAdapter;
    private final AtomicBoolean isBlockchainOperationInProgress = new AtomicBoolean(false);
    // Handlers
    private final Handler uiHandler = new Handler(Looper.getMainLooper());

    // Transaction State
    private TransactionProgressDialog progressDialog;
    private final Handler statusCheckHandler = new Handler(Looper.getMainLooper());
    // UI Components
    private TextInputEditText recipientAddressTeit;
    private TransactionAuthManager authManager;
    private Observer<TransactionResult> transactionObserver;
    private Observer<ConfirmationRequest> confirmationObserver;
    private String pendingAmount;
    private String pendingRecipient;
    private PaymentSheet paymentSheet;
    private String currentPaymentIntentId;
    private String lastTransactionHash;
    private Runnable statusCheckRunnable;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initializeComponents();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_fiat_transfer, container, false);

        viewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);
        CurrencyManager.initialize(requireContext());

        initializeViews(root);
        setupListeners();
        setupObservers();

        return root;
    }

    private void initializeComponents() {
        authManager = new TransactionAuthManager(requireActivity());

        String stripePublishableKey = "pk_test_51Rcoc4Qv2MLQq3uKltqQP5Fsf5Oq7hcBVtbhkzdEndhKKlx0kAKrTAQhlAxhdJyUbdqOM3BMY6nLTNKRlTKrDsi300kI54yv35";
        PaymentConfiguration.init(requireContext(), stripePublishableKey);

        stripeRepository = RepositoryFactory.createStripeRepository(
                requireContext(), ApiConfig.BASE_URL, ApiConfig.USERNAME, ApiConfig.PASSWORD
        );

        paymentSheet = new PaymentSheet(this, this::onPaymentSheetResult);
    }

    private void initializeViews(View root) {
        recipientAddressTeit = root.findViewById(R.id.recipient_address_teit);
        amountTeit = root.findViewById(R.id.amount_teit);
        currencySpinner = root.findViewById(R.id.currency_spinner);
        initiateTransferBtn = root.findViewById(R.id.initiate_transfer_btn);
        buttonProgressContainer = root.findViewById(R.id.buttonProgressContainer);
        progressBar = root.findViewById(R.id.progressBar);

        initializeCurrencySpinner();
    }

    private void setupListeners() {
        initiateTransferBtn.setOnClickListener(v -> handleTransferClick());
    }

    private void setupObservers() {
        viewModel.getTransactionConfirmation().observe(getViewLifecycleOwner(), confirmationRequest -> {
            if (confirmationRequest != null) {
                showConfirmationDialog(confirmationRequest);
            }
        });
    }

    private void showConfirmationDialog(ConfirmationRequest request) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Confirm Transaction")
                .setMessage(request.getMessage())
                .setPositiveButton("Confirm", (dialog, which) -> {
                    showTransactionProgressDialog();
                    updateProgressIfShowing(TransactionProgressDialog.TransactionState.SUBMITTING);

                    request.getOnConfirm().run();
                })
                .setNegativeButton("Cancel", (dialog, which) -> {
                    handleMintingCanceled();
                })
                .setCancelable(false)
                .show();
    }

    private void initializeCurrencySpinner() {
        List<Currency> currencies = new ArrayList<>();
        Currency usdCurrency = new Currency("USD", "US DOLLAR", R.drawable.ic_flag_usd);
        currencies.add(usdCurrency);

        currencyAdapter = new CurrencyAdapter(requireContext(), currencies);
        currencySpinner.setAdapter(currencyAdapter);

        if (!currencies.isEmpty()) {
            currencySpinner.setSelection(0);
        }
    }

    private void handleTransferClick() {
        if (!validateInputs()) {
            return;
        }

        if (!isTransactionInProgress.compareAndSet(false, true)) {
            Toast.makeText(requireContext(),
                    "Transaction already in progress",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        initiateTransfer();
    }

    private void initiateTransfer() {
        showLoadingState(true);

        TransferDetails details = extractTransferDetails();
        if (details == null) {
            resetTransactionState();
            return;
        }

        createPaymentIntent(details);
    }

    private TransferDetails extractTransferDetails() {
        try {
            String recipientAddress = Objects.requireNonNull(recipientAddressTeit.getText())
                    .toString().trim();
            String amountStr = Objects.requireNonNull(amountTeit.getText())
                    .toString().trim();
            Currency selectedCurrency = currencyAdapter.getSelectedCurrency();

            if (selectedCurrency == null) {
                showError("Please select a currency");
                return null;
            }

            double amount = Double.parseDouble(amountStr);
            String stripeCurrency = mapToStripeCurrency(selectedCurrency.getCode());

            return new TransferDetails(recipientAddress, amount, stripeCurrency, selectedCurrency);
        } catch (Exception e) {
            Log.e(TAG, "Error extracting transfer details", e);
            showError("Invalid transfer details");
            return null;
        }
    }

    private String mapToStripeCurrency(String currencyCode) {
        switch (currencyCode) {
            case EURSC:
                return "eur";
            case USDT:
                return "usd";
            default:
                return "usd";
        }
    }

    private void createPaymentIntent(TransferDetails details) {
        authManager.authorizeCardPayment(String.valueOf(details.amount), details.currency.getCode(), new TransactionAuthManager.AuthCallback() {
            @Override
            public void onAuthorized() {
                CreatePaymentIntentRequest request = new CreatePaymentIntentRequest(
                        details.amount,
                        details.stripeCurrency,
                        details.recipientAddress
                );

                showMessage(String.format("Creating payment for %.2f %s",
                        details.amount, details.currency.getCode()));

                stripeRepository.createPaymentIntent(request)
                        .thenAccept(response -> runOnUiThread(() -> handlePaymentIntentSuccess(response)))
                        .exceptionally(throwable -> {
                            runOnUiThread(() -> handlePaymentIntentError(throwable));
                            return null;
                        });
            }

            @Override
            public void onDenied(String reason) {
                showLoadingState(false);
                Toast.makeText(getContext(), "Transaction cancelled: " + reason, Toast.LENGTH_SHORT).show();
                resetTransactionState();
            }
        });
    }

    private void handlePaymentIntentSuccess(PaymentIntentResponse response) {
        showLoadingState(false);
        currentPaymentIntentId = response.getPaymentIntentId();
        presentPaymentSheet(response);
    }

    private void handlePaymentIntentError(Throwable throwable) {
        showLoadingState(false);
        Log.e(TAG, "Payment intent creation failed", throwable);
        showError("Failed to initiate payment: " + throwable.getMessage());
        resetTransactionState();
    }

    private void presentPaymentSheet(PaymentIntentResponse response) {
        PaymentSheet.Configuration configuration = new PaymentSheet.Configuration.Builder("Crypto Payment System")
                .merchantDisplayName("Crypto Payment System")
                .allowsDelayedPaymentMethods(true)
                .build();

        paymentSheet.presentWithPaymentIntent(
                response.getClientSecret(),
                configuration
        );
    }

    private void onPaymentSheetResult(PaymentSheetResult result) {
        if (result instanceof PaymentSheetResult.Completed) {
            handlePaymentCompleted();
        } else if (result instanceof PaymentSheetResult.Canceled) {
            handlePaymentCanceled();
        } else if (result instanceof PaymentSheetResult.Failed) {
            handlePaymentFailed((PaymentSheetResult.Failed) result);
        }
    }

    private void handlePaymentCompleted() {
        showMessage("Payment submitted successfully!");

        pendingAmount = Objects.requireNonNull(amountTeit.getText()).toString();
        pendingRecipient = Objects.requireNonNull(recipientAddressTeit.getText()).toString().trim();

        isBlockchainOperationInProgress.set(true);

        executeMintAndTransfer();

        startPaymentStatusMonitoring();
    }

    private void executeMintAndTransfer() {

        viewModel.clearTransactionConfirmation();

        viewModel.clearTransactionState();

        if (transactionObserver != null) {
            viewModel.getTransactionResult().removeObserver(transactionObserver);
        }

        transactionObserver = new Observer<TransactionResult>() {
            private boolean isMintingPhase = true;

            @Override
            public void onChanged(TransactionResult result) {
                if (result == null) return;

                if (isMintingPhase) {
                    handleMintingResult(result);
                } else {
                    handleTransferResult(result);
                }
            }

            private void handleMintingResult(TransactionResult result) {
                if (result.isSuccess()) {
                    if (result.getTransactionHash() != null && progressDialog != null) {
                        progressDialog.setTransactionHash(result.getTransactionHash());
                    }

                    updateProgressIfShowing(TransactionProgressDialog.TransactionState.PENDING);
                    showMessage("Tokens minted successfully. Initiating transfer...");

                    isMintingPhase = false;
                    double amount = Double.parseDouble(pendingAmount);
                    BigDecimal decimalAmount = BigDecimal.valueOf(amount);
                    BigDecimal tokenUnits = decimalAmount.multiply(BigDecimal.valueOf(1_000_000));
                    String formattedAmount = tokenUnits.toBigInteger().toString();

                    uiHandler.postDelayed(() -> {
                        updateProgressIfShowing(TransactionProgressDialog.TransactionState.SUBMITTING);
                        viewModel.sendMoney(pendingRecipient.toLowerCase(), USDT, formattedAmount);
                    }, 500);
                } else {
                    handleBlockchainOperationFailure("Minting failed: " + result.getMessage());
                    viewModel.getTransactionResult().removeObserver(this);
                    isBlockchainOperationInProgress.set(false);
                }
            }

            private void handleTransferResult(TransactionResult result) {
                if (result.isSuccess()) {
                    if (result.getTransactionHash() != null) {
                        lastTransactionHash = result.getTransactionHash();
                        if (progressDialog != null) {
                            progressDialog.setTransactionHash(result.getTransactionHash());
                        }
                    }

                    updateProgressIfShowing(TransactionProgressDialog.TransactionState.CONFIRMING);

                    uiHandler.postDelayed(() -> {
                        updateProgressIfShowing(TransactionProgressDialog.TransactionState.CONFIRMED);
                        showMessage("Transfer completed successfully!");

                        viewModel.getTransactionResult().removeObserver(this);
                        transactionObserver = null;

                        isBlockchainOperationInProgress.set(false);

                        // Navigate to TransactionResultFragment after a short delay
                        uiHandler.postDelayed(() -> {
                            dismissProgressDialog();
                            navigateToTransactionResult(true, result.getMessage());
                        }, DIALOG_DISMISS_DELAY);
                    }, 1000);
                } else {
                    handleBlockchainOperationFailure("Transfer failed: " + result.getMessage());
                    viewModel.getTransactionResult().removeObserver(this);
                    transactionObserver = null;
                    isBlockchainOperationInProgress.set(false);
                }
            }
        };

        viewModel.getTransactionResult().observe(getViewLifecycleOwner(), transactionObserver);

        viewModel.mintTokens(USDT, pendingAmount);
    }

    private void navigateToTransactionResult(boolean success, String message) {
        String displayAmount = pendingAmount + " " + USDT;

        TransactionResultFragment resultFragment = TransactionResultFragment.newInstance(
                success,
                lastTransactionHash != null ? lastTransactionHash : "",
                displayAmount,
                "Fiat Transfer",
                System.currentTimeMillis(),
                message != null ? message : (success ? "Transaction completed successfully" : "Transaction failed")
        );

        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.content_main, resultFragment)
                .addToBackStack(null)
                .commit();

        resetTransactionState();
    }

    private void handleMintingCanceled() {
        isBlockchainOperationInProgress.set(false);
        dismissProgressDialog();
        resetTransactionState();
        showMessage("Transaction canceled");
    }

    private void handleBlockchainOperationFailure(String errorMessage) {
        Log.e(TAG, errorMessage);
        updateProgressIfShowing(TransactionProgressDialog.TransactionState.FAILED);
        stopPaymentStatusMonitoring();

        uiHandler.postDelayed(() -> {
            dismissProgressDialog();
            navigateToTransactionResult(false, errorMessage);
        }, DIALOG_DISMISS_DELAY);
    }

    private void handlePaymentCanceled() {
        showMessage("Payment canceled");
        resetTransactionState();
    }

    private void handlePaymentFailed(PaymentSheetResult.Failed failed) {
        showError("Payment failed: " + failed.getError().getLocalizedMessage());
        resetTransactionState();
    }

    private void showTransactionProgressDialog() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }

        try {
            progressDialog = new TransactionProgressDialog(requireContext());
            progressDialog.show();
            progressDialog.updateState(TransactionProgressDialog.TransactionState.PREPARING);

        } catch (Exception e) {
            Log.e(TAG, "Error showing transaction dialog", e);
            showError("Error displaying transaction progress");
            resetTransactionState();
        }
    }

    private void updateProgressIfShowing(TransactionProgressDialog.TransactionState state) {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.updateState(state);
        }
    }

    private void startPaymentStatusMonitoring() {
        if (currentPaymentIntentId == null) return;

        statusCheckRunnable = new Runnable() {
            @Override
            public void run() {
                checkPaymentStatus();
                statusCheckHandler.postDelayed(this, STATUS_CHECK_INTERVAL);
            }
        };

        statusCheckHandler.postDelayed(statusCheckRunnable, PROGRESS_UPDATE_DELAY);
    }

    private void checkPaymentStatus() {
        if (currentPaymentIntentId == null) return;

        stripeRepository.getPaymentIntentStatus(currentPaymentIntentId)
                .thenAccept(response -> runOnUiThread(() ->
                        updateProgressDialogBasedOnStatus(response.getStatus())))
                .exceptionally(throwable -> {
                    Log.e(TAG, "Failed to check payment status", throwable);
                    return null;
                });
    }

    private void updateProgressDialogBasedOnStatus(String status) {
        if (progressDialog == null || !progressDialog.isShowing()) return;

        if (isBlockchainOperationInProgress.get()) {
            return;
        }

        PaymentStatus paymentStatus = PaymentStatus.fromString(status);
        progressDialog.updateState(paymentStatus.getProgressState());

        if (paymentStatus.isFinal()) {
            handleFinalPaymentStatus(paymentStatus);
        }
    }

    private void handleFinalPaymentStatus(PaymentStatus status) {
        stopPaymentStatusMonitoring();

        if (!isBlockchainOperationInProgress.get()) {
            uiHandler.postDelayed(() -> {
                dismissProgressDialog();
                resetTransactionState();

                if (status == PaymentStatus.SUCCEEDED) {
                    showMessage("Payment completed successfully!");
                } else {
                    showError("Payment failed. Please try again.");
                }
            }, DIALOG_DISMISS_DELAY);
        }
    }

    private void stopPaymentStatusMonitoring() {
        if (statusCheckRunnable != null) {
            statusCheckHandler.removeCallbacks(statusCheckRunnable);
            statusCheckRunnable = null;
        }
    }

    private boolean validateInputs() {
        String recipientAddress = Objects.requireNonNull(recipientAddressTeit.getText())
                .toString().trim();
        String amountStr = Objects.requireNonNull(amountTeit.getText())
                .toString().trim();

        if (recipientAddress.isEmpty()) {
            recipientAddressTeit.setError("Please enter recipient address");
            return false;
        }

        if (amountStr.isEmpty()) {
            amountTeit.setError("Please enter an amount");
            return false;
        }

        if (!Validate.hasAmount(amountStr, USDT, viewModel)) {
            amountTeit.setError(getString(R.string.insufficient_balance));
            return false;
        }

        try {
            double amount = Double.parseDouble(amountStr);
            if (amount <= 0) {
                amountTeit.setError("Amount must be greater than zero");
                return false;
            }
        } catch (NumberFormatException e) {
            amountTeit.setError("Please enter a valid number");
            return false;
        }

        if (currencySpinner.getSelectedItem() == null) {
            showMessage("Please select a currency");
            return false;
        }

        return true;
    }

    private void showLoadingState(boolean isLoading) {
        initiateTransferBtn.setVisibility(isLoading ? View.INVISIBLE : View.VISIBLE);
        buttonProgressContainer.setVisibility(isLoading ? View.VISIBLE : View.GONE);
    }

    private void resetTransactionState() {
        isTransactionInProgress.set(false);
        isBlockchainOperationInProgress.set(false);
        currentPaymentIntentId = null;
        lastTransactionHash = null;
    }

    private void dismissProgressDialog() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
            progressDialog = null;
        }
    }

    private void runOnUiThread(Runnable action) {
        if (getActivity() != null) {
            requireActivity().runOnUiThread(action);
        }
    }

    private void showMessage(String message) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
    }

    private void showError(String error) {
        Toast.makeText(requireContext(), error, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onDestroyView() {
        cleanup();
        super.onDestroyView();
    }

    private void cleanup() {
        stopPaymentStatusMonitoring();
        uiHandler.removeCallbacksAndMessages(null);
        statusCheckHandler.removeCallbacksAndMessages(null);

        if (transactionObserver != null && viewModel != null) {
            viewModel.getTransactionResult().removeObserver(transactionObserver);
            transactionObserver = null;
        }

        if (confirmationObserver != null && viewModel != null) {
            viewModel.getTransactionConfirmation().removeObserver(confirmationObserver);
            confirmationObserver = null;
        }

        if (viewModel != null) {
            viewModel.clearTransactionConfirmation();
        }

        dismissProgressDialog();
        resetTransactionState();
    }

    /**
     * Enum for payment status mapping
     */
    private enum PaymentStatus {
        REQUIRES_PAYMENT_METHOD("requires_payment_method",
                TransactionProgressDialog.TransactionState.SUBMITTING, false),
        REQUIRES_CONFIRMATION("requires_confirmation",
                TransactionProgressDialog.TransactionState.SUBMITTING, false),
        PROCESSING("processing",
                TransactionProgressDialog.TransactionState.PENDING, false),
        REQUIRES_CAPTURE("requires_capture",
                TransactionProgressDialog.TransactionState.CONFIRMING, false),
        SUCCEEDED("succeeded",
                TransactionProgressDialog.TransactionState.CONFIRMED, true),
        CANCELED("canceled",
                TransactionProgressDialog.TransactionState.FAILED, true),
        FAILED("failed",
                TransactionProgressDialog.TransactionState.FAILED, true);

        private final String status;
        private final TransactionProgressDialog.TransactionState progressState;
        private final boolean isFinal;

        PaymentStatus(String status,
                      TransactionProgressDialog.TransactionState progressState,
                      boolean isFinal) {
            this.status = status;
            this.progressState = progressState;
            this.isFinal = isFinal;
        }

        static PaymentStatus fromString(String status) {
            for (PaymentStatus ps : values()) {
                if (ps.status.equals(status)) {
                    return ps;
                }
            }
            return FAILED;
        }

        TransactionProgressDialog.TransactionState getProgressState() {
            return progressState;
        }

        boolean isFinal() {
            return isFinal;
        }
    }

    /**
     * Data class for transfer details
     */
    private static class TransferDetails {
        final String recipientAddress;
        final double amount;
        final String stripeCurrency;
        final Currency currency;

        TransferDetails(String recipientAddress, double amount,
                        String stripeCurrency, Currency currency) {
            this.recipientAddress = recipientAddress;
            this.amount = amount;
            this.stripeCurrency = stripeCurrency;
            this.currency = currency;
        }
    }
}