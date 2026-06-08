package com.lichen.codexusage;

import android.app.Activity;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class SettingsActivity extends Activity {
    private static final int LIGHT_BG = Color.rgb(251, 249, 255);
    private static final int LIGHT_SURFACE = Color.rgb(241, 239, 248);
    private static final int LIGHT_TEXT = Color.rgb(32, 33, 36);
    private static final int LIGHT_MUTED = Color.rgb(76, 73, 85);
    private static final int LIGHT_OUTLINE = Color.rgb(219, 216, 228);
    private static final int LIGHT_PRIMARY = Color.rgb(82, 99, 156);
    private static final int LIGHT_PRIMARY_CONTAINER = Color.rgb(224, 226, 255);

    private int BG = LIGHT_BG;
    private int SURFACE = LIGHT_SURFACE;
    private int TEXT = LIGHT_TEXT;
    private int MUTED = LIGHT_MUTED;
    private int OUTLINE = LIGHT_OUTLINE;
    private int PRIMARY = LIGHT_PRIMARY;
    private int PRIMARY_CONTAINER = LIGHT_PRIMARY_CONTAINER;

    private Switch refreshSwitch;
    private LinearLayout environmentPanel;
    private LinearLayout environmentSelector;
    private TextView environmentValueText;
    private TextView environmentChevronText;
    private TextView statusText;
    private final ArrayList<CodexUsageClient.CodexEnvironment> environments = new ArrayList<>();
    private boolean loadingEnvironments;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        applyPalette();
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        applySystemBarMode();
        setContentView(buildContent());
        renderSettings();
    }

    private void applyPalette() {
        boolean darkMode = (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)
                == Configuration.UI_MODE_NIGHT_YES;
        if (darkMode) {
            BG = Color.rgb(18, 18, 24);
            SURFACE = Color.rgb(39, 38, 48);
            TEXT = Color.rgb(240, 238, 246);
            MUTED = Color.rgb(204, 200, 215);
            OUTLINE = Color.rgb(79, 76, 90);
            PRIMARY = Color.rgb(178, 188, 255);
            PRIMARY_CONTAINER = Color.rgb(49, 48, 64);
        }
    }

    private void applySystemBarMode() {
        int flags = 0;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                && (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)
                != Configuration.UI_MODE_NIGHT_YES) {
            flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                && (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)
                != Configuration.UI_MODE_NIGHT_YES) {
            flags |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
        }
        getWindow().getDecorView().setSystemUiVisibility(flags);
    }

    private View buildContent() {
        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);
        scrollView.setBackgroundColor(BG);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(58), dp(18), dp(32));
        scrollView.addView(root, new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT,
                ScrollView.LayoutParams.WRAP_CONTENT
        ));

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        root.addView(header, matchWrap());

        TextView title = text("设置", 28, TEXT, Typeface.BOLD);
        title.setIncludeFontPadding(false);
        header.addView(title, new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1
        ));

        TextView closeButton = headerActionButton("返回", new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
        header.addView(closeButton, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                dp(44)
        ));

        LinearLayout refreshCard = card();
        LinearLayout.LayoutParams cardParams = matchWrap();
        cardParams.setMargins(0, dp(28), 0, 0);
        root.addView(refreshCard, cardParams);

        LinearLayout switchRow = new LinearLayout(this);
        switchRow.setOrientation(LinearLayout.HORIZONTAL);
        switchRow.setGravity(Gravity.CENTER_VERTICAL);
        refreshCard.addView(switchRow, matchWrap());

        LinearLayout switchTexts = new LinearLayout(this);
        switchTexts.setOrientation(LinearLayout.VERTICAL);
        switchRow.addView(switchTexts, new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1
        ));

        TextView switchTitle = text("5 小时窗口刷新", 16, TEXT, Typeface.BOLD);
        switchTexts.addView(switchTitle, matchWrap());

        TextView switchHint = text("打开后选择一个 Codex Cloud 环境", 13, MUTED, Typeface.NORMAL);
        switchHint.setPadding(0, dp(4), dp(12), 0);
        switchTexts.addView(switchHint, matchWrap());

        refreshSwitch = new Switch(this);
        switchRow.addView(refreshSwitch);
        refreshSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                CodexSettingsStore.saveFiveHourRefreshEnabled(SettingsActivity.this, isChecked);
                renderEnvironmentPanel(isChecked);
                if (isChecked && environments.isEmpty()) {
                    loadEnvironments();
                }
            }
        });

        environmentPanel = new LinearLayout(this);
        environmentPanel.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams panelParams = matchWrap();
        panelParams.setMargins(0, dp(18), 0, 0);
        refreshCard.addView(environmentPanel, panelParams);

        TextView environmentLabel = text("Codex Cloud 环境", 13, MUTED, Typeface.BOLD);
        environmentPanel.addView(environmentLabel, matchWrap());

        environmentSelector = new LinearLayout(this);
        environmentSelector.setOrientation(LinearLayout.HORIZONTAL);
        environmentSelector.setGravity(Gravity.CENTER_VERTICAL);
        environmentSelector.setMinimumHeight(dp(54));
        environmentSelector.setPadding(dp(16), 0, dp(14), 0);
        environmentSelector.setBackground(rounded(PRIMARY_CONTAINER, OUTLINE, 1, 18));
        environmentSelector.setClickable(true);
        environmentSelector.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (loadingEnvironments) {
                    return;
                }
                if (environments.isEmpty()) {
                    loadEnvironments();
                    return;
                }
                showEnvironmentMenu();
            }
        });
        LinearLayout.LayoutParams selectorParams = matchWrap();
        selectorParams.setMargins(0, dp(8), 0, 0);
        environmentPanel.addView(environmentSelector, selectorParams);

        environmentValueText = text("选择环境", 15, TEXT, Typeface.BOLD);
        environmentValueText.setSingleLine(true);
        environmentValueText.setEllipsize(TextUtils.TruncateAt.END);
        environmentSelector.addView(environmentValueText, new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1
        ));

        environmentChevronText = text("v", 18, PRIMARY, Typeface.BOLD);
        environmentChevronText.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams chevronParams = new LinearLayout.LayoutParams(dp(28), dp(28));
        chevronParams.setMargins(dp(10), 0, 0, 0);
        environmentSelector.addView(environmentChevronText, chevronParams);

        statusText = text("", 13, MUTED, Typeface.NORMAL);
        statusText.setPadding(0, dp(10), 0, 0);
        environmentPanel.addView(statusText, matchWrap());

        return scrollView;
    }

    private void renderSettings() {
        CodexSettingsStore settings = CodexSettingsStore.load(this);
        refreshSwitch.setChecked(settings.fiveHourRefreshEnabled);
        renderEnvironmentPanel(settings.fiveHourRefreshEnabled);
        environmentValueText.setText(settings.fiveHourEnvironmentId.length() > 0
                ? "正在加载已保存环境..."
                : "选择环境");
        if (settings.fiveHourRefreshEnabled) {
            loadEnvironments();
        }
    }

    private void renderEnvironmentPanel(boolean visible) {
        environmentPanel.setVisibility(visible ? View.VISIBLE : View.GONE);
        if (!visible) {
            statusText.setText("");
        }
    }

    private void loadEnvironments() {
        if (loadingEnvironments) {
            return;
        }
        if (!CodexAuthStore.load(this).isLoggedIn()) {
            statusText.setText("请先登录 ChatGPT 后再查询环境");
            Toast.makeText(this, "请先登录 ChatGPT", Toast.LENGTH_SHORT).show();
            return;
        }
        loadingEnvironments = true;
        environmentSelector.setEnabled(false);
        environmentSelector.setAlpha(0.72f);
        environmentValueText.setText("正在查询环境...");
        statusText.setText("正在查询环境...");
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    final List<CodexUsageClient.CodexEnvironment> environments =
                            CodexUsageClient.fetchEnvironments(SettingsActivity.this);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            renderEnvironments(environments);
                        }
                    });
                } catch (IOException | JSONException e) {
                    showError("环境查询失败: " + e.getMessage());
                }
            }
        }).start();
    }

    private void renderEnvironments(List<CodexUsageClient.CodexEnvironment> loadedEnvironments) {
        loadingEnvironments = false;
        environmentSelector.setEnabled(true);
        environmentSelector.setAlpha(1f);
        environments.clear();
        environments.addAll(loadedEnvironments);
        if (loadedEnvironments.isEmpty()) {
            environmentValueText.setText("暂无可用环境");
            statusText.setText("未查询到可用环境");
            return;
        }

        String savedEnvironmentId = CodexSettingsStore.load(this).fiveHourEnvironmentId;
        int selectedIndex = 0;
        for (int i = 0; i < loadedEnvironments.size(); i++) {
            if (loadedEnvironments.get(i).id.equals(savedEnvironmentId)) {
                selectedIndex = i;
                break;
            }
        }

        CodexUsageClient.CodexEnvironment selected = loadedEnvironments.get(selectedIndex);
        if (savedEnvironmentId.length() == 0) {
            CodexSettingsStore.saveFiveHourEnvironmentId(this, selected.id);
        }
        selectEnvironment(selected, false);
        statusText.setText("已加载 " + loadedEnvironments.size() + " 个环境");
    }

    private void showEnvironmentMenu() {
        final PopupWindow popupWindow = new PopupWindow(this);
        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(false);

        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setPadding(0, dp(6), 0, dp(6));
        list.setBackground(rounded(SURFACE, OUTLINE, 1, 18));
        scrollView.addView(list, new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT,
                ScrollView.LayoutParams.WRAP_CONTENT
        ));

        String selectedId = CodexSettingsStore.load(this).fiveHourEnvironmentId;
        for (int i = 0; i < environments.size(); i++) {
            final CodexUsageClient.CodexEnvironment environment = environments.get(i);
            TextView row = environmentRow(environment, environment.id.equals(selectedId));
            row.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    selectEnvironment(environment, true);
                    popupWindow.dismiss();
                }
            });
            list.addView(row, new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    dp(52)
            ));
        }

        int popupHeight = Math.min(dp(320), dp(64) * environments.size());
        popupWindow.setContentView(scrollView);
        popupWindow.setWidth(environmentSelector.getWidth());
        popupWindow.setHeight(popupHeight);
        popupWindow.setFocusable(true);
        popupWindow.setOutsideTouchable(true);
        popupWindow.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            popupWindow.setElevation(dp(8));
        }
        popupWindow.showAsDropDown(environmentSelector, 0, dp(6));
    }

    private TextView environmentRow(CodexUsageClient.CodexEnvironment environment, boolean selected) {
        TextView row = text(environment.label, 15, selected ? PRIMARY : TEXT, Typeface.BOLD);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setSingleLine(true);
        row.setEllipsize(TextUtils.TruncateAt.END);
        row.setPadding(dp(16), 0, dp(16), 0);
        row.setBackground(rounded(selected ? PRIMARY_CONTAINER : SURFACE, 0, 0, 14));
        return row;
    }

    private void selectEnvironment(CodexUsageClient.CodexEnvironment environment, boolean persist) {
        environmentValueText.setText(environment.label);
        if (persist) {
            CodexSettingsStore.saveFiveHourEnvironmentId(this, environment.id);
            statusText.setText("已选择: " + environment.label);
        }
    }

    private void showError(final String message) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                loadingEnvironments = false;
                environmentSelector.setEnabled(true);
                environmentSelector.setAlpha(1f);
                environmentValueText.setText("重新查询环境");
                statusText.setText(message);
                Toast.makeText(SettingsActivity.this, message, Toast.LENGTH_LONG).show();
            }
        });
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

    private TextView headerActionButton(String value, View.OnClickListener listener) {
        TextView button = text(value, 14, PRIMARY, Typeface.BOLD);
        button.setGravity(Gravity.CENTER);
        button.setMinWidth(dp(64));
        button.setPadding(dp(14), 0, dp(14), 0);
        button.setBackground(rounded(PRIMARY_CONTAINER, OUTLINE, 1, 22));
        button.setOnClickListener(listener);
        button.setClickable(true);
        return button;
    }

    private LinearLayout card() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(20), dp(20), dp(20), dp(20));
        card.setBackground(rounded(SURFACE, 0, 0, 28));
        return card;
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
