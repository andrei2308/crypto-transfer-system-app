package com.example.crypto_payment_system.ui.liquidity;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.example.crypto_payment_system.R;
import com.example.crypto_payment_system.domain.currency.Currency;
import com.example.crypto_payment_system.domain.token.TokenBalance;
import com.example.crypto_payment_system.ui.transaction.TransactionResultFragment;
import com.example.crypto_payment_system.utils.adapter.currency.CurrencyAdapter;
import com.example.crypto_payment_system.utils.currency.CurrencyManager;
import com.example.crypto_payment_system.utils.web3.TransactionResult;
import com.example.crypto_payment_system.view.viewmodels.MainViewModel;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class AddLiquidityFragment extends Fragment {

    private MainViewModel viewModel;
    private Spinner currencySpinner;
    private TextInputEditText amountEditText;
    private ProgressBar progressBar;
    private Button addLiquidityButton;
    private CurrencyAdapter currencyAdapter;
    private FrameLayout buttonProgressContainer;

    // Wallet balance views
    private TextView eurBalanceValue;
    private TextView usdBalanceValue;

    // Contract balance views
    private TextView contractEurBalanceValue;
    private TextView contractUsdBalanceValue;
    private Button refreshBalanceButton;

    private Observer<TransactionResult> transactionObserver;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_add_liquidity, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);

        // Initialize CurrencyManager if not already initialized
        CurrencyManager.initialize(requireContext());

        currencySpinner = view.findViewById(R.id.currencySpinner);
        amountEditText = view.findViewById(R.id.amountEditText);
        progressBar = view.findViewById(R.id.progressBar);
        addLiquidityButton = view.findViewById(R.id.addLiquidityButton);

        // Initialize wallet balance views
        eurBalanceValue = view.findViewById(R.id.eurBalanceValue);
        usdBalanceValue = view.findViewById(R.id.usdBalanceValue);

        // Initialize contract balance views
        contractEurBalanceValue = view.findViewById(R.id.contractEurBalanceValue);
        contractUsdBalanceValue = view.findViewById(R.id.contractUsdBalanceValue);
        refreshBalanceButton = view.findViewById(R.id.refreshBalanceButton);
        buttonProgressContainer = view.findViewById(R.id.buttonProgressContainer);

        refreshBalanceButton.setOnClickListener(v -> refreshBalances());

        setupCurrencySpinner();

        addLiquidityButton.setOnClickListener(v -> {
            Currency selectedCurrency = currencyAdapter.getSelectedCurrency();
            String amount = Objects.requireNonNull(amountEditText.getText()).toString();

            if (selectedCurrency != null && !amount.isEmpty()) {
                addLiquidity(selectedCurrency.getCode(), amount);
            } else if (selectedCurrency == null) {
                Toast.makeText(requireContext(), R.string.please_select_a_currency, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(requireContext(), R.string.please_enter_an_amount, Toast.LENGTH_SHORT).show();
            }
        });

        observeViewModel();

        refreshBalances();
    }

    private void setupCurrencySpinner() {
        // Create adapter with all available currencies
        currencyAdapter = new CurrencyAdapter(
                requireContext(),
                new ArrayList<>(CurrencyManager.getAvailableCurrencies())
        );
        currencySpinner.setAdapter(currencyAdapter);

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
        // Get all available currencies
        List<Currency> currencies = new ArrayList<>(CurrencyManager.getAvailableCurrencies());
        
        // Create new adapter
        currencyAdapter = new CurrencyAdapter(requireContext(), currencies);
        currencySpinner.setAdapter(currencyAdapter);
        
        // Default selection: EUR
        if (currencies.size() > 0) {
            for (int i = 0; i < currencyAdapter.getCount(); i++) {
                Currency currency = currencyAdapter.getItem(i);
                if (currency != null && "EUR".equals(currency.getCode())) {
                    currencySpinner.setSelection(i);
                    break;
                }
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
        }
    }

    private void resetCurrencySpinner() {
        // Reset to all available currencies
        List<Currency> currencies = new ArrayList<>(CurrencyManager.getAvailableCurrencies());
        
        // Create new adapter
        currencyAdapter = new CurrencyAdapter(requireContext(), currencies);
        currencySpinner.setAdapter(currencyAdapter);
        
        // Default selection: EUR
        for (int i = 0; i < currencyAdapter.getCount(); i++) {
            Currency currency = currencyAdapter.getItem(i);
            if (currency != null && "EUR".equals(currency.getCode())) {
                currencySpinner.setSelection(i);
                break;
            }
        }
    }

    private void addLiquidity(String currency, String amount) {
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

            Currency selectedCurrency = currencyAdapter.getSelectedCurrency();
            String currencyCode = selectedCurrency != null ? selectedCurrency.getCode() : "???";
            String amount = amountEditText.getText().toString().trim();
            long timestamp = System.currentTimeMillis();

            TransactionResultFragment fragment = TransactionResultFragment.newInstance(
                result.isSuccess(),
                result.getTransactionHash() != null ? result.getTransactionHash() : "-",
                amount + " " + currencyCode,
                "Add Liquidity",
                timestamp,
                result.getMessage() != null ? result.getMessage() : ""
            );
            requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.content_main, fragment)
                .addToBackStack(null)
                .commit();

            if (result.isSuccess()) {
                amountEditText.setText("");
                refreshBalances();
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

        buttonProgressContainer.setVisibility(isLoading ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onDestroyView() {
        if (transactionObserver != null) {
            viewModel.getTransactionResult().removeObserver(transactionObserver);
        }
        super.onDestroyView();
    }
}