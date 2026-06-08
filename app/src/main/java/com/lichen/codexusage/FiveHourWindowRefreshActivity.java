package com.lichen.codexusage;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

public class FiveHourWindowRefreshActivity extends Activity {
    public static final String ACTION_REFRESH_FIVE_HOUR_WINDOW =
            "com.lichen.codexusage.ACTION_REFRESH_FIVE_HOUR_WINDOW";

    private static final String TAG = "FiveHourRefresh";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        enqueueAndFinish();
    }

    @Override
    protected void onNewIntent(android.content.Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        enqueueAndFinish();
    }

    private void enqueueAndFinish() {
        Log.i(TAG, "5 hour refresh activity trigger received");
        FiveHourWindowRefreshWorker.enqueue(getApplicationContext());
        Toast.makeText(this, "已提交 5 小时窗口刷新", Toast.LENGTH_SHORT).show();
        finish();
    }
}
