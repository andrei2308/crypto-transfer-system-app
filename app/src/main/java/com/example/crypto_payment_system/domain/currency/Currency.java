package com.example.crypto_payment_system.domain.currency;

import android.util.Log;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;

/**
 * Model class to represent a currency in the application
 */
public class Currency {
    private final String code;
    private final String name;
    @DrawableRes
    private final int flagIconResourceId;
    private boolean isSelected;

    public Currency(String code, String name, int flagIconResourceId) {
        Log.d("Currency", "Creating currency: code='" + code + "', name='" + name + "', flagId=" + flagIconResourceId);

        this.code = code;
        this.name = name;
        this.flagIconResourceId = flagIconResourceId;
        this.isSelected = false;

        Log.d("Currency", "Currency created successfully: " + this.toString());
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public int getFlagIconResourceId() {
        return flagIconResourceId;
    }

    public boolean isSelected() {
        return isSelected;
    }

    public void setSelected(boolean selected) {
        isSelected = selected;
    }

    @NonNull
    @Override
    public String toString() {
        return code;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Currency currency = (Currency) o;
        return code.equals(currency.code);
    }

    @Override
    public int hashCode() {
        return code.hashCode();
    }
} 