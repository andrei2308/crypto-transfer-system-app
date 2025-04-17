package com.example.crypto_payment_system.ui.liquidity;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.example.crypto_payment_system.R;
import com.example.crypto_payment_system.models.TokenBalance;
import com.example.crypto_payment_system.repositories.TokenRepository;
import com.example.crypto_payment_system.viewmodels.MainViewModel;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;

public class AddLiquidityFragment extends Fragment {

    private MainViewModel viewModel;
    private Spinner currencySpinner;
    private TextInputEditText amountEditText;
    private TextView resultTextView;
    private ProgressBar progressBar;
    private Button addLiquidityButton;
    private ArrayAdapter<String> currencyAdapter;

    // Wallet balance views
    private TextView eurBalanceValue;
    private TextView usdBalanceValue;

    // Contract balance views
    private TextView contractEurBalanceValue;
    private TextView contractUsdBalanceValue;
    private Button refreshBalanceButton;

    private Observer<TokenRepository.TransactionResult> transactionObserver;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_add_liquidity, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);

        currencySpinner = view.findViewById(R.id.currencySpinner);
        amountEditText = view.findViewById(R.id.amountEditText);
        resultTextView = view.findViewById(R.id.resultTextView);
        progressBar = view.findViewById(R.id.progressBar);
        addLiquidityButton = view.findViewById(R.id.addLiquidityButton);

        // Initialize wallet balance views
        eurBalanceValue = view.findViewById(R.id.eurBalanceValue);
        usdBalanceValue = view.findViewById(R.id.usdBalanceValue);

        // Initialize contract balance views
        contractEurBalanceValue = view.findViewById(R.id.contractEurBalanceValue);
        contractUsdBalanceValue = view.findViewById(R.id.contractUsdBalanceValue);
        refreshBalanceButton = view.findViewById(R.id.refreshBalanceButton);

        refreshBalanceButton.setOnClickListener(v -> refreshBalances());

        setupCurrencySpinner();

        addLiquidityButton.setOnClickListener(v -> {
            if (currencySpinner.getSelectedItem() != null) {
                String selectedCurrency = currencySpinner.getSelectedItem().toString();
                String amount = Objects.requireNonNull(amountEditText.getText()).toString();

                if (!amount.isEmpty()) {
                    addLiquidity(selectedCurrency, amount);
                } else {
                    Toast.makeText(requireContext(), R.string.please_enter_an_amount, Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(requireContext(), R.string.please_select_a_currency, Toast.LENGTH_SHORT).show();
            }
        });

        observeViewModel();

        refreshBalances();
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

        if (currencyAdapter.getCount() > 0) {
            currencySpinner.setSelection(0);
        }
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

    private void addLiquidity(String currency, String amount) {
        resultTextView.setText(getString(R.string.adding_liquidity) + amount + " " + currency + "...");

        showLoading(true);

        viewModel.addLiquidity(currency, amount);
    }

    private void observeViewModel() {
        if (transactionObserver != null) {
            viewModel.getTransactionResult().removeObserver(transactionObserver);
        }

        transactionObserver = result -> {
            showLoading(false);

            if (result == null) return;

            if (result.isSuccess()) {
                resultTextView.setText(getString(R.string.added) +
                        amountEditText.getText().toString().trim() + " " +
                        currencySpinner.getSelectedItem().toString() +
                        getString(R.string.transaction_id) + result.getTransactionHash());

                amountEditText.setText("");

                refreshBalances();
            } else {
                resultTextView.setText(getString(R.string.transaction_failed) + result.getMessage());
            }
        };

        viewModel.getTransactionResult().observe(getViewLifecycleOwner(), transactionObserver);

        viewModel.getTokenBalances().observe(getViewLifecycleOwner(), this::updateWalletBalanceUI);

        viewModel.getTokenBalances().observe(getViewLifecycleOwner(), this::updateContractBalanceUI);
    }

    private void refreshBalances() {
        viewModel.checkAllBalances();
    }

    private void updateWalletBalanceUI(Map<String, TokenBalance> balances) {
        if (balances == null) return;

        if (balances.containsKey("EURC")) {
            eurBalanceValue.setText(balances.get("EURC").getFormattedWalletBalance() + " EUR");
        }

        if (balances.containsKey("USDT")) {
            usdBalanceValue.setText(balances.get("USDT").getFormattedWalletBalance() + " USD");
        }
    }

    private void updateContractBalanceUI(Map<String, TokenBalance> balances) {
        if (balances == null) return;

        if (balances.containsKey("EURC")) {
            contractEurBalanceValue.setText(balances.get("EURC").getFormattedContractBalance() + " EUR");
        }

        if (balances.containsKey("USDT")) {
            contractUsdBalanceValue.setText(balances.get("USDT").getFormattedContractBalance() + " USD");
        }
    }

    private void showLoading(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        addLiquidityButton.setEnabled(!isLoading);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (transactionObserver != null) {
            viewModel.getTransactionResult().removeObserver(transactionObserver);
        }
    }
}