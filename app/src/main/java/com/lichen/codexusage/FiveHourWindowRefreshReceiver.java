package com.lichen.codexusage;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class FiveHourWindowRefreshReceiver extends BroadcastReceiver {
    public static final String ACTION_REFRESH_FIVE_HOUR_WINDOW =
            "com.lichen.codexusage.ACTION_REFRESH_FIVE_HOUR_WINDOW";

    private static final String TAG = "FiveHourRefresh";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || !ACTION_REFRESH_FIVE_HOUR_WINDOW.equals(intent.getAction())) {
            return;
        }

        Log.i(TAG, "5 hour refresh broadcast received");
        FiveHourWindowRefreshWorker.enqueue(context.getApplicationContext());
    }
}
