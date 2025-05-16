package com.example.crypto_payment_system.ui.exchange;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.FrameLayout;
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
import com.example.crypto_payment_system.domain.currency.Currency;
import com.example.crypto_payment_system.domain.token.TokenBalance;
import com.example.crypto_payment_system.utils.adapter.currency.CurrencyAdapter;
import com.example.crypto_payment_system.utils.currency.CurrencyManager;
import com.example.crypto_payment_system.utils.web3.TransactionResult;
import com.example.crypto_payment_system.view.viewmodels.MainViewModel;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class ExchangeFragment extends Fragment {

    private MainViewModel viewModel;
    private Spinner fromCurrencySpinner;
    private Spinner toCurrencySpinner;
    private TextInputEditText fromAmountEditText;
    private TextView exchangeRateValue;
    private TextView estimatedAmountValue;
    private TextView resultTextView;
    private ProgressBar progressBar;
    private Button calculateButton;
    private Button exchangeButton;
    private CurrencyAdapter fromCurrencyAdapter;
    private CurrencyAdapter toCurrencyAdapter;
    private FrameLayout buttonProgressContainer;

    private TextView eurBalanceValue;
    private TextView usdBalanceValue;
    private Button refreshBalanceButton;
    private boolean isSelectionInProgress = false;


    private Observer<TransactionResult> transactionObserver;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_exchange, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);

        // Initialize CurrencyManager if not already initialized
        CurrencyManager.initialize(requireContext());

        fromCurrencySpinner = view.findViewById(R.id.fromCurrencySpinner);
        toCurrencySpinner = view.findViewById(R.id.toCurrencySpinner);
        fromAmountEditText = view.findViewById(R.id.fromAmountEditText);
        exchangeRateValue = view.findViewById(R.id.exchangeRateValue);
        estimatedAmountValue = view.findViewById(R.id.estimatedAmountValue);
        resultTextView = view.findViewById(R.id.resultTextView);
        progressBar = view.findViewById(R.id.progressBar);
        calculateButton = view.findViewById(R.id.calculateButton);
        exchangeButton = view.findViewById(R.id.exchangeButton);
        buttonProgressContainer = view.findViewById(R.id.buttonProgressContainer);

        eurBalanceValue = view.findViewById(R.id.eurBalanceValue);
        usdBalanceValue = view.findViewById(R.id.usdBalanceValue);
        refreshBalanceButton = view.findViewById(R.id.refreshBalanceButton);

        refreshBalanceButton.setOnClickListener(v -> refreshBalances());

        setupCurrencySpinners();

        calculateButton.setOnClickListener(v -> calculateExchangeRate());

        exchangeButton.setOnClickListener(v -> {
            Currency fromCurrency = fromCurrencyAdapter.getSelectedCurrency();
            String amount = Objects.requireNonNull(fromAmountEditText.getText()).toString();

            if (fromCurrency != null && !amount.isEmpty()) {
                executeExchange(fromCurrency.getCode(), amount);
            } else {
                Toast.makeText(requireContext(), R.string.please_select_a_currency_and_enter_an_amount,
                        Toast.LENGTH_SHORT).show();
            }
        });

        observeViewModel();

        refreshBalances();
    }

    private void setupCurrencySpinners() {
        // Create adapters with all available currencies
        List<Currency> currencies = new ArrayList<>(CurrencyManager.getAvailableCurrencies());
        fromCurrencyAdapter = new CurrencyAdapter(requireContext(), currencies);
        toCurrencyAdapter = new CurrencyAdapter(requireContext(), currencies);
        
        fromCurrencySpinner.setAdapter(fromCurrencyAdapter);
        toCurrencySpinner.setAdapter(toCurrencyAdapter);

        fromCurrencySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (isSelectionInProgress) return;

                isSelectionInProgress = true;

                Currency selectedCurrency = fromCurrencyAdapter.getItem(position);
                fromCurrencyAdapter.setSelectedCurrency(selectedCurrency.getCode());

                // Select the other currency in the to-spinner
                for (int i = 0; i < toCurrencyAdapter.getCount(); i++) {
                    Currency currency = toCurrencyAdapter.getItem(i);
                    if (currency != null && !currency.getCode().equals(selectedCurrency.getCode())) {
                        toCurrencySpinner.setSelection(i);
                        toCurrencyAdapter.setSelectedCurrency(currency.getCode());
                        break;
                    }
                }

                isSelectionInProgress = false;
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Nothing to do
            }
        });

        toCurrencySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (isSelectionInProgress) return;

                isSelectionInProgress = true;

                Currency selectedCurrency = toCurrencyAdapter.getItem(position);
                toCurrencyAdapter.setSelectedCurrency(selectedCurrency.getCode());

                // Select the other currency in the from-spinner
                for (int i = 0; i < fromCurrencyAdapter.getCount(); i++) {
                    Currency currency = fromCurrencyAdapter.getItem(i);
                    if (currency != null && !currency.getCode().equals(selectedCurrency.getCode())) {
                        fromCurrencySpinner.setSelection(i);
                        fromCurrencyAdapter.setSelectedCurrency(currency.getCode());
                        break;
                    }
                }

                isSelectionInProgress = false;
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                //
            }
        });

        updateCurrencySpinners();

        viewModel.getCurrentUser().observe(getViewLifecycleOwner(), user -> {
            if (user != null) {
                String preferredCurrencies = user.getPreferredCurrency();
                if (preferredCurrencies != null && !preferredCurrencies.isEmpty()) {
                    updateCurrencySpinners(preferredCurrencies);
                }
            } else {
                resetCurrencySpinners();
            }
        });
    }

    private void updateCurrencySpinners() {
        // Get all available currencies
        List<Currency> currencies = new ArrayList<>(CurrencyManager.getAvailableCurrencies());
        
        // Create new adapters
        fromCurrencyAdapter = new CurrencyAdapter(requireContext(), currencies);
        toCurrencyAdapter = new CurrencyAdapter(requireContext(), currencies);
        
        // Set adapters
        fromCurrencySpinner.setAdapter(fromCurrencyAdapter);
        toCurrencySpinner.setAdapter(toCurrencyAdapter);

        // Default selection: From = EUR, To = USD if available
        if (currencies.size() > 0) {
            // Select first currency (EUR) for 'from'
            fromCurrencySpinner.setSelection(0);
            
            if (currencies.size() > 1) {
                // Select second currency (USD) for 'to'
                toCurrencySpinner.setSelection(1);
            }
        }

        enableExchangeFunctionality();
    }

    private void updateCurrencySpinners(String preferredCurrencies) {
        String[] currencyCodes = preferredCurrencies.split(",");
        List<Currency> currencies = CurrencyManager.getCurrenciesByCodes(currencyCodes);

        if (currencies.size() <= 1) {
            disableExchangeFunctionality(getString(R.string.exchange_unavailable_only_one_currency_is_configured));

            // Create adapters with the limited currencies
            fromCurrencyAdapter = new CurrencyAdapter(requireContext(), currencies);
            toCurrencyAdapter = new CurrencyAdapter(requireContext(), currencies);
            
            // Set adapters
            fromCurrencySpinner.setAdapter(fromCurrencyAdapter);
            toCurrencySpinner.setAdapter(toCurrencyAdapter);

            if (currencies.size() == 1) {
                fromCurrencySpinner.setSelection(0);
                toCurrencySpinner.setSelection(0);
            }

            return;
        }

        enableExchangeFunctionality();

        // Remember current selections if any
        Currency currentFromSelection = null;
        Currency currentToSelection = null;

        if (fromCurrencyAdapter != null && fromCurrencyAdapter.getSelectedCurrency() != null) {
            currentFromSelection = fromCurrencyAdapter.getSelectedCurrency();
        }
        
        if (toCurrencyAdapter != null && toCurrencyAdapter.getSelectedCurrency() != null) {
            currentToSelection = toCurrencyAdapter.getSelectedCurrency();
        }

        // Create new adapters with preferred currencies
        fromCurrencyAdapter = new CurrencyAdapter(requireContext(), currencies);
        toCurrencyAdapter = new CurrencyAdapter(requireContext(), currencies);
        
        // Set adapters
        fromCurrencySpinner.setAdapter(fromCurrencyAdapter);
        toCurrencySpinner.setAdapter(toCurrencyAdapter);

        // Try to restore previous selections
        boolean fromSelectionRestored = false;
        if (currentFromSelection != null) {
            for (int i = 0; i < fromCurrencyAdapter.getCount(); i++) {
                Currency currency = fromCurrencyAdapter.getItem(i);
                if (currency != null && currency.getCode().equals(currentFromSelection.getCode())) {
                    fromCurrencySpinner.setSelection(i);
                    fromSelectionRestored = true;
                    break;
                }
            }
        }

        boolean toSelectionRestored = false;
        if (currentToSelection != null) {
            for (int i = 0; i < toCurrencyAdapter.getCount(); i++) {
                Currency currency = toCurrencyAdapter.getItem(i);
                if (currency != null && currency.getCode().equals(currentToSelection.getCode())) {
                    toCurrencySpinner.setSelection(i);
                    toSelectionRestored = true;
                    break;
                }
            }
        }

        if (!fromSelectionRestored && currencies.size() > 0) {
            fromCurrencySpinner.setSelection(0);
        }

        if (!toSelectionRestored && currencies.size() > 1) {
            toCurrencySpinner.setSelection(1);
        }
    }

    private void resetCurrencySpinners() {
        List<Currency> currencies = new ArrayList<>(CurrencyManager.getAvailableCurrencies());
        
        fromCurrencyAdapter = new CurrencyAdapter(requireContext(), currencies);
        toCurrencyAdapter = new CurrencyAdapter(requireContext(), currencies);
        
        fromCurrencySpinner.setAdapter(fromCurrencyAdapter);
        toCurrencySpinner.setAdapter(toCurrencyAdapter);

        if (currencies.size() > 0) {
            fromCurrencySpinner.setSelection(0);
            
            if (currencies.size() > 1) {
                toCurrencySpinner.setSelection(1);
            }
        }
    }

    private void disableExchangeFunctionality(String message) {
        fromAmountEditText.setEnabled(false);
        calculateButton.setEnabled(false);
        exchangeButton.setEnabled(false);
        exchangeRateValue.setText("--");
        estimatedAmountValue.setText("--");
        resultTextView.setText(message);
    }

    private void enableExchangeFunctionality() {
        fromAmountEditText.setEnabled(true);
        calculateButton.setEnabled(true);
        exchangeButton.setEnabled(true);
        resultTextView.setText(R.string.exchange_transaction_results_will_appear_here);
    }

    @SuppressLint("DefaultLocale")
    private void calculateExchangeRate() {
//        String amount = Objects.requireNonNull(fromAmountEditText.getText()).toString();
//        if (amount.isEmpty()) {
//            fromAmountEditText.setError(getString(R.string.please_enter_a_valid_amount));
//            return;
//        }
//
//        Currency fromCurrency = fromCurrencyAdapter.getSelectedCurrency();
//        Currency toCurrency = toCurrencyAdapter.getSelectedCurrency();
//
//        if (fromCurrency == null || toCurrency == null) {
//            Toast.makeText(requireContext(), R.string.please_select_a_currency, Toast.LENGTH_SHORT).show();
//            return;
//        }
//
//        String fromCode = fromCurrency.getCode();
//        String toCode = toCurrency.getCode();
//
//        progressBar.setVisibility(View.VISIBLE);
//        viewModel.getExchangeRate(fromCode, toCode, amount).thenAccept(rate -> {
//            requireActivity().runOnUiThread(() -> {
//                progressBar.setVisibility(View.GONE);
//
//                if (rate > 0) {
//                    double estimatedAmount = Double.parseDouble(amount) * rate;
//                    exchangeRateValue.setText(String.format("1 %s = %.4f %s", fromCode, rate, toCode));
//                    estimatedAmountValue.setText(String.format("%.2f %s", estimatedAmount, toCode));
//                    exchangeButton.setEnabled(true);
//                } else {
//                    exchangeRateValue.setText(R.string.exchange_rate_not_available_for_selected_currencies);
//                    estimatedAmountValue.setText("--");
//                    exchangeButton.setEnabled(false);
//                }
//            });
//        });
    }

    private void executeExchange(String fromCurrency, String amount) {
        if (fromAmountEditText.getText() == null || fromAmountEditText.getText().toString().isEmpty()) {
            Toast.makeText(requireContext(), R.string.please_fill_in_all_fields, Toast.LENGTH_SHORT).show();
            return;
        }

        resultTextView.setText(getString(R.string.starting_exchange_for) + amount + " " + fromCurrency);
        showLoading(true);
        viewModel.exchangeBasedOnPreference(fromCurrency, amount);
    }

    private void observeViewModel() {
        if (transactionObserver != null) {
            viewModel.getTransactionResult().removeObserver(transactionObserver);
        }

        transactionObserver = result -> {
            showLoading(false);

            if (result == null) return;

            if (result.isSuccess()) {
                Currency fromCurrency = fromCurrencyAdapter.getSelectedCurrency();
                Currency toCurrency = toCurrencyAdapter.getSelectedCurrency();
                String amount = fromAmountEditText.getText().toString();

                resultTextView.setText(getString(R.string.exchanged) +
                        amount + " " + (fromCurrency != null ? fromCurrency.getCode() : "???") +
                        " to " + (toCurrency != null ? toCurrency.getCode() : "???") +
                        "\nTransaction ID: " + result.getTransactionHash());

                fromAmountEditText.setText("");
                exchangeRateValue.setText("--");
                estimatedAmountValue.setText("--");

                refreshBalances();
            } else {
                resultTextView.setText(getString(R.string.transaction_failed) + result.getMessage());
            }
        };

        viewModel.getTransactionResult().observe(getViewLifecycleOwner(), transactionObserver);

        viewModel.getTokenBalances().observe(getViewLifecycleOwner(), this::updateBalanceUI);
    }

    private void updateBalanceUI(Map<String, TokenBalance> balances) {
        progressBar.setVisibility(View.GONE);

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


    private void showLoading(boolean isLoading) {
        progressBar.setVisibility(View.GONE);

        exchangeButton.setEnabled(!isLoading);
        calculateButton.setEnabled(!isLoading);

        if (isLoading) {
            buttonProgressContainer.setAlpha(0f);
            buttonProgressContainer.setVisibility(View.VISIBLE);
            buttonProgressContainer.animate()
                    .alpha(1f)
                    .setDuration(200)
                    .start();
        } else {
            buttonProgressContainer.animate()
                    .alpha(0f)
                    .setDuration(200)
                    .withEndAction(() -> buttonProgressContainer.setVisibility(View.GONE))
                    .start();
        }
    }

    private void refreshBalances() {
        progressBar.setVisibility(View.VISIBLE);
        viewModel.checkAllBalances();
    }

    @Override
    public void onDestroyView() {
        if (transactionObserver != null) {
            viewModel.getTransactionResult().removeObserver(transactionObserver);
        }
        super.onDestroyView();
    }
}