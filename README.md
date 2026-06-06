# CodeX Usage Widget

CodeX Usage Widget 是一个原生 Android 桌面小组件，用于展示 ChatGPT Codex 的 5 小时和 7 天用量窗口状态。

## 功能

- 通过 Codex Device Code 登录流程完成 ChatGPT 授权。
- 在应用私有 `SharedPreferences` 中保存认证状态，并在 access token 到期前刷新。
- 查询 ChatGPT Web 后端用量接口，解析 Codex 5 小时和 7 天窗口。
- 提供 2x2 和 4x2 两种桌面小组件尺寸。
- 展示账号、最近同步时间、剩余额度百分比和重置倒计时。
- 支持点击小组件打开应用，或通过刷新按钮触发后台同步。
- 获取登录验证码后自动复制到剪贴板，便于在授权页面直接粘贴。
- 主界面会读取系统壁纸颜色，并基于壁纸主色生成应用配色。
- 主界面和桌面小组件均支持系统深色模式。

## 界面预览

| 登录界面 | 登录后界面 | 桌面图标和小组件 |
| --- | --- | --- |
| <img src="docs/images/login.jpg" alt="登录界面" width="220"> | <img src="docs/images/usage.jpg" alt="登录后界面" width="220"> | <img src="docs/images/widgets.jpg" alt="桌面图标和小组件" width="220"> |

## 数据接口

项目使用的用量数据来自 ChatGPT Web 后端接口：

```text
https://chatgpt.com/backend-api/wham/usage
```

该接口不是 OpenAI Platform Usage API，可能会随 ChatGPT/Codex Web 后端调整而变化。

## 技术栈

- Android Gradle Plugin 8.7.3
- Java
- AndroidX WorkManager 2.10.0
- Minimum SDK 23（Android 6.0 Marshmallow）
- Target SDK 35（Android 15）

最低支持 Android 6.0/API 23。项目当前核心能力包括桌面小组件、SharedPreferences、本地剪贴板、网络请求和 WorkManager 后台刷新；壁纸取色等较新系统能力已经在代码中按 API 等级做运行时判断并提供降级路径，因此不需要为了这些增强特性提高最低版本。

## 构建

使用 Android Studio 打开项目并等待 Gradle 同步，或在已配置 Android SDK 的环境中执行：

```bash
./gradlew assembleDebug
```

生成的 debug APK 位于：

```text
app/build/outputs/apk/debug/app-debug.apk
```

Release 构建会生成通用 APK 以及常见 ABI 的 APK：

```bash
./gradlew assembleRelease
```

```text
app/build/outputs/apk/release/
```

包括 `universal`、`arm64-v8a`、`armeabi-v7a`、`x86`、`x86_64`。本项目没有 native 库，通用 APK 可安装在这些 CPU 架构的设备上；ABI 包主要用于发布页按设备架构下载。

## 自动发版

仓库包含 GitHub Actions 工作流 `.github/workflows/android-release.yml`。推送 `v*` tag 后会自动构建 release APK，上传工作流产物，并发布到对应 GitHub Release。

```bash
git tag v1.0.0
git push origin v1.0.0
```

CI 会从 tag 生成 `versionName`，例如 `v1.0.0` 对应 `1.0.0`；`versionCode` 使用 GitHub Actions run number。

如需使用正式签名，在仓库 Secrets 中配置：

```text
ANDROID_KEYSTORE_BASE64
ANDROID_KEYSTORE_PASSWORD
ANDROID_KEY_ALIAS
ANDROID_KEY_PASSWORD
```

未配置签名密钥时，CI 会使用 debug key 签名 release APK，便于 GitHub Release 侧载安装测试；正式分发建议配置独立 release keystore。

## 使用

1. 安装并启动应用。
2. 按界面提示完成 Codex Device Code 授权。
3. 应用获取登录验证码后会自动复制到剪贴板，在授权页面粘贴即可。
4. 返回 Android 桌面，添加 `CodeX用量 2x2` 或 `CodeX用量 4x2` 小组件。
5. 小组件会显示最近一次同步到的 Codex 用量信息，并跟随系统深色模式。

## 项目结构

```text
app/src/main/java/com/lichen/codexusage/
  MainActivity.java                 应用主界面与登录流程
  CodexUsageClient.java             授权、token 刷新与用量查询
  CodexAuthStore.java               本地认证状态存储
  UsageState.java                   用量状态模型与持久化
  UsageRefreshWorker.java           后台刷新任务
  UsageRefreshScheduler.java        刷新任务调度
  CodeXWidgetProvider.java          4x2 小组件入口
  CodeXWidgetCompactProvider.java   2x2 小组件入口
  WidgetUpdater.java                小组件渲染逻辑

app/src/main/res/
  layout/                           小组件布局
  drawable/                         小组件和按钮资源
  xml/                              小组件尺寸与配置
```

## 注意事项

- 本项目依赖 ChatGPT Web 后端的 Codex 用量接口，不保证长期稳定。
- refresh token 仅保存在应用私有存储中，不会写入仓库或外部文件。
- `local.properties`、构建产物和 IDE 本地配置已通过 `.gitignore` 排除。
