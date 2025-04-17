package com.example.crypto_payment_system;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
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
import com.example.crypto_payment_system.ui.exchange.ExchangeFragment;
import com.example.crypto_payment_system.ui.liquidity.AddLiquidityFragment;
import com.example.crypto_payment_system.ui.mintFunds.MintFragment;
import com.example.crypto_payment_system.ui.sendMoney.SendMoneyFragment;
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
    private ArrayAdapter<String> currencyAdapter;
    private View submenuView;
    private boolean isSubmenuVisible = false;

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

        FrameLayout submenuContainer = findViewById(R.id.submenu_container);
        submenuView = getLayoutInflater().inflate(R.layout.send_money_submenu,
                submenuContainer, false);
        submenuContainer.addView(submenuView);

        submenuView.setVisibility(View.GONE);

        ImageButton closeButton = submenuView.findViewById(R.id.btn_close_submenu);
        closeButton.setOnClickListener(v -> hideSubmenu());

        submenuView.findViewById(R.id.option_make_payment).setOnClickListener(v -> {
            hideSubmenu();
            navigateToFragment(new SendMoneyFragment());
            drawerLayout.closeDrawer(GravityCompat.START);
        });

        submenuView.findViewById(R.id.option_transfer_accounts).setOnClickListener(v -> {
            hideSubmenu();
            navigateToFragment(new ExchangeFragment());
            drawerLayout.closeDrawer(GravityCompat.START);
        });

        navigationView.setNavigationItemSelectedListener(this);

        View contentView = findViewById(R.id.content_main);
        resultTextView = contentView.findViewById(R.id.resultTextView);
        progressBar = contentView.findViewById(R.id.progressBar);
        currencySpinner = contentView.findViewById(R.id.currencySpinner);
        Button connectButton = contentView.findViewById(R.id.connectButton);
        Button checkAllBalancesButton = contentView.findViewById(R.id.checkAllBalancesButton);

        View headerView = navigationView.getHeaderView(0);
        walletAddressText = headerView.findViewById(R.id.walletAddressText);

        currencyAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                new ArrayList<>()
        );
        currencyAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        currencySpinner.setAdapter(currencyAdapter);
        setupCurrencySpinner();

        connectButton.setOnClickListener(v -> viewModel.connectToEthereum());

        checkAllBalancesButton.setOnClickListener(v -> viewModel.checkAllBalances());

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
                }
            } else {
                walletAddressText.setText(R.string.connect_to_view_wallet_address);
                resetCurrencySpinner();
            }
        });

        viewModel.getTokenBalances().observe(this, balances -> {
            StringBuilder sb = new StringBuilder();
            sb.append("YOUR WALLET BALANCES:\n");

            balances.forEach((symbol, balance) -> sb.append(symbol).append(": ").append(balance.getFormattedWalletBalance()).append("\n"));

            sb.append("\nCONTRACT BALANCES:\n");

            balances.forEach((symbol, balance) -> sb.append(symbol).append(": ").append(balance.getFormattedContractBalance()).append("\n"));

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
     *
     * @param preferredCurrencies Comma-separated list of currencies
     */
    private void updateCurrencySpinner(String preferredCurrencies) {
        String[] currencies = preferredCurrencies.split(",");

        currencyAdapter.clear();

        for (String currency : currencies) {
            String trimmedCurrency = currency.trim().toUpperCase();
            if (trimmedCurrency.equals("EUR") || trimmedCurrency.equals("USD")) {
                currencyAdapter.add(trimmedCurrency);
            }
        }

        currencyAdapter.notifyDataSetChanged();

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
        } else if (id == R.id.nav_send_money) {
            showSubmenu();
            return true;
        } else if (id == R.id.nav_transactions) {
            Toast.makeText(this, "Transactions feature coming soon", Toast.LENGTH_SHORT).show(); // maybe???
        } else if (id == R.id.nav_mint_tokens) {
            navigateToFragment(new MintFragment());
        } else if (id == R.id.nav_add_liquidity) {
            navigateToFragment(new AddLiquidityFragment());
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

    @SuppressLint("ClickableViewAccessibility")
    private void setupAccountSelection() {
        View accountSelectionView = findViewById(R.id.account_selection_layout);
        if (accountSelectionView == null) {
            return;
        }

        Spinner accountsSpinner = accountSelectionView.findViewById(R.id.accountsSpinner);
        Button addAccountButton = accountSelectionView.findViewById(R.id.addAccountButton);

        ArrayAdapter<WalletAccount> accountArrayAdapter = new AccountAdapter(this, new ArrayList<>());
        accountsSpinner.setAdapter(accountArrayAdapter);

        final String[] previouslySelectedAddress = {null};

        accountsSpinner.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                int position = accountsSpinner.getSelectedItemPosition();
                if (position >= 0 && position < accountArrayAdapter.getCount()) {
                    WalletAccount account = accountArrayAdapter.getItem(position);
                    if (account != null) {
                        previouslySelectedAddress[0] = account.getAddress();
                    }
                }
            }
            return false;
        });

        accountsSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                WalletAccount selectedAccount = (WalletAccount) parent.getItemAtPosition(position);
                if (selectedAccount != null) {
                    String newAddress = selectedAccount.getAddress();

                    if (!newAddress.equals(previouslySelectedAddress[0])) {
                        try {
                            progressBar.setVisibility(View.VISIBLE);

                            walletAddressText.setText(newAddress);

                            viewModel.switchAccount(newAddress);

                            Toast.makeText(MainActivity.this,
                                    "Switching to account: " + selectedAccount.getName(),
                                    Toast.LENGTH_SHORT).show();

                            previouslySelectedAddress[0] = newAddress;

                        } catch (JSONException e) {
                            Toast.makeText(MainActivity.this,
                                    "Error switching accounts: " + e.getMessage(),
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });

        addAccountButton.setOnClickListener(v -> showAccountDialog());

        viewModel.getAccounts().observe(this, accounts -> {
            int currentPosition = accountsSpinner.getSelectedItemPosition();
            String currentAddress = null;

            if (currentPosition >= 0 && currentPosition < accountArrayAdapter.getCount()) {
                WalletAccount currentAccount = accountArrayAdapter.getItem(currentPosition);
                if (currentAccount != null) {
                    currentAddress = currentAccount.getAddress();
                }
            }

            accountArrayAdapter.clear();
            accountArrayAdapter.addAll(accounts);
            accountArrayAdapter.notifyDataSetChanged();

            if (currentAddress != null) {
                for (int i = 0; i < accountArrayAdapter.getCount(); i++) {
                    WalletAccount account = accountArrayAdapter.getItem(i);
                    if (account != null && account.getAddress().equals(currentAddress)) {
                        accountsSpinner.setSelection(i);
                        break;
                    }
                }
            }
        });

        viewModel.getActiveAccount().observe(this, account -> {
            if (account != null) {
                String activeAddress = account.getAddress();

                walletAddressText.setText(activeAddress);

                previouslySelectedAddress[0] = activeAddress;

                for (int i = 0; i < accountArrayAdapter.getCount(); i++) {
                    WalletAccount adapterAccount = accountArrayAdapter.getItem(i);
                    if (adapterAccount != null && adapterAccount.getAddress().equals(activeAddress)) {
                        if (accountsSpinner.getSelectedItemPosition() != i) {
                            accountsSpinner.setSelection(i);
                        }
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

    private void setupCurrencySpinner() {
        currencySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedCurrency = parent.getItemAtPosition(position).toString();
                viewModel.setSelectedCurrency(selectedCurrency);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    private void showSubmenu() {
        submenuView.setVisibility(View.VISIBLE);
        isSubmenuVisible = true;
        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.getMenu().setGroupVisible(R.id.nav_main_group, false);
    }

    private void hideSubmenu() {
        submenuView.setVisibility(View.GONE);
        isSubmenuVisible = false;
        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.getMenu().setGroupVisible(R.id.nav_main_group, true);
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            if (isSubmenuVisible) {
                hideSubmenu();
            } else {
                drawerLayout.closeDrawer(GravityCompat.START);
            }
        } else {
            super.onBackPressed();
        }
    }
}