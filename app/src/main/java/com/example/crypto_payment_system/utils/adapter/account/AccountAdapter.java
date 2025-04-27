package com.example.crypto_payment_system.utils.adapter.account;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.example.crypto_payment_system.domain.account.WalletAccount;

import java.util.List;

/**
 * Custom adapter for displaying wallet accounts
 */
public class AccountAdapter extends ArrayAdapter<WalletAccount> {

    public AccountAdapter(Context context, List<WalletAccount> accounts) {
        super(context, android.R.layout.simple_spinner_item, accounts);
        setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        View view = super.getView(position, convertView, parent);
        TextView textView = view.findViewById(android.R.id.text1);
        WalletAccount account = getItem(position);

        if (account != null) {
            textView.setText(account.getName());
        }

        return view;
    }

    @Override
    public View getDropDownView(int position, View convertView, @NonNull ViewGroup parent) {
        View view = super.getDropDownView(position, convertView, parent);
        TextView textView = view.findViewById(android.R.id.text1);
        WalletAccount account = getItem(position);

        if (account != null) {
            String address = account.getAddress();
            String truncatedAddress = address.substring(0, 6) + "..." +
                    address.substring(address.length() - 4);
            textView.setText(account.getName() + " (" + truncatedAddress + ")");
        }

        return view;
    }
}
