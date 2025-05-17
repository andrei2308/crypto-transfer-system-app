package com.example.crypto_payment_system.ui.mintFunds;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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

public class MintFragment extends Fragment {

    private MainViewModel viewModel;
    private Spinner mintCurrencySpinner;
    private TextInputEditText mintAmountEditText;
    private TextInputEditText walletAddressEditText;
    private TextView mintStatusTextView;
    private ProgressBar progressBar;
    private CurrencyAdapter currencyAdapter;
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

        // Initialize CurrencyManager if not already initialized
        CurrencyManager.initialize(requireContext());

        mintCurrencySpinner = view.findViewById(R.id.mintCurrencySpinner);
        mintAmountEditText = view.findViewById(R.id.mintAmountEditText);
        walletAddressEditText = view.findViewById(R.id.walletAddressEditText);
        Button mintButton = view.findViewById(R.id.mintButton);
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
        // Create adapter with all available currencies
        currencyAdapter = new CurrencyAdapter(
                requireContext(),
                new ArrayList<>(CurrencyManager.getAvailableCurrencies())
        );
        mintCurrencySpinner.setAdapter(currencyAdapter);

        // Add item selection listener to handle currency changes
        mintCurrencySpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
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
        mintCurrencySpinner.setAdapter(currencyAdapter);
        
        // Default selection: EUR
        if (currencies.size() > 0) {
            for (int i = 0; i < currencyAdapter.getCount(); i++) {
                Currency currency = currencyAdapter.getItem(i);
                if (currency != null && "EUR".equals(currency.getCode())) {
                    mintCurrencySpinner.setSelection(i);
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
        mintCurrencySpinner.setAdapter(currencyAdapter);

        // Try to restore the previous selection
        if (currentSelection != null) {
            // Find the position of the previously selected currency
            for (int i = 0; i < currencyAdapter.getCount(); i++) {
                Currency currency = currencyAdapter.getItem(i);
                if (currency != null && currency.getCode().equals(currentSelection)) {
                    mintCurrencySpinner.setSelection(i);
                    return;
                }
            }
        }
        
        // If previous selection not found or no previous selection, select the first currency
        if (!currencies.isEmpty()) {
            mintCurrencySpinner.setSelection(0);
        }
    }

    private void resetCurrencySpinner() {
        // Reset to all available currencies
        List<Currency> currencies = new ArrayList<>(CurrencyManager.getAvailableCurrencies());
        
        // Create new adapter
        currencyAdapter = new CurrencyAdapter(requireContext(), currencies);
        mintCurrencySpinner.setAdapter(currencyAdapter);
        
        // Default selection: EUR
        for (int i = 0; i < currencyAdapter.getCount(); i++) {
            Currency currency = currencyAdapter.getItem(i);
            if (currency != null && "EUR".equals(currency.getCode())) {
                mintCurrencySpinner.setSelection(i);
                break;
            }
        }
    }

    private void observeViewModel() {
        if (transactionObserver != null) {
            viewModel.getTransactionResult().removeObserver(transactionObserver);
        }

        transactionObserver = result -> {
            progressBar.setVisibility(View.GONE);

            if (result == null) return;

            Currency selectedCurrency = currencyAdapter.getSelectedCurrency();
            String currencyCode = selectedCurrency != null ? selectedCurrency.getCode() : "???";
            String amount = mintAmountEditText.getText().toString().trim();
            long timestamp = System.currentTimeMillis();

            TransactionResultFragment fragment = TransactionResultFragment.newInstance(
                result.isSuccess(),
                result.getTransactionHash() != null ? result.getTransactionHash() : "-",
                amount + " " + currencyCode,
                "Mint Tokens",
                timestamp,
                result.getMessage() != null ? result.getMessage() : ""
            );
            requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.content_main, fragment)
                .addToBackStack(null)
                .commit();

            if (result.isSuccess()) {
                mintAmountEditText.setText("");
                refreshBalances();
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

            Currency selectedCurrency = currencyAdapter.getSelectedCurrency();
            if (selectedCurrency == null) {
                selectedCurrency = CurrencyManager.getCurrencyByCode("EUR"); // Fallback to EUR
            }
            
            String currency = selectedCurrency.getCode();

            progressBar.setVisibility(View.VISIBLE);

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
        if (transactionObserver != null) {
            viewModel.getTransactionResult().removeObserver(transactionObserver);
        }
        super.onDestroyView();
    }
}