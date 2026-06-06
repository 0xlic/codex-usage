package com.lichen.codexusage;

import android.content.Context;
import android.util.Base64;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

final class CodexUsageClient {
    static final String DEVICE_VERIFICATION_URL = "https://auth.openai.com/codex/device";

    private static final String CLIENT_ID = "app_EMoamEEZ73f0CkXaXp7hrann";
    private static final String DEVICE_AUTH_USERCODE_URL =
            "https://auth.openai.com/api/accounts/deviceauth/usercode";
    private static final String DEVICE_AUTH_TOKEN_URL =
            "https://auth.openai.com/api/accounts/deviceauth/token";
    private static final String OAUTH_TOKEN_URL = "https://auth.openai.com/oauth/token";
    private static final String DEVICE_REDIRECT_URI = "https://auth.openai.com/deviceauth/callback";
    private static final String USAGE_URL = "https://chatgpt.com/backend-api/wham/usage";
    private static final String USER_AGENT = "codex-cli";

    private CodexUsageClient() {
    }

    static DeviceCode startDeviceFlow() throws IOException, JSONException {
        JSONObject body = new JSONObject();
        body.put("client_id", CLIENT_ID);
        JSONObject response = postJson(DEVICE_AUTH_USERCODE_URL, body);
        return new DeviceCode(
                response.getString("device_auth_id"),
                response.getString("user_code"),
                response.optLong("expires_in", 900L),
                parseInterval(response.opt("interval"))
        );
    }

    static LoginResult pollForLogin(Context context, DeviceCode deviceCode)
            throws IOException, JSONException, PendingAuthorizationException {
        JSONObject body = new JSONObject();
        body.put("device_auth_id", deviceCode.deviceAuthId);
        body.put("user_code", deviceCode.userCode);

        HttpResult poll = postJsonResult(DEVICE_AUTH_TOKEN_URL, body);
        if (poll.statusCode == HttpURLConnection.HTTP_FORBIDDEN
                || poll.statusCode == HttpURLConnection.HTTP_NOT_FOUND) {
            throw new PendingAuthorizationException();
        }
        if (poll.statusCode == HttpURLConnection.HTTP_GONE) {
            throw new IOException("登录验证码已过期");
        }
        if (!poll.isSuccess()) {
            throw new IOException("登录轮询失败: HTTP " + poll.statusCode + " " + poll.body);
        }

        JSONObject success = new JSONObject(poll.body);
        JSONObject tokens = exchangeAuthorizationCode(
                success.getString("authorization_code"),
                success.getString("code_verifier")
        );
        LoginResult result = parseLoginResult(tokens);
        CodexAuthStore.saveLogin(
                context,
                result.accountId,
                result.email,
                result.refreshToken,
                result.accessToken,
                result.accessExpiresAtMillis
        );
        return result;
    }

    static UsageState fetchUsage(Context context) throws IOException, JSONException {
        CodexAuthStore auth = CodexAuthStore.load(context);
        if (!auth.isLoggedIn()) {
            throw new IOException("请先登录 ChatGPT");
        }

        String accessToken = auth.accessToken;
        if (!auth.hasFreshAccessToken()) {
            JSONObject refreshed = refreshAccessToken(auth.refreshToken);
            accessToken = refreshed.getString("access_token");
            long expiresAt = computeExpiresAt(refreshed.optLong("expires_in", 3600L));
            CodexAuthStore.saveAccessToken(context, accessToken, expiresAt);
        }

        HttpURLConnection connection = openConnection(USAGE_URL, "GET");
        connection.setRequestProperty("Authorization", "Bearer " + accessToken);
        connection.setRequestProperty("Accept", "application/json");
        connection.setRequestProperty("User-Agent", USER_AGENT);
        if (auth.accountId.length() > 0) {
            connection.setRequestProperty("ChatGPT-Account-Id", auth.accountId);
        }

        HttpResult result = readResult(connection);
        if (!result.isSuccess()) {
            throw new IOException("用量查询失败: HTTP " + result.statusCode + " " + result.body);
        }
        return parseUsage(new JSONObject(result.body), auth.displayName());
    }

