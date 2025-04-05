package com.example.crypto_payment_system;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
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
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.lifecycle.ViewModelProvider;

import com.example.crypto_payment_system.models.WalletAccount;
import com.example.crypto_payment_system.ui.settings.ManageAccountFragment;
import com.example.crypto_payment_system.utils.AccountAdapter;
import com.example.crypto_payment_system.viewmodels.MainViewModel;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.Objects;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private MainViewModel viewModel;
    private TextView resultTextView;
    private ProgressBar progressBar;
    private DrawerLayout drawerLayout;
    private TextView walletAddressText;
    private Spinner currencySpinner;
    private Button exchangeButton;
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
        NavigationView navigationView = findViewById(R.id.nav_view);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        navigationView.setNavigationItemSelectedListener(this);

        View contentView = findViewById(R.id.content_main);
        resultTextView = contentView.findViewById(R.id.resultTextView);
        progressBar = contentView.findViewById(R.id.progressBar);
        currencySpinner = contentView.findViewById(R.id.currencySpinner);
        Button connectButton = contentView.findViewById(R.id.connectButton);
        Button checkAllBalancesButton = contentView.findViewById(R.id.checkAllBalancesButton);
        Button mintTokenButton = contentView.findViewById(R.id.mintTokenButton);
        Button callTransactionMethodButton = contentView.findViewById(R.id.callTransactionMethodButton);
        exchangeButton = contentView.findViewById(R.id.exchangeButton);
        Button sendMoneyButton = contentView.findViewById(R.id.send_money_btn);
        addressTeit = contentView.findViewById(R.id.address_teit);

        View headerView = navigationView.getHeaderView(0);
        walletAddressText = headerView.findViewById(R.id.walletAddressText);

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

        exchangeButton.setOnClickListener(v -> viewModel.exchangeBasedOnPreference(currencySpinner.getSelectedItem().toString()));

        sendMoneyButton.setOnClickListener(v -> sendMoney());

        observeViewModel();

        setupAccountSelection();
    }

    private void observeViewModel() {
        viewModel.getConnectionStatus().observe(this, status -> resultTextView.setText(status));

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
                    updateCurrencySpinner(preferredCurrencies);

                    String primaryCurrency = preferredCurrencies.split(",")[0];
                    updateExchangeButtonText(primaryCurrency);
                }
            } else {
                walletAddressText.setText(R.string.connect_to_view_wallet_address);
                resetCurrencySpinner();
            }
        });

        viewModel.getTokenBalances().observe(this, balances -> {
            StringBuilder sb = new StringBuilder();
            sb.append("YOUR WALLET BALANCES:\n");

            balances.forEach((symbol, balance) -> sb.append(symbol).append(": ").append(balance.getWalletBalance()).append("\n"));

            sb.append("\nCONTRACT BALANCES:\n");

            balances.forEach((symbol, balance) -> sb.append(symbol).append(": ").append(balance.getContractBalance()).append("\n"));

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

        viewModel.getIsLoading().observe(this, isLoading -> progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE));
    }

    /**
     * Updates the currency spinner to only show the user's preferred currencies
     * @param preferredCurrencies Comma-separated list of currencies
     */
    private void updateCurrencySpinner(String preferredCurrencies) {
        // Parse comma-separated list into an array
        String[] currencies = preferredCurrencies.split(",");

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
            exchangeButton.setText(R.string.exchange_eur_usd);
        } else {
            exchangeButton.setText(R.string.exchange_usd_eur);
        }
    }

    private void sendMoney() {
        if (currencySpinner.getSelectedItem() == null) {
            Toast.makeText(this, "Please select a currency", Toast.LENGTH_SHORT).show();
            return;
        }

        String selectedCurrency = currencySpinner.getSelectedItem().toString();

        viewModel.sendMoney(Objects.requireNonNull(addressTeit.getText()).toString(),selectedCurrency);
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

    private void setupAccountSelection() {
        View accountSelectionView = findViewById(R.id.account_selection_layout);
        if(accountSelectionView == null){
            return;
        }

        Spinner accountsSpinner = accountSelectionView.findViewById(R.id.accountsSpinner);
        Button addAccountButton = accountSelectionView.findViewById(R.id.addAccountButton);

        ArrayAdapter<WalletAccount> accountArrayAdapter = new AccountAdapter(this,new ArrayList<>());
        accountsSpinner.setAdapter(accountArrayAdapter);

        accountsSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                WalletAccount selectedAccount = (WalletAccount) parent.getItemAtPosition(position);
                if(selectedAccount != null){
                    try {
                        viewModel.switchAccount(selectedAccount.getAddress());
                    } catch (JSONException e) {
                        throw new RuntimeException(e);
                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        addAccountButton.setOnClickListener(v -> showAccountDialog());

        viewModel.getAccounts().observe(this,accounts-> {
            accountArrayAdapter.clear();
            accountArrayAdapter.addAll(accounts);
            accountArrayAdapter.notifyDataSetChanged();
        });

        viewModel.getActiveAccount().observe(this,account->{
            if(account!=null){
                for(int i=0;i<accountArrayAdapter.getCount();i++){
                    if(Objects.requireNonNull(accountArrayAdapter.getItem(i)).getAddress().equals(account.getAddress())){
                        accountsSpinner.setSelection(i);
                        break;
                    }
                }
            }
        });
    }

    /**
     * Show dialog to add a new Ethereum account
     */
    private void showAccountDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add Ethereum Account");

        View dialogView = LayoutInflater.from(this).inflate(R.layout.add_account_dialog, null);
        builder.setView(dialogView);

        TextInputEditText accountNameEditText = dialogView.findViewById(R.id.accountNameEditText);
        TextInputEditText privateKeyEditText = dialogView.findViewById(R.id.privateKeyEditText);

        builder.setPositiveButton("Add", (dialog, which) -> {
            String name = Objects.requireNonNull(accountNameEditText.getText()).toString().trim();
            String privateKey = Objects.requireNonNull(privateKeyEditText.getText()).toString().trim();

            if (name.isEmpty()) {
                Toast.makeText(this, "Account name is required", Toast.LENGTH_SHORT).show();
                return;
            }

            if (privateKey.isEmpty()) {
                Toast.makeText(this, "Private key is required", Toast.LENGTH_SHORT).show();
                return;
            }

            boolean success;
            try {
                success = viewModel.addAccount(name, privateKey);
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
            if (success) {
                Toast.makeText(this, "Account added successfully", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Account already exists", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();
        dialog.show();
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