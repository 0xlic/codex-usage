package com.lichen.codexusage;

import android.content.Context;
import android.content.SharedPreferences;

final class CodexAuthStore {
    private static final String PREFS = "codex_auth";
    private static final String KEY_ACCOUNT_ID = "account_id";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_REFRESH_TOKEN = "refresh_token";
    private static final String KEY_ACCESS_TOKEN = "access_token";
    private static final String KEY_ACCESS_EXPIRES_AT = "access_expires_at";

    final String accountId;
    final String email;
    final String refreshToken;
    final String accessToken;
    final long accessExpiresAtMillis;

    private CodexAuthStore(
            String accountId,
            String email,
            String refreshToken,
            String accessToken,
            long accessExpiresAtMillis
    ) {
        this.accountId = accountId;
        this.email = email;
        this.refreshToken = refreshToken;
        this.accessToken = accessToken;
        this.accessExpiresAtMillis = accessExpiresAtMillis;
    }

    static CodexAuthStore load(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        return new CodexAuthStore(
                prefs.getString(KEY_ACCOUNT_ID, ""),
                prefs.getString(KEY_EMAIL, ""),
                prefs.getString(KEY_REFRESH_TOKEN, ""),
                prefs.getString(KEY_ACCESS_TOKEN, ""),
                prefs.getLong(KEY_ACCESS_EXPIRES_AT, 0L)
        );
    }

    static void saveLogin(
            Context context,
            String accountId,
            String email,
            String refreshToken,
            String accessToken,
            long accessExpiresAtMillis
    ) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_ACCOUNT_ID, accountId)
                .putString(KEY_EMAIL, email)
                .putString(KEY_REFRESH_TOKEN, refreshToken)
                .putString(KEY_ACCESS_TOKEN, accessToken)
                .putLong(KEY_ACCESS_EXPIRES_AT, accessExpiresAtMillis)
                .apply();
    }

    static void saveAccessToken(Context context, String accessToken, long accessExpiresAtMillis) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_ACCESS_TOKEN, accessToken)
                .putLong(KEY_ACCESS_EXPIRES_AT, accessExpiresAtMillis)
                .apply();
    }

    static void clear(Context context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().apply();
    }

    boolean isLoggedIn() {
        return accountId.length() > 0 && refreshToken.length() > 0;
    }

    boolean hasFreshAccessToken() {
        return accessToken.length() > 0
                && accessExpiresAtMillis - System.currentTimeMillis() > 60_000L;
    }

    String displayName() {
        if (email.length() > 0) {
            return email;
        }
        if (accountId.length() > 0) {
            return accountId;
        }
        return "未登录";
    }
}
