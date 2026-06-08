package com.lichen.codexusage;

import android.content.Context;
import android.content.SharedPreferences;

final class CodexSettingsStore {
    private static final String PREFS = "codex_settings";
    private static final String KEY_FIVE_HOUR_REFRESH_ENABLED = "five_hour_refresh_enabled";
    private static final String KEY_FIVE_HOUR_ENVIRONMENT_ID = "five_hour_environment_id";

    final boolean fiveHourRefreshEnabled;
    final String fiveHourEnvironmentId;

    private CodexSettingsStore(boolean fiveHourRefreshEnabled, String fiveHourEnvironmentId) {
        this.fiveHourRefreshEnabled = fiveHourRefreshEnabled;
        this.fiveHourEnvironmentId = fiveHourEnvironmentId;
    }

    static CodexSettingsStore load(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        return new CodexSettingsStore(
                prefs.getBoolean(KEY_FIVE_HOUR_REFRESH_ENABLED, false),
                prefs.getString(KEY_FIVE_HOUR_ENVIRONMENT_ID, "")
        );
    }

    static void saveFiveHourRefreshEnabled(Context context, boolean enabled) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_FIVE_HOUR_REFRESH_ENABLED, enabled)
                .apply();
    }

    static void saveFiveHourEnvironmentId(Context context, String environmentId) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_FIVE_HOUR_ENVIRONMENT_ID, environmentId)
                .apply();
    }
}
