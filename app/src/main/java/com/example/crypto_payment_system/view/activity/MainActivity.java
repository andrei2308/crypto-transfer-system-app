package com.example.crypto_payment_system.view.activity;

import android.annotation.SuppressLint;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
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
import androidx.constraintlayout.widget.Group;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.crypto_payment_system.R;
import com.example.crypto_payment_system.domain.account.User;
import com.example.crypto_payment_system.domain.account.WalletAccount;
import com.example.crypto_payment_system.domain.token.TokenBalance;
import com.example.crypto_payment_system.domain.transaction.Transaction;
import com.example.crypto_payment_system.ui.exchange.ExchangeFragment;
import com.example.crypto_payment_system.ui.liquidity.AddLiquidityFragment;
import com.example.crypto_payment_system.ui.mintFunds.MintFragment;
import com.example.crypto_payment_system.ui.sendMoney.SendMoneyFragment;
import com.example.crypto_payment_system.ui.settings.ManageAccountFragment;
import com.example.crypto_payment_system.ui.transaction.TransactionDetailsDialogFragment;
import com.example.crypto_payment_system.utils.adapter.account.AccountAdapter;
import com.example.crypto_payment_system.utils.adapter.transaction.TransactionAdapter;
import com.example.crypto_payment_system.view.viewmodels.MainViewModel;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.textfield.TextInputEditText;
import com.example.crypto_payment_system.databinding.ActivityMainBinding;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener, TransactionAdapter.TransactionClickListener {

    private MainViewModel viewModel;
    private ProgressBar progressBar;
    private DrawerLayout drawerLayout;
    private TextView walletAddressText;
    private Spinner currencySpinner;
    private ArrayAdapter<String> currencyAdapter;
    private View submenuView;
    private boolean isSubmenuVisible = false;
    private View connectionRequiredMessage;
    private Group postConnectionUiGroup;
    private boolean isConnected = false;
    private TextView eurBalanceValue;
    private TextView usdBalanceValue;
    private ActivityMainBinding binding;
    private TransactionAdapter transactionAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this).get(MainViewModel.class);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        drawerLayout = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        setupSubmenu();

        navigationView.setNavigationItemSelectedListener(this);


        connectionRequiredMessage = findViewById(R.id.connection_required_message);
        postConnectionUiGroup = findViewById(R.id.post_connection_ui_group);

        updateConnectionUi(false);

        View contentView = findViewById(R.id.content_main);
        progressBar = contentView.findViewById(R.id.progressBar);
        progressBar.setVisibility(View.VISIBLE);
        currencySpinner = contentView.findViewById(R.id.currencySpinner);
        eurBalanceValue = contentView.findViewById(R.id.eurBalanceValue);
        usdBalanceValue = contentView.findViewById(R.id.usdBalanceValue);
        Button connectButton = contentView.findViewById(R.id.connectButton);
        connectButton.setOnClickListener(v -> {
            String selectedAddress = getSelectedAccountAddress();
            if (selectedAddress == null || selectedAddress.isEmpty()) {
                Toast.makeText(this, R.string.please_select_an_account_first, Toast.LENGTH_SHORT).show();
                return;
            }

            progressBar.setVisibility(View.VISIBLE);

            viewModel.connectToEthereum();
        });
        connectButton.setOnClickListener(v -> viewModel.connectToEthereum());

        View headerView = navigationView.getHeaderView(0);
        walletAddressText = headerView.findViewById(R.id.walletAddressText);

        currencyAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, new ArrayList<>());
        currencyAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        currencySpinner.setAdapter(currencyAdapter);
        setupCurrencySpinner();

        observeViewModel();

        setupAccountSelection();

        setupTransactionsList();
    }

    private void observeViewModel() {
        viewModel.getConnectionStatus().observe(this, status -> {

            boolean isNowConnected = status != null && status.contains("Connected") && !status.contains("not");
            if (isNowConnected != isConnected) {
                isConnected = isNowConnected;
                updateConnectionUi(isConnected);

                Button connectButton = findViewById(R.id.connectButton);
                if (isConnected) {
                    connectButton.setText(R.string.connected);
                    connectButton.setEnabled(false);
                } else {
                    connectButton.setText(R.string.connect_to_ethereum);
                    connectButton.setEnabled(true);
                }
            }
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
            sb.append(getString(R.string.your_wallet_balances));

            balances.forEach((symbol, balance) -> sb.append(symbol).append(": ").append(balance.getFormattedWalletBalance()).append("\n"));

            sb.append(getString(R.string.contract_balances));

            balances.forEach((symbol, balance) -> sb.append(symbol).append(": ").append(balance.getFormattedContractBalance()).append("\n"));

            updateWalletBalanceUI(balances);
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
                sb.append(getString(R.string.hash)).append(result.getTransactionHash());
            }

        });

        viewModel.getTransactions().observe(this, transactions -> {
            Log.d("MainActivity", "Received " + (transactions != null ? transactions.size() : 0) + " transactions");
            transactionAdapter.submitList(transactions);

            boolean isEmpty = transactions == null || transactions.isEmpty();
            binding.contentMain.emptyTransactionsMessage.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
            binding.contentMain.transactionsRecyclerView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        });

        viewModel.getIsLoading().observe(this, isLoading -> progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE));
    }

    private void setupSubmenu() {
        FrameLayout submenuContainer = findViewById(R.id.submenu_container);
        submenuView = getLayoutInflater().inflate(R.layout.send_money_submenu, submenuContainer, false);
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

        if (!isConnected && (id == R.id.nav_send_money || id == R.id.nav_transactions || id == R.id.nav_mint_tokens || id == R.id.nav_add_liquidity)) {
            Toast.makeText(this, R.string.please_connect_to_ethereum_first, Toast.LENGTH_SHORT).show();
            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        }

        if (id == R.id.nav_home) {
            clearFragmentBackStack();
        } else if (id == R.id.nav_manage_account) {
            navigateToFragment(new ManageAccountFragment());
        } else if (id == R.id.nav_logout) {
            if (isConnected) {
//                disconnectFromEthereum();
            } else {
                Toast.makeText(this, R.string.not_connected, Toast.LENGTH_SHORT).show();
            }
        } else if (id == R.id.nav_send_money) {
            showSubmenu();
            return true;
        } else if (id == R.id.nav_transactions) {
            Toast.makeText(this, "Transactions feature coming soon", Toast.LENGTH_SHORT).show();
        } else if (id == R.id.nav_mint_tokens) {
            navigateToFragment(new MintFragment());
        } else if (id == R.id.nav_add_liquidity) {
            navigateToFragment(new AddLiquidityFragment());
        }

        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    private void navigateToFragment(androidx.fragment.app.Fragment fragment) {
        boolean requiresConnection = fragment instanceof SendMoneyFragment || fragment instanceof ExchangeFragment || fragment instanceof MintFragment || fragment instanceof AddLiquidityFragment;
        if (requiresConnection && !isConnected) {
            Toast.makeText(this, R.string.please_connect_to_ethereum_first, Toast.LENGTH_SHORT).show();
            return;
        }

        View contentView = findViewById(R.id.content_main);

        if (contentView instanceof ViewGroup) {
            ((ViewGroup) contentView).removeAllViews();

            getSupportFragmentManager().beginTransaction().replace(R.id.content_main, fragment).addToBackStack(null).commit();
        }
    }

    private void clearFragmentBackStack() {
        getSupportFragmentManager().popBackStack(null, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE);

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

                            Toast.makeText(MainActivity.this, getString(R.string.switching_to_account) + selectedAccount.getName(), Toast.LENGTH_SHORT).show();

                            previouslySelectedAddress[0] = newAddress;

                        } catch (JSONException e) {
                            Toast.makeText(MainActivity.this, getString(R.string.error_switching_accounts) + e.getMessage(), Toast.LENGTH_SHORT).show();
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
                Toast.makeText(this, R.string.account_name_is_required, Toast.LENGTH_SHORT).show();
                return;
            }

            if (privateKey.isEmpty()) {
                Toast.makeText(this, R.string.private_key_is_required, Toast.LENGTH_SHORT).show();
                return;
            }

            boolean success;
            try {
                success = viewModel.addAccount(name, privateKey);
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
            if (success) {
                Toast.makeText(this, R.string.account_added_successfully, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, R.string.account_already_exists, Toast.LENGTH_SHORT).show();
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

    private void setupTransactionsList() {
        String currentAddress = "";
        if (viewModel.getActiveAccount().getValue() != null) {
            currentAddress = viewModel.getActiveAccount().getValue().getAddress();
        }

        transactionAdapter = new TransactionAdapter(this, currentAddress);
        binding.contentMain.transactionsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.contentMain.transactionsRecyclerView.setAdapter(transactionAdapter);

        viewModel.getActiveAccount().observe(this, account -> {
            if (account != null) {
                String newAddress = account.getAddress();
                transactionAdapter = new TransactionAdapter(this, newAddress);
                binding.contentMain.transactionsRecyclerView.setAdapter(transactionAdapter);

                if (viewModel.getTransactions().getValue() != null) {
                    transactionAdapter.submitList(viewModel.getTransactions().getValue());
                }
            }
        });

        viewModel.getTransactions().observe(this, transactions -> {
            if (transactions != null) {
                transactionAdapter.submitList(transactions);
                boolean isEmpty = transactions.isEmpty();
                binding.contentMain.emptyTransactionsMessage.setVisibility(
                        isEmpty ? View.VISIBLE : View.GONE);
                binding.contentMain.transactionsRecyclerView.setVisibility(
                        isEmpty ? View.GONE : View.VISIBLE);
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

    private void updateConnectionUi(boolean connected) {
        if (connected) {
            connectionRequiredMessage.setVisibility(View.GONE);

            int[] referencedIds = postConnectionUiGroup.getReferencedIds();
            for (int id : referencedIds) {
                View view = findViewById(id);
                if (view != null) {
                    view.setVisibility(View.VISIBLE);
                }
            }
            enableConnectedMenuItems(true);
        } else {
            connectionRequiredMessage.setVisibility(View.VISIBLE);
            int[] referencedIds = postConnectionUiGroup.getReferencedIds();
            for (int id : referencedIds) {
                View view = findViewById(id);
                if (view != null) {
                    view.setVisibility(View.GONE);
                }
            }
            enableConnectedMenuItems(false);
        }
    }

    private void enableConnectedMenuItems(boolean enable) {
        NavigationView navigationView = findViewById(R.id.nav_view);
        Menu menu = navigationView.getMenu();

        menu.findItem(R.id.nav_send_money).setEnabled(enable);
        menu.findItem(R.id.nav_transactions).setEnabled(enable);
        menu.findItem(R.id.nav_mint_tokens).setEnabled(enable);
        menu.findItem(R.id.nav_add_liquidity).setEnabled(enable);

        if (!enable) {
            menu.findItem(R.id.nav_send_money).setIcon(applyGrayScale(menu.findItem(R.id.nav_send_money).getIcon()));
            menu.findItem(R.id.nav_transactions).setIcon(applyGrayScale(menu.findItem(R.id.nav_transactions).getIcon()));
            menu.findItem(R.id.nav_mint_tokens).setIcon(applyGrayScale(menu.findItem(R.id.nav_mint_tokens).getIcon()));
            menu.findItem(R.id.nav_add_liquidity).setIcon(applyGrayScale(menu.findItem(R.id.nav_add_liquidity).getIcon()));
        }
    }

    private Drawable applyGrayScale(Drawable drawable) {
        if (drawable == null) return null;

        Drawable mutableDrawable = drawable.mutate();
        ColorMatrix matrix = new ColorMatrix();
        matrix.setSaturation(0);
        ColorMatrixColorFilter filter = new ColorMatrixColorFilter(matrix);
        mutableDrawable.setColorFilter(filter);
        return mutableDrawable;
    }

    private String getSelectedAccountAddress() {
        Spinner accountSpinner = findViewById(R.id.accountsSpinner);
        if (accountSpinner.getSelectedItem() == null) return null;

        User selectedAccount = (User) accountSpinner.getSelectedItem();
        return selectedAccount.getWalletAddress();
    }

    private void updateWalletBalanceUI(Map<String, TokenBalance> balances) {
        if (balances == null) return;

        if (balances.containsKey("EURC")) {
            eurBalanceValue.setText(balances.get("EURC").getFormattedWalletBalance() + " EUR");
        }

        if (balances.containsKey("USDT")) {
            usdBalanceValue.setText(balances.get("USDT").getFormattedWalletBalance() + " USD");
        }
    }

    private void refreshData() {
        viewModel.checkAllBalances();
    }

    @Override
    public void onTransactionClick(Transaction transaction) {
        TransactionDetailsDialogFragment dialog = TransactionDetailsDialogFragment.newInstance(transaction);
        dialog.show(getSupportFragmentManager(),"transaction_details");
    }
}