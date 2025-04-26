package com.example.crypto_payment_system.ui.settings;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.crypto_payment_system.R;
import com.example.crypto_payment_system.domain.account.User;
import com.example.crypto_payment_system.view.viewmodels.MainViewModel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ManageAccountFragment extends Fragment {

    private MainViewModel viewModel;
    private CheckBox checkBoxEUR;
    private CheckBox checkBoxUSD;
    private TextView walletAddressText;
    private Button saveButton;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_manage_account, container, false);

        viewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);

        checkBoxEUR = root.findViewById(R.id.checkbox_eur);
        checkBoxUSD = root.findViewById(R.id.checkbox_usd);
        walletAddressText = root.findViewById(R.id.wallet_address_text);
        saveButton = root.findViewById(R.id.save_currencies_button);

        viewModel.getCurrentUser().observe(getViewLifecycleOwner(), this::updateUI);

        saveButton.setOnClickListener(v -> savePreferredCurrencies());

        return root;
    }

    private void updateUI(User user) {
        if (user == null) {
            walletAddressText.setText(R.string.not_connected_to_wallet);
            checkBoxEUR.setEnabled(false);
            checkBoxUSD.setEnabled(false);
            saveButton.setEnabled(false);
            return;
        }

        walletAddressText.setText(user.getWalletAddress());

        checkBoxEUR.setEnabled(true);
        checkBoxUSD.setEnabled(true);
        saveButton.setEnabled(true);

        List<String> preferredCurrencies = new ArrayList<>(
                Arrays.asList(user.getPreferredCurrency().split(",")));

        checkBoxEUR.setChecked(preferredCurrencies.contains("EUR"));
        checkBoxUSD.setChecked(preferredCurrencies.contains("USD"));
    }

    private void savePreferredCurrencies() {
        List<String> selectedCurrencies = new ArrayList<>();

        if (checkBoxEUR.isChecked()) {
            selectedCurrencies.add("EUR");
        }

        if (checkBoxUSD.isChecked()) {
            selectedCurrencies.add("USD");
        }

        if (selectedCurrencies.isEmpty()) {
            Toast.makeText(getContext(), R.string.please_select_at_least_one_currency, Toast.LENGTH_SHORT).show();
            return;
        }

        String preferredCurrency = String.join(",", selectedCurrencies);

        viewModel.updatePreferredCurrency(preferredCurrency);

        Toast.makeText(getContext(), R.string.preferences_saved, Toast.LENGTH_SHORT).show();
    }
}