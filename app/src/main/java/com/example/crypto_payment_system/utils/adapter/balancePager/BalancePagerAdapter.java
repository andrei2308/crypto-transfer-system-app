package com.example.crypto_payment_system.utils.adapter.balancePager;

import static com.example.crypto_payment_system.config.Constants.EURSC;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.lifecycle.LifecycleOwner;
import androidx.recyclerview.widget.RecyclerView;

import com.example.crypto_payment_system.R;
import com.example.crypto_payment_system.view.activity.MainActivity;
import com.example.crypto_payment_system.view.viewmodels.MainViewModel;

import java.util.List;

/**
 * Adapter for the balance ViewPager that shows different currency balance cards
 */
public class BalancePagerAdapter extends RecyclerView.Adapter<BalanceViewHolder> {
    private final List<String> currencies;
    private final Context context;
    private final LifecycleOwner lifecycleOwner;
    private final MainViewModel viewModel;
    private final RefreshClickListener refreshClickListener;

    public interface RefreshClickListener {
        void onRefreshClick(int position);
    }

    public BalancePagerAdapter(Context context, RefreshClickListener refreshClickListener, List<String> preferredCurrencies) {
        this.context = context;
        this.lifecycleOwner = (LifecycleOwner) context;
        this.viewModel = ((MainActivity) context).getViewModel();
        this.refreshClickListener = refreshClickListener;
        this.currencies = preferredCurrencies;
    }

    @NonNull
    @Override
    public BalanceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layoutResId;
        if (EURSC.equals(currencies.get(viewType))) {
            layoutResId = R.layout.balance_card_eur;
        } else {
            layoutResId = R.layout.balance_card_usd;
        }

        View view = LayoutInflater.from(context).inflate(layoutResId, parent, false);

        ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
        if (layoutParams != null) {
            layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT;
            layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT;
            view.setLayoutParams(layoutParams);
        }

        return new BalanceViewHolder(view, refreshClickListener);
    }

    @Override
    public void onBindViewHolder(@NonNull BalanceViewHolder holder, int position) {
        holder.bind(position, currencies.get(position), viewModel, lifecycleOwner);
    }

    @Override
    public int getItemCount() {
        return currencies.size();
    }

    @Override
    public int getItemViewType(int position) {
        return position;
    }
}