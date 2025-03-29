package com.example.crypto_payment_system;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.crypto_payment_system.models.TokenBalance;
import com.example.crypto_payment_system.repositories.TokenRepository.TransactionResult;
import com.example.crypto_payment_system.viewmodels.MainViewModel;
import com.google.android.material.textfield.TextInputEditText;

import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private MainViewModel viewModel;
    private TextView resultTextView;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        viewModel = new ViewModelProvider(this).get(MainViewModel.class);

        resultTextView = findViewById(R.id.resultTextView);
        progressBar = findViewById(R.id.progressBar);
        Spinner currencySpinner = findViewById(R.id.currencySpinner);
        Button connectButton = findViewById(R.id.connectButton);
        Button checkAllBalancesButton = findViewById(R.id.checkAllBalancesButton);
        Button mintTokenButton = findViewById(R.id.mintTokenButton);
        Button callTransactionMethodButton = findViewById(R.id.callTransactionMethodButton);
        Button exchangeButton = findViewById(R.id.exchangeButton);
        Button sendMoneyButton = findViewById(R.id.send_money_btn);
        TextInputEditText addressTeit = findViewById(R.id.address_teit);

        // Set up currency spinner
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

        exchangeButton.setOnClickListener(v -> viewModel.exchangeEurToUsd());

        sendMoneyButton.setOnClickListener(v->viewModel.sendMoney(addressTeit.getText().toString()));

        observeViewModel();
    }

    private void observeViewModel() {
        viewModel.getConnectionStatus().observe(this, status -> {
            resultTextView.setText(status);
        });

        viewModel.getTokenAddresses().observe(this, addresses -> {
            StringBuilder sb = new StringBuilder();
            sb.append("Token Addresses:\n");
            for (Map.Entry<String, String> entry : addresses.entrySet()) {
                sb.append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
            }
            resultTextView.setText(sb.toString());
        });

        viewModel.getTokenBalances().observe(this, balances -> {
            StringBuilder sb = new StringBuilder();
            sb.append("YOUR WALLET BALANCES:\n");

            TokenBalance eurcBalance = balances.get("EURC");
            TokenBalance usdtBalance = balances.get("USDT");

            if (eurcBalance != null) {
                sb.append("EURC: ").append(eurcBalance.getWalletBalance()).append("\n");
            }

            if (usdtBalance != null) {
                sb.append("USDT: ").append(usdtBalance.getWalletBalance()).append("\n\n");
            }

            sb.append("CONTRACT BALANCES:\n");

            if (eurcBalance != null) {
                sb.append("EURC: ").append(eurcBalance.getContractBalance()).append("\n");
            }

            if (usdtBalance != null) {
                sb.append("USDT: ").append(usdtBalance.getContractBalance());
            }

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
}