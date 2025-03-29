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

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                new String[]{"EUR", "USD"}
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        currencySpinner.setAdapter(adapter);

        connectButton.setOnClickListener(v -> viewModel.connectToEthereum());

        checkAllBalancesButton.setOnClickListener(v -> viewModel.checkAllBalances());

        mintTokenButton.setOnClickListener(v -> {
            String selectedCurrency = currencySpinner.getSelectedItem().toString();
            viewModel.mintTokens(selectedCurrency);
        });

        callTransactionMethodButton.setOnClickListener(v -> {
            String selectedCurrency = currencySpinner.getSelectedItem().toString();
            viewModel.addLiquidity(selectedCurrency);
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
                    String primaryCurrency = preferredCurrencies.split(",")[0];
                    int spinnerPosition = ((ArrayAdapter) currencySpinner.getAdapter())
                            .getPosition(primaryCurrency);
                    currencySpinner.setSelection(spinnerPosition);

                    updateExchangeButtonText(primaryCurrency);
                }
            } else {
                walletAddressText.setText("Connect to view wallet address");
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

    private void updateExchangeButtonText(String primaryCurrency) {
        if ("EUR".equals(primaryCurrency)) {
            exchangeButton.setText("Exchange EUR → USD");
        } else {
            exchangeButton.setText("Exchange USD → EUR");
        }
    }

    private void sendMoney() {
        viewModel.sendMoney(addressTeit.getText().toString());
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