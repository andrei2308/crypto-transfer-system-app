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
import com.example.crypto_payment_system.repositories.api.StripeRepositoryImpl;
import com.example.crypto_payment_system.utils.adapter.currency.CurrencyAdapter;
import com.example.crypto_payment_system.utils.currency.CurrencyManager;
import com.example.crypto_payment_system.utils.progress.TransactionProgressDialog;
import com.example.crypto_payment_system.utils.web3.TransactionResult;
import com.example.crypto_payment_system.view.viewmodels.MainViewModel;
import com.google.android.material.textfield.TextInputEditText;
import com.stripe.android.PaymentConfiguration;
import com.stripe.android.paymentsheet.PaymentSheet;
import com.stripe.android.paymentsheet.PaymentSheetResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class FiatTransferFragment extends Fragment {

    private MainViewModel viewModel;
    private StripeRepository stripeRepository;
    private TextInputEditText recipientAddressTeit;
    private TextInputEditText amountTeit;
    private Spinner currencySpinner;
    private CurrencyAdapter currencyAdapter;
    private Button initiateTransferBtn;
    private FrameLayout buttonProgressContainer;
    private ProgressBar progressBar;

    private TransactionProgressDialog progressDialog;
    private boolean isTransactionInProgress = false;
    private TransactionAuthManager authManager;
    private Observer<TransactionResult> transactionObserver;
    private PaymentSheet paymentSheet;
    private String currentPaymentIntentId;
    private Handler handler = new Handler(Looper.getMainLooper());
    private Handler statusCheckHandler = new Handler(Looper.getMainLooper());
    private Runnable statusCheckRunnable;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        authManager = new TransactionAuthManager(requireActivity());

        // TODO: Move this to BuildConfig or secure configuration
        PaymentConfiguration.init(requireContext(), "pk_test_51Rcoc4Qv2MLQq3uKltqQP5Fsf5Oq7hcBVtbhkzdEndhKKlx0kAKrTAQhlAxhdJyUbdqOM3BMY6nLTNKRlTKrDsi300kI54yv35");

        stripeRepository = new StripeRepositoryImpl(ApiConfig.BASE_URL,
                ApiConfig.USERNAME,
                ApiConfig.PASSWORD);

        paymentSheet = new PaymentSheet(this, this::onPaymentSheetResult);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_fiat_transfer, container, false);

        viewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);

        CurrencyManager.initialize(requireContext());

        initializeViews(root);
        setupListeners();

        return root;
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
        initiateTransferBtn.setOnClickListener(v -> {
            if (validateInputs() && !isTransactionInProgress) {
                initiateTransfer();
            }
        });
    }

    private void initializeCurrencySpinner() {
        List<Currency> currencies = new ArrayList<>();
        Currency usdCurrency = CurrencyManager.getCurrencyByCode(USDT);
        if (usdCurrency != null) {
            currencies.add(usdCurrency);
        }

        currencyAdapter = new CurrencyAdapter(requireContext(), currencies);
        currencySpinner.setAdapter(currencyAdapter);

        if (!currencies.isEmpty()) {
            currencySpinner.setSelection(0);
        }
    }

    private void showTransactionProgressDialog() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }

        progressDialog = new TransactionProgressDialog(requireContext());

        try {
            progressDialog.show();
            progressDialog.updateState(TransactionProgressDialog.TransactionState.PREPARING);

            simulateTransactionProgress();
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Error showing transaction dialog: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            isTransactionInProgress = false;
        }
    }

    private void simulateTransactionProgress() {

        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.updateState(TransactionProgressDialog.TransactionState.PREPARING);
        }

        initiateTransferBtn.postDelayed(() -> {
            if (progressDialog != null && progressDialog.isShowing()) {
                progressDialog.updateState(TransactionProgressDialog.TransactionState.SUBMITTING);
            }
        }, 1000);

        initiateTransferBtn.postDelayed(() -> {
            if (progressDialog != null && progressDialog.isShowing()) {
                progressDialog.updateState(TransactionProgressDialog.TransactionState.PENDING);
            }
        }, 2000);

        initiateTransferBtn.postDelayed(() -> {
            if (progressDialog != null && progressDialog.isShowing()) {
                progressDialog.updateState(TransactionProgressDialog.TransactionState.CONFIRMING);
            }
        }, 4000);
    }

    private void initiateTransfer() {
        isTransactionInProgress = true;
        showLoadingState(true);

        String recipientAddress = Objects.requireNonNull(recipientAddressTeit.getText()).toString().trim();
        String amountStr = Objects.requireNonNull(amountTeit.getText()).toString().trim();
        Currency selectedCurrency = currencyAdapter.getSelectedCurrency();

        if (selectedCurrency == null) {
            showLoadingState(false);
            Toast.makeText(requireContext(), "Please select a currency", Toast.LENGTH_SHORT).show();
            isTransactionInProgress = false;
            return;
        }

        double amount = Double.parseDouble(amountStr);

        String currencyForStripe = "";

        if (selectedCurrency.getCode().equals(EURSC)) {
            currencyForStripe = "eur";
        } else if (selectedCurrency.getCode().equals(USDT)) {
            currencyForStripe = "usd";
        }

        CreatePaymentIntentRequest request = new CreatePaymentIntentRequest(
                amount,
                currencyForStripe,
                recipientAddress
        );

        Toast.makeText(requireContext(),
                "Creating payment for " + amountStr + " " + selectedCurrency.getCode(),
                Toast.LENGTH_SHORT).show();

        stripeRepository.createPaymentIntent(request)
                .thenAccept(response -> {
                    requireActivity().runOnUiThread(() -> {
                        showLoadingState(false);
                        currentPaymentIntentId = response.getPaymentIntentId();
                        presentPaymentSheet(response);
                    });
                })
                .exceptionally(throwable -> {
                    requireActivity().runOnUiThread(() -> {
                        showLoadingState(false);
                        Log.e("FiatTransfer", "Payment intent creation failed", throwable);
                        Toast.makeText(requireContext(),
                                "Failed to initiate payment: " + throwable.getMessage(),
                                Toast.LENGTH_LONG).show();
                        isTransactionInProgress = false;
                    });
                    return null;
                });
    }

    private void onPaymentSheetResult(PaymentSheetResult paymentSheetResult) {
        if (paymentSheetResult instanceof PaymentSheetResult.Completed) {
            Toast.makeText(requireContext(), "Payment submitted successfully!", Toast.LENGTH_SHORT).show();
            showTransactionProgressDialog();
            startPaymentStatusMonitoring();

        } else if (paymentSheetResult instanceof PaymentSheetResult.Canceled) {
            Toast.makeText(requireContext(), "Payment canceled", Toast.LENGTH_SHORT).show();
            isTransactionInProgress = false;
            currentPaymentIntentId = null;

        } else if (paymentSheetResult instanceof PaymentSheetResult.Failed) {
            PaymentSheetResult.Failed failed = (PaymentSheetResult.Failed) paymentSheetResult;
            Toast.makeText(requireContext(),
                    "Payment failed: " + failed.getError().getLocalizedMessage(),
                    Toast.LENGTH_LONG).show();
            isTransactionInProgress = false;
            currentPaymentIntentId = null;
        }
    }

    private void startPaymentStatusMonitoring() {
        if (currentPaymentIntentId == null) return;

        statusCheckRunnable = new Runnable() {
            @Override
            public void run() {
                checkPaymentStatus();
                statusCheckHandler.postDelayed(this, 2000);
            }
        };

        statusCheckHandler.postDelayed(statusCheckRunnable, 1000);
    }

    private void checkPaymentStatus() {
        if (currentPaymentIntentId == null) return;

        stripeRepository.getPaymentIntentStatus(currentPaymentIntentId)
                .thenAccept(response -> {
                    requireActivity().runOnUiThread(() -> {
                        updateProgressDialogBasedOnStatus(response.getStatus());
                    });
                })
                .exceptionally(throwable -> {
                    Log.e("FiatTransfer", "Failed to check payment status", throwable);
                    return null;
                });
    }

    private void updateProgressDialogBasedOnStatus(String status) {
        if (progressDialog == null || !progressDialog.isShowing()) return;

        switch (status) {
            case "requires_payment_method":
            case "requires_confirmation":
                progressDialog.updateState(TransactionProgressDialog.TransactionState.SUBMITTING);
                break;

            case "processing":
                progressDialog.updateState(TransactionProgressDialog.TransactionState.PENDING);
                break;

            case "requires_capture":
                progressDialog.updateState(TransactionProgressDialog.TransactionState.CONFIRMING);
                break;

            case "succeeded":
                progressDialog.updateState(TransactionProgressDialog.TransactionState.CONFIRMED);
                stopPaymentStatusMonitoring();
                handler.postDelayed(() -> {
                    if (progressDialog != null && progressDialog.isShowing()) {
                        progressDialog.dismiss();
                    }
                    isTransactionInProgress = false;
                    currentPaymentIntentId = null;
                    Toast.makeText(requireContext(),
                            "Transfer completed successfully!",
                            Toast.LENGTH_LONG).show();
                }, 2000);
                break;

            case "canceled":
            case "failed":
                progressDialog.updateState(TransactionProgressDialog.TransactionState.FAILED);
                stopPaymentStatusMonitoring();
                handler.postDelayed(() -> {
                    if (progressDialog != null && progressDialog.isShowing()) {
                        progressDialog.dismiss();
                    }
                    isTransactionInProgress = false;
                    currentPaymentIntentId = null;
                    Toast.makeText(requireContext(),
                            "Transfer failed. Please try again.",
                            Toast.LENGTH_LONG).show();
                }, 2000);
                break;
        }
    }

    private void stopPaymentStatusMonitoring() {
        if (statusCheckRunnable != null) {
            statusCheckHandler.removeCallbacks(statusCheckRunnable);
            statusCheckRunnable = null;
        }
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

    private void showLoadingState(boolean isLoading) {
        if (isLoading) {
            initiateTransferBtn.setVisibility(View.INVISIBLE);
            buttonProgressContainer.setVisibility(View.VISIBLE);
        } else {
            initiateTransferBtn.setVisibility(View.VISIBLE);
            buttonProgressContainer.setVisibility(View.GONE);
        }
    }

    private boolean validateInputs() {
        String recipientAddress = Objects.requireNonNull(recipientAddressTeit.getText()).toString().trim();
        String amountStr = Objects.requireNonNull(amountTeit.getText()).toString().trim();

        if (recipientAddress.isEmpty()) {
            recipientAddressTeit.setError("Please enter recipient address");
            return false;
        }

        if (amountStr.isEmpty()) {
            amountTeit.setError("Please enter an amount");
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
            Toast.makeText(requireContext(), "Please select a currency", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    @Override
    public void onDestroyView() {
        stopPaymentStatusMonitoring();
        handler.removeCallbacksAndMessages(null);
        statusCheckHandler.removeCallbacksAndMessages(null);

        if (transactionObserver != null) {
            viewModel.getTransactionResult().removeObserver(transactionObserver);
            transactionObserver = null;
        }
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
            progressDialog = null;
        }
        isTransactionInProgress = false;
        currentPaymentIntentId = null;
        super.onDestroyView();
    }
}