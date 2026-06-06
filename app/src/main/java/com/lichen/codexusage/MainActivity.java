package com.lichen.codexusage;

import android.app.Activity;
import android.app.WallpaperColors;
import android.app.WallpaperManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.ColorStateList;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;

import java.io.IOException;
import java.util.Locale;

public class MainActivity extends Activity {
    private static final int DEFAULT_BG = Color.rgb(251, 249, 255);
    private static final int DEFAULT_SURFACE = Color.rgb(241, 239, 248);
    private static final int DEFAULT_TEXT = Color.rgb(32, 33, 36);
    private static final int DEFAULT_MUTED = Color.rgb(76, 73, 85);
    private static final int DEFAULT_OUTLINE = Color.rgb(219, 216, 228);
    private static final int DEFAULT_PRIMARY = Color.rgb(82, 99, 156);
    private static final int DEFAULT_PRIMARY_CONTAINER = Color.rgb(224, 226, 255);
    private static final int DEFAULT_SUCCESS = Color.rgb(66, 91, 153);
    private static final int DEFAULT_SUCCESS_CONTAINER = Color.rgb(226, 227, 245);
    private static final int DEFAULT_WARN = Color.rgb(181, 91, 42);

    private int BG = DEFAULT_BG;
    private int SURFACE = DEFAULT_SURFACE;
    private int TEXT = DEFAULT_TEXT;
    private int MUTED = DEFAULT_MUTED;
    private int OUTLINE = DEFAULT_OUTLINE;
    private int PRIMARY = DEFAULT_PRIMARY;
    private int PRIMARY_CONTAINER = DEFAULT_PRIMARY_CONTAINER;
    private int SUCCESS = DEFAULT_SUCCESS;
    private int SUCCESS_CONTAINER = DEFAULT_SUCCESS_CONTAINER;
    private int WARN = DEFAULT_WARN;
    private int DISABLED_CONTAINER = Color.rgb(235, 233, 242);
    private int PROGRESS_TRACK = Color.rgb(219, 216, 228);
    private boolean darkMode;

    private TextView accountText;
    private TextView statusText;
    private TextView statusBadge;
    private TextView loginCodeText;
    private LinearLayout loginCodePanel;
    private LinearLayout usagePanel;
    private TextView fivePercentText;
    private TextView fiveResetText;
    private ProgressBar fiveProgress;
    private TextView sevenPercentText;
    private TextView sevenResetText;
    private ProgressBar sevenProgress;
    private Button loginButton;
    private Button refreshButton;
    private Button logoutButton;
    private TextView widgetTransparencyValue;
    private SeekBar widgetTransparencySeekBar;

