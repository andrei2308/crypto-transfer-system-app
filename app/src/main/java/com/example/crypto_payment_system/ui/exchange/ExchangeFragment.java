package com.example.crypto_payment_system.ui.exchange;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
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
    private ArrayAdapter<String> fromCurrencyAdapter;
    private ArrayAdapter<String> toCurrencyAdapter;

    private TextView eurBalanceValue;
    private TextView usdBalanceValue;
    private Button refreshBalanceButton;
    private boolean isSelectionInProgress = false;


    private Observer<TokenRepository.TransactionResult> transactionObserver;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_exchange, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);

        fromCurrencySpinner = view.findViewById(R.id.fromCurrencySpinner);
        toCurrencySpinner = view.findViewById(R.id.toCurrencySpinner);
        fromAmountEditText = view.findViewById(R.id.fromAmountEditText);
        exchangeRateValue = view.findViewById(R.id.exchangeRateValue);
        estimatedAmountValue = view.findViewById(R.id.estimatedAmountValue);
        resultTextView = view.findViewById(R.id.resultTextView);
        progressBar = view.findViewById(R.id.progressBar);
        calculateButton = view.findViewById(R.id.calculateButton);
        exchangeButton = view.findViewById(R.id.exchangeButton);

        eurBalanceValue = view.findViewById(R.id.eurBalanceValue);
        usdBalanceValue = view.findViewById(R.id.usdBalanceValue);
        refreshBalanceButton = view.findViewById(R.id.refreshBalanceButton);

        refreshBalanceButton.setOnClickListener(v -> refreshBalances());

        setupCurrencySpinners();

        calculateButton.setOnClickListener(v -> calculateExchangeRate());

        exchangeButton.setOnClickListener(v -> {
            String fromCurrency = (String) fromCurrencySpinner.getSelectedItem();
            String amount = Objects.requireNonNull(fromAmountEditText.getText()).toString();

            if (fromCurrency != null && !amount.isEmpty()) {
                executeExchange(fromCurrency, amount);
            } else {
                Toast.makeText(requireContext(), "Please select a currency and enter an amount",
                        Toast.LENGTH_SHORT).show();
            }
        });

        observeViewModel();

        refreshBalances();
    }

    private void setupCurrencySpinners() {
        fromCurrencyAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                new ArrayList<>()
        );
        fromCurrencyAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        fromCurrencySpinner.setAdapter(fromCurrencyAdapter);

        toCurrencyAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                new ArrayList<>()
        );
        toCurrencyAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        toCurrencySpinner.setAdapter(toCurrencyAdapter);

        fromCurrencySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (isSelectionInProgress) return;

                isSelectionInProgress = true;

                String selectedCurrency = (String) parent.getItemAtPosition(position);

                for (int i = 0; i < toCurrencyAdapter.getCount(); i++) {
                    String currency = toCurrencyAdapter.getItem(i);
                    if (currency != null && !currency.equals(selectedCurrency)) {
                        toCurrencySpinner.setSelection(i);
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

                String selectedCurrency = (String) parent.getItemAtPosition(position);

                for (int i = 0; i < fromCurrencyAdapter.getCount(); i++) {
                    String currency = fromCurrencyAdapter.getItem(i);
                    if (currency != null && !currency.equals(selectedCurrency)) {
                        fromCurrencySpinner.setSelection(i);
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
        fromCurrencyAdapter.clear();
        fromCurrencyAdapter.add("EUR");
        fromCurrencyAdapter.add("USD");
        fromCurrencyAdapter.notifyDataSetChanged();

        toCurrencyAdapter.clear();
        toCurrencyAdapter.add("EUR");
        toCurrencyAdapter.add("USD");
        toCurrencyAdapter.notifyDataSetChanged();

        fromCurrencySpinner.setSelection(0);
        if (toCurrencyAdapter.getCount() > 1) {
            toCurrencySpinner.setSelection(1);
        }

        enableExchangeFunctionality();
    }

    private void updateCurrencySpinners(String preferredCurrencies) {
        String[] currencies = preferredCurrencies.split(",");

        ArrayList<String> validCurrencies = new ArrayList<>();
        for (String currency : currencies) {
            String trimmedCurrency = currency.trim().toUpperCase();
            if (trimmedCurrency.equals("EUR") || trimmedCurrency.equals("USD")) {
                validCurrencies.add(trimmedCurrency);
            }
        }

        if (validCurrencies.size() <= 1) {
            disableExchangeFunctionality("Exchange unavailable - only one currency is configured");

            fromCurrencyAdapter.clear();
            toCurrencyAdapter.clear();

            if (validCurrencies.size() == 1) {
                String currency = validCurrencies.get(0);
                fromCurrencyAdapter.add(currency);
                toCurrencyAdapter.add(currency);
            }

            fromCurrencyAdapter.notifyDataSetChanged();
            toCurrencyAdapter.notifyDataSetChanged();

            if (fromCurrencyAdapter.getCount() > 0) {
                fromCurrencySpinner.setSelection(0);
                toCurrencySpinner.setSelection(0);
            }

            return;
        }

        enableExchangeFunctionality();

        String currentFromSelection = null;
        String currentToSelection = null;

        if (fromCurrencySpinner.getSelectedItem() != null) {
            currentFromSelection = fromCurrencySpinner.getSelectedItem().toString();
        }
        if (toCurrencySpinner.getSelectedItem() != null) {
            currentToSelection = toCurrencySpinner.getSelectedItem().toString();
        }

        fromCurrencyAdapter.clear();
        toCurrencyAdapter.clear();

        for (String currency : validCurrencies) {
            fromCurrencyAdapter.add(currency);
            toCurrencyAdapter.add(currency);
        }

        fromCurrencyAdapter.notifyDataSetChanged();
        toCurrencyAdapter.notifyDataSetChanged();

        boolean fromSelectionRestored = false;
        if (currentFromSelection != null) {
            for (int i = 0; i < fromCurrencyAdapter.getCount(); i++) {
                if (Objects.equals(fromCurrencyAdapter.getItem(i), currentFromSelection)) {
                    fromCurrencySpinner.setSelection(i);
                    fromSelectionRestored = true;
                    break;
                }
            }
        }

        boolean toSelectionRestored = false;
        if (currentToSelection != null) {
            for (int i = 0; i < toCurrencyAdapter.getCount(); i++) {
                if (Objects.equals(toCurrencyAdapter.getItem(i), currentToSelection)) {
                    toCurrencySpinner.setSelection(i);
                    toSelectionRestored = true;
                    break;
                }
            }
        }

        if (!fromSelectionRestored && fromCurrencyAdapter.getCount() > 0) {
            fromCurrencySpinner.setSelection(0);
        }

        if (!toSelectionRestored) {
            if (toCurrencyAdapter.getCount() > 1 && fromCurrencySpinner.getSelectedItem() != null) {
                String fromCurrency = fromCurrencySpinner.getSelectedItem().toString();
                for (int i = 0; i < toCurrencyAdapter.getCount(); i++) {
                    if (!Objects.equals(toCurrencyAdapter.getItem(i), fromCurrency)) {
                        toCurrencySpinner.setSelection(i);
                        return;
                    }
                }
            }

            if (toCurrencyAdapter.getCount() > 0) {
                toCurrencySpinner.setSelection(0);
            }
        }
    }

    private void resetCurrencySpinners() {
        fromCurrencyAdapter.clear();
        fromCurrencyAdapter.add("EUR");
        fromCurrencyAdapter.add("USD");
        fromCurrencyAdapter.notifyDataSetChanged();

        toCurrencyAdapter.clear();
        toCurrencyAdapter.add("EUR");
        toCurrencyAdapter.add("USD");
        toCurrencyAdapter.notifyDataSetChanged();

        fromCurrencySpinner.setSelection(0);
        if (toCurrencyAdapter.getCount() > 1) {
            toCurrencySpinner.setSelection(1);
        } else {
            toCurrencySpinner.setSelection(0);
        }

        enableExchangeFunctionality();
    }

    private void disableExchangeFunctionality(String message) {
        fromCurrencySpinner.setEnabled(false);
        toCurrencySpinner.setEnabled(false);
        fromAmountEditText.setEnabled(false);
        calculateButton.setEnabled(false);
        exchangeButton.setEnabled(false);

        resultTextView.setText(message);
        exchangeRateValue.setText("--");
        estimatedAmountValue.setText("--");
    }

    private void enableExchangeFunctionality() {
        fromCurrencySpinner.setEnabled(true);
        toCurrencySpinner.setEnabled(true);
        fromAmountEditText.setEnabled(true);
        calculateButton.setEnabled(true);
        exchangeButton.setEnabled(true);

        resultTextView.setText("Exchange transaction results will appear here");
    }

    private void calculateExchangeRate() {
        String fromCurrency = (String) fromCurrencySpinner.getSelectedItem();
        String toCurrency = (String) toCurrencySpinner.getSelectedItem();
        String amount = Objects.requireNonNull(fromAmountEditText.getText()).toString();

        if (fromCurrency != null && toCurrency != null && !amount.isEmpty()) {
            double rate = 0;
            if ("EUR".equals(fromCurrency) && "USD".equals(toCurrency)) {
                rate = 1.09;
            } else if ("USD".equals(fromCurrency) && "EUR".equals(toCurrency)) {
                rate = 0.92;
            }

            if (rate > 0) {
                try {
                    double amountValue = Double.parseDouble(amount);
                    double estimatedAmount = amountValue * rate;

                    exchangeRateValue.setText(String.format("1 %s = %.2f %s",
                            fromCurrency, rate, toCurrency));
                    estimatedAmountValue.setText(String.format("%.2f %s",
                            estimatedAmount, toCurrency));
                } catch (NumberFormatException e) {
                    Toast.makeText(requireContext(), "Please enter a valid amount",
                            Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(requireContext(), "Exchange rate not available for selected currencies",
                        Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(requireContext(), "Please fill in all fields",
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void executeExchange(String fromCurrency, String amount) {
        resultTextView.setText("Starting exchange for " + amount + " " + fromCurrency + "...");

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
                resultTextView.setText("Exchanged " +
                        fromAmountEditText.getText().toString().trim() + " " +
                        fromCurrencySpinner.getSelectedItem().toString() +
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

    private void refreshBalances() {
        viewModel.checkAllBalances();
    }

    private void updateBalanceUI(Map<String, TokenBalance> balances) {
        if (balances == null) return;

        if (balances.containsKey("EURC")) {
            eurBalanceValue.setText(balances.get("EURC").getFormattedWalletBalance() + " EUR");
        }
        if (balances.containsKey("USDT")) {
            usdBalanceValue.setText(balances.get("USDT").getFormattedWalletBalance() + " USD");
        }
    }

    private void showLoading(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        exchangeButton.setEnabled(!isLoading);
        calculateButton.setEnabled(!isLoading);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (transactionObserver != null) {
            viewModel.getTransactionResult().removeObserver(transactionObserver);
        }
    }
}