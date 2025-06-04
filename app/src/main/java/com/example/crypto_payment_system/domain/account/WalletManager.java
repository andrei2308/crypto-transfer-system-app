package com.example.crypto_payment_system.domain.account;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.web3j.crypto.Credentials;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Manager class to handle multiple Ethereum wallet accounts
 */
public class WalletManager {
    private final List<WalletAccount> accounts = new ArrayList<>();
    private WalletAccount activeAccount;
    private final MutableLiveData<List<WalletAccount>> accountsLiveData = new MutableLiveData<>();
    private final MutableLiveData<WalletAccount> activeAccountLiveData = new MutableLiveData<>();
    private final SharedPreferences preferences;
    private static final String PREF_ACCOUNTS = "ethereum_accounts";
    private static final String PREF_ACTIVE_ACCOUNT = "active_account";

    public WalletManager(Context context) throws JSONException {
        preferences = context.getSharedPreferences("wallet_prefs", Context.MODE_PRIVATE);
        loadSavedAccounts();
    }

    /**
     * Load saved accounts from SharedPreferences
     */
    private void loadSavedAccounts() throws JSONException {
        String accountsJson = preferences.getString(PREF_ACCOUNTS, "[]");
        String activeAccountAddress = preferences.getString(PREF_ACTIVE_ACCOUNT, null);

        try {
            JSONArray jsonArray = new JSONArray(accountsJson);
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject accountObj = jsonArray.getJSONObject(i);
                WalletAccount account = new WalletAccount(
                        accountObj.getString("name"),
                        accountObj.getString("address"),
                        accountObj.getString("privateKey")
                );
                accounts.add(account);

                // Set active account if it matches the saved active account
                if (account.getAddress().equals(activeAccountAddress)) {
                    activeAccount = account;
                }
            }

            // If no active account was found but we have accounts, set the first one active
            if (activeAccount == null && !accounts.isEmpty()) {
                activeAccount = accounts.get(0);
            }

            // Update LiveData
            accountsLiveData.postValue(accounts);
            activeAccountLiveData.postValue(activeAccount);

        } catch (JSONException e) {
            throw new JSONException("Could not parse json");
        }
    }

    /**
     * Save accounts to SharedPreferences
     */
    private void saveAccounts() throws JSONException {
        try {
            JSONArray jsonArray = new JSONArray();
            for (WalletAccount account : accounts) {
                JSONObject accountObj = new JSONObject();
                accountObj.put("name", account.getName());
                accountObj.put("address", account.getAddress());
                accountObj.put("privateKey", account.getPrivateKey());
                jsonArray.put(accountObj);
            }

            SharedPreferences.Editor editor = preferences.edit();
            editor.putString(PREF_ACCOUNTS, jsonArray.toString());

            if (activeAccount != null) {
                editor.putString(PREF_ACTIVE_ACCOUNT, activeAccount.getAddress());
            }

            editor.apply();
        } catch (JSONException e) {
            throw new JSONException("Could not parse json");
        }
    }

    /**
     * Add a new account
     *
     * @param name       User-friendly name for the account
     * @param privateKey Ethereum private key
     * @return The newly created account, or null if account already exists
     */
    public WalletAccount addAccount(String name, String privateKey) throws JSONException {
        Credentials credentials = Credentials.create(privateKey);
        String address = credentials.getAddress();

        for (WalletAccount account : accounts) {
            if (account.getAddress().equals(address)) {
                return null;
            }
        }

        WalletAccount newAccount = new WalletAccount(name, address, privateKey);
        accounts.add(newAccount);

        if (accounts.size() == 1) {
            activeAccount = newAccount;
            activeAccountLiveData.postValue(activeAccount);
        }

        accountsLiveData.postValue(new ArrayList<>(accounts));
        saveAccounts();
        return newAccount;
    }

    /**
     * Switch to a different account
     *
     * @param address Address of the account to switch to
     */
    public void switchAccount(String address) throws JSONException {
        for (WalletAccount account : accounts) {
            if (account.getAddress().equals(address)) {
                activeAccount = account;
                activeAccountLiveData.postValue(activeAccount);
                saveAccounts();
                return;
            }
        }
    }

    /**
     * Remove an account
     *
     * @param address Address of the account to remove
     * @return true if successful, false if account not found
     */
    public boolean removeAccount(String address) throws JSONException {
        Iterator<WalletAccount> iterator = accounts.iterator();
        while (iterator.hasNext()) {
            WalletAccount account = iterator.next();
            if (account.getAddress().equals(address)) {
                iterator.remove();

                if (activeAccount != null && activeAccount.getAddress().equals(address)) {
                    activeAccount = accounts.isEmpty() ? null : accounts.get(0);
                    activeAccountLiveData.postValue(activeAccount);
                }

                accountsLiveData.postValue(new ArrayList<>(accounts));
                saveAccounts();
                return true;
            }
        }
        return false;
    }

    /**
     * Get the active account's Credentials
     */
    public Credentials getActiveCredentials() {
        if (activeAccount != null) {
            return Credentials.create(activeAccount.getPrivateKey());
        }
        return null;
    }

    /**
     * Get the currently active account
     */
    public WalletAccount getActiveAccount() {
        return activeAccount;
    }

    /**
     * Get a copy of all accounts
     */
    public List<WalletAccount> getAccounts() {
        return new ArrayList<>(accounts);
    }

    /**
     * Get LiveData for observing account list changes
     */
    public LiveData<List<WalletAccount>> getAccountsLiveData() {
        return accountsLiveData;
    }

    /**
     * Get LiveData for observing active account changes
     */
    public LiveData<WalletAccount> getActiveAccountLiveData() {
        return activeAccountLiveData;
    }
}