package com.lichen.codexusage;

import android.content.Context;

import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import java.util.concurrent.TimeUnit;

final class UsageRefreshScheduler {
    private static final String UNIQUE_WORK_NAME = "codex_usage_periodic_refresh";

    private UsageRefreshScheduler() {
    }

    static void sync(Context context) {
        if (CodexAuthStore.load(context).isLoggedIn()) {
            schedule(context);
        } else {
            cancel(context);
        }
    }

    static void schedule(Context context) {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();
        PeriodicWorkRequest request = new PeriodicWorkRequest.Builder(
                UsageRefreshWorker.class,
                1,
                TimeUnit.HOURS
        )
                .setConstraints(constraints)
                .build();
        WorkManager.getInstance(context.getApplicationContext()).enqueueUniquePeriodicWork(
                UNIQUE_WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
        );
    }

    static void cancel(Context context) {
        WorkManager.getInstance(context.getApplicationContext()).cancelUniqueWork(UNIQUE_WORK_NAME);
    }
}
