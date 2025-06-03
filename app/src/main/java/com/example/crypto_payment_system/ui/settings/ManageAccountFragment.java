package com.example.crypto_payment_system.ui.settings;

import static com.example.crypto_payment_system.config.Constants.EURSC;
import static com.example.crypto_payment_system.config.Constants.USDT;

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
    private static final String ARG_NEW_USER = "is_new_user";
    public boolean isNewUser = false;
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

    public static ManageAccountFragment newInstance(boolean isNewUser){
        ManageAccountFragment fragment = new ManageAccountFragment();
        Bundle args = new Bundle();
        args.putBoolean(ARG_NEW_USER, isNewUser);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            isNewUser = getArguments().getBoolean(ARG_NEW_USER, false);
        }
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

        checkBoxEUR.setChecked(preferredCurrencies.contains(EURSC));
        checkBoxUSD.setChecked(preferredCurrencies.contains(USDT));
    }

    private void savePreferredCurrencies() {
        List<String> selectedCurrencies = new ArrayList<>();

        if (checkBoxEUR.isChecked()) {
            selectedCurrencies.add(EURSC);
        }

        if (checkBoxUSD.isChecked()) {
            selectedCurrencies.add(USDT);
        }

        if (selectedCurrencies.isEmpty()) {
            Toast.makeText(getContext(), R.string.please_select_at_least_one_currency, Toast.LENGTH_SHORT).show();
            return;
        }

        String preferredCurrency = String.join(",", selectedCurrencies);

        viewModel.updatePreferredCurrency(preferredCurrency);

        Toast.makeText(getContext(), R.string.preferences_saved, Toast.LENGTH_SHORT).show();
        if(isNewUser)
        {
            requireActivity().getSupportFragmentManager().popBackStack();
        }
    }
}