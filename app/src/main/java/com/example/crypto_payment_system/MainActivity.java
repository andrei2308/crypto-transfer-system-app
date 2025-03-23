package com.example.crypto_payment_system;


import android.os.Bundle;
import android.widget.Button;
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
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.EthCall;
import org.web3j.protocol.core.methods.response.Web3ClientVersion;
import org.web3j.protocol.http.HttpService;
import org.web3j.abi.datatypes.Function;

import java.io.IOException;
import java.io.InputStream;
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
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        result = findViewById(R.id.resultTextView);
        Button connectButton = findViewById(R.id.connectButton);
        Button callViewMethodButton = findViewById(R.id.callViewMethodButton);
        Button callTransactionMethodButton = findViewById(R.id.callTransactionMethodButton);

        connectButton.setOnClickListener(v -> connectToEthereum());
        callViewMethodButton.setOnClickListener(v->callContractViewMethod());
    }

    private void callContractViewMethod() {
        if(web3j == null || contractAddress == null){
            Toast.makeText(this,"Connect to Ethereum first", Toast.LENGTH_LONG).show();
            return;
        }
        new Thread(()->{
            try{
                Function function = new Function(
                        "getEurcAddress",
                        Collections.emptyList(),
                        Arrays.asList(new TypeReference<org.web3j.abi.datatypes.Address>(){})
                );
                String encodedFunction = FunctionEncoder.encode(function);

                EthCall response = web3j.ethCall(
                        Transaction.createEthCallTransaction(WALLET_ADDRESS,contractAddress,encodedFunction),
                        DefaultBlockParameterName.LATEST
                ).sendAsync().get();

                List<Type> decodedResponse = FunctionReturnDecoder.decode(response.getValue(),function.getOutputParameters());

                String eurcAddress = "";
                if(!decodedResponse.isEmpty()){
                    eurcAddress = decodedResponse.get(0).getValue().toString();
                }

                final String address = eurcAddress;
                runOnUiThread(()->{
                    this.result.setText("Euro Token Address: " + address);
                });
            } catch (Exception e) {
                throw new RuntimeException(e);
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

               runOnUiThread(()->{
                   result.setText("Cnnected to:" + connectedVersion + "\nContract at: " + contractAddress);
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
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (web3j != null) {
            web3j.shutdown();
        }
    }
}