    private static JSONObject exchangeAuthorizationCode(String code, String codeVerifier)
            throws IOException, JSONException {
        String form = formEncode(
                "grant_type", "authorization_code",
                "code", code,
                "redirect_uri", DEVICE_REDIRECT_URI,
                "client_id", CLIENT_ID,
                "code_verifier", codeVerifier
        );
        return new JSONObject(postForm(OAUTH_TOKEN_URL, form).body);
    }

    private static JSONObject refreshAccessToken(String refreshToken)
            throws IOException, JSONException {
        String form = formEncode(
                "grant_type", "refresh_token",
                "refresh_token", refreshToken,
                "client_id", CLIENT_ID,
                "scope", "openid profile email"
        );
        HttpResult result = postForm(OAUTH_TOKEN_URL, form);
        if (!result.isSuccess()) {
            throw new IOException("登录已失效，请重新登录: HTTP " + result.statusCode);
        }
        return new JSONObject(result.body);
    }

    private static LoginResult parseLoginResult(JSONObject tokens) throws JSONException, IOException {
        String accessToken = tokens.getString("access_token");
        String refreshToken = tokens.optString("refresh_token", "");
        if (refreshToken.length() == 0) {
            throw new IOException("登录响应缺少 refresh_token");
        }

        JSONObject claims = null;
        String idToken = tokens.optString("id_token", "");
        if (idToken.length() > 0) {
            claims = parseJwtPayload(idToken);
        }
        if (claims == null) {
            claims = parseJwtPayload(accessToken);
        }
        if (claims == null) {
            throw new IOException("无法解析登录账号");
        }

        String accountId = claims.optString("chatgpt_account_id", "");
        JSONObject openAiAuth = claims.optJSONObject("https://api.openai.com/auth");
        if (accountId.length() == 0 && openAiAuth != null) {
            accountId = openAiAuth.optString("chatgpt_account_id", "");
        }
        if (accountId.length() == 0) {
            JSONArray organizations = claims.optJSONArray("organizations");
            if (organizations != null && organizations.length() > 0) {
                JSONObject firstOrg = organizations.optJSONObject(0);
                if (firstOrg != null) {
                    accountId = firstOrg.optString("chatgpt_account_id", "");
                }
            }
        }
        if (accountId.length() == 0) {
            throw new IOException("无法从 token 中提取 ChatGPT 账号 ID");
        }

        return new LoginResult(
                accountId,
                claims.optString("email", ""),
                refreshToken,
                accessToken,
                computeExpiresAt(tokens.optLong("expires_in", 3600L))
        );
    }

    private static UsageState parseUsage(JSONObject response, String accountLabel) {
        JSONObject rateLimit = response.optJSONObject("rate_limit");
        RateWindow five = null;
        RateWindow seven = null;
        if (rateLimit != null) {
            RateWindow primary = parseWindow(rateLimit.optJSONObject("primary_window"));
            RateWindow secondary = parseWindow(rateLimit.optJSONObject("secondary_window"));
            for (RateWindow window : new RateWindow[]{primary, secondary}) {
                if (window == null) {
                    continue;
                }
                if (window.windowSeconds == 18_000L) {
                    five = window;
                } else if (window.windowSeconds == 604_800L) {
                    seven = window;
                }
            }
            if (five == null) {
                five = primary;
            }
            if (seven == null) {
                seven = secondary;
            }
        }

        return new UsageState(
                accountLabel,
                five == null ? 0d : five.usedPercent,
                five == null ? 0L : five.resetAtMillis,
                seven == null ? 0d : seven.usedPercent,
                seven == null ? 0L : seven.resetAtMillis,
                System.currentTimeMillis(),
                ""
        );
    }

    private static RateWindow parseWindow(JSONObject object) {
        if (object == null) {
            return null;
        }
        return new RateWindow(
                object.optDouble("used_percent", 0d),
                object.optLong("limit_window_seconds", 0L),
                object.optLong("reset_at", 0L) * 1000L
        );
    }

    private static JSONObject parseJwtPayload(String jwt) {
        String[] parts = jwt.split("\\.");
        if (parts.length < 2) {
            return null;
        }
        try {
            byte[] decoded = Base64.decode(parts[1], Base64.URL_SAFE | Base64.NO_PADDING | Base64.NO_WRAP);
            return new JSONObject(new String(decoded, StandardCharsets.UTF_8));
        } catch (IllegalArgumentException | JSONException e) {
            return null;
        }
    }

