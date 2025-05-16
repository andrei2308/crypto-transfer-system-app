package com.example.crypto_payment_system.ui.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
import com.example.crypto_payment_system.domain.transaction.Transaction;
import com.example.crypto_payment_system.ui.transaction.TransactionDetailsDialogFragment;
import com.example.crypto_payment_system.utils.adapter.balancePager.BalancePagerAdapter;
import com.example.crypto_payment_system.utils.adapter.transaction.TransactionAdapter;
import com.example.crypto_payment_system.view.viewmodels.MainViewModel;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import java.util.ArrayList;
import java.util.List;

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

        String walletAddress = viewModel.getCurrentUser().getValue() != null ?
                viewModel.getCurrentUser().getValue().getWalletAddress() : "";

        if (!walletAddress.isEmpty()) {
            viewModel.loadTransactionsForWallet(walletAddress.toLowerCase());
            viewModel.checkAllBalances();
        }
    }

    private void setupViews() {
        balanceViewPager = binding.accountsPager;
        balanceTabLayout = binding.pagerIndicator;
        emptyTransactionsMessage = new TextView(requireContext());
        emptyTransactionsMessage.setVisibility(View.GONE);
        
        // Get references from the included layout
        userAddressTextView = binding.profileSection.userAddressTextView;
        welcomeTextView = binding.profileSection.welcomeTextView;
        connectedStatusTextView = binding.profileSection.connectedStatusTextView;
        
        // Set welcome text and wallet address when available
        if (viewModel != null && viewModel.getCurrentUser().getValue() != null) {
            String walletAddress = viewModel.getCurrentUser().getValue().getWalletAddress();
            if (walletAddress != null && !walletAddress.isEmpty()) {
                userAddressTextView.setText(walletAddress);
            }
        }
    }

    private void setupObservers() {
        viewModel.getFilteredTransactions().observe(getViewLifecycleOwner(), transactions -> {
            transactionAdapter.submitList(transactions);
            if (transactions != null) {
                boolean isEmpty = transactions.isEmpty();
                binding.transactionsRecyclerView.scrollToPosition(0);
                emptyTransactionsMessage.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
                binding.transactionsRecyclerView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
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
                
                // Update wallet address when user data changes
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
            binding.transactionsRecyclerView.scrollToPosition(0);
        }

        transactionAdapter = new TransactionAdapter(this, currentAddress);
        binding.transactionsRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.transactionsRecyclerView.setAdapter(transactionAdapter);

        viewModel.getActiveAccount().observe(getViewLifecycleOwner(), account -> {
            if (account != null) {
                String newAddress = account.getAddress();
                transactionAdapter = new TransactionAdapter(this, newAddress);
                binding.transactionsRecyclerView.setAdapter(transactionAdapter);

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