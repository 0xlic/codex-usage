package com.lichen.codexusage;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;

public class CodeXWidgetCompactProvider extends AppWidgetProvider {
    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        WidgetUpdater.updateCompact(context, appWidgetManager, appWidgetIds);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (WidgetUpdater.ACTION_REFRESH_WIDGET.equals(intent.getAction())) {
            final PendingResult pendingResult = goAsync();
            WidgetUpdater.refreshFromNetwork(context.getApplicationContext(), new Runnable() {
                @Override
                public void run() {
                    pendingResult.finish();
                }
            });
            return;
        }
        super.onReceive(context, intent);
    }
}
