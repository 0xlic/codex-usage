package com.lichen.codexusage;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Constraints;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import org.json.JSONException;

import java.io.IOException;

public final class FiveHourWindowRefreshWorker extends Worker {
    private static final String TAG = "FiveHourRefresh";
    private static final String UNIQUE_WORK_NAME = "codex_five_hour_window_refresh";

    public FiveHourWindowRefreshWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    static void enqueue(Context context) {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();
        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(FiveHourWindowRefreshWorker.class)
                .setConstraints(constraints)
                .build();
        WorkManager.getInstance(context.getApplicationContext()).enqueueUniqueWork(
                UNIQUE_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                request
        );
    }

    @NonNull
    @Override
    public Result doWork() {
        Context context = getApplicationContext();
        CodexSettingsStore settings = CodexSettingsStore.load(context);
        if (!settings.fiveHourRefreshEnabled) {
            Log.i(TAG, "5 hour refresh is disabled");
            return Result.success();
        }
        if (settings.fiveHourEnvironmentId.length() == 0) {
            Log.i(TAG, "5 hour refresh environment is not configured");
            return Result.success();
        }

        try {
            CodexUsageClient.refreshFiveHourWindow(context, settings.fiveHourEnvironmentId);
            Log.i(TAG, "5 hour refresh task created");
            return Result.success();
        } catch (IOException | JSONException e) {
            Log.e(TAG, "5 hour refresh failed, not retrying to avoid duplicate cloud tasks", e);
            return Result.failure();
        }
    }
}
