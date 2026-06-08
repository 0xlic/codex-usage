package com.lichen.codexusage;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import org.json.JSONException;

import java.io.IOException;

public class FiveHourWindowRefreshReceiver extends BroadcastReceiver {
    public static final String ACTION_REFRESH_FIVE_HOUR_WINDOW =
            "com.lichen.codexusage.ACTION_REFRESH_FIVE_HOUR_WINDOW";

    private static final String TAG = "FiveHourRefresh";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || !ACTION_REFRESH_FIVE_HOUR_WINDOW.equals(intent.getAction())) {
            return;
        }

        final Context appContext = context.getApplicationContext();
        final PendingResult pendingResult = goAsync();
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    CodexSettingsStore settings = CodexSettingsStore.load(appContext);
                    if (!settings.fiveHourRefreshEnabled) {
                        Log.i(TAG, "5 hour refresh is disabled");
                        return;
                    }
                    if (settings.fiveHourEnvironmentId.length() == 0) {
                        Log.i(TAG, "5 hour refresh environment is not configured");
                        return;
                    }
                    CodexUsageClient.refreshFiveHourWindow(appContext, settings.fiveHourEnvironmentId);
                    Log.i(TAG, "5 hour refresh task created");
                } catch (IOException | JSONException e) {
                    Log.e(TAG, "5 hour refresh failed", e);
                } finally {
                    pendingResult.finish();
                }
            }
        }).start();
    }
}
