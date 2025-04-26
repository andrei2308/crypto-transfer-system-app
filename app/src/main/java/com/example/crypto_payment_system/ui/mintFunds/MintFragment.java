package com.example.crypto_payment_system.ui.mintFunds;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.example.crypto_payment_system.R;
import com.example.crypto_payment_system.domain.token.TokenBalance;
import com.example.crypto_payment_system.utils.web3.TransactionResult;
import com.example.crypto_payment_system.view.viewmodels.MainViewModel;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;

public class MintFragment extends Fragment {

    private MainViewModel viewModel;
    private Spinner mintCurrencySpinner;
    private TextInputEditText mintAmountEditText;
    private TextInputEditText walletAddressEditText;
    private TextView mintStatusTextView;
    private ProgressBar progressBar;
    private ArrayAdapter<String> currencyAdapter;
    private TextView ethBalanceValue;
    private TextView eurBalanceValue;
    private TextView usdBalanceValue;
    private Observer<TransactionResult> transactionObserver;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_mint_token, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mintCurrencySpinner = view.findViewById(R.id.mintCurrencySpinner);
        mintAmountEditText = view.findViewById(R.id.mintAmountEditText);
        walletAddressEditText = view.findViewById(R.id.walletAddressEditText);
        Button mintButton = view.findViewById(R.id.mintButton);
        mintStatusTextView = view.findViewById(R.id.mintStatusTextView);
        progressBar = view.findViewById(R.id.mintProgressBar);
        ethBalanceValue = view.findViewById(R.id.ethBalanceValue);
        eurBalanceValue = view.findViewById(R.id.eurBalanceValue);
        usdBalanceValue = view.findViewById(R.id.usdBalanceValue);
        Button refreshBalanceButton = view.findViewById(R.id.refreshBalanceButton);

        setupCurrencySpinner();

        mintButton.setOnClickListener(v -> mintFunds());

        refreshBalanceButton.setOnClickListener(v -> refreshBalances());

        observeViewModel();

        refreshBalances();
    }

    private void updateBalanceDisplay() {
        progressBar.setVisibility(View.VISIBLE);
        viewModel.checkAllBalances();
    }

    private void refreshBalances() {
        progressBar.setVisibility(View.VISIBLE);
        viewModel.checkAllBalances();
    }

    private void setupCurrencySpinner() {
        currencyAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                new ArrayList<>()
        );
        currencyAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mintCurrencySpinner.setAdapter(currencyAdapter);

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
        if (mintCurrencySpinner.getSelectedItem() != null) {
            currentSelection = mintCurrencySpinner.getSelectedItem().toString();
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
                    mintCurrencySpinner.setSelection(i);
                    return;
                }
            }
        }

        if (currencyAdapter.getCount() > 0) {
            mintCurrencySpinner.setSelection(0);
        }
    }

    private void resetCurrencySpinner() {
        currencyAdapter.clear();
        currencyAdapter.add("EUR");
        currencyAdapter.add("USD");
        currencyAdapter.notifyDataSetChanged();
        mintCurrencySpinner.setSelection(0);
    }

    private void observeViewModel() {
        if (transactionObserver != null) {
            viewModel.getTransactionResult().removeObserver(transactionObserver);
        }

        transactionObserver = result -> {
            progressBar.setVisibility(View.GONE);

            if (result == null) return;

            if (result.isSuccess()) {
                mintStatusTextView.setText(getString(R.string.transaction_successful_minted) +
                        mintAmountEditText.getText().toString().trim() + " " +
                        mintCurrencySpinner.getSelectedItem().toString() +
                        "\nTransaction ID: " + result.getTransactionHash());
                mintAmountEditText.setText("");

                refreshBalances();
            } else {
                mintStatusTextView.setText(getString(R.string.transaction_failed) + result.getMessage());
            }
        };

        viewModel.getTransactionResult().observe(getViewLifecycleOwner(), transactionObserver);

        viewModel.getTokenBalances().observe(getViewLifecycleOwner(), this::updateBalanceUI);
    }

    /**
     * Update the UI with the latest token balances
     */
    private void updateBalanceUI(Map<String, TokenBalance> balances) {
        progressBar.setVisibility(View.GONE);

        if (balances.containsKey("ETH")) {
            ethBalanceValue.setText(balances.get("ETH").getFormattedWalletBalance() + " ETH");
        } else {
            ethBalanceValue.setText("0 ETH");
        }

        if (balances.containsKey("EURC")) {
            eurBalanceValue.setText(balances.get("EURC").getFormattedWalletBalance() + " EUR");
        } else {
            eurBalanceValue.setText("0 EUR");
        }

        if (balances.containsKey("USDT")) {
            usdBalanceValue.setText(balances.get("USDT").getFormattedWalletBalance() + " USD");
        } else {
            usdBalanceValue.setText("0 USD");
        }
    }

    private void mintFunds() {
        String amountStr = Objects.requireNonNull(mintAmountEditText.getText()).toString().trim();
        String targetWalletAddress = Objects.requireNonNull(walletAddressEditText.getText()).toString().trim();

        if (amountStr.isEmpty()) {
            mintAmountEditText.setError("Please enter the amount to mint");
            return;
        }

        try {
            double amount = Double.parseDouble(amountStr);
            if (amount <= 0) {
                mintAmountEditText.setError("Amount must be greater than zero");
                return;
            }

            String currency = "EUR";
            if (mintCurrencySpinner.getSelectedItem() != null) {
                currency = mintCurrencySpinner.getSelectedItem().toString();
            }

            progressBar.setVisibility(View.VISIBLE);
            mintStatusTextView.setText(R.string.processing_transaction);

            if (!targetWalletAddress.isEmpty()) {
                // Uncomment when the method is implemented
                // viewModel.mintTokensToAddress(currency, String.valueOf(amount), targetWalletAddress);
            } else {
                viewModel.mintTokens(currency, String.valueOf(amount));
            }

        } catch (NumberFormatException e) {
            mintAmountEditText.setError("Please enter a valid amount");
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (transactionObserver != null) {
            viewModel.getTransactionResult().removeObserver(transactionObserver);
            transactionObserver = null;
        }
    }
}