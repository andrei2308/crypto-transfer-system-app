package com.example.crypto_payment_system.ui.fiatTransfer;

import static com.example.crypto_payment_system.config.Constants.EURSC;

import android.os.Bundle;
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
import com.example.crypto_payment_system.config.biometric.classes.TransactionAuthManager;
import com.example.crypto_payment_system.domain.currency.Currency;
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

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        authManager = new TransactionAuthManager(requireActivity());

        // TODO: Move this to BuildConfig or secure configuration
        PaymentConfiguration.init(requireContext(), "pk_test_your_stripe_publishable_key");

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
        currencyAdapter = new CurrencyAdapter(
                requireContext(),
                new ArrayList<>(CurrencyManager.getAvailableCurrencies())
        );
        currencySpinner.setAdapter(currencyAdapter);

        updateCurrencySpinner();

        currencySpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                Currency selectedCurrency = currencyAdapter.getItem(position);
                if (selectedCurrency != null) {
                    currencyAdapter.setSelectedCurrency(selectedCurrency.getCode());
                }
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
                // Do nothing
            }
        });

        viewModel.getCurrentUser().observe(getViewLifecycleOwner(), user -> {
            if (user != null) {
                String preferredCurrencies = user.getPreferredCurrency();
                if (preferredCurrencies != null && !preferredCurrencies.isEmpty()) {
                    updateCurrencySpinner(preferredCurrencies);
                }
            } else {
                resetCurrencySpinner();
            }
        });
    }

    private void updateCurrencySpinner() {
        List<Currency> currencies = new ArrayList<>(CurrencyManager.getAvailableCurrencies());
        currencyAdapter = new CurrencyAdapter(requireContext(), currencies);
        currencySpinner.setAdapter(currencyAdapter);

        for (int i = 0; i < currencyAdapter.getCount(); i++) {
            Currency currency = currencyAdapter.getItem(i);
            if (currency != null && EURSC.equals(currency.getCode())) {
                currencySpinner.setSelection(i);
                break;
            }
        }
    }

    private void updateCurrencySpinner(String preferredCurrencies) {
        String[] currencyCodes = preferredCurrencies.split(",");
        List<Currency> currencies = CurrencyManager.getCurrenciesByCodes(currencyCodes);

        String currentSelection = null;
        if (currencyAdapter != null && currencyAdapter.getSelectedCurrency() != null) {
            currentSelection = currencyAdapter.getSelectedCurrency().getCode();
        }

        currencyAdapter = new CurrencyAdapter(requireContext(), currencies);
        currencySpinner.setAdapter(currencyAdapter);

        if (currentSelection != null) {
            for (int i = 0; i < currencyAdapter.getCount(); i++) {
                Currency currency = currencyAdapter.getItem(i);
                if (currency != null && currency.getCode().equals(currentSelection)) {
                    currencySpinner.setSelection(i);
                    return;
                }
            }
        }

        if (!currencies.isEmpty()) {
            currencySpinner.setSelection(0);
            currencyAdapter.setSelectedCurrency(currencies.get(0).getCode());
        }
    }

    private void resetCurrencySpinner() {
        List<Currency> currencies = new ArrayList<>(CurrencyManager.getAvailableCurrencies());
        currencyAdapter = new CurrencyAdapter(requireContext(), currencies);
        currencySpinner.setAdapter(currencyAdapter);

        for (int i = 0; i < currencyAdapter.getCount(); i++) {
            Currency currency = currencyAdapter.getItem(i);
            if (currency != null && EURSC.equals(currency.getCode())) {
                currencySpinner.setSelection(i);
                break;
            }
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
        String selectedCurrency = currencySpinner.getSelectedItem() != null ?
                currencySpinner.getSelectedItem().toString() : "";

        // TODO: Implement transfer logic
        Toast.makeText(requireContext(),
                "Transfer initiated: " + amountStr + " " + selectedCurrency + " to " + recipientAddress,
                Toast.LENGTH_SHORT).show();
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

    private void onPaymentSheetResult(PaymentSheetResult paymentSheetResult) {
        if (paymentSheetResult instanceof PaymentSheetResult.Completed) {
            Toast.makeText(requireContext(), "Payment successful!", Toast.LENGTH_SHORT).show();
            // TODO: Process the successful payment
        } else if (paymentSheetResult instanceof PaymentSheetResult.Canceled) {
            Toast.makeText(requireContext(), "Payment canceled", Toast.LENGTH_SHORT).show();
            showLoadingState(false);
            isTransactionInProgress = false;
        } else if (paymentSheetResult instanceof PaymentSheetResult.Failed) {
            PaymentSheetResult.Failed failed = (PaymentSheetResult.Failed) paymentSheetResult;
            Toast.makeText(requireContext(),
                    "Payment failed: " + failed.getError().getLocalizedMessage(),
                    Toast.LENGTH_LONG).show();
            showLoadingState(false);
            isTransactionInProgress = false;
        }
    }

    @Override
    public void onDestroyView() {
        if (transactionObserver != null) {
            viewModel.getTransactionResult().removeObserver(transactionObserver);
            transactionObserver = null;
        }
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
        isTransactionInProgress = false;
        super.onDestroyView();
    }
}