package com.example.crypto_payment_system.api;

import android.content.Context;
import android.util.Log;

import com.example.crypto_payment_system.config.Constants;
import com.example.crypto_payment_system.utils.Web3Utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.core.methods.response.Web3ClientVersion;
import org.web3j.protocol.http.HttpService;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * Service class for Web3 basic operations and connection management
 */
public class Web3Service {
    private Web3j web3j;
    private String contractAddress;
    private JSONArray contractAbi;
    private final Context context;

    public Web3Service(Context context) {
        this.context = context;
    }

    /**
     * Initialize Web3 connection and load contract data
     */
    public String connect() throws Exception {
        try {
            web3j = Web3j.build(new HttpService(Constants.LOCALCHAIN_URL));

            // Test connection with timeout
            Web3ClientVersion clientVersion = web3j.web3ClientVersion()
                    .sendAsync()
                    .get(10, TimeUnit.SECONDS); // Add timeout

            String connectedVersion = clientVersion.getWeb3ClientVersion();

            if (connectedVersion == null || connectedVersion.isEmpty()) {
                throw new Exception("Connected, but received empty client version");
            }

            // Log success
            Log.d("Web3Service", "Successfully connected to: " + connectedVersion);

            try {
                loadContractFromJson();
                Log.d("Web3Service", "Contract loaded successfully at: " + contractAddress);
            } catch (Exception e) {
                Log.e("Web3Service", "Error loading contract: " + e.getMessage());
                throw new Exception("Connected to Ethereum, but failed to load contract: " + e.getMessage());
            }

            return connectedVersion;
        } catch (Exception e) {
            Log.e("Web3Service", "Connection error: " + e.getMessage(), e);
            throw new Exception("Failed to connect to Ethereum: " + e.getMessage());
        }
    }

    /**
     * Load contract data from JSON asset file
     */
    private void loadContractFromJson() throws JSONException {
        String contractJson = loadJsonFromAsset(Constants.CONTRACT_JSON_FILE);
        JSONObject jsonObject = new JSONObject(contractJson);
        contractAddress = jsonObject.getString("address");
        contractAbi = jsonObject.getJSONArray("abi");
    }

    /**
     * Helper method to load JSON from assets
     */
    private String loadJsonFromAsset(String fileName) {
        String json;
        try {
            InputStream is = context.getAssets().open(fileName);
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            json = new String(buffer, "UTF-8");
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
        return json;
    }

    /**
     * Wait for transaction receipt with timeout
     */
    public TransactionReceipt waitForTransactionReceipt(String transactionHash)
            throws InterruptedException, ExecutionException, Exception {
        return Web3Utils.waitForTransactionReceipt(web3j, transactionHash);
    }

    /**
     * Clean up resources when no longer needed
     */
    public void shutdown() {
        if (web3j != null) {
            web3j.shutdown();
        }
    }
    public Web3j getWeb3j() {
        return web3j;
    }

    public String getContractAddress() {
        return contractAddress;
    }

    public JSONArray getContractAbi() {
        return contractAbi;
    }

    public boolean isConnected() {
        return web3j != null && contractAddress != null;
    }
}