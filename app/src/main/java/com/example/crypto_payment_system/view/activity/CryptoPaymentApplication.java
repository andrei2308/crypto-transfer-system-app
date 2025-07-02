package com.example.crypto_payment_system.view.activity;

import static com.example.crypto_payment_system.config.Constants.CONTRACT_CREATOR_ADDRESS;

import android.annotation.SuppressLint;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
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
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProvider;

import com.example.crypto_payment_system.BuildConfig;
import com.example.crypto_payment_system.R;
import com.example.crypto_payment_system.databinding.ActivityMainBinding;
import com.example.crypto_payment_system.domain.account.AccountNetworkInfo;
import com.example.crypto_payment_system.domain.account.NetworkVerificationCallback;
import com.example.crypto_payment_system.domain.account.WalletAccount;
import com.example.crypto_payment_system.ui.exchange.ExchangeFragment;
import com.example.crypto_payment_system.ui.fiatTransfer.FiatTransferFragment;
import com.example.crypto_payment_system.ui.home.HomeFragment;
import com.example.crypto_payment_system.ui.liquidity.AddLiquidityFragment;
import com.example.crypto_payment_system.ui.mintFunds.MintFragment;
import com.example.crypto_payment_system.ui.sendMoney.SendMoneyFragment;
import com.example.crypto_payment_system.ui.settings.ManageAccountFragment;
import com.example.crypto_payment_system.utils.adapter.account.AccountAdapter;
import com.example.crypto_payment_system.utils.currency.CurrencyManager;
import com.example.crypto_payment_system.view.viewmodels.MainViewModel;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONException;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.response.EthGetBalance;
import org.web3j.protocol.core.methods.response.EthGetTransactionCount;
import org.web3j.protocol.http.HttpService;
import org.web3j.utils.Convert;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class CryptoPaymentApplication extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private MainViewModel viewModel;
    private final static String TAG = "MAIN";
    private ProgressBar progressBar;
    private ProgressBar connectionProgressBar;
    private DrawerLayout drawerLayout;
    private TextView walletAddressText;
    private View submenuView;
    private boolean isSubmenuVisible = false;
    private CardView connectionCard;
    private ConstraintLayout mainContentLayout;
    private boolean isConnected = false;
    private ActivityMainBinding binding;
    private ActionBarDrawerToggle drawerToggle;
    private Toolbar toolbar;
    private View loadingOverlay;
    private TextView loadingText;
    Button connectButton;
    private Handler loadingDelayHandler = new Handler(Looper.getMainLooper());
    private static final int MIN_LOADING_DURATION_MS = 4000;
    private long loadingStartTime;
    private AccountAdapter accountArrayAdapter;

    private boolean initialDataLoaded = false;
    private boolean isInitialConnection = true;
    private boolean isDataLoading = false;
    private AtomicInteger dataLoadingCounter = new AtomicInteger(0);
    private ExecutorService executorService = Executors.newFixedThreadPool(2);


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this).get(MainViewModel.class);

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        drawerLayout = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);

        drawerToggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close
        );

        drawerLayout.addDrawerListener(drawerToggle);
        drawerToggle.syncState();

        setupSubmenu();
        CurrencyManager.initialize(this);

        navigationView.setNavigationItemSelectedListener(this);

        connectionCard = findViewById(R.id.connectionCard);
        mainContentLayout = findViewById(R.id.mainContentLayout);
        progressBar = findViewById(R.id.progressBar);
        connectionProgressBar = findViewById(R.id.connectionProgressBar);

        loadingOverlay = findViewById(R.id.loading_overlay);
        loadingText = findViewById(R.id.loading_text);

        updateConnectionUi(isConnected);

        connectButton = findViewById(R.id.connectButton);
        connectButton.setOnClickListener(v -> {
            String selectedAddress = getSelectedAccountAddress();
            if (selectedAddress == null || selectedAddress.isEmpty()) {
                Toast.makeText(this, R.string.please_select_an_account_first, Toast.LENGTH_SHORT).show();
                return;
            }

            connectButton.setEnabled(false);
            connectionProgressBar.setVisibility(View.VISIBLE);

            isDataLoading = true;
            dataLoadingCounter.set(3);
            isInitialConnection = true;

            showLoadingOverlay("Connecting to Ethereum...");

            viewModel.connectToEthereum();
        });

        View headerView = navigationView.getHeaderView(0);
        walletAddressText = headerView.findViewById(R.id.walletAddressText);

        setupAccountSelection();
        observeViewModel();

        if (savedInstanceState != null) {
            initialDataLoaded = savedInstanceState.getBoolean("initialDataLoaded", false);
            isDataLoading = savedInstanceState.getBoolean("isDataLoading", false);
            loadingStartTime = savedInstanceState.getLong("loadingStartTime", 0);

            if (savedInstanceState.getBoolean("isLoadingVisible", false) && loadingOverlay != null) {
                loadingOverlay.setVisibility(View.VISIBLE);

                long currentTime = System.currentTimeMillis();
                long elapsed = currentTime - loadingStartTime;
                long remainingTime = Math.max(MIN_LOADING_DURATION_MS - elapsed, 0);

                if (remainingTime > 0) {
                    loadingDelayHandler.postDelayed(() -> {
                        if (loadingOverlay != null) {
                            loadingOverlay.setVisibility(View.GONE);
                        }
                    }, remainingTime);
                }
            }
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("initialDataLoaded", initialDataLoaded);
        outState.putBoolean("isDataLoading", isDataLoading);
        outState.putLong("loadingStartTime", loadingStartTime);
        outState.putBoolean("isLoadingVisible", loadingOverlay != null && loadingOverlay.getVisibility() == View.VISIBLE);
    }

    private void observeViewModel() {
        viewModel.getConnectionStatus().observe(this, status -> {
            boolean isNowConnected = status != null && status.contains("Connected") && !status.contains("not");

            if (isNowConnected != isConnected) {
                isConnected = isNowConnected;

                if (isConnected) {
                    Button connectButton = findViewById(R.id.connectButton);
                    connectionProgressBar.setVisibility(View.GONE);
                    connectButton.setText(R.string.connected);
                    connectButton.setEnabled(false);

                    connectionCard.setVisibility(View.GONE);
                    mainContentLayout.setVisibility(View.VISIBLE);

                    enableDrawer(true);

                    if (loadingText != null) {
                        loadingText.setText("Loading your wallet data...");
                    }

                    navigateToHomeFragment();

                    viewModel.checkAllBalances();
                    enableConnectedMenuItems(true);
                } else {
                    Button connectButton = findViewById(R.id.connectButton);
                    connectButton.setText(R.string.connect_to_ethereum);
                    connectButton.setEnabled(true);
                    connectionProgressBar.setVisibility(View.GONE);

                    hideLoadingOverlay();
                    isInitialConnection = true;
                    updateConnectionUi(false);
                }
            }
        });

        viewModel.isNewUser().observe(this, isNew -> {
            if (isNew) {
                hideLoadingOverlay();
                ManageAccountFragment manageAccountFragment = ManageAccountFragment.newInstance(true);
                navigateToFragment(manageAccountFragment);
            }
        });

        viewModel.getCurrentUser().observe(this, user -> {
            if (user != null) {
                walletAddressText.setText(user.getWalletAddress());

                updateAddLiquidityVisibility(user.getWalletAddress());

                completeDataLoadingTask();
            } else {
                walletAddressText.setText(R.string.connect_to_view_wallet_address);

                updateAddLiquidityVisibility(null);
            }
        });

        viewModel.getTokenBalances().observe(this, balances -> {
            completeDataLoadingTask();
        });

        viewModel.getTransactions().observe(this, allTransactions -> {
            String currentCurrency = viewModel.getSelectedCurrency().getValue();
            if (currentCurrency != null) {
                viewModel.updateFilteredTransactions(currentCurrency);
            }
            completeDataLoadingTask();
        });

        viewModel.getFilteredTransactions().observe(this, transactions -> {
            if (isDataLoading && transactions != null) {
                completeDataLoadingTask();
            }
        });

        viewModel.getIsLoading().observe(this, isLoading -> {
            progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        });
    }

    /**
     * Check if the given wallet address is the contract creator
     */
    private boolean isContractCreator(String walletAddress) {
        if (walletAddress == null || walletAddress.trim().isEmpty()) {
            return false;
        }

        String cleanUserAddress = normalizeAddress(walletAddress);
        String cleanCreatorAddress = normalizeAddress(CONTRACT_CREATOR_ADDRESS);

        return cleanCreatorAddress.equalsIgnoreCase(cleanUserAddress);
    }

    /**
     * Normalize an Ethereum address by removing spaces and ensuring proper format
     */
    private String normalizeAddress(String address) {
        if (address == null) return "";

        String cleaned = address.trim();

        if (!cleaned.startsWith("0x") && !cleaned.startsWith("0X")) {
            cleaned = "0x" + cleaned;
        }

        return cleaned;
    }

    /**
     * Update the visibility of the Add Liquidity menu item based on wallet address
     */
    private void updateAddLiquidityVisibility(String walletAddress) {
        NavigationView navigationView = findViewById(R.id.nav_view);
        if (navigationView != null) {
            Menu menu = navigationView.getMenu();
            MenuItem addLiquidityItem = menu.findItem(R.id.nav_add_liquidity);

            if (addLiquidityItem != null) {
                boolean canAccess = isContractCreator(walletAddress);
                addLiquidityItem.setVisible(canAccess);

                Log.d(TAG, "Add Liquidity menu item visibility: " + canAccess + " for address: " + walletAddress);
            }
        }
    }

    private void enableConnectedMenuItems(boolean enable) {
        NavigationView navigationView = findViewById(R.id.nav_view);
        Menu menu = navigationView.getMenu();

        menu.findItem(R.id.nav_send_money).setEnabled(enable);
        menu.findItem(R.id.nav_mint_tokens).setEnabled(enable);

        MenuItem addLiquidityItem = menu.findItem(R.id.nav_add_liquidity);
        if (addLiquidityItem != null) {
            String currentWalletAddress = getCurrentWalletAddress();
            boolean canAccessLiquidity = enable && isContractCreator(currentWalletAddress);
            addLiquidityItem.setEnabled(canAccessLiquidity);

            if (!canAccessLiquidity) {
                addLiquidityItem.setIcon(applyGrayScale(addLiquidityItem.getIcon()));
            }
        }

        if (!enable) {
            menu.findItem(R.id.nav_send_money).setIcon(applyGrayScale(menu.findItem(R.id.nav_send_money).getIcon()));
            menu.findItem(R.id.nav_mint_tokens).setIcon(applyGrayScale(menu.findItem(R.id.nav_mint_tokens).getIcon()));
        }
    }

    /**
     * Get the current wallet address from the selected account
     */
    private String getCurrentWalletAddress() {
        String selectedAddress = getSelectedAccountAddress();
        if (selectedAddress != null && !selectedAddress.isEmpty()) {
            return selectedAddress;
        }

        if (viewModel.getCurrentUser().getValue() != null) {
            return viewModel.getCurrentUser().getValue().getWalletAddress();
        }

        return null;
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setupAccountSelection() {
        try {
            Log.d(TAG, "Setting up account selection");

            View accountSelectionView = findViewById(R.id.account_selection_layout);

            if (accountSelectionView == null) {
                Log.e(TAG, "Account selection layout not found");
                return;
            }

            Spinner accountsSpinner = accountSelectionView.findViewById(R.id.accountsSpinner);

            if (accountsSpinner == null) {
                Log.e(TAG, "Account spinner not found in the account selection layout");
                return;
            }

            MaterialButton addAccountButton = accountSelectionView.findViewById(R.id.addAccountButton);

            accountSelectionView.setOnTouchListener((v, event) -> false);

            accountArrayAdapter = new AccountAdapter(this, new ArrayList<>());

            accountArrayAdapter.setOnAccountDeleteListener(this::showDeleteAccountDialog);

            accountsSpinner.setAdapter(accountArrayAdapter);

            accountsSpinner.setClickable(true);
            accountsSpinner.setFocusable(true);
            accountsSpinner.setEnabled(true);

            accountsSpinner.setOnTouchListener((v, event) -> {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    accountsSpinner.performClick();
                    return true;
                }
                return false;
            });

            final String[] previouslySelectedAddress = {null};

            accountsSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    WalletAccount selectedAccount = (WalletAccount) parent.getItemAtPosition(position);
                    if (selectedAccount != null) {
                        String newAddress = selectedAccount.getAddress();

                        updateAddLiquidityVisibility(newAddress);

                        boolean isConnected = viewModel.getConnectionStatus().getValue() != null &&
                                !viewModel.getConnectionStatus().getValue().startsWith("Error");

                        if (!isConnected) {
                            if (walletAddressText != null) {
                                walletAddressText.setText(newAddress);
                            }
                            previouslySelectedAddress[0] = newAddress;
                            return;
                        }

                        if (!newAddress.equals(previouslySelectedAddress[0])) {
                            try {
                                if (progressBar != null) {
                                    progressBar.setVisibility(View.VISIBLE);
                                }

                                if (walletAddressText != null) {
                                    walletAddressText.setText(newAddress);
                                }

                                viewModel.switchAccount(newAddress);

                                Toast.makeText(CryptoPaymentApplication.this, getString(R.string.switching_to_account) + selectedAccount.getName(), Toast.LENGTH_SHORT).show();
                                previouslySelectedAddress[0] = newAddress;

                            } catch (JSONException e) {
                                Toast.makeText(CryptoPaymentApplication.this, getString(R.string.error_switching_accounts) + e.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                    updateAddLiquidityVisibility(null);
                }
            });

            viewModel.getAccounts().observe(this, accounts -> {
                accountArrayAdapter.clear();
                accountArrayAdapter.addAll(accounts);
                accountArrayAdapter.notifyDataSetChanged();

                WalletAccount activeAccount = viewModel.getActiveAccount().getValue();
                if (activeAccount != null) {
                    for (int i = 0; i < accountArrayAdapter.getCount(); i++) {
                        WalletAccount account = accountArrayAdapter.getItem(i);
                        if (account != null && account.getAddress().equals(activeAccount.getAddress())) {
                            accountsSpinner.setSelection(i);
                            previouslySelectedAddress[0] = activeAccount.getAddress();

                            // Update Add Liquidity visibility for the active account
                            updateAddLiquidityVisibility(activeAccount.getAddress());
                            break;
                        }
                    }
                }
            });

            if (addAccountButton != null) {
                addAccountButton.setOnClickListener(v -> {
                    showAccountDialog();
                });
            }
        } catch (Exception e) {
            Log.e(TAG, "Error setting up account selection", e);
            Toast.makeText(this, "Error setting up account selection", Toast.LENGTH_SHORT).show();
        }
    }

    private void showDeleteAccountDialog(WalletAccount account, int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Delete Account");

        String message = String.format(
                "Are you sure you want to delete the account?\n\n" +
                        "Name: %s\n" +
                        "Address: %s\n\n" +
                        "This action cannot be undone.",
                account.getName(),
                formatAddress(account.getAddress())
        );

        builder.setMessage(message);

        builder.setPositiveButton("Yes, Delete", (dialog, which) -> {
            deleteAccount(account, position);
            dialog.dismiss();
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> {
            dialog.dismiss();
        });

        builder.setCancelable(true);
        AlertDialog dialog = builder.create();
        dialog.show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(
                getColor(android.R.color.holo_red_dark)
        );
    }


    private void deleteAccount(WalletAccount account, int position) {
        try {
            if (accountArrayAdapter != null && accountArrayAdapter.getCount() <= 1) {
                Toast.makeText(this, "Cannot delete the last account. Add another account first.", Toast.LENGTH_LONG).show();
                return;
            }

            boolean success = viewModel.removeAccount(account.getAddress());

            if (success) {
                Toast.makeText(this,
                        String.format("Account '%s' deleted successfully", account.getName()),
                        Toast.LENGTH_SHORT).show();


                WalletAccount activeAccount = viewModel.getActiveAccount().getValue();
                if (activeAccount != null && activeAccount.getAddress().equals(account.getAddress())) {
                    Log.d(TAG, "Deleted active account, ViewModel will select new active account");
                }

            } else {
                Toast.makeText(this, "Failed to delete account", Toast.LENGTH_SHORT).show();
            }

        } catch (Exception e) {
            Toast.makeText(this, "Error deleting account: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Error deleting account", e);
        }
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

        submenuView.findViewById(R.id.option_card_payment).setOnClickListener(v -> {
            hideSubmenu();
            navigateToFragment(new FiatTransferFragment());
            drawerLayout.closeDrawer(GravityCompat.START);
        });
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (!isConnected && (id == R.id.nav_send_money || id == R.id.nav_mint_tokens || id == R.id.nav_add_liquidity)) {
            Toast.makeText(this, R.string.please_connect_to_ethereum_first, Toast.LENGTH_SHORT).show();
            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        }

        if (id == R.id.nav_home) {
            if (isConnected) {
                showLoadingOverlay("Loading transactions...");
                isDataLoading = true;
                dataLoadingCounter.set(1);
                isInitialConnection = false;

                navigateToHomeFragment();
            }
        } else if (id == R.id.nav_manage_account) {
            navigateToFragment(new ManageAccountFragment());
        } else if (id == R.id.nav_logout) {
            if (isConnected) {
                isConnected = false;
                updateConnectionUi(false);

                Button connectButton = findViewById(R.id.connectButton);
                connectButton.setText(R.string.connect_to_ethereum);
                connectButton.setEnabled(true);

                isInitialConnection = true;
            } else {
                Toast.makeText(this, R.string.not_connected, Toast.LENGTH_SHORT).show();
            }
        } else if (id == R.id.nav_send_money) {
            showSubmenu();
            return true;
        } else if (id == R.id.nav_mint_tokens) {
            navigateToFragment(new MintFragment());
        } else if (id == R.id.nav_add_liquidity) {
            navigateToFragment(new AddLiquidityFragment());
        }

        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    public void navigateToHomeFragment() {
        clearFragmentBackStack();
        mainContentLayout.setVisibility(View.VISIBLE);

        FragmentManager fragmentManager = getSupportFragmentManager();
        HomeFragment homeFragment = HomeFragment.newInstance();

        fragmentManager.beginTransaction()
                .replace(R.id.content_main, homeFragment)
                .commit();
    }

    private void clearBackStack() {
        getSupportFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
    }

    private void navigateToFragment(Fragment fragment) {
        boolean requiresConnection = fragment instanceof SendMoneyFragment || fragment instanceof ExchangeFragment || fragment instanceof MintFragment || fragment instanceof AddLiquidityFragment;
        if (requiresConnection && !isConnected) {
            Toast.makeText(this, R.string.please_connect_to_ethereum_first, Toast.LENGTH_SHORT).show();
            return;
        }

        hideLoadingOverlay();
        isDataLoading = false;

        View contentView = findViewById(R.id.content_main);

        if (contentView instanceof ViewGroup) {
            clearBackStack();

            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.content_main, fragment)
                    .addToBackStack(null)
                    .commit();
        }
    }

    private void clearFragmentBackStack() {
        getSupportFragmentManager().popBackStack(null, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE);
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
        ProgressBar networkCheckProgress = dialogView.findViewById(R.id.networkCheckProgress);
        TextView networkStatusText = dialogView.findViewById(R.id.networkStatusText);

        builder.setPositiveButton("Add", null);
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();
        dialog.show();

        Button addButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        addButton.setOnClickListener(v -> {
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

            if (!isValidPrivateKey(privateKey)) {
                Toast.makeText(this, R.string.invalid_private_key, Toast.LENGTH_SHORT).show();
                return;
            }

            addButton.setEnabled(false);
            networkCheckProgress.setVisibility(View.VISIBLE);
            networkStatusText.setText(R.string.verifying_account_on_network);
            networkStatusText.setVisibility(View.VISIBLE);

            verifyAccountOnNetwork(privateKey, new NetworkVerificationCallback() {
                @Override
                public void onSuccess(String walletAddress, AccountNetworkInfo networkInfo) {
                    runOnUiThread(() -> {
                        networkCheckProgress.setVisibility(View.GONE);
                        networkStatusText.setVisibility(View.GONE);
                        showNetworkConfirmation(name, privateKey, walletAddress, networkInfo, dialog);
                    });
                }

                @SuppressLint("SetTextI18n")
                @Override
                public void onError(String error) {
                    runOnUiThread(() -> {
                        networkCheckProgress.setVisibility(View.GONE);
                        networkStatusText.setText(getString(R.string.network_verification_failed) + error);
                        networkStatusText.setTextColor(getColor(android.R.color.holo_red_dark));
                        addButton.setEnabled(true);
                        Toast.makeText(CryptoPaymentApplication.this, "Cannot verify account on network. Please check your private key.", Toast.LENGTH_LONG).show();
                    });
                }
            });
        });
    }

    private boolean isValidPrivateKey(String privateKey) {
        try {
            String cleanKey = privateKey.startsWith("0x") ? privateKey.substring(2) : privateKey;

            if (cleanKey.length() != 64) {
                return false;
            }

            BigInteger keyValue = new BigInteger(cleanKey, 16);
            BigInteger secp256k1Order = new BigInteger("FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFEBAAEDCE6AF48A03BBFD25E8CD0364141", 16);

            if (keyValue.equals(BigInteger.ZERO) || keyValue.compareTo(secp256k1Order) >= 0) {
                return false;
            }

            Credentials credentials = Credentials.create(privateKey);
            String address = credentials.getAddress();

            return address != null && address.startsWith("0x") && address.length() == 42;

        } catch (Exception e) {
            return false;
        }
    }

    private void verifyAccountOnNetwork(String privateKey, NetworkVerificationCallback callback) {
        executorService.execute(() -> {
            try {
                Credentials credentials = Credentials.create(privateKey);
                String walletAddress = credentials.getAddress();
                AccountNetworkInfo networkInfo = checkAccountOnBlockchain(walletAddress);
                callback.onSuccess(walletAddress, networkInfo);
            } catch (Exception e) {
                callback.onError(e.getMessage());
            }
        });
    }

    private AccountNetworkInfo checkAccountOnBlockchain(String address) throws Exception {
        Web3j web3j = Web3j.build(new HttpService(BuildConfig.ALCHEMY_NODE));

        try {
            EthGetBalance balanceResponse = web3j.ethGetBalance(address, DefaultBlockParameterName.LATEST).send();
            BigInteger balance = balanceResponse.getBalance();

            EthGetTransactionCount txCountResponse = web3j.ethGetTransactionCount(address, DefaultBlockParameterName.LATEST).send();
            BigInteger transactionCount = txCountResponse.getTransactionCount();

            boolean hasActivity = transactionCount.compareTo(BigInteger.ZERO) > 0;
            BigDecimal ethBalance = Convert.fromWei(new BigDecimal(balance), Convert.Unit.ETHER);

            if (!hasActivity && ethBalance.compareTo(BigDecimal.ZERO) == 0) {
                throw new Exception("Account has never been used on the blockchain");
            }

            return new AccountNetworkInfo(
                    address,
                    ethBalance,
                    transactionCount.longValue(),
                    hasActivity,
                    "Sepolia Testnet"
            );
        } finally {
            web3j.shutdown();
        }
    }

    private void showNetworkConfirmation(String name, String privateKey, String address,
                                         AccountNetworkInfo networkInfo, AlertDialog parentDialog) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Account Verified");

        @SuppressLint("DefaultLocale") String message = String.format(
                "Account successfully verified on %s:\n\n" +
                        "Address: %s\n" +
                        "Balance: %s\n" +
                        "Transactions: %d\n" +
                        "Status: %s\n\n" +
                        "Add this account to your wallet?",
                networkInfo.getNetworkName(),
                formatAddress(networkInfo.getAddress()),
                networkInfo.getFormattedBalance(),
                networkInfo.getTransactionCount(),
                networkInfo.hasActivity() ? "Active" : "New Account"
        );

        builder.setMessage(message);
        builder.setIcon(R.drawable.ic_check_circle_green);

        builder.setPositiveButton("Add Account", (dialog, which) -> {
            addVerifiedAccount(name, privateKey, networkInfo);
            parentDialog.dismiss();
            dialog.dismiss();
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> {
            Button addButton = parentDialog.getButton(AlertDialog.BUTTON_POSITIVE);
            addButton.setEnabled(true);
            TextView networkStatusText = parentDialog.findViewById(R.id.networkStatusText);
            if (networkStatusText != null) {
                networkStatusText.setVisibility(View.GONE);
            }
            dialog.dismiss();
        });

        builder.setCancelable(false);
        builder.show();
    }

    private String formatAddress(String address) {
        if (address.length() > 10) {
            return address.substring(0, 6) + "..." + address.substring(address.length() - 4);
        }
        return address;
    }

    private void addVerifiedAccount(String name, String privateKey, AccountNetworkInfo networkInfo) {
        boolean success = viewModel.addAccount(name, privateKey);
        if (success) {
            String message = String.format("Account '%s' added successfully!\nBalance: %s",
                    name, networkInfo.getFormattedBalance());
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, R.string.account_already_exists, Toast.LENGTH_SHORT).show();
        }
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

    /**
     * Update the UI based on connection status
     *
     * @param connected true if connected, false otherwise
     */
    private void updateConnectionUi(boolean connected) {
        if (connected) {
            connectionCard.setVisibility(View.GONE);
            mainContentLayout.setVisibility(View.VISIBLE);

            enableDrawer(true);
            enableConnectedMenuItems(true);

            Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.content_main);
            if (!(currentFragment instanceof HomeFragment)) {
                navigateToHomeFragment();
            }
        } else {
            connectionCard.setVisibility(View.VISIBLE);
            mainContentLayout.setVisibility(View.GONE);

            clearFragmentContainer();

            enableDrawer(false);
            enableConnectedMenuItems(false);
        }
    }

    /**
     * Clear any fragments from the container when disconnecting
     */
    private void clearFragmentContainer() {
        Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.content_main);
        if (currentFragment != null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .remove(currentFragment)
                    .commitNow();
        }
    }

    /**
     * Enable or disable the drawer navigation
     *
     * @param enable True to enable drawer, false to disable
     */
    private void enableDrawer(boolean enable) {
        if (enable) {
            drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            }
            drawerToggle.setDrawerIndicatorEnabled(true);
            drawerToggle.syncState();
        } else {
            if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                drawerLayout.closeDrawer(GravityCompat.START);
            }

            drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);

            drawerToggle.setDrawerIndicatorEnabled(false);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(false);
            }
            drawerToggle.syncState();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (drawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        return super.onOptionsItemSelected(item);
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
            if (!isConnected && connectionCard.getVisibility() == View.VISIBLE) {
                finish();
            } else {
                super.onBackPressed();
            }
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
        if (accountSpinner == null || accountSpinner.getSelectedItem() == null) return null;

        WalletAccount selectedAccount = (WalletAccount) accountSpinner.getSelectedItem();
        return selectedAccount.getAddress();
    }

    /**
     * Show loading overlay with full white background
     *
     * @param message Message to display
     */
    private void showLoadingOverlay(String message) {
        loadingStartTime = System.currentTimeMillis();

        runOnUiThread(() -> {
            if (loadingText != null) {
                loadingText.setText(message);
            }

            if (loadingOverlay != null) {
                loadingOverlay.setVisibility(View.VISIBLE);

                if (loadingOverlay.getParent() instanceof ViewGroup) {
                    ViewGroup parent = (ViewGroup) loadingOverlay.getParent();
                    parent.bringChildToFront(loadingOverlay);
                }
            }

            drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
        });
    }

    /**
     * Hide loading overlay with a minimum delay to ensure it's visible for at least 4 seconds
     */
    private void hideLoadingOverlay() {
        if (loadingOverlay == null) {
            return;
        }

        if (loadingOverlay.getVisibility() != View.VISIBLE) {
            return;
        }

        long currentTime = System.currentTimeMillis();
        long timeElapsed = currentTime - loadingStartTime;

        if (timeElapsed >= MIN_LOADING_DURATION_MS) {
            loadingOverlay.setVisibility(View.GONE);
        } else {
            long remainingDelay = MIN_LOADING_DURATION_MS - timeElapsed;

            loadingDelayHandler.postDelayed(() -> {
                if (loadingOverlay != null) {
                    loadingOverlay.setVisibility(View.GONE);
                }
            }, remainingDelay);
        }
    }

    /**
     * Register a data loading task
     *
     * @return A unique ID for the task
     */
    private int registerDataLoadingTask() {
        return dataLoadingCounter.incrementAndGet();
    }

    /**
     * Complete a data loading task and check if all tasks are complete
     */
    private void completeDataLoadingTask() {
        int remainingTasks = dataLoadingCounter.decrementAndGet();

        if (remainingTasks <= 0 && isDataLoading) {
            isDataLoading = false;
            initialDataLoaded = true;

            runOnUiThread(() -> {
                hideLoadingOverlay();
            });
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (loadingDelayHandler != null) {
            loadingDelayHandler.removeCallbacksAndMessages(null);
            loadingDelayHandler = null;
            if (executorService != null && !executorService.isShutdown()) {
                executorService.shutdown();
            }
        }
    }

    public MainViewModel getViewModel() {
        return viewModel;
    }
}