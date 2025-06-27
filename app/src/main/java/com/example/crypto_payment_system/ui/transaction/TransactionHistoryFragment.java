package com.example.crypto_payment_system.ui.transaction;

import static com.example.crypto_payment_system.config.Constants.EURSC;
import static com.example.crypto_payment_system.config.Constants.USDT;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.crypto_payment_system.R;
import com.example.crypto_payment_system.databinding.FragmentTransactionHistoryBinding;
import com.example.crypto_payment_system.domain.transaction.Transaction;
import com.example.crypto_payment_system.utils.adapter.transaction.TransactionAdapter;
import com.example.crypto_payment_system.view.viewmodels.MainViewModel;

import java.math.BigDecimal;
import java.util.List;

public class TransactionHistoryFragment extends Fragment implements TransactionAdapter.TransactionClickListener {
    private FragmentTransactionHistoryBinding binding;
    private MainViewModel viewModel;
    private TransactionAdapter transactionAdapter;
    private String currency;

    public static TransactionHistoryFragment newInstance(String currency) {
        TransactionHistoryFragment fragment = new TransactionHistoryFragment();
        Bundle args = new Bundle();
        args.putString("currency", currency);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentTransactionHistoryBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        View logo = requireActivity().findViewById(R.id.appLogoContainer);
        View appName = requireActivity().findViewById(R.id.appNameText);
        if (logo != null) logo.setVisibility(View.GONE);
        if (appName != null) appName.setVisibility(View.GONE);

        viewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);

        if (getArguments() != null) {
            currency = getArguments().getString("currency", EURSC);
            viewModel.setSelectedCurrency(currency);
        }

        setupViews();
        setupObservers();
        setupTransactionsList();
    }

    private void setupViews() {
        binding.currencyTextView.setText(currency);
        binding.backButton.setOnClickListener(v -> requireActivity().onBackPressed());
    }

    private void setupObservers() {
        viewModel.getFilteredTransactions().observe(getViewLifecycleOwner(), transactions -> {
            transactionAdapter.submitList(transactions);
            updateStatistics(transactions);
        });
    }

    private void setupTransactionsList() {
        String currentAddress = "";
        if (viewModel.getActiveAccount().getValue() != null) {
            currentAddress = viewModel.getActiveAccount().getValue().getAddress();
        }

        transactionAdapter = new TransactionAdapter(this, currentAddress);
        transactionAdapter.setCurrentSelectedCurrency(currency);
        binding.transactionsRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.transactionsRecyclerView.setAdapter(transactionAdapter);
    }

    private void updateStatistics(List<Transaction> transactions) {
        if (transactions == null || transactions.isEmpty()) {
            binding.totalSentValue.setText("0");
            binding.totalReceivedValue.setText("0");
            binding.transactionCountValue.setText("0");
            return;
        }

        BigDecimal totalSent = BigDecimal.ZERO;
        BigDecimal totalReceived = BigDecimal.ZERO;
        String currentAddress = viewModel.getActiveAccount().getValue() != null ?
                viewModel.getActiveAccount().getValue().getAddress() : "";

        for (Transaction transaction : transactions) {
            BigDecimal amount = new BigDecimal(transaction.getAmount());

            if (!transaction.getWalletAddressTo().equals(transaction.getWalletAddress())) {
                if (transaction.getWalletAddress().equals(currentAddress)) {
                    totalSent = totalSent.add(amount);
                } else if (transaction.getWalletAddressTo().equals(currentAddress)) {
                    totalReceived = totalReceived.add(amount);
                }
            } else {
                if (currency.equals(EURSC) && transaction.getSentCurrency() == 1) {
                    totalSent = totalSent.add(amount);
                } else if (currency.equals(EURSC) && transaction.getReceivedCurrency() == 1) {
                    totalReceived = totalReceived.add(amount.multiply(BigDecimal.valueOf(Long.valueOf(transaction.getExchangeRate())).divide(BigDecimal.valueOf(100000000L))));
                } else if (currency.equals(USDT) && transaction.getSentCurrency() == 2) {
                    totalSent = totalSent.add(amount);
                } else if (currency.equals(USDT) && transaction.getReceivedCurrency() == 2) {
                    totalReceived = totalReceived.add(amount.multiply(BigDecimal.valueOf(Long.valueOf(transaction.getExchangeRate())).divide(BigDecimal.valueOf(100000000L))));
                }
            }
        }

        binding.totalSentValue.setText((totalSent.divide(BigDecimal.valueOf(1000000L))).toString()); // 6 decimals
        binding.totalReceivedValue.setText((totalReceived.divide(BigDecimal.valueOf(1000000L))).toString()); // 6 decimals
        binding.transactionCountValue.setText(String.valueOf(transactions.size()));
    }

    @Override
    public void onTransactionClick(Transaction transaction, String amount, int amountColor) {
        TransactionDetailsDialogFragment dialog = TransactionDetailsDialogFragment.newInstance(transaction, amount, amountColor);
        dialog.show(requireActivity().getSupportFragmentManager(), "transaction_details");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        View logo = requireActivity().findViewById(R.id.appLogoContainer);
        View appName = requireActivity().findViewById(R.id.appNameText);
        if (logo != null) logo.setVisibility(View.VISIBLE);
        if (appName != null) appName.setVisibility(View.VISIBLE);
        binding = null;
    }
} 