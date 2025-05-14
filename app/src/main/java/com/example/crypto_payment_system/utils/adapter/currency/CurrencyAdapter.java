package com.example.crypto_payment_system.utils.adapter.currency;

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
import com.example.crypto_payment_system.domain.currency.Currency;

import java.util.List;

/**
 * Custom adapter for displaying currencies in a spinner or dropdown
 */
public class CurrencyAdapter extends ArrayAdapter<Currency> {

    private final LayoutInflater inflater;
    private final List<Currency> currencies;
    private final boolean isDropdown;

    /**
     * Create a new CurrencyAdapter for regular spinner views
     *
     * @param context    The context
     * @param currencies The list of currencies to display
     */
    public CurrencyAdapter(@NonNull Context context, @NonNull List<Currency> currencies) {
        this(context, currencies, false);
    }

    /**
     * Create a new CurrencyAdapter
     *
     * @param context    The context
     * @param currencies The list of currencies to display
     * @param isDropdown Whether this adapter is for a dropdown view
     */
    public CurrencyAdapter(@NonNull Context context, @NonNull List<Currency> currencies, boolean isDropdown) {
        super(context, isDropdown ? R.layout.item_currency_dropdown : R.layout.item_currency_spinner, currencies);
        this.inflater = LayoutInflater.from(context);
        this.currencies = currencies;
        this.isDropdown = isDropdown;
    }

    /**
     * Get a Currency by its code
     *
     * @param currencyCode The currency code to find
     * @return The found Currency or null if not found
     */
    @Nullable
    public Currency getCurrencyByCode(String currencyCode) {
        if (currencyCode == null) return null;
        
        for (Currency currency : currencies) {
            if (currency.getCode().equals(currencyCode)) {
                return currency;
            }
        }
        return null;
    }

    /**
     * Set the selected currency by code
     *
     * @param currencyCode The currency code to select
     */
    public void setSelectedCurrency(String currencyCode) {
        boolean selectionChanged = false;
        
        for (Currency currency : currencies) {
            boolean shouldBeSelected = currency.getCode().equals(currencyCode);
            if (currency.isSelected() != shouldBeSelected) {
                selectionChanged = true;
                currency.setSelected(shouldBeSelected);
            }
        }
        
        if (selectionChanged) {
            notifyDataSetChanged();
        }
    }

    /**
     * Get the currently selected currency
     *
     * @return The selected Currency or null if none is selected
     */
    @Nullable
    public Currency getSelectedCurrency() {
        for (Currency currency : currencies) {
            if (currency.isSelected()) {
                return currency;
            }
        }
        return currencies.size() > 0 ? currencies.get(0) : null;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        return createView(position, convertView, parent);
    }

    @Override
    public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        return createView(position, convertView, parent, true);
    }

    private View createView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        return createView(position, convertView, parent, isDropdown);
    }

    private View createView(int position, @Nullable View convertView, @NonNull ViewGroup parent, boolean isDropdown) {
        ViewHolder holder;
        
        if (convertView == null) {
            convertView = inflater.inflate(
                    isDropdown ? R.layout.item_currency_dropdown : R.layout.item_currency_spinner,
                    parent,
                    false
            );
            
            holder = new ViewHolder();
            holder.flagIcon = convertView.findViewById(R.id.currencyFlagIcon);
            holder.codeText = convertView.findViewById(R.id.currencyCode);
            holder.nameText = convertView.findViewById(R.id.currencyName);
            
            if (isDropdown) {
                holder.selectedIcon = convertView.findViewById(R.id.currencySelectedIcon);
            }
            
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        
        Currency currency = getItem(position);
        if (currency != null) {
            holder.flagIcon.setImageResource(currency.getFlagIconResourceId());
            holder.codeText.setText(currency.getCode());
            holder.nameText.setText(currency.getName());
            
            if (isDropdown && holder.selectedIcon != null) {
                holder.selectedIcon.setVisibility(currency.isSelected() ? View.VISIBLE : View.GONE);
            }
        }
        
        return convertView;
    }

    private static class ViewHolder {
        ImageView flagIcon;
        TextView codeText;
        TextView nameText;
        ImageView selectedIcon; // Only used in dropdown views
    }
} 