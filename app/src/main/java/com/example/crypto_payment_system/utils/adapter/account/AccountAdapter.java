package com.example.crypto_payment_system.utils.adapter.account;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.crypto_payment_system.R;
import com.example.crypto_payment_system.domain.account.WalletAccount;

import java.util.List;

public class AccountAdapter extends ArrayAdapter<WalletAccount> {

    private OnAccountDeleteListener deleteListener;

    public interface OnAccountDeleteListener {
        void onAccountDelete(WalletAccount account, int position);
    }

    public AccountAdapter(Context context, List<WalletAccount> accounts) {
        super(context, 0, accounts);
    }

    public void setOnAccountDeleteListener(OnAccountDeleteListener listener) {
        this.deleteListener = listener;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        return getCustomView(position, convertView, parent, R.layout.item_account_spinner);
    }

    @Override
    public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        return getCustomView(position, convertView, parent, R.layout.item_account_dropdown);
    }

    private View getCustomView(int position, View convertView, ViewGroup parent, int layoutResource) {
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(layoutResource, parent, false);
        }

        TextView accountName = convertView.findViewById(R.id.accountName);
        WalletAccount account = getItem(position);

        if (account != null && accountName != null) {
            accountName.setText(account.getName());

            if (layoutResource == R.layout.item_account_dropdown) {
                TextView accountAddress = convertView.findViewById(R.id.accountAddress);
                ImageView deleteButton = convertView.findViewById(R.id.deleteAccountButton);

                if (accountAddress != null) {
                    String address = account.getAddress();
                    if (address != null && address.length() > 10) {
                        String formattedAddress = address.substring(0, 6) + "..." +
                                address.substring(address.length() - 4);
                        accountAddress.setText(formattedAddress);
                    } else {
                        accountAddress.setText(address);
                    }
                }

                if (deleteButton != null && deleteListener != null) {
                    deleteButton.setOnClickListener(v -> {
                        deleteListener.onAccountDelete(account, position);
                    });
                }
            }
        }

        return convertView;
    }
}