    private static JSONObject postJson(String url, JSONObject body) throws IOException, JSONException {
        HttpResult result = postJsonResult(url, body);
        if (!result.isSuccess()) {
            throw new IOException("HTTP " + result.statusCode + " " + result.body);
        }
        return new JSONObject(result.body);
    }

    private static HttpResult postJsonResult(String url, JSONObject body) throws IOException {
        HttpURLConnection connection = openConnection(url, "POST");
        connection.setRequestProperty("Content-Type", "application/json");
        writeBody(connection, body.toString());
        return readResult(connection);
    }

    private static HttpResult postForm(String url, String body) throws IOException {
        HttpURLConnection connection = openConnection(url, "POST");
        connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        writeBody(connection, body);
        return readResult(connection);
    }

    private static HttpURLConnection openConnection(String url, String method) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestMethod(method);
        connection.setConnectTimeout(15_000);
        connection.setReadTimeout(15_000);
        connection.setRequestProperty("User-Agent", USER_AGENT);
        return connection;
    }

    private static void writeBody(HttpURLConnection connection, String body) throws IOException {
        connection.setDoOutput(true);
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        connection.setFixedLengthStreamingMode(bytes.length);
        try (OutputStream outputStream = connection.getOutputStream()) {
            outputStream.write(bytes);
        }
    }

    private static HttpResult readResult(HttpURLConnection connection) throws IOException {
        int status = connection.getResponseCode();
        InputStream stream = status >= 200 && status < 300
                ? connection.getInputStream()
                : connection.getErrorStream();
        String body = stream == null ? "" : readAll(stream);
        connection.disconnect();
        return new HttpResult(status, body);
    }

    private static String readAll(InputStream stream) throws IOException {
        StringBuilder builder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
        }
        return builder.toString();
    }

    private static String formEncode(String... pairs) throws IOException {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < pairs.length; i += 2) {
            if (i > 0) {
                builder.append('&');
            }
            builder.append(URLEncoder.encode(pairs[i], "UTF-8"));
            builder.append('=');
            builder.append(URLEncoder.encode(pairs[i + 1], "UTF-8"));
        }
        return builder.toString();
    }

    private static long computeExpiresAt(long expiresInSeconds) {
        return System.currentTimeMillis() + Math.max(60L, expiresInSeconds) * 1000L;
    }

    private static long parseInterval(Object value) {
        if (value instanceof Number) {
            return Math.max(3L, ((Number) value).longValue());
        }
        if (value instanceof String) {
            try {
                return Math.max(3L, Long.parseLong((String) value));
            } catch (NumberFormatException ignored) {
                return 5L;
            }
        }
        return 5L;
    }

    static final class DeviceCode {
        final String deviceAuthId;
        final String userCode;
        final long expiresInSeconds;
        final long intervalSeconds;

        DeviceCode(String deviceAuthId, String userCode, long expiresInSeconds, long intervalSeconds) {
            this.deviceAuthId = deviceAuthId;
            this.userCode = userCode;
            this.expiresInSeconds = expiresInSeconds;
            this.intervalSeconds = intervalSeconds;
        }
    }

    static final class LoginResult {
        final String accountId;
        final String email;
        final String refreshToken;
        final String accessToken;
        final long accessExpiresAtMillis;

        LoginResult(
                String accountId,
                String email,
                String refreshToken,
                String accessToken,
                long accessExpiresAtMillis
        ) {
            this.accountId = accountId;
            this.email = email;
            this.refreshToken = refreshToken;
            this.accessToken = accessToken;
            this.accessExpiresAtMillis = accessExpiresAtMillis;
        }
    }

    static final class PendingAuthorizationException extends Exception {
    }

    private static final class RateWindow {
        final double usedPercent;
        final long windowSeconds;
        final long resetAtMillis;

        RateWindow(double usedPercent, long windowSeconds, long resetAtMillis) {
            this.usedPercent = usedPercent;
            this.windowSeconds = windowSeconds;
            this.resetAtMillis = resetAtMillis;
        }
    }

    private static final class HttpResult {
        final int statusCode;
        final String body;

        HttpResult(int statusCode, String body) {
            this.statusCode = statusCode;
            this.body = body;
        }

        boolean isSuccess() {
            return statusCode >= 200 && statusCode < 300;
        }
    }
}
