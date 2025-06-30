package com.example.crypto_payment_system.ui.mintFunds;

import static com.example.crypto_payment_system.config.Constants.ETH;
import static com.example.crypto_payment_system.config.Constants.USDT;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
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
    private ProgressBar progressBar;
    private CurrencyAdapter currencyAdapter;
    private TextView ethBalanceValue;
    private TextView eurBalanceValue;
    private TextView usdBalanceValue;
    private Observer<TransactionResult> transactionObserver;
    private TransactionProgressDialog progressDialog;
    private TransactionAuthManager authManager;

    private boolean isTransactionInProgress = false;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);
        authManager = new TransactionAuthManager(requireActivity());
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

        CurrencyManager.initialize(requireContext());

        mintCurrencySpinner = view.findViewById(R.id.mintCurrencySpinner);
        mintAmountEditText = view.findViewById(R.id.mintAmountEditText);
        Button mintButton = view.findViewById(R.id.mintButton);
        progressBar = view.findViewById(R.id.mintProgressBar);
        ethBalanceValue = view.findViewById(R.id.ethBalanceValue);
        eurBalanceValue = view.findViewById(R.id.eurBalanceValue);
        usdBalanceValue = view.findViewById(R.id.usdBalanceValue);

        setupCurrencySpinner();

        mintButton.setOnClickListener(v -> {
            if (isTransactionInProgress) {
                Toast.makeText(requireContext(), "Transaction already in progress", Toast.LENGTH_SHORT).show();
                return;
            }
            mintFunds();
        });

        observeViewModel();

        refreshBalances();
    }

    private void refreshBalances() {
        progressBar.setVisibility(View.VISIBLE);
        viewModel.checkAllBalances();
    }

    private void setupCurrencySpinner() {
        List<Currency> currencies = new ArrayList<>();
        Currency usdCurrency = CurrencyManager.getCurrencyByCode(USDT);
        if (usdCurrency != null) {
            currencies.add(usdCurrency);
        }

        currencyAdapter = new CurrencyAdapter(requireContext(), currencies);
        mintCurrencySpinner.setAdapter(currencyAdapter);

        if (!currencies.isEmpty()) {
            mintCurrencySpinner.setSelection(0);
        }
    }

    private void observeViewModel() {
        viewModel.resetTransactionConfirmation();

        viewModel.getTransactionConfirmation().observe(getViewLifecycleOwner(), confirmationRequest -> {
            if (confirmationRequest == null) return;

            AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
            builder.setTitle("Confirm Transaction");
            builder.setMessage(confirmationRequest.getMessage());

            builder.setPositiveButton("Confirm", (dialog, which) -> {
                confirmationRequest.confirm();

                showTransactionProgressDialog();

                viewModel.resetTransactionConfirmation();
            });

            builder.setNegativeButton("Cancel", (dialog, which) -> {
                isTransactionInProgress = false;
                viewModel.resetTransactionConfirmation();
            });

            builder.setCancelable(false);

            AlertDialog dialog = builder.create();
            dialog.show();
        });

        viewModel.getTokenBalances().observe(getViewLifecycleOwner(), this::updateBalanceUI);
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
            Toast.makeText(requireContext(), "Error showing transaction dialog: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            isTransactionInProgress = false;
        }
    }

    private void simulateTransactionProgress() {

        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.updateState(TransactionProgressDialog.TransactionState.PREPARING);
        }

        mintAmountEditText.postDelayed(() -> {
            if (progressDialog != null && progressDialog.isShowing()) {
                progressDialog.updateState(TransactionProgressDialog.TransactionState.SUBMITTING);
            }
        }, 1000);

        mintAmountEditText.postDelayed(() -> {
            if (progressDialog != null && progressDialog.isShowing()) {
                progressDialog.updateState(TransactionProgressDialog.TransactionState.PENDING);
            }
        }, 2000);

        mintAmountEditText.postDelayed(() -> {
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

                mintAmountEditText.postDelayed(() -> {
                    if (progressDialog != null && progressDialog.isShowing()) {
                        progressDialog.dismiss();
                        progressDialog = null;
                    }
                }, 1500);
            }

            mintAmountEditText.postDelayed(() -> {
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
        String amount = mintAmountEditText.getText().toString().trim();
        long timestamp = System.currentTimeMillis();

        TransactionResultFragment fragment = TransactionResultFragment.newInstance(
                result.isSuccess(),
                result.getTransactionHash() != null ? result.getTransactionHash() : "-",
                amount + " " + (selectedCurrency != null ? selectedCurrency.getCode() : "???"),
                "Mint",
                timestamp,
                result.getMessage() != null ? result.getMessage() : ""
        );
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.content_main, fragment)
                .addToBackStack(null)
                .commit();
    }

    /**
     * Update the UI with the latest token balances
     */
    private void updateBalanceUI(Map<String, TokenBalance> balances) {
        progressBar.setVisibility(View.GONE);

        if (balances.containsKey(ETH)) {
            ethBalanceValue.setText(balances.get(ETH).getFormattedWalletBalance() + " ETH");
        } else {
            ethBalanceValue.setText("0 ETH");
        }

        if (balances.containsKey(USDT)) {
            usdBalanceValue.setText(balances.get(USDT).getFormattedWalletBalance() + " USD");
        } else {
            usdBalanceValue.setText("0 USD");
        }
    }

    private void mintFunds() {
        String amountStr = Objects.requireNonNull(mintAmountEditText.getText()).toString().trim();

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

            authManager.authorizeMinting(amountStr, USDT, new TransactionAuthManager.AuthCallback() {
                @Override
                public void onAuthorized() {
                    isTransactionInProgress = true;

                    viewModel.resetTransactionResult();

                    setupTransactionObserver();

                    viewModel.mintTokens(USDT, String.valueOf(amount));
                }

                @Override
                public void onDenied(String reason) {
                    Toast.makeText(getContext(), "Transaction cancelled: " + reason, Toast.LENGTH_SHORT).show();
                }
            });

        } catch (NumberFormatException e) {
            mintAmountEditText.setError("Please enter a valid amount");
            isTransactionInProgress = false;
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