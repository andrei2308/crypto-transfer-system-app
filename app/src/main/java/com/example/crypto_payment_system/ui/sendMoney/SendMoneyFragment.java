package com.example.crypto_payment_system.ui.sendMoney;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.example.crypto_payment_system.R;
import com.example.crypto_payment_system.domain.account.User;
import com.example.crypto_payment_system.domain.currency.Currency;
import com.example.crypto_payment_system.utils.adapter.currency.CurrencyAdapter;
import com.example.crypto_payment_system.utils.currency.CurrencyManager;
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
    private TextView resultTextView;
    private ProgressBar progressBar;
    private CurrencyAdapter currencyAdapter;
    private Observer<TransactionResult> transactionObserver;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_send_money, container, false);

        viewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);

        // Initialize CurrencyManager if not already initialized
        CurrencyManager.initialize(requireContext());

        addressTeit = root.findViewById(R.id.address_teit);
        amountTeit = root.findViewById(R.id.amount_teit);
        Button sendMoneyBtn = root.findViewById(R.id.send_money_btn);
        currencySpinner = root.findViewById(R.id.currencySpinner);
        resultTextView = root.findViewById(R.id.resultTextView);
        progressBar = root.findViewById(R.id.progressBar);

        setupCurrencySpinner();

        viewModel.getCurrentUser().observe(getViewLifecycleOwner(), this::updateUI);

        sendMoneyBtn.setOnClickListener(view -> sendMoney());

        return root;
    }

    private void setupCurrencySpinner() {
        // Create adapter with all available currencies
        currencyAdapter = new CurrencyAdapter(
                requireContext(),
                new ArrayList<>(CurrencyManager.getAvailableCurrencies())
        );
        currencySpinner.setAdapter(currencyAdapter);

        // Setup default currencies
        updateCurrencySpinner();
        
        // Add item selection listener to handle currency changes
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
        // Default to showing all available currencies
        List<Currency> currencies = new ArrayList<>(CurrencyManager.getAvailableCurrencies());
        currencyAdapter = new CurrencyAdapter(requireContext(), currencies);
        currencySpinner.setAdapter(currencyAdapter);
        
        // Select EUR by default
        for (int i = 0; i < currencyAdapter.getCount(); i++) {
            Currency currency = currencyAdapter.getItem(i);
            if (currency != null && "EUR".equals(currency.getCode())) {
                currencySpinner.setSelection(i);
                break;
            }
        }
    }

    private void updateCurrencySpinner(String preferredCurrencies) {
        String[] currencyCodes = preferredCurrencies.split(",");
        List<Currency> currencies = CurrencyManager.getCurrenciesByCodes(currencyCodes);

        // Remember the previously selected currency code
        String currentSelection = null;
        if (currencyAdapter != null && currencyAdapter.getSelectedCurrency() != null) {
            currentSelection = currencyAdapter.getSelectedCurrency().getCode();
        }

        // Create a new adapter with the preferred currencies
        currencyAdapter = new CurrencyAdapter(requireContext(), currencies);
        currencySpinner.setAdapter(currencyAdapter);

        // Try to restore the previous selection
        if (currentSelection != null) {
            // Find the position of the previously selected currency
            for (int i = 0; i < currencyAdapter.getCount(); i++) {
                Currency currency = currencyAdapter.getItem(i);
                if (currency != null && currency.getCode().equals(currentSelection)) {
                    currencySpinner.setSelection(i);
                    return;
                }
            }
        }
        
        // If previous selection not found or no previous selection, select the first currency
        if (!currencies.isEmpty()) {
            currencySpinner.setSelection(0);
            currencyAdapter.setSelectedCurrency(currencies.get(0).getCode());
        }
    }

    private void resetCurrencySpinner() {
        // Reset to all available currencies
        List<Currency> currencies = new ArrayList<>(CurrencyManager.getAvailableCurrencies());
        currencyAdapter = new CurrencyAdapter(requireContext(), currencies);
        currencySpinner.setAdapter(currencyAdapter);
        
        // Select EUR by default
        for (int i = 0; i < currencyAdapter.getCount(); i++) {
            Currency currency = currencyAdapter.getItem(i);
            if (currency != null && "EUR".equals(currency.getCode())) {
                currencySpinner.setSelection(i);
                break;
            }
        }
    }

    @Override
    public void onDestroyView() {
        if (transactionObserver != null) {
            viewModel.getTransactionResult().removeObserver(transactionObserver);
        }
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

            // Get the selected currency
            Currency selectedCurrency = currencyAdapter.getSelectedCurrency();
            if (selectedCurrency == null) {
                // Fallback to EUR if no currency is selected
                selectedCurrency = CurrencyManager.getCurrencyByCode("EUR");
            }
            
            String currency = selectedCurrency.getCode();

            BigDecimal decimalAmount = BigDecimal.valueOf(amount);
            BigDecimal tokenUnits = decimalAmount.multiply(BigDecimal.valueOf(1_000_000));
            String formattedAmount = tokenUnits.toBigInteger().toString();

            progressBar.setVisibility(View.VISIBLE);
            resultTextView.setText(getString(R.string.processing_transaction));

            if (transactionObserver != null) {
                viewModel.getTransactionResult().removeObserver(transactionObserver);
            }

            viewModel.resetTransactionResult();

            final double finalAmount = amount;
            final String finalCurrency = currency;
            transactionObserver = result -> {
                progressBar.setVisibility(View.GONE);
                if (result == null) {
                    return;
                }
                if (result.isSuccess()) {
                    resultTextView.setText(getString(R.string.transaction_successful_sent) + finalAmount + " " +
                            finalCurrency + " to " + address +
                            "\nTransaction ID: " + result.getTransactionHash());

                    addressTeit.setText("");
                    amountTeit.setText("");
                } else {
                    resultTextView.setText("Transaction failed: " + result.getMessage());
                }
            };

            viewModel.getTransactionResult().observe(getViewLifecycleOwner(), transactionObserver);

            viewModel.sendMoney(address, currency, formattedAmount);

        } catch (NumberFormatException e) {
            amountTeit.setError("Please enter a valid number");
        }
    }
}