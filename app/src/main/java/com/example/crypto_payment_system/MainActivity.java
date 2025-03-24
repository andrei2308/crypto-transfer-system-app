//TODO: MODULARIZE APPLICATION BY REUSING FUNCTIONS AND REFACTOR THE CODE FOR ABSTRACTION
package com.example.crypto_payment_system;


import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Type;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.RawTransaction;
import org.web3j.crypto.TransactionEncoder;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.EthCall;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.protocol.core.methods.response.Web3ClientVersion;
import org.web3j.protocol.http.HttpService;
import org.web3j.abi.datatypes.Function;
import org.web3j.utils.Numeric;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;


public class MainActivity extends AppCompatActivity {

    private static final String LOCALCHAIN_URL = "http://10.0.2.2:8545";
    private Web3j web3j;
    private String contractAddress;
    private JSONArray contractAbi;
    private final String WALLET_ADDRESS = "0xf39Fd6e51aad88F6F4ce6aB8827279cffFb92266"; // Default Anvil first account
    private final String PRIVATE_KEY = "0xac0974bec39a17e36ba4a6b4d238ff944bacb478cbed5efcae784d7bf4f2ff80"; //Defaul Anvil account private key
    private TextView result;
    private String eurcAddress = ""; // Store EURC address
    private String usdtAddress = ""; // Store USDT address for future use

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        Spinner currencySpinner = findViewById(R.id.currencySpinner);

        result = findViewById(R.id.resultTextView);

        Button connectButton = findViewById(R.id.connectButton);

        Button checkAllBalancesButton = findViewById(R.id.checkAllBalancesButton);

        Button mintTokenButton = findViewById(R.id.mintTokenButton);
        Button approveTokenButton = findViewById(R.id.approveTokenButton);
        Button callTransactionMethodButton = findViewById(R.id.callTransactionMethodButton);
        Button exchangeButton = findViewById(R.id.exchangeButton);

        connectButton.setOnClickListener(v -> connectToEthereum());

        checkAllBalancesButton.setOnClickListener(v -> checkAllBalances());

