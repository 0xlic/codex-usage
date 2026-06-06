package com.lichen.codexusage;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import org.json.JSONException;

import java.io.IOException;

public final class UsageRefreshWorker extends Worker {
    public UsageRefreshWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context context = getApplicationContext();
        CodexAuthStore auth = CodexAuthStore.load(context);
        if (!auth.isLoggedIn()) {
            WidgetUpdater.clearRefreshing(context);
            WidgetUpdater.updateAll(context);
            return Result.success();
        }
        try {
            UsageState state = CodexUsageClient.fetchUsage(context);
            state.save(context);
            WidgetUpdater.clearRefreshing(context);
            WidgetUpdater.updateAll(context);
            return Result.success();
        } catch (IOException | JSONException e) {
            WidgetUpdater.clearRefreshing(context);
            WidgetUpdater.updateAll(context);
            return Result.retry();
        }
    }
}