    private volatile boolean pollingLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        applyDynamicPalette();
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        applySystemBarMode();
        setContentView(buildContent());
        UsageRefreshScheduler.sync(this);
        renderState();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        renderState();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (accountText != null && !pollingLogin) {
            renderState();
        }
    }

    private void applyDynamicPalette() {
        darkMode = (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)
                == Configuration.UI_MODE_NIGHT_YES;
        int seed = wallpaperSeedColor();
        if (darkMode) {
            BG = blend(Color.rgb(18, 18, 24), seed, 0.07f);
            SURFACE = blend(Color.rgb(39, 38, 48), seed, 0.12f);
            TEXT = Color.rgb(240, 238, 246);
            MUTED = Color.rgb(204, 200, 215);
            OUTLINE = blend(Color.rgb(79, 76, 90), seed, 0.10f);
            PRIMARY = blend(seed, Color.WHITE, 0.48f);
            PRIMARY_CONTAINER = blend(Color.rgb(49, 48, 64), seed, 0.25f);
            SUCCESS = PRIMARY;
            SUCCESS_CONTAINER = blend(Color.rgb(52, 53, 66), seed, 0.23f);
            WARN = Color.rgb(255, 181, 141);
            DISABLED_CONTAINER = Color.rgb(48, 47, 56);
            PROGRESS_TRACK = blend(SURFACE, Color.WHITE, 0.12f);
        } else {
            BG = blend(Color.rgb(251, 249, 255), seed, 0.06f);
            SURFACE = blend(Color.rgb(241, 239, 248), seed, 0.08f);
            TEXT = DEFAULT_TEXT;
            MUTED = DEFAULT_MUTED;
            OUTLINE = blend(DEFAULT_OUTLINE, seed, 0.05f);
            PRIMARY = blend(seed, Color.rgb(32, 33, 36), 0.22f);
            PRIMARY_CONTAINER = blend(Color.rgb(231, 229, 255), seed, 0.22f);
            SUCCESS = PRIMARY;
            SUCCESS_CONTAINER = blend(Color.rgb(230, 231, 246), seed, 0.16f);
            WARN = DEFAULT_WARN;
            DISABLED_CONTAINER = blend(Color.rgb(235, 233, 242), seed, 0.04f);
            PROGRESS_TRACK = blend(Color.rgb(220, 217, 229), seed, 0.05f);
        }
    }

    private int wallpaperSeedColor() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            try {
                WallpaperColors colors = WallpaperManager.getInstance(this)
                        .getWallpaperColors(WallpaperManager.FLAG_SYSTEM);
                if (colors != null && colors.getPrimaryColor() != null) {
                    int seed = colors.getPrimaryColor().toArgb();
                    if (contrastRatio(seed, darkMode ? Color.BLACK : Color.WHITE) > 1.15f) {
                        return seed;
                    }
                }
            } catch (RuntimeException ignored) {
                // Wallpaper colors are a personalization hint; keep the default palette if unavailable.
            }
        }
        return DEFAULT_PRIMARY;
    }

    private void applySystemBarMode() {
        int flags = 0;
        if (!darkMode && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
        }
        if (!darkMode && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            flags |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
        }
        getWindow().getDecorView().setSystemUiVisibility(flags);
    }

    private View buildContent() {
        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);
        scrollView.setBackgroundColor(BG);
        scrollView.setClipToPadding(false);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(58), dp(18), dp(32));
        scrollView.addView(root, new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT,
                ScrollView.LayoutParams.WRAP_CONTENT
        ));

        TextView title = text(getString(R.string.app_name), 28, TEXT, Typeface.BOLD);
        title.setIncludeFontPadding(false);
        root.addView(title, matchWrap());

        LinearLayout statusCard = card();
        LinearLayout.LayoutParams statusCardParams = matchWrap();
        statusCardParams.setMargins(0, dp(28), 0, 0);
        root.addView(statusCard, statusCardParams);

        LinearLayout accountRow = new LinearLayout(this);
        accountRow.setOrientation(LinearLayout.HORIZONTAL);
        accountRow.setGravity(Gravity.CENTER_VERTICAL);
        statusCard.addView(accountRow, matchWrap());

        ImageView statusIcon = new ImageView(this);
        statusIcon.setImageResource(R.drawable.ic_launcher_foreground);
        statusIcon.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        statusIcon.setBackground(rounded(SUCCESS_CONTAINER, 0, 0, 26));
        statusIcon.setPadding(dp(6), dp(6), dp(6), dp(6));
        LinearLayout.LayoutParams statusIconParams = new LinearLayout.LayoutParams(dp(52), dp(52));
        statusIconParams.setMargins(0, 0, dp(16), 0);
        accountRow.addView(statusIcon, statusIconParams);

        LinearLayout accountTexts = new LinearLayout(this);
        accountTexts.setOrientation(LinearLayout.VERTICAL);
        accountRow.addView(accountTexts, new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1
        ));

        TextView accountLabel = text("账号", 18, TEXT, Typeface.BOLD);
        accountTexts.addView(accountLabel, matchWrap());

        accountText = text("", 14, MUTED, Typeface.NORMAL);
        accountText.setSingleLine(true);
        accountText.setEllipsize(TextUtils.TruncateAt.END);
        accountText.setPadding(0, dp(4), 0, 0);
        accountTexts.addView(accountText, matchWrap());

        statusBadge = text("", 12, PRIMARY, Typeface.BOLD);
        statusBadge.setGravity(Gravity.CENTER);
        statusBadge.setPadding(dp(12), dp(6), dp(12), dp(6));
        accountRow.addView(statusBadge);

        TextView statusLabel = label("当前状态");
        statusLabel.setPadding(0, dp(18), 0, dp(6));
        statusCard.addView(statusLabel);

        statusText = text("", 16, TEXT, Typeface.NORMAL);
        statusText.setLineSpacing(dp(2), 1f);
        statusCard.addView(statusText, matchWrap());

        usagePanel = new LinearLayout(this);
        usagePanel.setOrientation(LinearLayout.VERTICAL);
        usagePanel.setPadding(0, dp(18), 0, 0);
        usagePanel.addView(quotaBlock("5 小时限制", false), matchWrap());
        usagePanel.addView(quotaBlock("7 天限制", true), topMargin(14));
        statusCard.addView(usagePanel, matchWrap());

        loginCodePanel = card();
        loginCodePanel.setBackground(rounded(PRIMARY_CONTAINER, 0, 0, 28));
        LinearLayout.LayoutParams codeParams = matchWrap();
        codeParams.setMargins(0, dp(16), 0, 0);
        root.addView(loginCodePanel, codeParams);

        TextView codeLabel = text("登录验证码", 13, PRIMARY, Typeface.BOLD);
        loginCodePanel.addView(codeLabel);

        loginCodeText = text("", 24, TEXT, Typeface.BOLD);
        loginCodeText.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        loginCodeText.setPadding(0, dp(6), 0, dp(4));
        loginCodePanel.addView(loginCodeText, matchWrap());

        TextView codeHint = text("在浏览器页面输入此代码完成授权", 13, MUTED, Typeface.NORMAL);
        loginCodePanel.addView(codeHint, matchWrap());
        loginCodePanel.setVisibility(View.GONE);

        loginButton = primaryButton("登录 ChatGPT", new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startLogin();
            }
        });
        root.addView(loginButton, topMargin(18));

        refreshButton = primaryButton("刷新用量", new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                refreshUsage();
            }
        });
        root.addView(refreshButton, topMargin(18));

        logoutButton = secondaryButton("退出登录", new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                CodexAuthStore.clear(MainActivity.this);
                UsageState.empty().save(MainActivity.this);
                WidgetUpdater.clearRefreshing(MainActivity.this);
                UsageRefreshScheduler.cancel(MainActivity.this);
                WidgetUpdater.updateAll(MainActivity.this);
                loginCodeText.setText("");
                loginCodePanel.setVisibility(View.GONE);
                renderState();
            }
        });
        root.addView(logoutButton, topMargin(12));

        LinearLayout widgetStyleCard = card();
        LinearLayout.LayoutParams widgetStyleParams = matchWrap();
        widgetStyleParams.setMargins(0, dp(20), 0, 0);
        root.addView(widgetStyleCard, widgetStyleParams);

        LinearLayout widgetStyleHeader = new LinearLayout(this);
        widgetStyleHeader.setOrientation(LinearLayout.HORIZONTAL);
        widgetStyleHeader.setGravity(Gravity.CENTER_VERTICAL);
        widgetStyleCard.addView(widgetStyleHeader, matchWrap());

        TextView widgetStyleTitle = text("组件外观", 14, TEXT, Typeface.BOLD);
        widgetStyleHeader.addView(widgetStyleTitle, new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1
        ));

        widgetTransparencyValue = text("", 13, PRIMARY, Typeface.BOLD);
        widgetStyleHeader.addView(widgetTransparencyValue);

        TextView widgetStyleHint = text("卡片透明度", 13, MUTED, Typeface.NORMAL);
        widgetStyleHint.setPadding(0, dp(10), 0, 0);
        widgetStyleCard.addView(widgetStyleHint, matchWrap());

        widgetTransparencySeekBar = new SeekBar(this);
        widgetTransparencySeekBar.setMax(65);
        widgetTransparencySeekBar.setProgress(WidgetUpdater.loadCardAlphaPercent(this) - 35);
        LinearLayout.LayoutParams transparencyParams = matchWrap();
        transparencyParams.setMargins(0, dp(8), 0, 0);
        widgetStyleCard.addView(widgetTransparencySeekBar, transparencyParams);
        updateWidgetTransparencyLabel(currentWidgetAlphaPercent());
        widgetTransparencySeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                updateWidgetTransparencyLabel(progress + 35);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                int alphaPercent = seekBar.getProgress() + 35;
                WidgetUpdater.saveCardAlphaPercent(MainActivity.this, alphaPercent);
                WidgetUpdater.updateAll(MainActivity.this);
                Toast.makeText(MainActivity.this, "组件透明度已更新", Toast.LENGTH_SHORT).show();
            }
        });

        TextView note = text(
                "登录使用 OpenAI Codex Device Code 流程。应用会保存 refresh token，并缓存短期 access token；登录后每 1 小时自动刷新一次桌面用量。",
                12,
                MUTED,
                Typeface.NORMAL
        );
        note.setLineSpacing(dp(2), 1f);
        LinearLayout.LayoutParams noteParams = matchWrap();
        noteParams.setMargins(0, dp(24), 0, 0);
        root.addView(note, noteParams);

        return scrollView;
    }

    private void startLogin() {
        if (pollingLogin) {
            Toast.makeText(this, "正在等待登录完成", Toast.LENGTH_SHORT).show();
            return;
        }
        setBusy(true, "正在生成登录验证码...");
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    final CodexUsageClient.DeviceCode deviceCode = CodexUsageClient.startDeviceFlow();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            loginCodeText.setText(deviceCode.userCode);
                            loginCodePanel.setVisibility(View.VISIBLE);
                            copyLoginCode(deviceCode.userCode);
                            openLoginPage();
                            Toast.makeText(MainActivity.this, "验证码已复制，在浏览器粘贴完成授权", Toast.LENGTH_LONG).show();
                        }
                    });
                    pollLogin(deviceCode);
                } catch (IOException | JSONException e) {
                    showError("登录启动失败: " + e.getMessage());
                }
            }
        }).start();
    }

    private void pollLogin(CodexUsageClient.DeviceCode deviceCode) {
        pollingLogin = true;
        long deadline = System.currentTimeMillis() + deviceCode.expiresInSeconds * 1000L;
        while (System.currentTimeMillis() < deadline) {
            try {
                Thread.sleep(deviceCode.intervalSeconds * 1000L);
                CodexUsageClient.pollForLogin(this, deviceCode);
                pollingLogin = false;
                runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            UsageRefreshScheduler.schedule(MainActivity.this);
                            WidgetUpdater.clearRefreshing(MainActivity.this);
                            loginCodeText.setText("");
                            loginCodePanel.setVisibility(View.GONE);
                            Toast.makeText(MainActivity.this, "登录成功", Toast.LENGTH_SHORT).show();
                            renderState();
                            refreshUsage();
                    }
                });
                return;
            } catch (CodexUsageClient.PendingAuthorizationException ignored) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        statusText.setText("等待授权完成...");
                    }
                });
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (IOException | JSONException e) {
                pollingLogin = false;
                showError("登录失败: " + e.getMessage());
                return;
            }
        }
        pollingLogin = false;
        showError("登录验证码已过期，请重新登录");
    }

    private void refreshUsage() {
        CodexAuthStore auth = CodexAuthStore.load(this);
        if (!auth.isLoggedIn()) {
            Toast.makeText(this, "请先登录 ChatGPT", Toast.LENGTH_SHORT).show();
            return;
        }
        final long refreshToken = WidgetUpdater.beginRefreshing(this);
        WidgetUpdater.updateAll(this);
        if (statusText != null) {
            statusText.setText("正在刷新用量...");
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    UsageState state = CodexUsageClient.fetchUsage(MainActivity.this);
                    if (WidgetUpdater.isLatestRefresh(MainActivity.this, refreshToken)) {
                        state.save(MainActivity.this);
                        WidgetUpdater.clearRefreshing(MainActivity.this);
                        WidgetUpdater.updateAll(MainActivity.this);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(MainActivity.this, "已刷新", Toast.LENGTH_SHORT).show();
                                renderState();
                            }
                        });
                    }
                } catch (IOException | JSONException e) {
                    final String message = "刷新失败: " + e.getMessage();
                    if (WidgetUpdater.isLatestRefresh(MainActivity.this, refreshToken)) {
                        UsageState.error(MainActivity.this, message).save(MainActivity.this);
                        WidgetUpdater.clearRefreshing(MainActivity.this);
                        WidgetUpdater.updateAll(MainActivity.this);
                        showError(message);
                    }
                }
            }
        }).start();
    }

    private void openLoginPage() {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(CodexUsageClient.DEVICE_VERIFICATION_URL));
        startActivity(intent);
    }

    private void copyLoginCode(String code) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard != null) {
            clipboard.setPrimaryClip(ClipData.newPlainText("Codex 登录验证码", code));
        }
    }

    private void renderState() {
        CodexAuthStore auth = CodexAuthStore.load(this);
        UsageState state = UsageState.load(this);
        accountText.setText(auth.displayName());
        statusText.setTextColor(TEXT);

        if (state.error.length() > 0) {
            statusText.setText(state.error);
            statusText.setTextColor(WARN);
            usagePanel.setVisibility(View.GONE);
        } else if (state.hasData()) {
            statusText.setText(UsageState.formatRelativeUpdate(state.updatedAtMillis));
            fivePercentText.setText(String.format(Locale.CHINA, "剩余 %d%%", state.fiveRemainingPercent()));
            fiveResetText.setText(String.format(
                    Locale.CHINA,
                    "重置 %s",
                    UsageState.formatDate(state.fiveResetAtMillis)
            ));
            fiveProgress.setProgress(state.fiveRemainingPercent());
            sevenPercentText.setText(String.format(Locale.CHINA, "剩余 %d%%", state.sevenRemainingPercent()));
            sevenResetText.setText(String.format(
                    Locale.CHINA,
                    "重置 %s",
                    UsageState.formatDate(state.sevenResetAtMillis)
            ));
            sevenProgress.setProgress(state.sevenRemainingPercent());
            usagePanel.setVisibility(View.VISIBLE);
        } else if (auth.isLoggedIn()) {
            statusText.setText("已登录，尚未同步用量");
            usagePanel.setVisibility(View.GONE);
        } else {
            statusText.setText("登录后可查看 5 小时和 7 天用量窗口。");
            usagePanel.setVisibility(View.GONE);
        }

        boolean loggedIn = auth.isLoggedIn();
        statusBadge.setText(loggedIn ? "已登录" : "未登录");
        statusBadge.setTextColor(loggedIn ? SUCCESS : MUTED);
        statusBadge.setBackground(rounded(loggedIn ? SUCCESS_CONTAINER : DISABLED_CONTAINER, 0, 0, 16));
        loginButton.setVisibility(loggedIn ? View.GONE : View.VISIBLE);
        refreshButton.setVisibility(loggedIn ? View.VISIBLE : View.GONE);
        logoutButton.setVisibility(loggedIn ? View.VISIBLE : View.GONE);
        if (loginCodeText.getText().length() == 0) {
            loginCodePanel.setVisibility(View.GONE);
        }
        setBusy(false, null);
    }

    private void setBusy(boolean busy, String message) {
        if (loginButton != null) {
            loginButton.setEnabled(!busy);
        }
        if (refreshButton != null) {
            refreshButton.setEnabled(!busy);
        }
        if (message != null && statusText != null) {
            statusText.setText(message);
        }
    }

    private void showError(final String message) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this, message, Toast.LENGTH_LONG).show();
                if (!CodexAuthStore.load(MainActivity.this).isLoggedIn()) {
                    loginCodeText.setText("");
                    loginCodePanel.setVisibility(View.GONE);
                }
                renderState();
            }
        });
    }

    private TextView label(String value) {
        TextView label = text(value, 13, MUTED, Typeface.BOLD);
        label.setPadding(0, dp(14), 0, dp(6));
        return label;
    }

    private int currentWidgetAlphaPercent() {
        return widgetTransparencySeekBar != null
                ? widgetTransparencySeekBar.getProgress() + 35
                : WidgetUpdater.loadCardAlphaPercent(this);
    }

    private void updateWidgetTransparencyLabel(int alphaPercent) {
        if (widgetTransparencyValue != null) {
            widgetTransparencyValue.setText(String.format(Locale.CHINA, "%d%%", alphaPercent));
        }
    }

    private TextView text(String value, int sp, int color, int style) {
        TextView textView = new TextView(this);
        textView.setText(value);
        textView.setTextSize(sp);
        textView.setTextColor(color);
        textView.setTypeface(Typeface.DEFAULT, style);
        textView.setIncludeFontPadding(true);
        return textView;
    }

    private Button primaryButton(String value, View.OnClickListener listener) {
        Button button = new Button(this);
        button.setText(value);
        button.setTextColor(contrastText(PRIMARY));
        button.setTextSize(16);
        button.setAllCaps(false);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setBackground(rounded(PRIMARY, 0, 0, 24));
        button.setMinHeight(dp(52));
        button.setPadding(dp(16), 0, dp(16), 0);
        button.setOnClickListener(listener);
        return button;
    }

    private Button secondaryButton(String value, View.OnClickListener listener) {
        Button button = new Button(this);
        button.setText(value);
        button.setTextColor(PRIMARY);
        button.setTextSize(15);
        button.setAllCaps(false);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setBackground(rounded(SURFACE, OUTLINE, 1, 24));
        button.setMinHeight(dp(52));
        button.setPadding(dp(16), 0, dp(16), 0);
        button.setOnClickListener(listener);
        return button;
    }

    private LinearLayout card() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(20), dp(20), dp(20), dp(20));
        card.setBackground(rounded(SURFACE, 0, 0, 28));
        return card;
    }

    private LinearLayout quotaBlock(String title, boolean sevenDay) {
        LinearLayout block = new LinearLayout(this);
        block.setOrientation(LinearLayout.VERTICAL);

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        block.addView(row, matchWrap());

        TextView titleText = text(title, 14, TEXT, Typeface.BOLD);
        row.addView(titleText, new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1
        ));

        TextView percentText = text("", 16, SUCCESS, Typeface.BOLD);
        row.addView(percentText);

        ProgressBar progressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progressBar.setMax(100);
        progressBar.setProgress(0);
        progressBar.setProgressTintList(ColorStateList.valueOf(SUCCESS));
        progressBar.setProgressBackgroundTintList(ColorStateList.valueOf(PROGRESS_TRACK));
        LinearLayout.LayoutParams progressParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(8)
        );
        progressParams.setMargins(0, dp(8), 0, 0);
        block.addView(progressBar, progressParams);

        TextView resetText = text("", 13, MUTED, Typeface.NORMAL);
        resetText.setPadding(0, dp(6), 0, 0);
        block.addView(resetText, matchWrap());

        if (sevenDay) {
            sevenPercentText = percentText;
            sevenResetText = resetText;
            sevenProgress = progressBar;
        } else {
            fivePercentText = percentText;
            fiveResetText = resetText;
            fiveProgress = progressBar;
        }
        return block;
    }

    private View divider() {
        View divider = new View(this);
        divider.setBackgroundColor(OUTLINE);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(1)
        );
        divider.setLayoutParams(params);
        return divider;
    }

    private GradientDrawable rounded(int color, int strokeColor, int strokeWidthDp, int radiusDp) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(radiusDp));
        if (strokeWidthDp > 0) {
            drawable.setStroke(dp(strokeWidthDp), strokeColor);
        }
        return drawable;
    }

    private int blend(int from, int to, float amount) {
        float ratio = Math.max(0f, Math.min(1f, amount));
        int red = Math.round(Color.red(from) + (Color.red(to) - Color.red(from)) * ratio);
        int green = Math.round(Color.green(from) + (Color.green(to) - Color.green(from)) * ratio);
        int blue = Math.round(Color.blue(from) + (Color.blue(to) - Color.blue(from)) * ratio);
        return Color.rgb(red, green, blue);
    }

    private int contrastText(int background) {
        return contrastRatio(background, Color.WHITE) >= contrastRatio(background, Color.rgb(32, 33, 36))
                ? Color.WHITE
                : Color.rgb(32, 33, 36);
    }

    private float contrastRatio(int first, int second) {
        float firstLum = luminance(first) + 0.05f;
        float secondLum = luminance(second) + 0.05f;
        return Math.max(firstLum, secondLum) / Math.min(firstLum, secondLum);
    }

    private float luminance(int color) {
        float red = linearChannel(Color.red(color) / 255f);
        float green = linearChannel(Color.green(color) / 255f);
        float blue = linearChannel(Color.blue(color) / 255f);
        return 0.2126f * red + 0.7152f * green + 0.0722f * blue;
    }

    private float linearChannel(float value) {
        if (value <= 0.03928f) {
            return value / 12.92f;
        }
        return (float) Math.pow((value + 0.055f) / 1.055f, 2.4f);
    }

    private LinearLayout.LayoutParams topMargin(int topDp) {
        LinearLayout.LayoutParams params = matchWrap();
        params.setMargins(0, dp(topDp), 0, 0);
        return params;
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