        mintTokenButton.setOnClickListener(v ->
                {
                   String selectedCurrency = currencySpinner.getSelectedItem().toString();
                   mintEurcTokens(selectedCurrency);
                });
        approveTokenButton.setOnClickListener(v -> {
            String selectedCurrency = currencySpinner.getSelectedItem().toString();
            approveTokenSpending(selectedCurrency);
        });
        callTransactionMethodButton.setOnClickListener(v -> {
            String selectedCurrency = currencySpinner.getSelectedItem().toString();
            callContractTransactionMethod(selectedCurrency);
        });
        exchangeButton.setOnClickListener(v -> exchangeEurToUsd());

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                new String[]{"EUR","USD"}
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        currencySpinner.setAdapter(adapter);

    }

    private String getTokenAddress(String methodName) throws Exception {
        if (web3j == null || contractAddress == null) {
            throw new Exception("Not connected to Ethereum");
        }

        Function function = new Function(
                methodName,
                Collections.emptyList(),
                Arrays.asList(new TypeReference<org.web3j.abi.datatypes.Address>() {})
        );

        String encodedFunction = FunctionEncoder.encode(function);

        EthCall response = web3j.ethCall(
                Transaction.createEthCallTransaction(
                        WALLET_ADDRESS,
                        contractAddress,
                        encodedFunction
                ),
                DefaultBlockParameterName.LATEST
        ).sendAsync().get();

        List<Type> decodedResponse = FunctionReturnDecoder.decode(
                response.getValue(),
                function.getOutputParameters()
        );

        if (decodedResponse.isEmpty()) {
            throw new Exception("Failed to get token address from " + methodName);
        }

        return decodedResponse.get(0).getValue().toString();
    }

    private void callContractTransactionMethod(String stableCoin) {
        String addressOfAddedLiquidity = stableCoin == "USD" ? usdtAddress : eurcAddress;
        if(web3j == null || contractAddress == null){
            Toast.makeText(this,"Connect to Ethereum first",Toast.LENGTH_LONG).show();
            return;
        }
        result.setText("Calling addLiquidity method");

        new Thread(()->{
            try{
                Credentials credentials = Credentials.create(PRIVATE_KEY);

                Function function = new Function("addLiquidity",Arrays.asList(new org.web3j.abi.datatypes.Address(addressOfAddedLiquidity),new org.web3j.abi.datatypes.generated.Uint256(BigInteger.valueOf(100000000))),Collections.emptyList());
                String encodedFunction = FunctionEncoder.encode(function);

                BigInteger nonce = web3j.ethGetTransactionCount(credentials.getAddress(),DefaultBlockParameterName.LATEST).sendAsync().get().getTransactionCount();

                BigInteger gasPrice = web3j.ethGasPrice().sendAsync().get().getGasPrice();
                BigInteger gasLimit = BigInteger.valueOf(300000);

                RawTransaction rawTransaction = RawTransaction.createTransaction(nonce,gasPrice,gasLimit,contractAddress,encodedFunction);
                byte[] signedMessage = TransactionEncoder.signMessage(rawTransaction,credentials);
                String hexValue = Numeric.toHexString(signedMessage);

                EthSendTransaction ethSendTransaction = web3j.ethSendRawTransaction(hexValue).sendAsync().get();

                if(ethSendTransaction.hasError()){
                    throw new Exception("Error sending transaction: " + ethSendTransaction.getError().getMessage());
                }

                String transactionHash = ethSendTransaction.getTransactionHash();

                runOnUiThread(()->{
                    result.setText("Transaction submitted!\nHash: " + transactionHash + "\nWaiting for confirmation...");
                });

                org.web3j.protocol.core.methods.response.TransactionReceipt receipt = waitForTransactionReceipt(transactionHash);

                if (receipt.isStatusOK()) {
                    runOnUiThread(()->{
                        result.setText("addLiquidity transaction successful!\nHash: " + transactionHash);
                    });
                } else {
                    runOnUiThread(()->{
                        result.setText("Transaction reverted on chain!\nHash: " + transactionHash);
                    });
                }
            } catch (Exception e){
                e.printStackTrace();
                final String errorMessage = e.getMessage();
                runOnUiThread(()->{
                    result.setText("Error: " + errorMessage);
                });
            }
        }).start();
    }

    private void approveTokenSpending(String stableCoin) {
        String addressOfStableCoinToApprove = stableCoin == "USD" ? usdtAddress : eurcAddress;
        if(web3j == null || contractAddress == null){
            Toast.makeText(this,"Connect to Ethereum first", Toast.LENGTH_LONG).show();
            return;
        }
        result.setText("Approving token spending...");

        new Thread(() -> {
            try {
                Credentials credentials = Credentials.create(PRIVATE_KEY);

                BigInteger approvalAmount = new BigInteger("1000000000000");

                Function approveFunction = new Function(
                        "approve",
                        Arrays.asList(
                                new org.web3j.abi.datatypes.Address(contractAddress),
                                new org.web3j.abi.datatypes.generated.Uint256(approvalAmount)
                        ),
                        Collections.emptyList()
                );

                String encodedApproveFunction = FunctionEncoder.encode(approveFunction);

                BigInteger nonce = web3j.ethGetTransactionCount(
                        credentials.getAddress(),
                        DefaultBlockParameterName.LATEST
                ).sendAsync().get().getTransactionCount();

                BigInteger gasPrice = web3j.ethGasPrice().sendAsync().get().getGasPrice();
                BigInteger gasLimit = BigInteger.valueOf(100000);

                RawTransaction rawTransaction = RawTransaction.createTransaction(
                        nonce,
                        gasPrice,
                        gasLimit,
                        addressOfStableCoinToApprove,
                        encodedApproveFunction
                );

                byte[] signedMessage = TransactionEncoder.signMessage(rawTransaction, credentials);
                String hexValue = Numeric.toHexString(signedMessage);

                EthSendTransaction ethSendTransaction = web3j.ethSendRawTransaction(hexValue).sendAsync().get();

                if(ethSendTransaction.hasError()){
                    throw new Exception("Error sending approval transaction: " + ethSendTransaction.getError().getMessage());
                }

                String transactionHash = ethSendTransaction.getTransactionHash();

                runOnUiThread(() -> {
                    result.setText("Approval transaction submitted!\nHash: " + transactionHash + "\nWaiting for confirmation...");
                });

                org.web3j.protocol.core.methods.response.TransactionReceipt receipt = waitForTransactionReceipt(transactionHash);

                if (receipt.isStatusOK()) {
                    runOnUiThread(() -> {
                        result.setText("Token approval successful!\nYou can now add liquidity.");
                    });
                } else {
                    runOnUiThread(() -> {
                        result.setText("Token approval failed (reverted on chain)!\nHash: " + transactionHash);
                    });
                }

            } catch (Exception e) {
                e.printStackTrace();
                final String errorMessage = e.getMessage();
                runOnUiThread(() -> {
                    result.setText("Error: " + errorMessage);
                });
            }
        }).start();
    }

    private org.web3j.protocol.core.methods.response.TransactionReceipt waitForTransactionReceipt(String transactionHash) throws Exception {
        int attempts = 0;
        int maxAttempts = 40;
        org.web3j.protocol.core.methods.response.TransactionReceipt receipt = null;

        while (attempts < maxAttempts) {
            org.web3j.protocol.core.methods.response.EthGetTransactionReceipt ethGetTransactionReceipt =
                    web3j.ethGetTransactionReceipt(transactionHash).sendAsync().get();

            if (ethGetTransactionReceipt.getTransactionReceipt().isPresent()) {
                receipt = ethGetTransactionReceipt.getTransactionReceipt().get();
                break;
            }

            Thread.sleep(15000);
            attempts++;
        }

        if (receipt == null) {
            throw new Exception("Transaction not mined after " + maxAttempts + " attempts");
        }

        return receipt;
    }

    private void mintEurcTokens(String stablecoin) {
        final String addressOfStableCoinToMint = stablecoin == "USD" ? usdtAddress : eurcAddress;
        if(web3j == null || contractAddress == null){
            Toast.makeText(this,"Connect to Ethereum first", Toast.LENGTH_LONG).show();
            return;
        }

        result.setText("Preparing to mint EURC tokens...");

        new Thread(() -> {
            try {
                Credentials credentials = Credentials.create(PRIVATE_KEY);

                BigInteger mintAmount = new BigInteger("1000000000");

                Function mintFunction = new Function(
                        "mint",
                        Arrays.asList(
                                new org.web3j.abi.datatypes.Address(credentials.getAddress()),
                                new org.web3j.abi.datatypes.generated.Uint256(mintAmount)
                        ),
                        Collections.emptyList()
                );

                String encodedMintFunction = FunctionEncoder.encode(mintFunction);

                BigInteger nonce = web3j.ethGetTransactionCount(
                        credentials.getAddress(),
                        DefaultBlockParameterName.LATEST
                ).sendAsync().get().getTransactionCount();

                BigInteger gasPrice = web3j.ethGasPrice().sendAsync().get().getGasPrice();
                BigInteger gasLimit = BigInteger.valueOf(200000);

                RawTransaction rawTransaction = RawTransaction.createTransaction(
                        nonce,
                        gasPrice,
                        gasLimit,
                        addressOfStableCoinToMint,
                        encodedMintFunction
                );

                byte[] signedMessage = TransactionEncoder.signMessage(rawTransaction, credentials);
                String hexValue = Numeric.toHexString(signedMessage);

                EthSendTransaction ethSendTransaction = web3j.ethSendRawTransaction(hexValue).sendAsync().get();

                if(ethSendTransaction.hasError()){
                    throw new Exception("Error sending mint transaction: " + ethSendTransaction.getError().getMessage());
                }

                String transactionHash = ethSendTransaction.getTransactionHash();

                runOnUiThread(() -> {
                    result.setText("Mint transaction submitted!\nHash: " + transactionHash + "\nWaiting for confirmation...");
                });

                org.web3j.protocol.core.methods.response.TransactionReceipt receipt = waitForTransactionReceipt(transactionHash);

                if (receipt.isStatusOK()) {
                    runOnUiThread(() -> {
                        result.setText("EURC tokens minted successfully!\nYou can now approve and add liquidity.");
                    });
                } else {
                    runOnUiThread(() -> {
                        result.setText("Token minting failed (reverted on chain)!\nHash: " + transactionHash);
                    });
                }

            } catch (Exception e) {
                e.printStackTrace();
                final String errorMessage = e.getMessage();
                runOnUiThread(() -> {
                    result.setText("Error: " + errorMessage);
                });
            }
        }).start();
    }
    private void connectToEthereum() {
        new Thread(()->{
            try{
                web3j = Web3j.build(new HttpService(LOCALCHAIN_URL));

                Web3ClientVersion clientVersion = web3j.web3ClientVersion().sendAsync().get();
                String connectedVersion = clientVersion.getWeb3ClientVersion();

                loadContractFromJson();

                    eurcAddress = getTokenAddress("getEurcAddress");
                    try {
                        usdtAddress = getTokenAddress("getUsdtAddress");
                    } catch (Exception e) {
                        usdtAddress = "Not available";
                    }

                    final String eurc = eurcAddress;
                    final String usdt = usdtAddress;

                    runOnUiThread(()->{
                        this.result.setText("Euro Token Address: " + eurc + "\nUSD Token Address: " + usdt);
                    });
                runOnUiThread(()->{
                    result.setText("Connected to: " + connectedVersion + "\nContract at: " + contractAddress);
                });
            }catch(Exception e){
                runOnUiThread(()->{
                    result.setText("Error: " + e.getMessage());
                });
            }
        }).start();
    }

    private void loadContractFromJson() throws JSONException {
        String contractJson = loadJsonFromAsset("Exchange.json");
        JSONObject jsonObject = new JSONObject(contractJson);
        contractAddress = jsonObject.getString("address");
        contractAbi = jsonObject.getJSONArray("abi");
    }

    private String loadJsonFromAsset(String s) {
        String json;
        try{
            InputStream is = getAssets().open(s);
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            json = new String(buffer,"UTF-8");
        }catch(IOException ex){
            ex.printStackTrace();
            return null;
        }
        return json;
    }
    private void exchangeEurToUsd() {
        if(web3j == null || contractAddress == null){
            Toast.makeText(this,"Connect to Ethereum first", Toast.LENGTH_LONG).show();
            return;
        }

        result.setText("Preparing to exchange EUR to USD...");

        new Thread(() -> {
            try {
                Credentials credentials = Credentials.create(PRIVATE_KEY);

                BigInteger exchangeAmount = new BigInteger("10000000");

                checkAndApproveIfNeeded(credentials, exchangeAmount);

                Function exchangeFunction = new Function(
                        "exchangeEurToUsd",
                        Arrays.asList(new org.web3j.abi.datatypes.generated.Uint256(exchangeAmount)),
                        Arrays.asList(new TypeReference<org.web3j.abi.datatypes.generated.Uint256>() {})
                );

                String encodedExchangeFunction = FunctionEncoder.encode(exchangeFunction);

                BigInteger nonce = web3j.ethGetTransactionCount(
                        credentials.getAddress(),
                        DefaultBlockParameterName.LATEST
                ).sendAsync().get().getTransactionCount();

                BigInteger gasPrice = web3j.ethGasPrice().sendAsync().get().getGasPrice();
                BigInteger gasLimit = BigInteger.valueOf(300000);

                RawTransaction rawTransaction = RawTransaction.createTransaction(
                        nonce,
                        gasPrice,
                        gasLimit,
                        contractAddress,
                        encodedExchangeFunction
                );

                byte[] signedMessage = TransactionEncoder.signMessage(rawTransaction, credentials);
                String hexValue = Numeric.toHexString(signedMessage);

                EthSendTransaction ethSendTransaction = web3j.ethSendRawTransaction(hexValue).sendAsync().get();

                if(ethSendTransaction.hasError()){
                    throw new Exception("Error sending exchange transaction: " + ethSendTransaction.getError().getMessage());
                }

                String transactionHash = ethSendTransaction.getTransactionHash();

                runOnUiThread(() -> {
                    result.setText("Exchange transaction submitted!\nHash: " + transactionHash + "\nWaiting for confirmation...");
                });

                org.web3j.protocol.core.methods.response.TransactionReceipt receipt = waitForTransactionReceipt(transactionHash);

                if (receipt.isStatusOK()) {
                    String usdAmount = "unknown";
                    try {
                        // Look for ExchangeCompleted event in the logs
                        List<org.web3j.protocol.core.methods.response.Log> logs = receipt.getLogs();
                        // Processing the logs would require more complex parsing
                        // This is just a placeholder for a more complete implementation
                    }
                    catch (Exception e) {
                        // Ignore errors in log parsing
                    }

                    runOnUiThread(() -> {
                        result.setText("Exchange transaction successful!\nHash: " + transactionHash);
                    });
                } else {
                    runOnUiThread(() -> {
                        result.setText("Exchange transaction failed (reverted on chain)!\nHash: " + transactionHash);
                    });
                }

            } catch (Exception e) {
                e.printStackTrace();
                final String errorMessage = e.getMessage();
                runOnUiThread(() -> {
                    result.setText("Error: " + errorMessage);
                });
            }
        }).start();
    }

    private void checkAndApproveIfNeeded(Credentials credentials, BigInteger amount) throws Exception {
        Function allowanceFunction = new Function(
                "allowance",
                Arrays.asList(
                        new org.web3j.abi.datatypes.Address(credentials.getAddress()),
                        new org.web3j.abi.datatypes.Address(contractAddress)
                ),
                Arrays.asList(new TypeReference<org.web3j.abi.datatypes.generated.Uint256>() {})
        );

        String encodedAllowanceFunction = FunctionEncoder.encode(allowanceFunction);

        EthCall allowanceResponse = web3j.ethCall(
                Transaction.createEthCallTransaction(
                        credentials.getAddress(),
                        eurcAddress,
                        encodedAllowanceFunction
                ),
                DefaultBlockParameterName.LATEST
        ).sendAsync().get();

        List<Type> decodedAllowanceResponse = FunctionReturnDecoder.decode(
                allowanceResponse.getValue(),
                allowanceFunction.getOutputParameters()
        );

        BigInteger currentAllowance = BigInteger.ZERO;
        if (!decodedAllowanceResponse.isEmpty()) {
            currentAllowance = (BigInteger) decodedAllowanceResponse.get(0).getValue();
        }

        if (currentAllowance.compareTo(amount) < 0) {
            runOnUiThread(() -> {
                result.setText("Insufficient allowance. Approving tokens...");
            });

            BigInteger approvalAmount = new BigInteger("1000000000000");

            Function approveFunction = new Function(
                    "approve",
                    Arrays.asList(
                            new org.web3j.abi.datatypes.Address(contractAddress),
                            new org.web3j.abi.datatypes.generated.Uint256(approvalAmount)
                    ),
                    Collections.emptyList()
            );

            String encodedApproveFunction = FunctionEncoder.encode(approveFunction);

            BigInteger nonce = web3j.ethGetTransactionCount(
                    credentials.getAddress(),
                    DefaultBlockParameterName.LATEST
            ).sendAsync().get().getTransactionCount();

            BigInteger gasPrice = web3j.ethGasPrice().sendAsync().get().getGasPrice();
            BigInteger gasLimit = BigInteger.valueOf(100000);

            RawTransaction rawTransaction = RawTransaction.createTransaction(
                    nonce,
                    gasPrice,
                    gasLimit,
                    eurcAddress,
                    encodedApproveFunction
            );

            byte[] signedMessage = TransactionEncoder.signMessage(rawTransaction, credentials);
            String hexValue = Numeric.toHexString(signedMessage);

            EthSendTransaction ethSendTransaction = web3j.ethSendRawTransaction(hexValue).sendAsync().get();

            if(ethSendTransaction.hasError()){
                throw new Exception("Error sending approval transaction: " + ethSendTransaction.getError().getMessage());
            }

            String transactionHash = ethSendTransaction.getTransactionHash();

            runOnUiThread(() -> {
                result.setText("Approval transaction submitted!\nHash: " + transactionHash + "\nWaiting for confirmation...");
            });

            org.web3j.protocol.core.methods.response.TransactionReceipt receipt = waitForTransactionReceipt(transactionHash);

            if (!receipt.isStatusOK()) {
                throw new Exception("Token approval failed");
            }

            runOnUiThread(() -> {
                result.setText("Token approval successful! Now proceeding with exchange...");
            });
        }
    }
    private void checkAllBalances() {
        if(web3j == null || contractAddress == null){
            Toast.makeText(this,"Connect to Ethereum first", Toast.LENGTH_LONG).show();
            return;
        }

        result.setText("Checking all balances...");

        new Thread(() -> {
            try {
                Credentials credentials = Credentials.create(PRIVATE_KEY);

                BigInteger walletEurcBalance = getTokenBalance(credentials.getAddress(), eurcAddress);

                BigInteger walletUsdtBalance = getTokenBalance(credentials.getAddress(), usdtAddress);

                BigInteger contractEurcBalance = getContractTokenBalance("getContractEurcBalance", eurcAddress);

                BigInteger contractUsdtBalance = getContractTokenBalance("getContractUsdtBalance", usdtAddress);

                final StringBuilder balanceInfo = new StringBuilder();
                balanceInfo.append("YOUR WALLET BALANCES:\n");
                balanceInfo.append("EURC: ").append(walletEurcBalance).append("\n");
                balanceInfo.append("USDT: ").append(walletUsdtBalance).append("\n\n");

                balanceInfo.append("CONTRACT BALANCES:\n");
                balanceInfo.append("EURC: ").append(contractEurcBalance).append("\n");
                balanceInfo.append("USDT: ").append(contractUsdtBalance);

                runOnUiThread(() -> {
                    result.setText(balanceInfo.toString());
                });

            } catch (Exception e) {
                e.printStackTrace();
                final String errorMessage = e.getMessage();
                runOnUiThread(() -> {
                    result.setText("Error checking balances: " + errorMessage);
                });
            }
        }).start();
    }
    private BigInteger getTokenBalance(String address, String tokenAddress) throws Exception {
        Function function = new Function(
                "balanceOf",
                Arrays.asList(new org.web3j.abi.datatypes.Address(address)),
                Arrays.asList(new TypeReference<org.web3j.abi.datatypes.generated.Uint256>() {})
        );

        String encodedFunction = FunctionEncoder.encode(function);

        EthCall response = web3j.ethCall(
                Transaction.createEthCallTransaction(
                        WALLET_ADDRESS,
                        tokenAddress,
                        encodedFunction
                ),
                DefaultBlockParameterName.LATEST
        ).sendAsync().get();

        List<Type> decodedResponse = FunctionReturnDecoder.decode(
                response.getValue(),
                function.getOutputParameters()
        );

        if(!decodedResponse.isEmpty()) {
            return (BigInteger) decodedResponse.get(0).getValue();
        }
        return BigInteger.ZERO;
    }
    private BigInteger getContractTokenBalance(String methodName, String tokenAddress) throws Exception {
        Function function = new Function(
                methodName,
                Arrays.asList(new org.web3j.abi.datatypes.Address(tokenAddress)),
                Arrays.asList(new TypeReference<org.web3j.abi.datatypes.generated.Uint256>() {})
        );

        String encodedFunction = FunctionEncoder.encode(function);

        EthCall response = web3j.ethCall(
                Transaction.createEthCallTransaction(
                        WALLET_ADDRESS,
                        contractAddress,
                        encodedFunction
                ),
                DefaultBlockParameterName.LATEST
        ).sendAsync().get();

        List<Type> decodedResponse = FunctionReturnDecoder.decode(
                response.getValue(),
                function.getOutputParameters()
        );

        if(!decodedResponse.isEmpty()) {
            return (BigInteger) decodedResponse.get(0).getValue();
        }
        return BigInteger.ZERO;
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (web3j != null) {
            web3j.shutdown();
        }
    }
}