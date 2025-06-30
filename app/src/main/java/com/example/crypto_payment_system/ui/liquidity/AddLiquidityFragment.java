package com.example.crypto_payment_system.ui.liquidity;

import static com.example.crypto_payment_system.config.Constants.EURSC;
import static com.example.crypto_payment_system.config.Constants.USDT;

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
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.example.crypto_payment_system.R;
import com.example.crypto_payment_system.config.biometric.classes.TransactionAuthManager;
import com.example.crypto_payment_system.domain.currency.Currency;
import com.example.crypto_payment_system.domain.token.TokenBalance;
import com.example.crypto_payment_system.ui.transaction.TransactionResultFragment;
import com.example.crypto_payment_system.utils.adapter.currency.CurrencyAdapter;
import com.example.crypto_payment_system.utils.currency.CurrencyManager;
import com.example.crypto_payment_system.utils.progress.TransactionProgressDialog;
import com.example.crypto_payment_system.utils.validations.Validate;
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

    private Observer<TransactionResult> transactionObserver;
    private TransactionProgressDialog progressDialog;
    private boolean isTransactionInProgress = false;
    private TransactionAuthManager authManager;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        authManager = new TransactionAuthManager(requireActivity());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_add_liquidity, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);

        CurrencyManager.initialize(requireContext());

        currencySpinner = view.findViewById(R.id.currencySpinner);
        amountEditText = view.findViewById(R.id.amountEditText);
        progressBar = view.findViewById(R.id.progressBar);
        addLiquidityButton = view.findViewById(R.id.addLiquidityButton);

        eurBalanceValue = view.findViewById(R.id.eurBalanceValue);
        usdBalanceValue = view.findViewById(R.id.usdBalanceValue);

        contractEurBalanceValue = view.findViewById(R.id.contractEurBalanceValue);
        contractUsdBalanceValue = view.findViewById(R.id.contractUsdBalanceValue);
        buttonProgressContainer = view.findViewById(R.id.buttonProgressContainer);

        setupCurrencySpinner();

        addLiquidityButton.setOnClickListener(v -> {
            // Prevent multiple simultaneous transactions
            if (isTransactionInProgress) {
                Toast.makeText(requireContext(), R.string.transaction_already_in_progress, Toast.LENGTH_SHORT).show();
                return;
            }

            Currency selectedCurrency = currencyAdapter.getSelectedCurrency();
            String amount = Objects.requireNonNull(amountEditText.getText()).toString();

            if (selectedCurrency != null && !amount.isEmpty()) {
                if (!Validate.hasAmount(amount, selectedCurrency.getCode(), viewModel)) {
                    amountEditText.setError(getString(R.string.insufficient_balance));
                    return;
                }
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
        currencyAdapter = new CurrencyAdapter(
                requireContext(),
                new ArrayList<>(CurrencyManager.getAvailableCurrencies())
        );
        currencySpinner.setAdapter(currencyAdapter);

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
        List<Currency> currencies = new ArrayList<>(CurrencyManager.getAvailableCurrencies());

        currencyAdapter = new CurrencyAdapter(requireContext(), currencies);
        currencySpinner.setAdapter(currencyAdapter);

        // Default selection: EUR
        if (!currencies.isEmpty()) {
            for (int i = 0; i < currencyAdapter.getCount(); i++) {
                Currency currency = currencyAdapter.getItem(i);
                if (currency != null && EURSC.equals(currency.getCode())) {
                    currencySpinner.setSelection(i);
                    break;
                }
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

    private void addLiquidity(String currency, String amount) {

        authManager.authorizeLiquidityTransfer(amount, currency, new TransactionAuthManager.AuthCallback() {
            @Override
            public void onAuthorized() {
                isTransactionInProgress = true;

                viewModel.resetTransactionResult();

                setupTransactionObserver();

                showTransactionProgressDialog();

                viewModel.addLiquidity(currency, amount);
            }

            @Override
            public void onDenied(String reason) {
                Toast.makeText(getContext(), R.string.transaction_cancelled + reason, Toast.LENGTH_SHORT).show();
            }
        });
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
            Toast.makeText(requireContext(), R.string.error_showing_transaction_dialog + e.getMessage(), Toast.LENGTH_SHORT).show();
            isTransactionInProgress = false;
        }
    }

    private void simulateTransactionProgress() {

        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.updateState(TransactionProgressDialog.TransactionState.PREPARING);
        }

        addLiquidityButton.postDelayed(() -> {
            if (progressDialog != null && progressDialog.isShowing()) {
                progressDialog.updateState(TransactionProgressDialog.TransactionState.SUBMITTING);
            }
        }, 1000);

        addLiquidityButton.postDelayed(() -> {
            if (progressDialog != null && progressDialog.isShowing()) {
                progressDialog.updateState(TransactionProgressDialog.TransactionState.PENDING);
            }
        }, 2000);

        addLiquidityButton.postDelayed(() -> {
            if (progressDialog != null && progressDialog.isShowing()) {
                progressDialog.updateState(TransactionProgressDialog.TransactionState.CONFIRMING);
            }
        }, 4000);
    }

    private void setupTransactionObserver() {
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

            if (progressDialog != null && progressDialog.isShowing()) {
                if (result.isSuccess()) {
                    progressDialog.updateState(TransactionProgressDialog.TransactionState.CONFIRMED);
                    if (result.getTransactionHash() != null) {
                        progressDialog.setTransactionHash(result.getTransactionHash());
                    }
                } else {
                    progressDialog.updateState(TransactionProgressDialog.TransactionState.FAILED);
                }
            }

            addLiquidityButton.postDelayed(() -> {
                showTransactionResult(result);
            }, 2000);

            if (result.isSuccess()) {
                refreshBalances();
            }
        };

        viewModel.getTransactionResult().observe(getViewLifecycleOwner(), transactionObserver);
    }

    private void showTransactionResult(TransactionResult result) {
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
    }

    private void observeViewModel() {
        viewModel.getTokenBalances().observe(getViewLifecycleOwner(), this::updateWalletBalanceUI);
        viewModel.getTokenBalances().observe(getViewLifecycleOwner(), this::updateContractBalanceUI);
    }

    private void refreshBalances() {
        viewModel.checkAllBalances();
    }

    private void updateWalletBalanceUI(Map<String, TokenBalance> balances) {
        if (balances == null) return;

        if (balances.containsKey(EURSC)) {
            eurBalanceValue.setText(balances.get(EURSC).getFormattedWalletBalance() + " EURSC");
        }

        if (balances.containsKey(USDT)) {
            usdBalanceValue.setText(balances.get(USDT).getFormattedWalletBalance() + " USDT");
        }
    }

    private void updateContractBalanceUI(Map<String, TokenBalance> balances) {
        if (balances == null) return;

        if (balances.containsKey(EURSC)) {
            contractEurBalanceValue.setText(balances.get(EURSC).getFormattedContractBalance() + " EURSC");
        }

        if (balances.containsKey(USDT)) {
            contractUsdBalanceValue.setText(balances.get(USDT).getFormattedContractBalance() + " USDT");
        }
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
}