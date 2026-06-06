package com.lichen.codexusage;

import android.content.Context;
import android.content.SharedPreferences;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

final class UsageState {
    static final String PREFS = "codex_usage_state";

    private static final String KEY_ACCOUNT_LABEL = "account_label";
    private static final String KEY_FIVE_USED_PERCENT = "five_used_percent";
    private static final String KEY_FIVE_RESET_AT = "five_reset_at";
    private static final String KEY_SEVEN_USED_PERCENT = "seven_used_percent";
    private static final String KEY_SEVEN_RESET_AT = "seven_reset_at";
    private static final String KEY_UPDATED_AT = "updated_at";
    private static final String KEY_ERROR = "error";

    private static final SimpleDateFormat DATE_FORMAT =
            new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA);
    private static final SimpleDateFormat WIDGET_DATE_FORMAT =
            new SimpleDateFormat("MM/dd HH:mm", Locale.CHINA);

    final String accountLabel;
    final double fiveUsedPercent;
    final long fiveResetAtMillis;
    final double sevenUsedPercent;
    final long sevenResetAtMillis;
    final long updatedAtMillis;
    final String error;

    UsageState(
            String accountLabel,
            double fiveUsedPercent,
            long fiveResetAtMillis,
            double sevenUsedPercent,
            long sevenResetAtMillis,
            long updatedAtMillis,
            String error
    ) {
        this.accountLabel = accountLabel;
        this.fiveUsedPercent = fiveUsedPercent;
        this.fiveResetAtMillis = fiveResetAtMillis;
        this.sevenUsedPercent = sevenUsedPercent;
        this.sevenResetAtMillis = sevenResetAtMillis;
        this.updatedAtMillis = updatedAtMillis;
        this.error = error;
    }

    static UsageState empty() {
        return new UsageState("", 0d, 0L, 0d, 0L, 0L, "");
    }

    static UsageState load(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        return new UsageState(
                prefs.getString(KEY_ACCOUNT_LABEL, ""),
                Double.longBitsToDouble(prefs.getLong(KEY_FIVE_USED_PERCENT, Double.doubleToLongBits(0d))),
                prefs.getLong(KEY_FIVE_RESET_AT, 0L),
                Double.longBitsToDouble(prefs.getLong(KEY_SEVEN_USED_PERCENT, Double.doubleToLongBits(0d))),
                prefs.getLong(KEY_SEVEN_RESET_AT, 0L),
                prefs.getLong(KEY_UPDATED_AT, 0L),
                prefs.getString(KEY_ERROR, "")
        );
    }

    void save(Context context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_ACCOUNT_LABEL, accountLabel)
                .putLong(KEY_FIVE_USED_PERCENT, Double.doubleToLongBits(fiveUsedPercent))
                .putLong(KEY_FIVE_RESET_AT, fiveResetAtMillis)
                .putLong(KEY_SEVEN_USED_PERCENT, Double.doubleToLongBits(sevenUsedPercent))
                .putLong(KEY_SEVEN_RESET_AT, sevenResetAtMillis)
                .putLong(KEY_UPDATED_AT, updatedAtMillis)
                .putString(KEY_ERROR, error)
                .apply();
    }

    int fiveRemainingPercent() {
        return remainingPercent(fiveUsedPercent);
    }

    int sevenRemainingPercent() {
        return remainingPercent(sevenUsedPercent);
    }

    boolean hasData() {
        return updatedAtMillis > 0L && error.length() == 0;
    }

    static UsageState error(Context context, String message) {
        UsageState previous = load(context);
        return new UsageState(
                previous.accountLabel,
                previous.fiveUsedPercent,
                previous.fiveResetAtMillis,
                previous.sevenUsedPercent,
                previous.sevenResetAtMillis,
                System.currentTimeMillis(),
                message
        );
    }

    private static int remainingPercent(double usedPercent) {
        int value = (int) Math.round(100d - usedPercent);
        return Math.max(0, Math.min(100, value));
    }

    static String formatDate(long millis) {
        if (millis <= 0L) {
            return "未知";
        }
        synchronized (DATE_FORMAT) {
            return DATE_FORMAT.format(new Date(millis));
        }
    }

    static String formatRelativeUpdate(long millis) {
        if (millis <= 0L) {
            return "未同步";
        }
        long seconds = Math.max(0L, (System.currentTimeMillis() - millis) / 1000L);
        if (seconds < 60L) {
            return "刚刚同步";
        }
        long minutes = seconds / 60L;
        if (minutes < 60L) {
            return String.format(Locale.CHINA, "%d分钟前同步", minutes);
        }
        long hours = minutes / 60L;
        if (hours < 24L) {
            return String.format(Locale.CHINA, "%d小时前同步", hours);
        }
        return formatDate(millis);
    }

    static String formatWidgetDateTime(long millis) {
        if (millis <= 0L) {
            return "未同步";
        }
        synchronized (WIDGET_DATE_FORMAT) {
            return WIDGET_DATE_FORMAT.format(new Date(millis));
        }
    }
}
