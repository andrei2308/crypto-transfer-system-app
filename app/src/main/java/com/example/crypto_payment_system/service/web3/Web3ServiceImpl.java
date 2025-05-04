package com.example.crypto_payment_system.service.web3;

import android.content.Context;
import android.util.Log;

import com.example.crypto_payment_system.BuildConfig;
import com.example.crypto_payment_system.config.Constants;
import com.example.crypto_payment_system.utils.web3.Web3Utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.core.methods.response.Web3ClientVersion;
import org.web3j.protocol.http.HttpService;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

/**
 * Service class for Web3 basic operations and connection management
 */
public class Web3ServiceImpl implements Web3Service {
    private Web3j web3j;
    private String contractAddress;
    private JSONArray contractAbi;
    private final Context context;

    public Web3ServiceImpl(Context context) {
        this.context = context;
    }

    /**
     * Initialize Web3 connection and load contract data
     */
    @Override
    public String connect() throws Exception {
        try {
            web3j = Web3j.build(new HttpService(BuildConfig.ALCHEMY_NODE));

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
    private void loadContractFromJson() throws JSONException, IOException {
        String contractJson = loadJsonFromAsset();
        JSONObject jsonObject = new JSONObject(contractJson);
        contractAddress = jsonObject.getString("address");
        contractAbi = jsonObject.getJSONArray("abi");
    }

    /**
     * Helper method to load JSON from assets
     */
    private String loadJsonFromAsset() throws IOException {
        String json;
        try {
            InputStream is = context.getAssets().open(Constants.CONTRACT_JSON_FILE);
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            json = new String(buffer, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new IOException("Could not read the contract json file");
        }
        return json;
    }

    /**
     * Wait for transaction receipt with timeout
     */
    @Override
    public TransactionReceipt waitForTransactionReceipt(String transactionHash)
            throws Exception {
        return Web3Utils.waitForTransactionReceipt(web3j, transactionHash);
    }

    /**
     * Clean up resources when no longer needed
     */
    @Override
    public void shutdown() {
        if (web3j != null) {
            web3j.shutdown();
        }
    }

    @Override
    public Web3j getWeb3j() {
        return web3j;
    }

    @Override
    public String getContractAddress() {
        return contractAddress;
    }

    public JSONArray getContractAbi() {
        return contractAbi;
    }

    @Override
    public boolean isConnected() {
        return web3j != null && contractAddress != null;
    }
}