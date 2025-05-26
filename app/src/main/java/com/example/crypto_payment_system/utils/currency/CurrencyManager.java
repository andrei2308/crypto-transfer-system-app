package com.example.crypto_payment_system.utils.currency;

import static com.example.crypto_payment_system.config.Constants.EURSC;
import static com.example.crypto_payment_system.config.Constants.USDT;

import android.content.Context;

import com.example.crypto_payment_system.R;
import com.example.crypto_payment_system.domain.currency.Currency;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Utility class for managing currencies in the application
 */
public class CurrencyManager {

    private static final List<Currency> availableCurrencies = new ArrayList<>();

    /**
     * Initialize the list of available currencies
     * 
     * @param context The context for accessing resources
     */
    public static void initialize(Context context) {
        if (!availableCurrencies.isEmpty()) {
            return; // Already initialized
        }

        // Add currencies with their names and flag icons
        availableCurrencies.add(new Currency(EURSC, context.getString(R.string.euro), R.drawable.ic_flag_eur));
        availableCurrencies.add(new Currency(USDT, context.getString(R.string.us_dollar), R.drawable.ic_flag_usd));
    }

    /**
     * Get all available currencies
     * 
     * @return An unmodifiable list of all available currencies
     */
    public static List<Currency> getAvailableCurrencies() {
        return Collections.unmodifiableList(availableCurrencies);
    }

    /**
     * Get currencies by their codes
     * 
     * @param currencyCodes Array of currency codes to retrieve
     * @return List of found currencies
     */
    public static List<Currency> getCurrenciesByCodes(String... currencyCodes) {
        List<String> codesList = Arrays.asList(currencyCodes);
        List<Currency> result = new ArrayList<>();
        
        for (Currency currency : availableCurrencies) {
            if (codesList.contains(currency.getCode())) {
                result.add(currency);
            }
        }
        
        return result;
    }

    /**
     * Get a currency by its code
     * 
     * @param currencyCode The currency code to find
     * @return The found Currency or null if not found
     */
    public static Currency getCurrencyByCode(String currencyCode) {
        for (Currency currency : availableCurrencies) {
            if (currency.getCode().equals(currencyCode)) {
                return currency;
            }
        }
        
        return null;
    }
} 