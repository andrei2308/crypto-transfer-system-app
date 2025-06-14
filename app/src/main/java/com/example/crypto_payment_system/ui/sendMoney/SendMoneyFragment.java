package com.example.crypto_payment_system.ui.sendMoney;

import static com.example.crypto_payment_system.config.Constants.EURSC;

import android.annotation.SuppressLint;
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
import com.example.crypto_payment_system.domain.account.User;
import com.example.crypto_payment_system.domain.currency.Currency;
import com.example.crypto_payment_system.ui.transaction.TransactionResultFragment;
import com.example.crypto_payment_system.utils.adapter.currency.CurrencyAdapter;
import com.example.crypto_payment_system.utils.currency.CurrencyManager;
import com.example.crypto_payment_system.utils.progress.TransactionProgressDialog;
import com.example.crypto_payment_system.utils.web3.TransactionResult;
import com.example.crypto_payment_system.view.viewmodels.MainViewModel;
import com.google.android.material.textfield.TextInputEditText;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class SendMoneyFragment extends Fragment {

    private MainViewModel viewModel;
    private TextInputEditText addressTeit;
    private TextInputEditText amountTeit;
    private Spinner currencySpinner;
    private ProgressBar progressBar;
    private Button sendMoneyBtn;
    private FrameLayout buttonProgressContainer;
    private CurrencyAdapter currencyAdapter;
    private Observer<TransactionResult> transactionObserver;
    private TransactionProgressDialog progressDialog;
    private boolean isTransactionInProgress = false;
    private TransactionAuthManager authManager;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        authManager = new TransactionAuthManager(requireActivity());
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_send_money, container, false);

        viewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);

        CurrencyManager.initialize(requireContext());

        addressTeit = root.findViewById(R.id.address_teit);
        amountTeit = root.findViewById(R.id.amount_teit);
        sendMoneyBtn = root.findViewById(R.id.send_money_btn);
        currencySpinner = root.findViewById(R.id.currencySpinner);
        progressBar = root.findViewById(R.id.progressBar);
        buttonProgressContainer = root.findViewById(R.id.buttonProgressContainer);

        setupCurrencySpinner();

        viewModel.getCurrentUser().observe(getViewLifecycleOwner(), this::updateUI);

        sendMoneyBtn.setOnClickListener(view -> {
            if (isTransactionInProgress) {
                Toast.makeText(requireContext(), "Transaction already in progress", Toast.LENGTH_SHORT).show();
                return;
            }
            sendMoney();
        });

        return root;
    }

    private void setupCurrencySpinner() {
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

        sendMoneyBtn.postDelayed(() -> {
            if (progressDialog != null && progressDialog.isShowing()) {
                progressDialog.updateState(TransactionProgressDialog.TransactionState.SUBMITTING);
            }
        }, 1000);

        sendMoneyBtn.postDelayed(() -> {
            if (progressDialog != null && progressDialog.isShowing()) {
                progressDialog.updateState(TransactionProgressDialog.TransactionState.PENDING);
            }
        }, 2000);

        sendMoneyBtn.postDelayed(() -> {
            if (progressDialog != null && progressDialog.isShowing()) {
                progressDialog.updateState(TransactionProgressDialog.TransactionState.CONFIRMING);
            }
        }, 4000);
    }

    private void setupTransactionObserver(final double finalAmount, final String finalCurrency, final String finalAddress) {
        if (transactionObserver != null) {
            viewModel.getTransactionResult().removeObserver(transactionObserver);
        }

        transactionObserver = result -> {
            if (result == null) {
                return;
            }

            viewModel.getTransactionResult().removeObserver(transactionObserver);
            transactionObserver = null;

            isTransactionInProgress = false;

            showLoading(false);

            if (progressDialog != null && progressDialog.isShowing()) {
                if (result.isSuccess()) {
                    progressDialog.updateState(TransactionProgressDialog.TransactionState.CONFIRMED);
                    if (result.getTransactionHash() != null) {
                        progressDialog.setTransactionHash(result.getTransactionHash());
                    }
                } else {
                    progressDialog.updateState(TransactionProgressDialog.TransactionState.FAILED);
                }

                sendMoneyBtn.postDelayed(() -> {
                    if (progressDialog != null && progressDialog.isShowing()) {
                        progressDialog.dismiss();
                        progressDialog = null;
                    }
                }, 1500);
            }

            sendMoneyBtn.postDelayed(() -> {
                showTransactionResult(result, finalAmount, finalCurrency, finalAddress);
            }, 2000);

            if (result.isSuccess()) {
                addressTeit.setText("");
                amountTeit.setText("");
            }
        };

        viewModel.getTransactionResult().observe(getViewLifecycleOwner(), transactionObserver);
    }

    private void showTransactionResult(TransactionResult result, double finalAmount, String finalCurrency, String finalAddress) {
        long timestamp = System.currentTimeMillis();
        TransactionResultFragment fragment = TransactionResultFragment.newInstance(
                result.isSuccess(),
                result.getTransactionHash() != null ? result.getTransactionHash() : "-",
                finalAmount + " " + finalCurrency,
                "Send Money",
                timestamp,
                result.getMessage() != null ? result.getMessage() : "Sent to: " + finalAddress
        );
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.content_main, fragment)
                .addToBackStack(null)
                .commit();
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

    private void updateUI(User user) {
//        if (user != null) {
//
//        }
    }

    @SuppressLint("SetTextI18n")
    private void sendMoney() {
        String address = Objects.requireNonNull(addressTeit.getText()).toString().trim();
        String amountStr = Objects.requireNonNull(amountTeit.getText()).toString().trim();

        if (address.isEmpty()) {
            addressTeit.setError(getString(R.string.please_enter_a_valid_address));
            return;
        }

        if (amountStr.isEmpty()) {
            amountTeit.setError(getString(R.string.please_enter_an_amount));
            return;
        }

        try {
            double amount = Double.parseDouble(amountStr);
            if (amount <= 0) {
                amountTeit.setError(getString(R.string.amount_must_be_greater_than_zero));
                return;
            }


            Currency selectedCurrency = currencyAdapter.getSelectedCurrency();
            if (selectedCurrency == null) {
                selectedCurrency = CurrencyManager.getCurrencyByCode(EURSC);
            }

            String currency = selectedCurrency.getCode();

            authManager.authorizeTokenTransfer(amountStr, currency, address, new TransactionAuthManager.AuthCallback() {
                @Override
                public void onAuthorized() {
                    BigDecimal decimalAmount = BigDecimal.valueOf(amount);
                    BigDecimal tokenUnits = decimalAmount.multiply(BigDecimal.valueOf(1_000_000));
                    String formattedAmount = tokenUnits.toBigInteger().toString();

                    isTransactionInProgress = true;

                    viewModel.resetTransactionResult();

                    setupTransactionObserver(amount, currency, address);

                    showTransactionProgressDialog();

                    viewModel.sendMoney(address, currency, formattedAmount);
                }

                @Override
                public void onDenied(String reason) {
                    Toast.makeText(getContext(), "Transaction cancelled: " + reason, Toast.LENGTH_SHORT).show();
                }
            });
        } catch (NumberFormatException e) {
            amountTeit.setError("Please enter a valid number");
            isTransactionInProgress = false;
        }
    }

    private void showLoading(boolean isLoading) {
        progressBar.setVisibility(View.GONE);

        sendMoneyBtn.setEnabled(!isLoading);

        if (isLoading) {
            buttonProgressContainer.setVisibility(View.VISIBLE);
            buttonProgressContainer.setAlpha(1f);

            System.out.println("ShowLoading called with isLoading = true");
        } else {
            buttonProgressContainer.setVisibility(View.GONE);

            System.out.println("ShowLoading called with isLoading = false");
        }
    }
}