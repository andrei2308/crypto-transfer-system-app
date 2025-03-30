package com.example.crypto_payment_system;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.lifecycle.ViewModelProvider;

import com.example.crypto_payment_system.ui.settings.ManageAccountFragment;
import com.example.crypto_payment_system.viewmodels.MainViewModel;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private MainViewModel viewModel;
    private TextView resultTextView;
    private ProgressBar progressBar;
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private TextView walletAddressText;
    private Spinner currencySpinner;
    private Button connectButton;
    private Button checkAllBalancesButton;
    private Button mintTokenButton;
    private Button callTransactionMethodButton;
    private Button exchangeButton;
    private Button sendMoneyButton;
    private TextInputEditText addressTeit;
    private ArrayAdapter<String> currencyAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        viewModel = new ViewModelProvider(this).get(MainViewModel.class);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        navigationView.setNavigationItemSelectedListener(this);

        View contentView = findViewById(R.id.content_main);
        resultTextView = contentView.findViewById(R.id.resultTextView);
        progressBar = contentView.findViewById(R.id.progressBar);
        currencySpinner = contentView.findViewById(R.id.currencySpinner);
        connectButton = contentView.findViewById(R.id.connectButton);
        checkAllBalancesButton = contentView.findViewById(R.id.checkAllBalancesButton);
        mintTokenButton = contentView.findViewById(R.id.mintTokenButton);
        callTransactionMethodButton = contentView.findViewById(R.id.callTransactionMethodButton);
        exchangeButton = contentView.findViewById(R.id.exchangeButton);
        sendMoneyButton = contentView.findViewById(R.id.send_money_btn);
        addressTeit = contentView.findViewById(R.id.address_teit);

        View headerView = navigationView.getHeaderView(0);
        walletAddressText = headerView.findViewById(R.id.walletAddressText);

        // Initialize the adapter with an empty list
        currencyAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                new ArrayList<>()
        );
        currencyAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        currencySpinner.setAdapter(currencyAdapter);

        connectButton.setOnClickListener(v -> viewModel.connectToEthereum());

        checkAllBalancesButton.setOnClickListener(v -> viewModel.checkAllBalances());

        mintTokenButton.setOnClickListener(v -> {
            if (currencySpinner.getSelectedItem() != null) {
                String selectedCurrency = currencySpinner.getSelectedItem().toString();
                viewModel.mintTokens(selectedCurrency);
            } else {
                Toast.makeText(this, "Please select a currency", Toast.LENGTH_SHORT).show();
            }
        });

        callTransactionMethodButton.setOnClickListener(v -> {
            if (currencySpinner.getSelectedItem() != null) {
                String selectedCurrency = currencySpinner.getSelectedItem().toString();
                viewModel.addLiquidity(selectedCurrency);
            } else {
                Toast.makeText(this, "Please select a currency", Toast.LENGTH_SHORT).show();
            }
        });

        exchangeButton.setOnClickListener(v -> viewModel.exchangeBasedOnPreference());

        sendMoneyButton.setOnClickListener(v -> sendMoney());

        observeViewModel();
    }

    private void observeViewModel() {
        viewModel.getConnectionStatus().observe(this, status -> {
            resultTextView.setText(status);
        });

        viewModel.isNewUser().observe(this, isNew -> {
            if (isNew) {
                navigateToFragment(new ManageAccountFragment());
            }
        });

        viewModel.getCurrentUser().observe(this, user -> {
            if (user != null) {
                walletAddressText.setText(user.getWalletAddress());

                String preferredCurrencies = user.getPreferredCurrency();
                if (preferredCurrencies != null && !preferredCurrencies.isEmpty()) {
                    // Update the spinner with only the user's preferred currencies
                    updateCurrencySpinner(preferredCurrencies);

                    // Get primary currency (first in the list)
                    String primaryCurrency = preferredCurrencies.split(",")[0];
                    updateExchangeButtonText(primaryCurrency);
                }
            } else {
                walletAddressText.setText("Connect to view wallet address");
                // Reset the spinner with default options
                resetCurrencySpinner();
            }
        });

        viewModel.getTokenBalances().observe(this, balances -> {
            StringBuilder sb = new StringBuilder();
            sb.append("YOUR WALLET BALANCES:\n");

            balances.forEach((symbol, balance) -> {
                sb.append(symbol).append(": ").append(balance.getWalletBalance()).append("\n");
            });

            sb.append("\nCONTRACT BALANCES:\n");

            balances.forEach((symbol, balance) -> {
                sb.append(symbol).append(": ").append(balance.getContractBalance()).append("\n");
            });

            resultTextView.setText(sb.toString());
        });

        viewModel.getTransactionResult().observe(this, result -> {
            if (result == null) return;

            if (!result.isSuccess() && result.getTransactionHash() == null) {
                Toast.makeText(this, result.getMessage(), Toast.LENGTH_LONG).show();
                return;
            }

            StringBuilder sb = new StringBuilder();
            sb.append(result.getMessage()).append("\n");

            if (result.getTransactionHash() != null) {
                sb.append("Hash: ").append(result.getTransactionHash());
            }

            resultTextView.setText(sb.toString());
        });

        viewModel.getIsLoading().observe(this, isLoading -> {
            progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        });
    }

    /**
     * Updates the currency spinner to only show the user's preferred currencies
     * @param preferredCurrencies Comma-separated list of currencies
     */
    private void updateCurrencySpinner(String preferredCurrencies) {
        // Parse comma-separated list into an array
        List<String> currencies = Arrays.asList(preferredCurrencies.split(","));

        // Clear existing items
        currencyAdapter.clear();

        // Add only the preferred currencies to the adapter
        for (String currency : currencies) {
            String trimmedCurrency = currency.trim().toUpperCase();
            if (trimmedCurrency.equals("EUR") || trimmedCurrency.equals("USD")) {
                currencyAdapter.add(trimmedCurrency);
            }
        }

        // Notify the adapter that data has changed
        currencyAdapter.notifyDataSetChanged();

        // Select the first currency if available
        if (currencyAdapter.getCount() > 0) {
            currencySpinner.setSelection(0);
        }
    }

    /**
     * Resets the spinner to default options
     */
    private void resetCurrencySpinner() {
        currencyAdapter.clear();
        currencyAdapter.add("EUR");
        currencyAdapter.add("USD");
        currencyAdapter.notifyDataSetChanged();
        currencySpinner.setSelection(0);
    }

    private void updateExchangeButtonText(String primaryCurrency) {
        if ("EUR".equals(primaryCurrency)) {
            exchangeButton.setText("Exchange EUR → USD");
        } else {
            exchangeButton.setText("Exchange USD → EUR");
        }
    }

    private void sendMoney() {
        if (currencySpinner.getSelectedItem() == null) {
            Toast.makeText(this, "Please select a currency", Toast.LENGTH_SHORT).show();
            return;
        }

        String selectedCurrency = currencySpinner.getSelectedItem().toString();
        if (selectedCurrency.equals("EUR")) {
            viewModel.sendMoney(addressTeit.getText().toString(), 1);
        } else if (selectedCurrency.equals("USD")) {
            viewModel.sendMoney(addressTeit.getText().toString(), 2);
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.nav_home) {
            clearFragmentBackStack();
        } else if (id == R.id.nav_manage_account) {
            navigateToFragment(new ManageAccountFragment());
        } else if (id == R.id.nav_logout) {
            Toast.makeText(this, "Disconnected", Toast.LENGTH_SHORT).show();
            // viewModel.disconnect(); // to be implemented
        } else if (id == R.id.nav_transactions) {
            Toast.makeText(this, "Transactions feature coming soon", Toast.LENGTH_SHORT).show(); // maybe???
        }

        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    private void navigateToFragment(androidx.fragment.app.Fragment fragment) {
        View contentView = findViewById(R.id.content_main);

        if (contentView instanceof ViewGroup) {
            ((ViewGroup) contentView).removeAllViews();

            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.content_main, fragment)
                    .addToBackStack(null)
                    .commit();
        }
    }

    private void clearFragmentBackStack() {
        getSupportFragmentManager().popBackStack(null,
                androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE);

        recreate();
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }
}