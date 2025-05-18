package com.example.crypto_payment_system.ui.exchange;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
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
import com.example.crypto_payment_system.config.ApiConfig;
import com.example.crypto_payment_system.domain.currency.Currency;
import com.example.crypto_payment_system.domain.token.TokenBalance;
import com.example.crypto_payment_system.repositories.api.ExchangeRateRepository;
import com.example.crypto_payment_system.repositories.api.ExchangeRateRepositoryImpl;
import com.example.crypto_payment_system.ui.transaction.TransactionResultFragment;
import com.example.crypto_payment_system.utils.adapter.currency.CurrencyAdapter;
import com.example.crypto_payment_system.utils.currency.CurrencyManager;
import com.example.crypto_payment_system.utils.web3.TransactionResult;
import com.example.crypto_payment_system.view.viewmodels.MainViewModel;
import com.google.android.material.textfield.TextInputEditText;

import java.text.DecimalFormat;
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
    private ProgressBar progressBar;
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
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_exchange, container, false);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);

        CurrencyManager.initialize(requireContext());

        fromCurrencySpinner = view.findViewById(R.id.fromCurrencySpinner);
        toCurrencySpinner = view.findViewById(R.id.toCurrencySpinner);
        fromAmountEditText = view.findViewById(R.id.fromAmountEditText);
        exchangeRateValue = view.findViewById(R.id.exchangeRateValue);
        estimatedAmountValue = view.findViewById(R.id.estimatedAmountValue);
        progressBar = view.findViewById(R.id.progressBar);
        exchangeButton = view.findViewById(R.id.exchangeButton);
        buttonProgressContainer = view.findViewById(R.id.buttonProgressContainer);

        eurBalanceValue = view.findViewById(R.id.eurBalanceValue);
        usdBalanceValue = view.findViewById(R.id.usdBalanceValue);
        refreshBalanceButton = view.findViewById(R.id.refreshBalanceButton);

        refreshBalanceButton.setOnClickListener(v -> refreshBalances());

        setupCurrencySpinners();

        fetchExchangeRate();

        exchangeButton.setOnClickListener(v -> {
            Currency fromCurrency = fromCurrencyAdapter.getSelectedCurrency();
            String amount = Objects.requireNonNull(fromAmountEditText.getText()).toString();

            if (fromCurrency != null && !amount.isEmpty()) {
                executeExchange(fromCurrency.getCode(), amount);
            } else {
                Toast.makeText(requireContext(), R.string.please_select_a_currency_and_enter_an_amount, Toast.LENGTH_SHORT).show();
            }
        });

        fromAmountEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s != null && !s.toString().isEmpty()) {
                    calculateExchangeRate(false);
                } else {
                    estimatedAmountValue.setText("--");
                }
            }
        });


        observeViewModel();

        refreshBalances();
    }

    private void setupCurrencySpinners() {
        List<Currency> currencies = new ArrayList<>(CurrencyManager.getAvailableCurrencies());
        fromCurrencyAdapter = new CurrencyAdapter(requireContext(), List.of(currencies.get(0)));
        toCurrencyAdapter = new CurrencyAdapter(requireContext(), List.of(currencies.get(1)));

        fromCurrencySpinner.setAdapter(fromCurrencyAdapter);
        toCurrencySpinner.setAdapter(toCurrencyAdapter);

        fromCurrencySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (isSelectionInProgress) return;

                Currency selectedCurrency = fromCurrencyAdapter.getItem(position);
                if (selectedCurrency == null) return;

                fromCurrencyAdapter.setSelectedCurrency(selectedCurrency.getCode());

                Currency toCurrency = toCurrencyAdapter.getSelectedCurrency();
                if (toCurrency != null && toCurrency.getCode().equals(selectedCurrency.getCode())) {
                    isSelectionInProgress = true;

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

                fetchExchangeRate();
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

                Currency selectedCurrency = toCurrencyAdapter.getItem(position);
                if (selectedCurrency == null) return;

                toCurrencyAdapter.setSelectedCurrency(selectedCurrency.getCode());

                Currency fromCurrency = fromCurrencyAdapter.getSelectedCurrency();
                if (fromCurrency != null && fromCurrency.getCode().equals(selectedCurrency.getCode())) {
                    isSelectionInProgress = true;

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

                fetchExchangeRate();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Nothing to do
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

        enableExchangeFunctionality();
    }

    private void updateCurrencySpinners(String preferredCurrencies) {
        String[] currencyCodes = preferredCurrencies.split(",");
        List<Currency> currencies = CurrencyManager.getCurrenciesByCodes(currencyCodes);

        if (currencies.size() <= 1) {
            disableExchangeFunctionality(getString(R.string.exchange_unavailable_only_one_currency_is_configured));

            fromCurrencyAdapter = new CurrencyAdapter(requireContext(), currencies);
            toCurrencyAdapter = new CurrencyAdapter(requireContext(), currencies);

            fromCurrencySpinner.setAdapter(fromCurrencyAdapter);
            toCurrencySpinner.setAdapter(toCurrencyAdapter);

            if (currencies.size() == 1) {
                fromCurrencySpinner.setSelection(0);
                toCurrencySpinner.setSelection(0);
            }

            return;
        }

        enableExchangeFunctionality();

        Currency currentFromSelection = null;
        Currency currentToSelection = null;

        if (fromCurrencyAdapter != null && fromCurrencyAdapter.getSelectedCurrency() != null) {
            currentFromSelection = fromCurrencyAdapter.getSelectedCurrency();
        }

        if (toCurrencyAdapter != null && toCurrencyAdapter.getSelectedCurrency() != null) {
            currentToSelection = toCurrencyAdapter.getSelectedCurrency();
        }

        fromCurrencyAdapter = new CurrencyAdapter(requireContext(), currencies);
        toCurrencyAdapter = new CurrencyAdapter(requireContext(), currencies);

        fromCurrencySpinner.setAdapter(fromCurrencyAdapter);
        toCurrencySpinner.setAdapter(toCurrencyAdapter);

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
        exchangeRateValue.setText("--");
        estimatedAmountValue.setText("--");
    }

    private void enableExchangeFunctionality() {
        fromAmountEditText.setEnabled(true);
    }

    private void fetchExchangeRate() {
        Currency fromCurrency = fromCurrencyAdapter.getSelectedCurrency();
        Currency toCurrency = toCurrencyAdapter.getSelectedCurrency();

        String fromCode = fromCurrency.getCode();
        String toCode = toCurrency.getCode();


        exchangeRateValue.setText("Loading...");

        ExchangeRateRepository exchangeRateRepository = new ExchangeRateRepositoryImpl(ApiConfig.BASE_URL, ApiConfig.USERNAME, ApiConfig.PASSWORD);

        exchangeRateRepository.getExchangeRate().thenAccept(exchangeRate -> {
            requireActivity().runOnUiThread(() -> {

                if (fromCurrency.getCode().equals("USD")) {
                    exchangeRate.setRate(1 / exchangeRate.getRate());
                }

                if (exchangeRate != null && exchangeRate.getRate() > 0) {
                    double rate = exchangeRate.getRate();

                    viewModel.setCurrentExchangeRate(rate);

                    DecimalFormat df = new DecimalFormat("#.####");
                    if (fromCode.equals("EUR")) {
                        exchangeRateValue.setText(String.format("1 %s = %s %s", "EURSC", df.format(rate), "USDT"));
                    } else {
                        exchangeRateValue.setText(String.format("1 %s = %s %s", "USDT", df.format(rate), "EURSC"));
                    }


                    String amount = fromAmountEditText.getText().toString();
                    if (!amount.isEmpty()) {
                        try {
                            double inputAmount = Double.parseDouble(amount);
                            double estimatedAmount = inputAmount * rate;
                            if (fromCode.equals("EUR")) {
                                estimatedAmountValue.setText(String.format("%.2f %s", estimatedAmount, "USDT"));
                            } else {
                                estimatedAmountValue.setText(String.format("%.2f %s", estimatedAmount, "EURSC"));
                            }
                        } catch (NumberFormatException e) {
                            estimatedAmountValue.setText("--");
                        }
                    } else {
                        estimatedAmountValue.setText("--");
                    }
                } else {
                    exchangeRateValue.setText(R.string.exchange_rate_not_available_for_selected_currencies);
                    estimatedAmountValue.setText("--");
                }
            });
        }).exceptionally(throwable -> {
            requireActivity().runOnUiThread(() -> {
                exchangeRateValue.setText("Error getting exchange rate");
                estimatedAmountValue.setText("--");

                Log.e("ExchangeFragment", "Error fetching exchange rate", throwable);
            });
            return null;
        });
    }

    @SuppressLint("DefaultLocale")
    private void calculateExchangeRate(boolean showLoading) {
        String amount = Objects.requireNonNull(fromAmountEditText.getText()).toString();
        if (amount.isEmpty()) {
            estimatedAmountValue.setText("--");
            return;
        }

        double rate = viewModel.getCurrentExchangeRate();

        if (rate <= 0) {
            fetchExchangeRate();
            return;
        }

        Currency toCurrency = toCurrencyAdapter.getSelectedCurrency();
        if (toCurrency == null) return;

        String fromCurrency = fromCurrencyAdapter.getSelectedCurrency().getCode();

        String toCurrencyCode = "";

        if (fromCurrency.equals("EUR")) {
            toCurrencyCode = "USDT";
        } else {
            toCurrencyCode = "EURSC";
        }

        try {
            double inputAmount = Double.parseDouble(amount);
            double estimatedAmount = inputAmount * rate;
            estimatedAmountValue.setText(String.format("%.2f %s", estimatedAmount, toCurrencyCode));
        } catch (NumberFormatException e) {
            estimatedAmountValue.setText("--");
        }
    }

    private void executeExchange(String fromCurrency, String amount) {
        if (fromAmountEditText.getText() == null || fromAmountEditText.getText().toString().isEmpty()) {
            Toast.makeText(requireContext(), R.string.please_fill_in_all_fields, Toast.LENGTH_SHORT).show();
            return;
        }

        setupTransactionObserver();

        viewModel.resetTransactionResult();

        showLoading(true);
        viewModel.exchangeBasedOnPreference(fromCurrency, amount);
    }

    private void setupTransactionObserver() {
        if (transactionObserver != null) {
            viewModel.getTransactionResult().removeObserver(transactionObserver);
        }

        transactionObserver = result -> {
            showLoading(false);

            if (result == null) return;

            Currency fromCurrency = fromCurrencyAdapter.getSelectedCurrency();
            String amount = fromAmountEditText.getText().toString();
            long timestamp = System.currentTimeMillis();

            TransactionResultFragment fragment = TransactionResultFragment.newInstance(result.isSuccess(), result.getTransactionHash() != null ? result.getTransactionHash() : "-", amount + " " + (fromCurrency != null ? fromCurrency.getCode() : "???"), "Exchange", timestamp, result.getMessage() != null ? result.getMessage() : "");
            requireActivity().getSupportFragmentManager().beginTransaction().replace(R.id.content_main, fragment).addToBackStack(null).commit();

            if (result.isSuccess()) {
                fromAmountEditText.setText("");
                exchangeRateValue.setText("--");
                estimatedAmountValue.setText("--");
                refreshBalances();
            }
        };

        viewModel.getTransactionResult().observe(getViewLifecycleOwner(), transactionObserver);
    }

    private void observeViewModel() {
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
        
        if (isLoading) {
            buttonProgressContainer.setAlpha(0f);
            buttonProgressContainer.setVisibility(View.VISIBLE);
            buttonProgressContainer.animate().alpha(1f).setDuration(200).start();
        } else {
            buttonProgressContainer.animate().alpha(0f).setDuration(200).withEndAction(() -> buttonProgressContainer.setVisibility(View.GONE)).start();
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