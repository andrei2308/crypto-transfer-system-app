package com.example.crypto_payment_system.ui.exchangeRate;

import static com.example.crypto_payment_system.config.Constants.EURSC;
import static com.example.crypto_payment_system.config.Constants.USDT;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.crypto_payment_system.R;
import com.example.crypto_payment_system.config.ApiConfig;
import com.example.crypto_payment_system.domain.exchangeRate.ExchangeRate;
import com.example.crypto_payment_system.repositories.api.ExchangeRateRepository;
import com.example.crypto_payment_system.utils.simpleFactory.RepositoryFactory;

import java.text.DecimalFormat;

/**
 * Fragment to display exchange rate data
 */
public class ExchangeRateFragment extends Fragment {
    
    private static final String TAG = "ExchangeRateFragment";
    private ExchangeRateRepository exchangeRateRepository;
    private TextView tvExchangeRate;
    private TextView tvFromCurrency;
    private TextView tvToCurrency;
    private TextView tvTimestamp;
    
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        exchangeRateRepository = RepositoryFactory.createExchangeRepository(requireContext(), ApiConfig.BASE_URL, ApiConfig.USERNAME, ApiConfig.PASSWORD);
    }
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_exchange_rate, container, false);
        
        // Initialize views
        tvExchangeRate = view.findViewById(R.id.tv_exchange_rate);
        tvFromCurrency = view.findViewById(R.id.tv_from_currency);
        tvToCurrency = view.findViewById(R.id.tv_to_currency);
        tvTimestamp = view.findViewById(R.id.tv_timestamp);
        
        view.findViewById(R.id.btn_refresh).setOnClickListener(v -> fetchExchangeRate());
        
        return view;
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        fetchExchangeRate();
    }
    
    /**
     * Fetch exchange rate data from the API
     */
    private void fetchExchangeRate() {
        tvExchangeRate.setText("Loading...");
        
        exchangeRateRepository.getExchangeRate()
                .thenAccept(this::updateUI)
                .exceptionally(throwable -> {
                    handleError(throwable);
                    return null;
                });
    }
    
    /**
     * Update UI with exchange rate data
     */
    private void updateUI(ExchangeRate exchangeRate) {
        if (getActivity() == null || !isAdded()) return;
        
        getActivity().runOnUiThread(() -> {
            DecimalFormat df = new DecimalFormat("#.####");
            tvExchangeRate.setText(df.format(exchangeRate.getEurUsd()));
            tvFromCurrency.setText(EURSC);
            tvToCurrency.setText(USDT);
            tvTimestamp.setText(String.valueOf(exchangeRate.getLastUpdated()));
        });
    }
    
    /**
     * Handle API errors
     */
    private void handleError(Throwable throwable) {
        Log.e(TAG, "Error fetching exchange rate", throwable);
        
        if (getActivity() == null || !isAdded()) return;
        
        getActivity().runOnUiThread(() -> {
            tvExchangeRate.setText("Error");
            Toast.makeText(getContext(), 
                    "Failed to load exchange rate: " + throwable.getMessage(),
                    Toast.LENGTH_SHORT).show();
        });
    }
} 