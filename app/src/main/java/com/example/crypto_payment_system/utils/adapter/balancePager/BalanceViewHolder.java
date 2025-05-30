package com.example.crypto_payment_system.utils.adapter.balancePager;

import static com.example.crypto_payment_system.config.Constants.EURSC;
import static com.example.crypto_payment_system.config.Constants.USDT;

import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.lifecycle.LifecycleOwner;
import androidx.recyclerview.widget.RecyclerView;

import com.example.crypto_payment_system.R;
import com.example.crypto_payment_system.domain.token.TokenBalance;
import com.example.crypto_payment_system.view.viewmodels.MainViewModel;

/**
 * ViewHolder for currency balance cards in the ViewPager
 */
public class BalanceViewHolder extends RecyclerView.ViewHolder {
    private final TextView balanceValue;
    private final BalancePagerAdapter.RefreshClickListener refreshClickListener;
    public BalanceViewHolder(@NonNull View itemView, BalancePagerAdapter.RefreshClickListener refreshClickListener) {
        super(itemView);
        balanceValue = itemView.findViewById(R.id.balanceValue);
        this.refreshClickListener = refreshClickListener;
    }

    public void bind(int position, String currency, MainViewModel viewModel, LifecycleOwner lifecycleOwner) {
        viewModel.getTokenBalances().observe(lifecycleOwner, tokenBalances -> {
            if (tokenBalances != null) {
                String formattedBalance = "0.00";
                if (currency.equals(EURSC) && tokenBalances.containsKey(EURSC)) {
                    TokenBalance eurBalance = tokenBalances.get(EURSC);
                    if (eurBalance != null) {
                        formattedBalance = eurBalance.getFormattedWalletBalance();
                    }
                } else if (currency.equals(USDT) && tokenBalances.containsKey(USDT)) {
                    TokenBalance usdBalance = tokenBalances.get(USDT);
                    if (usdBalance != null) {
                        formattedBalance = usdBalance.getFormattedWalletBalance();
                    }
                }
                balanceValue.setText(String.format("%s %s", formattedBalance, currency));
            }
        });
    }
}