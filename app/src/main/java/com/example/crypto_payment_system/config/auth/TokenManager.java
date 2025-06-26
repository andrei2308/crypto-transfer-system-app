package com.example.crypto_payment_system.config.auth;

import android.content.Context;
import android.content.SharedPreferences;

public class TokenManager {
    private static final String PREF_NAME = "auth_prefs";
    private static final String KEY_JWT_TOKEN = "jwt_token";
    private static final String KEY_TOKEN_EXPIRY = "token_expiry";

    private final SharedPreferences prefs;

    public TokenManager(Context context) {
        this.prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public void saveToken(String token, long expiryTime) {
        prefs.edit()
                .putString(KEY_JWT_TOKEN, token)
                .putLong(KEY_TOKEN_EXPIRY, expiryTime)
                .apply();
    }

    public String getToken() {
        return prefs.getString(KEY_JWT_TOKEN, null);
    }

    public boolean isTokenValid() {
        String token = getToken();
        if (token == null) return false;

        long expiry = prefs.getLong(KEY_TOKEN_EXPIRY, 0);
        return System.currentTimeMillis() < expiry;
    }

    public void clearToken() {
        prefs.edit()
                .remove(KEY_JWT_TOKEN)
                .remove(KEY_TOKEN_EXPIRY)
                .apply();
    }
}
