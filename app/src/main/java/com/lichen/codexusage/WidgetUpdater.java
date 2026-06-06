package com.lichen.codexusage;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.widget.RemoteViews;

import java.util.Locale;

final class WidgetUpdater {
    static final String ACTION_REFRESH_WIDGET = "com.lichen.codexusage.ACTION_REFRESH_WIDGET";
    private static final String PREFS = "widget_settings";
    private static final String KEY_CARD_ALPHA_PERCENT = "card_alpha_percent";
    private static final String KEY_IS_REFRESHING = "is_refreshing";
    private static final String KEY_REFRESH_STARTED_AT = "refresh_started_at";
    private static final String KEY_REFRESH_TOKEN = "refresh_token";
    private static final int DEFAULT_CARD_ALPHA_PERCENT = 100;
    private static final long REFRESH_STALE_AFTER_MILLIS = 2L * 60L * 1000L;

    private WidgetUpdater() {
    }

    static void updateAll(Context context) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        int[] wideIds = manager.getAppWidgetIds(new ComponentName(context, CodeXWidgetProvider.class));
        update(context, manager, wideIds);
        int[] compactIds = manager.getAppWidgetIds(new ComponentName(context, CodeXWidgetCompactProvider.class));
        updateCompact(context, manager, compactIds);
    }

    static void update(Context context, AppWidgetManager manager, int[] widgetIds) {
        UsageState state = UsageState.load(context);
        for (int widgetId : widgetIds) {
            manager.updateAppWidget(widgetId, buildWideViews(context, state));
        }
    }

    static void updateCompact(Context context, AppWidgetManager manager, int[] widgetIds) {
        UsageState state = UsageState.load(context);
        for (int widgetId : widgetIds) {
            manager.updateAppWidget(widgetId, buildCompactViews(context, state));
        }
    }

    static void refreshFromNetwork(final Context context, final Runnable onComplete) {
        final long refreshToken = beginRefreshing(context);
        updateAll(context);
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    UsageState state = CodexUsageClient.fetchUsage(context.getApplicationContext());
                    if (isLatestRefresh(context.getApplicationContext(), refreshToken)) {
                        state.save(context.getApplicationContext());
                    }
                } catch (Exception e) {
                    if (isLatestRefresh(context.getApplicationContext(), refreshToken)) {
                        UsageState.error(context.getApplicationContext(), "刷新失败").save(context.getApplicationContext());
                    }
                } finally {
                    if (isLatestRefresh(context.getApplicationContext(), refreshToken)) {
                        setRefreshing(context.getApplicationContext(), false);
                        updateAll(context.getApplicationContext());
                    }
                    if (onComplete != null) {
                        onComplete.run();
                    }
                }
            }
        }).start();
    }

    static long beginRefreshing(Context context) {
        long refreshToken = System.nanoTime();
        android.content.SharedPreferences.Editor editor =
                context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit();
        editor.putBoolean(KEY_IS_REFRESHING, true);
        editor.putLong(KEY_REFRESH_STARTED_AT, System.currentTimeMillis());
        editor.putLong(KEY_REFRESH_TOKEN, refreshToken);
        editor.commit();
        return refreshToken;
    }

    static boolean isLatestRefresh(Context context, long refreshToken) {
        android.content.SharedPreferences prefs =
                context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_IS_REFRESHING, false)
                && prefs.getLong(KEY_REFRESH_TOKEN, 0L) == refreshToken;
    }

    static void clearRefreshing(Context context) {
        setRefreshing(context, false);
    }

    private static RemoteViews buildWideViews(Context context, UsageState state) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_codex_usage_4x2);
        views.setInt(R.id.widget_background, "setImageAlpha", backgroundAlpha(context));
        views.setTextViewText(R.id.widget_sync_text, compactStatusText(context, state));
        views.setTextViewText(R.id.widget_4x2_five_percent, compactPercentText(state, state.fiveRemainingPercent()));
        views.setTextViewText(R.id.widget_4x2_five_reset, wideResetLine(state, state.fiveResetAtMillis));
        views.setProgressBar(R.id.widget_4x2_five_progress, 100, progress(state, state.fiveRemainingPercent()), false);
        views.setTextViewText(R.id.widget_4x2_seven_percent, compactPercentText(state, state.sevenRemainingPercent()));
        views.setTextViewText(R.id.widget_4x2_seven_reset, wideResetLine(state, state.sevenResetAtMillis));
        views.setProgressBar(R.id.widget_4x2_seven_progress, 100, progress(state, state.sevenRemainingPercent()), false);
        views.setTextViewText(R.id.widget_4x2_updated_at, updatedAtLine(state));
        views.setOnClickPendingIntent(R.id.widget_root, openAppIntent(context));
        views.setOnClickPendingIntent(R.id.widget_header_area, refreshIntent(context));
        views.setOnClickPendingIntent(R.id.widget_refresh_button, refreshIntent(context));
        views.setOnClickPendingIntent(R.id.widget_refresh_area, refreshIntent(context));
        return views;
    }

    private static RemoteViews buildCompactViews(Context context, UsageState state) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_codex_usage_2x2);
        views.setInt(R.id.widget_background, "setImageAlpha", backgroundAlpha(context));
        views.setTextViewText(R.id.widget_compact_sync_text, compactStatusText(context, state));
        views.setTextViewText(R.id.widget_2x2_five_percent, compactPercentText(state, state.fiveRemainingPercent()));
        views.setTextViewText(R.id.widget_2x2_five_reset, compactResetLine(state, state.fiveResetAtMillis));
        views.setProgressBar(R.id.widget_2x2_five_progress, 100, progress(state, state.fiveRemainingPercent()), false);
        views.setTextViewText(R.id.widget_2x2_seven_percent, compactPercentText(state, state.sevenRemainingPercent()));
        views.setTextViewText(R.id.widget_2x2_seven_reset, compactResetLine(state, state.sevenResetAtMillis));
        views.setProgressBar(R.id.widget_2x2_seven_progress, 100, progress(state, state.sevenRemainingPercent()), false);
        views.setOnClickPendingIntent(R.id.widget_root, openAppIntent(context));
        views.setOnClickPendingIntent(R.id.widget_header_area, refreshIntent(context));
        views.setOnClickPendingIntent(R.id.widget_refresh_button, refreshIntent(context));
        views.setOnClickPendingIntent(R.id.widget_refresh_area, refreshIntent(context));
        return views;
    }

    private static PendingIntent openAppIntent(Context context) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TOP
                        | Intent.FLAG_ACTIVITY_SINGLE_TOP
        );
        return PendingIntent.getActivity(context, 10, intent, pendingFlags());
    }

    private static PendingIntent refreshIntent(Context context) {
        Intent intent = new Intent(context, CodeXWidgetProvider.class);
        intent.setAction(ACTION_REFRESH_WIDGET);
        return PendingIntent.getBroadcast(context, 20, intent, pendingFlags());
    }

    private static int pendingFlags() {
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        return flags;
    }

    static int loadCardAlphaPercent(Context context) {
        return clampAlphaPercent(context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getInt(KEY_CARD_ALPHA_PERCENT, DEFAULT_CARD_ALPHA_PERCENT));
    }

    static void saveCardAlphaPercent(Context context, int alphaPercent) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .putInt(KEY_CARD_ALPHA_PERCENT, clampAlphaPercent(alphaPercent))
                .apply();
    }

    private static boolean isRefreshing(Context context) {
        android.content.SharedPreferences prefs =
                context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        if (!prefs.getBoolean(KEY_IS_REFRESHING, false)) {
            return false;
        }
        long startedAt = prefs.getLong(KEY_REFRESH_STARTED_AT, 0L);
        if (startedAt <= 0L || System.currentTimeMillis() - startedAt > REFRESH_STALE_AFTER_MILLIS) {
            setRefreshing(context, false);
            return false;
        }
        return true;
    }

    private static void setRefreshing(Context context, boolean refreshing) {
        android.content.SharedPreferences.Editor editor =
                context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit();
        editor.putBoolean(KEY_IS_REFRESHING, refreshing);
        if (refreshing) {
            editor.putLong(KEY_REFRESH_STARTED_AT, System.currentTimeMillis());
            editor.putLong(KEY_REFRESH_TOKEN, System.nanoTime());
        } else {
            editor.remove(KEY_REFRESH_STARTED_AT);
            editor.remove(KEY_REFRESH_TOKEN);
        }
        editor.commit();
    }

    private static String statusText(UsageState state) {
        if (state.error.length() > 0) {
            return state.error;
        }
        return UsageState.formatRelativeUpdate(state.updatedAtMillis);
    }

    private static String percentText(UsageState state, int remainingPercent) {
        if (!state.hasData()) {
            return "剩余  --";
        }
        return String.format(Locale.CHINA, "剩余  %d%%", remainingPercent);
    }

    private static String compactPercentText(UsageState state, int remainingPercent) {
        if (!state.hasData()) {
            return "--";
        }
        return String.format(Locale.CHINA, "%d%%", remainingPercent);
    }

    private static String resetLine(UsageState state, long resetAtMillis) {
        if (!state.hasData()) {
            return "登录后同步";
        }
        if (resetAtMillis <= 0L) {
            return "待重置";
        }
        return String.format(Locale.CHINA, "%s重置", resetText(resetAtMillis));
    }

    private static String compactResetLine(UsageState state, long resetAtMillis) {
        if (!state.hasData()) {
            return "登录后同步";
        }
        if (resetAtMillis <= 0L) {
            return "待重置";
        }
        return resetShortText(resetAtMillis);
    }

    private static String wideResetLine(UsageState state, long resetAtMillis) {
        if (!state.hasData()) {
            return "登录后同步";
        }
        if (resetAtMillis <= 0L) {
            return "待重置";
        }
        return String.format(
                Locale.CHINA,
                "%s - %s",
                UsageState.formatWidgetDateTime(resetAtMillis),
                resetShortText(resetAtMillis)
        );
    }

    private static String compactStatusText(Context context, UsageState state) {
        if (isRefreshing(context)) {
            return "刷新中";
        }
        if (state.error.length() > 0) {
            return state.error;
        }
        if (state.updatedAtMillis <= 0L) {
            return "未同步";
        }
        return UsageState.formatWidgetDateTime(state.updatedAtMillis);
    }

    private static int progress(UsageState state, int remainingPercent) {
        return state.hasData() ? remainingPercent : 0;
    }

    private static int backgroundAlpha(Context context) {
        return Math.round(loadCardAlphaPercent(context) * 255f / 100f);
    }

    private static int clampAlphaPercent(int alphaPercent) {
        return Math.max(35, Math.min(100, alphaPercent));
    }

    private static String resetText(long resetAtMillis) {
        long remaining = resetAtMillis - System.currentTimeMillis();
        if (remaining <= 0L) {
            return "待重置";
        }
        long totalMinutes = remaining / 60000L;
        long days = totalMinutes / (24L * 60L);
        long hours = (totalMinutes % (24L * 60L)) / 60L;
        long minutes = totalMinutes % 60L;
        if (days > 0L) {
            return String.format(Locale.CHINA, "%d天%d小时后", days, hours);
        }
        if (hours > 0L) {
            return String.format(Locale.CHINA, "%d小时%d分钟后", hours, minutes);
        }
        return String.format(Locale.CHINA, "%d分钟后", Math.max(1L, minutes));
    }

    private static String resetShortText(long resetAtMillis) {
        long remaining = resetAtMillis - System.currentTimeMillis();
        if (remaining <= 0L) {
            return "待重置";
        }
        long totalMinutes = remaining / 60000L;
        long days = totalMinutes / (24L * 60L);
        long hours = (totalMinutes % (24L * 60L)) / 60L;
        long minutes = totalMinutes % 60L;
        if (days > 0L) {
            return String.format(Locale.CHINA, "%d天%d时", days, hours);
        }
        if (hours > 0L) {
            return String.format(Locale.CHINA, "%d时%d分", hours, minutes);
        }
        return String.format(Locale.CHINA, "%d分", Math.max(1L, minutes));
    }

    private static String updatedAtLine(UsageState state) {
        if (state.error.length() > 0) {
            return state.error;
        }
        if (state.updatedAtMillis <= 0L) {
            return "更新 未同步";
        }
        return String.format(Locale.CHINA, "更新 %s", UsageState.formatWidgetDateTime(state.updatedAtMillis));
    }
}
