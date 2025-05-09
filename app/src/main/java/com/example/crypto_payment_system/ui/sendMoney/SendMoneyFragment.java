package com.example.crypto_payment_system.ui.sendMoney;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
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
import com.example.crypto_payment_system.utils.web3.TransactionResult;
import com.example.crypto_payment_system.view.viewmodels.MainViewModel;
import com.google.android.material.textfield.TextInputEditText;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Objects;

public class SendMoneyFragment extends Fragment {

    private MainViewModel viewModel;
    private TextInputEditText addressTeit;
    private TextInputEditText amountTeit;
    private Spinner currencySpinner;
    private TextView resultTextView;
    private ProgressBar progressBar;
    private ArrayAdapter<String> currencyAdapter;
    private Observer<TransactionResult> transactionObserver;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_send_money, container, false);

        viewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);

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
        currencyAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                new ArrayList<>()
        );
        currencyAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        currencySpinner.setAdapter(currencyAdapter);

        updateCurrencySpinner();

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
        currencyAdapter.clear();
        currencyAdapter.add("EUR");
        currencyAdapter.add("USD");
        currencyAdapter.notifyDataSetChanged();
    }

    private void updateCurrencySpinner(String preferredCurrencies) {
        String[] currencies = preferredCurrencies.split(",");

        String currentSelection = null;
        if (currencySpinner.getSelectedItem() != null) {
            currentSelection = currencySpinner.getSelectedItem().toString();
        }

        currencyAdapter.clear();

        for (String currency : currencies) {
            String trimmedCurrency = currency.trim().toUpperCase();
            if (trimmedCurrency.equals("EUR") || trimmedCurrency.equals("USD")) {
                currencyAdapter.add(trimmedCurrency);
            }
        }

        currencyAdapter.notifyDataSetChanged();

        if (currentSelection != null) {
            for (int i = 0; i < currencyAdapter.getCount(); i++) {
                if (Objects.equals(currencyAdapter.getItem(i), currentSelection)) {
                    currencySpinner.setSelection(i);
                    return;
                }
            }
        }

        if (currencyAdapter.getCount() > 0) {
            currencySpinner.setSelection(0);
        }
    }

    private void resetCurrencySpinner() {
        currencyAdapter.clear();
        currencyAdapter.add("EUR");
        currencyAdapter.add("USD");
        currencyAdapter.notifyDataSetChanged();
        currencySpinner.setSelection(0);
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

            String currency = "EUR";
            if (currencySpinner.getSelectedItem() != null) {
                currency = currencySpinner.getSelectedItem().toString();
            }

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