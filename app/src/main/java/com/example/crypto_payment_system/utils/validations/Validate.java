package com.example.crypto_payment_system.utils.validations;

import com.example.crypto_payment_system.domain.token.TokenBalance;
import com.example.crypto_payment_system.view.viewmodels.MainViewModel;

import java.math.BigDecimal;

public class Validate {

    public static boolean hasAmount(String amount, String currency, MainViewModel viewModel) {
        if (viewModel == null || amount == null || currency == null) {
            return false;
        }

        try {
            var tokenBalances = viewModel.getTokenBalances().getValue();
            if (tokenBalances == null) return false;

            TokenBalance tokenBalance = tokenBalances.get(currency);
            if (tokenBalance == null) return false;

            String userBalance = tokenBalance.getFormattedWalletBalance();
            if (userBalance == null) return false;

            BigDecimal requestedAmount = new BigDecimal(amount);
            BigDecimal availableBalance = new BigDecimal(userBalance);

            return availableBalance.compareTo(requestedAmount) >= 0;

        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Validates an Ethereum address by checking only the length
     *
     * @param address The address to validate
     * @return true if address has correct length (42 characters), false otherwise
     */
    public static boolean isValidEthereumAddress(String address) {
        if (address == null) {
            return false;
        }

        return address.length() == 42;
    }
}