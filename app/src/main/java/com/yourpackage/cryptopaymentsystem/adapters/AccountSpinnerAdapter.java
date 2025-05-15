package com.yourpackage.cryptopaymentsystem.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.crypto_payment_system.R;

import java.util.List;

public class AccountSpinnerAdapter extends ArrayAdapter<String> {
    
    private final LayoutInflater inflater;
    
    public AccountSpinnerAdapter(Context context, List<String> accounts) {
        super(context, R.layout.item_account_spinner, accounts);
        this.inflater = LayoutInflater.from(context);
    }
    
    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View view = convertView;
        if (view == null) {
            view = inflater.inflate(R.layout.item_account_spinner, parent, false);
        }
        
        String account = getItem(position);
        if (account != null) {
            TextView accountName = view.findViewById(R.id.accountName);
            accountName.setText(account);
        }
        
        return view;
    }
    
    @Override
    public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View view = convertView;
        if (view == null) {
            view = inflater.inflate(R.layout.item_account_spinner, parent, false);
        }
        
        String account = getItem(position);
        if (account != null) {
            TextView accountName = view.findViewById(R.id.accountName);
            accountName.setText(account);
        }
        
        return view;
    }
} 