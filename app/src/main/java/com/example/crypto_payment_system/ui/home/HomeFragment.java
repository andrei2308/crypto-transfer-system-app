package com.example.crypto_payment_system.ui.home;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.viewpager2.widget.ViewPager2;

import com.example.crypto_payment_system.R;
import com.example.crypto_payment_system.databinding.FragmentHomeBinding;
import com.example.crypto_payment_system.domain.account.User;
import com.example.crypto_payment_system.domain.currency.Currency;
import com.example.crypto_payment_system.domain.exchangeRate.ExchangeRate;
import com.example.crypto_payment_system.domain.transaction.Transaction;
import com.example.crypto_payment_system.ui.exchange.ExchangeFragment;
import com.example.crypto_payment_system.ui.transaction.TransactionDetailsDialogFragment;
import com.example.crypto_payment_system.ui.transaction.TransactionHistoryFragment;
import com.example.crypto_payment_system.utils.adapter.balancePager.BalancePagerAdapter;
import com.example.crypto_payment_system.utils.adapter.currency.CurrencyAdapter;
import com.example.crypto_payment_system.utils.adapter.transaction.TransactionAdapter;
import com.example.crypto_payment_system.utils.currency.CurrencyManager;
import com.example.crypto_payment_system.view.viewmodels.MainViewModel;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.android.material.textfield.TextInputEditText;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class HomeFragment extends Fragment implements TransactionAdapter.TransactionClickListener {

    private FragmentHomeBinding binding;
    private MainViewModel viewModel;
    private TransactionAdapter transactionAdapter;
    private ViewPager2 balanceViewPager;
    private TabLayout balanceTabLayout;
    private BalancePagerAdapter balancePagerAdapter;
    private TextView emptyTransactionsMessage;
    private TextView userAddressTextView;
    private TextView welcomeTextView;
    private TextView connectedStatusTextView;

    private TextInputEditText quickFromAmountEditText;
    private TextView quickEstimatedAmountValue;
    private TextView quickExchangeRateValue;
    private TextView lastUpdatedText;

    public static HomeFragment newInstance() {
        return new HomeFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);

        setupViews();
        setupObservers();
        setupBalanceViewPager();
        setupTransactionsList();
        setupSimpleExchangeCalculator();

        String walletAddress = viewModel.getCurrentUser().getValue() != null ?
                viewModel.getCurrentUser().getValue().getWalletAddress() : "";

        if (!walletAddress.isEmpty()) {
            viewModel.loadTransactionsForWallet(walletAddress.toLowerCase());
            viewModel.checkAllBalances();
            viewModel.fetchExchangeRate();
        }
    }

    private void setupViews() {
        balanceViewPager = binding.accountsPager;
        balanceTabLayout = binding.pagerIndicator;
        emptyTransactionsMessage = new TextView(requireContext());
        emptyTransactionsMessage.setVisibility(View.GONE);

        userAddressTextView = binding.profileSection.userAddressTextView;
        welcomeTextView = binding.profileSection.welcomeTextView;
        connectedStatusTextView = binding.profileSection.connectedStatusTextView;

        if (viewModel != null && viewModel.getCurrentUser().getValue() != null) {
            String walletAddress = viewModel.getCurrentUser().getValue().getWalletAddress();
            if (walletAddress != null && !walletAddress.isEmpty()) {
                userAddressTextView.setText(walletAddress);
            }
        }

        binding.seeMoreTransactionsLabel.setOnClickListener(v -> {
            String currentCurrency = viewModel.getSelectedCurrency().getValue();
            if (currentCurrency != null) {
                TransactionHistoryFragment fragment = TransactionHistoryFragment.newInstance(currentCurrency);
                requireActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.content_main, fragment)
                        .addToBackStack(null)
                        .commit();
            }
        });

        binding.goToExchangeLabel.setOnClickListener(v -> {
            ExchangeFragment fragment = new ExchangeFragment();
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.content_main, fragment)
                    .addToBackStack(null)
                    .commit();
        });
    }

    private void setupSimpleExchangeCalculator() {
        quickFromAmountEditText = binding.quickFromAmountEditText;
        quickEstimatedAmountValue = binding.quickEstimatedAmountValue;
        quickExchangeRateValue = binding.quickExchangeRateValue;
        lastUpdatedText = binding.lastUpdatedText;

        TextView fromCurrencyLabel = binding.fromCurrencyLabel;
        TextView toCurrencyLabel = binding.toCurrencyLabel;
        fromCurrencyLabel.setText("EUR Amount");
        toCurrencyLabel.setText("USD Equivalent");

        binding.quickFromCurrencySpinner.setVisibility(View.GONE);
        binding.quickToCurrencySpinner.setVisibility(View.GONE);

        quickFromAmountEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (s != null && !s.toString().isEmpty()) {
                    calculateEurToUsdRate();
                } else {
                    quickEstimatedAmountValue.setText("0.00 USD");
                }
            }
        });

        viewModel.getExchangeRateData().observe(getViewLifecycleOwner(), exchangeRate -> {
            if (exchangeRate != null) {
                updateExchangeRateUI(exchangeRate);
            }
        });

        viewModel.fetchExchangeRate();
    }

    private void calculateEurToUsdRate() {
        String amount = Objects.requireNonNull(quickFromAmountEditText.getText()).toString();
        if (amount.isEmpty()) {
            quickEstimatedAmountValue.setText("0.00 USD");
            return;
        }

        double rate = viewModel.getCurrentExchangeRate();
        if (rate <= 0) {
            quickEstimatedAmountValue.setText("-- USD");
            viewModel.fetchExchangeRate();
            return;
        }

        try {
            double inputAmount = Double.parseDouble(amount);
            double estimatedAmount = inputAmount * rate;
            DecimalFormat df = new DecimalFormat("#,##0.00");
            quickEstimatedAmountValue.setText(df.format(estimatedAmount) + " USD");
        } catch (NumberFormatException e) {
            quickEstimatedAmountValue.setText("0.00 USD");
        }
    }

    private void updateExchangeRateUI(ExchangeRate exchangeRate) {
        if (exchangeRate == null) return;

        double rate = exchangeRate.getRate();

        viewModel.setCurrentExchangeRate(rate);

        DecimalFormat df = new DecimalFormat("#.####");
        quickExchangeRateValue.setText(String.format("1 EUR = %s USD", df.format(rate)));

        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
        lastUpdatedText.setText("Last updated: " + sdf.format(new Date()));

        calculateEurToUsdRate();
    }

    private void setupObservers() {
        viewModel.getFilteredTransactions().observe(getViewLifecycleOwner(), transactions -> {
            if (transactions != null) {
                List<Transaction> recentTransactions = transactions.size() > 5 ?
                        transactions.subList(0, 5) : transactions;
                transactionAdapter.submitList(recentTransactions);

                boolean isEmpty = transactions.isEmpty();
                binding.transactionsRecyclerView.scrollToPosition(0);
                emptyTransactionsMessage.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
                binding.transactionsRecyclerView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
                binding.seeMoreTransactionsLabel.setVisibility(transactions.size() > 5 ? View.VISIBLE : View.GONE);
            }
        });

        viewModel.getSelectedCurrency().observe(getViewLifecycleOwner(), currency -> {
            viewModel.updateFilteredTransactions(currency);
        });

        viewModel.getCurrentUser().observe(getViewLifecycleOwner(), user -> {
            if (user != null && balanceViewPager.getAdapter() != null) {
                List<String> preferredCurrencies = getPreferredCurrencies(user);

                balancePagerAdapter = new BalancePagerAdapter(
                        requireContext(),
                        position -> viewModel.checkAllBalances(),
                        preferredCurrencies
                );

                balanceViewPager.setAdapter(balancePagerAdapter);
                updateAdapterPreferredCurrencies();

                if (preferredCurrencies.size() > 1) {
                    balanceTabLayout.setVisibility(View.VISIBLE);
                    new TabLayoutMediator(balanceTabLayout, balanceViewPager, (tab, position) -> {
                    }).attach();
                } else {
                    balanceTabLayout.setVisibility(View.GONE);
                }

                String walletAddress = user.getWalletAddress();
                if (walletAddress != null && !walletAddress.isEmpty()) {
                    userAddressTextView.setText(walletAddress);
                }
            }
        });
    }

    private void setupBalanceViewPager() {
        if (balanceViewPager == null || balanceViewPager.getAdapter() != null) {
            return;
        }

        User currentUser = viewModel.getCurrentUser().getValue();
        List<String> preferredCurrencies = new ArrayList<>();

        if (currentUser != null && currentUser.getPreferredCurrency() != null) {
            String[] currencies = currentUser.getPreferredCurrency().split(",");
            for (String currency : currencies) {
                String trimmedCurrency = currency.trim().toUpperCase();
                if ("EUR".equals(trimmedCurrency) || "USD".equals(trimmedCurrency)) {
                    preferredCurrencies.add(trimmedCurrency);
                }
            }
        }

        if (preferredCurrencies.isEmpty()) {
            preferredCurrencies.add("EUR");
            preferredCurrencies.add("USD");
        }

        balancePagerAdapter = new BalancePagerAdapter(
                requireContext(),
                position -> viewModel.checkAllBalances(),
                preferredCurrencies
        );

        balanceViewPager.setAdapter(balancePagerAdapter);

        if (preferredCurrencies.size() > 1) {
            balanceTabLayout.setVisibility(View.VISIBLE);
            new TabLayoutMediator(balanceTabLayout, balanceViewPager, (tab, position) -> {
            }).attach();
        } else {
            balanceTabLayout.setVisibility(View.GONE);
        }

        balanceViewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                String selectedCurrency = position == 0 ? "EUR" : "USD";
                viewModel.setSelectedCurrency(selectedCurrency);

                if (transactionAdapter != null) {
                    transactionAdapter.setCurrentSelectedCurrency(selectedCurrency);

                    binding.transactionsRecyclerView.scrollToPosition(0);

                }
            }
        });
    }

    private void setupTransactionsList() {
        String currentAddress = "";
        if (viewModel.getActiveAccount().getValue() != null) {
            currentAddress = viewModel.getActiveAccount().getValue().getAddress();
        }

        transactionAdapter = new TransactionAdapter(this, currentAddress);
        binding.transactionsRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.transactionsRecyclerView.setAdapter(transactionAdapter);

        viewModel.getActiveAccount().observe(getViewLifecycleOwner(), account -> {
            if (account != null) {
                String newAddress = account.getAddress();
                transactionAdapter = new TransactionAdapter(this, newAddress);
                binding.transactionsRecyclerView.setAdapter(transactionAdapter);
                binding.transactionsRecyclerView.scrollToPosition(0);
                if (viewModel.getFilteredTransactions().getValue() != null) {
                    transactionAdapter.submitList(viewModel.getFilteredTransactions().getValue());
                }
            }
        });
    }


    private static List<String> getPreferredCurrencies(User user) {
        String preferredCurrenciesStr = user.getPreferredCurrency();
        List<String> preferredCurrencies = new ArrayList<>();

        if (preferredCurrenciesStr != null && !preferredCurrenciesStr.isEmpty()) {
            String[] currencies = preferredCurrenciesStr.split(",");
            for (String currency : currencies) {
                String trimmedCurrency = currency.trim().toUpperCase();
                if ("EUR".equals(trimmedCurrency) || "USD".equals(trimmedCurrency)) {
                    preferredCurrencies.add(trimmedCurrency);
                }
            }
        }

        if (preferredCurrencies.isEmpty()) {
            preferredCurrencies.add("EUR");
            preferredCurrencies.add("USD");
        }
        return preferredCurrencies;
    }

    private void updateAdapterPreferredCurrencies() {
        if (transactionAdapter != null) {
            List<String> preferredCurrencies = viewModel.getPreferredCurrencyList();
            transactionAdapter.setPrefferedCurrencies(preferredCurrencies);
        }
    }

    @Override
    public void onTransactionClick(Transaction transaction) {
        TransactionDetailsDialogFragment dialog = TransactionDetailsDialogFragment.newInstance(transaction);
        dialog.show(requireActivity().getSupportFragmentManager(), "transaction_details");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}