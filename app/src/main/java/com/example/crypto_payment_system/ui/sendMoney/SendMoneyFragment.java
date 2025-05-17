package com.example.crypto_payment_system.ui.sendMoney;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.example.crypto_payment_system.R;
import com.example.crypto_payment_system.domain.account.User;
import com.example.crypto_payment_system.domain.currency.Currency;
import com.example.crypto_payment_system.ui.transaction.TransactionResultFragment;
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
    private ProgressBar progressBar;
    private Button sendMoneyBtn;
    private FrameLayout buttonProgressContainer;
    private CurrencyAdapter currencyAdapter;
    private Observer<TransactionResult> transactionObserver;

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

        sendMoneyBtn.setOnClickListener(view -> sendMoney());

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
            if (currency != null && "EUR".equals(currency.getCode())) {
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

            Currency selectedCurrency = currencyAdapter.getSelectedCurrency();
            if (selectedCurrency == null) {
                selectedCurrency = CurrencyManager.getCurrencyByCode("EUR");
            }

            String currency = selectedCurrency.getCode();

            BigDecimal decimalAmount = BigDecimal.valueOf(amount);
            BigDecimal tokenUnits = decimalAmount.multiply(BigDecimal.valueOf(1_000_000));
            String formattedAmount = tokenUnits.toBigInteger().toString();

            showLoading(true);

            if (transactionObserver != null) {
                viewModel.getTransactionResult().removeObserver(transactionObserver);
            }

            viewModel.resetTransactionResult();

            final double finalAmount = amount;
            final String finalCurrency = currency;
            final String finalAddress = address;
            transactionObserver = result -> {
                if (result == null) {
                    return;
                } else {
                    showLoading(false);
                }
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

                if (result.isSuccess()) {
                    addressTeit.setText("");
                    amountTeit.setText("");
                }
            };

            viewModel.getTransactionResult().observe(getViewLifecycleOwner(), transactionObserver);

            viewModel.sendMoney(address, currency, formattedAmount);

        } catch (NumberFormatException e) {
            amountTeit.setError("Please enter a valid number");
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