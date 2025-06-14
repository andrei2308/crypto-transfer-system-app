package com.example.crypto_payment_system.domain.account;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.web3j.crypto.Credentials;

import java.security.KeyStore;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/**
 * Manager class to handle multiple Ethereum wallet accounts
 * Enhanced with basic encryption (no biometric requirement)
 */
public class WalletManager {
    private final List<WalletAccount> accounts = new ArrayList<>();
    private WalletAccount activeAccount;
    private final MutableLiveData<List<WalletAccount>> accountsLiveData = new MutableLiveData<>();
    private final MutableLiveData<WalletAccount> activeAccountLiveData = new MutableLiveData<>();
    private final SharedPreferences preferences;
    private static final String PREF_ACCOUNTS = "ethereum_accounts";
    private static final String PREF_ACTIVE_ACCOUNT = "active_account";
    private static final String KEYSTORE_ALIAS = "wallet_encryption_key_v2";

    public WalletManager(Context context) throws JSONException {
        preferences = context.getSharedPreferences("wallet_prefs", Context.MODE_PRIVATE);
        loadSavedAccounts();
    }

    /**
     * Get or create encryption key (NO biometric requirement)
     */
    private SecretKey getOrCreateEncryptionKey() throws Exception {
        KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
        keyStore.load(null);

        if (!keyStore.containsAlias(KEYSTORE_ALIAS)) {
            KeyGenerator keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");

            KeyGenParameterSpec keyGenParameterSpec = new KeyGenParameterSpec.Builder(
                    KEYSTORE_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setUserAuthenticationRequired(false)
                    .build();

            keyGenerator.init(keyGenParameterSpec);
            keyGenerator.generateKey();
        }

        return (SecretKey) keyStore.getKey(KEYSTORE_ALIAS, null);
    }

    /**
     * Encrypt private key
     */
    private String encryptPrivateKey(String privateKey) throws Exception {
        SecretKey secretKey = getOrCreateEncryptionKey();

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);

        byte[] iv = cipher.getIV();
        byte[] encryptedData = cipher.doFinal(privateKey.getBytes());

        byte[] combined = new byte[iv.length + encryptedData.length];
        System.arraycopy(iv, 0, combined, 0, iv.length);
        System.arraycopy(encryptedData, 0, combined, iv.length, encryptedData.length);

        return Base64.encodeToString(combined, Base64.DEFAULT);
    }

    /**
     * Decrypt private key (always accessible)
     */
    private String decryptPrivateKey(String encryptedData) throws Exception {
        SecretKey secretKey = getOrCreateEncryptionKey();

        byte[] combined = Base64.decode(encryptedData, Base64.DEFAULT);

        byte[] iv = new byte[12];
        byte[] encrypted = new byte[combined.length - 12];
        System.arraycopy(combined, 0, iv, 0, 12);
        System.arraycopy(combined, 12, encrypted, 0, encrypted.length);

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        GCMParameterSpec spec = new GCMParameterSpec(128, iv);
        cipher.init(Cipher.DECRYPT_MODE, secretKey, spec);

        byte[] decryptedData = cipher.doFinal(encrypted);
        return new String(decryptedData);
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

                if (account.getAddress().equals(activeAccountAddress)) {
                    activeAccount = account;
                }
            }

            if (activeAccount == null && !accounts.isEmpty()) {
                activeAccount = accounts.get(0);
            }

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
                accountObj.put("privateKey", account.getPrivateKey()); // Already encrypted
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
     */
    public WalletAccount addAccount(String name, String privateKey) throws Exception {
        Credentials credentials = Credentials.create(privateKey);
        String address = credentials.getAddress();

        for (WalletAccount account : accounts) {
            if (account.getAddress().equals(address)) {
                return null;
            }
        }

        String encryptedPrivateKey = encryptPrivateKey(privateKey);

        WalletAccount newAccount = new WalletAccount(name, address, encryptedPrivateKey);
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
     * Get the active account's Credentials (always accessible)
     */
    public Credentials getActiveCredentials() throws Exception {
        if (activeAccount != null) {
            String decryptedPrivateKey = decryptPrivateKey(activeAccount.getPrivateKey());
            return Credentials.create(decryptedPrivateKey);
        }
        return null;
    }

    /**
     * Get decrypted private key for any account (always accessible)
     */
    public String getDecryptedPrivateKey(String accountName) throws Exception {
        for (WalletAccount account : accounts) {
            if (account.getName().equals(accountName)) {
                return decryptPrivateKey(account.getPrivateKey());
            }
        }
        throw new Exception("Account not found: " + accountName);
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

    /**
     * Set active account
     */
    public void setActiveAccount(WalletAccount account) throws JSONException {
        activeAccount = account;
        activeAccountLiveData.postValue(activeAccount);
        saveAccounts();
    }

    /**
     * Check if account exists
     */
    public boolean accountExists(String name) {
        for (WalletAccount account : accounts) {
            if (account.getName().equals(name)) {
                return true;
            }
        }
        return false;
    }
}