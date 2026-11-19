# 双生思辨 · DialecticSelf

一款 100% 本地离线运行的自我辩证笔记与流式语音思辨 Android 应用（Kotlin + Jetpack Compose + Room + Sherpa-ONNX 中英双语端侧识别）。

## 在 GitHub 直接下载可安装的 APK

仓库已配置自动化构建工作流（`.github/workflows/build-apk.yml`）。每次推送代码到 GitHub 后，会自动完成单元测试，并同时打包发布 **Release 正式版** 与 **Debug 调试版** APK：

### 方式一：通过 Releases 直接下载 `.apk`（推荐，手机可直接下载安装）
1. 进入 GitHub 仓库主页右侧的 **Releases**（`latest-apk` 最新发行版）。
2. 在 **Assets** 下直接点击下载 `DialecticSelf-release.apk`（推荐日常使用）或 `DialecticSelf-debug.apk`。
3. 下载后直接在 Android 设备上点击安装即可（已内置离线的 Sherpa-ONNX 中英双语识别模型，无需联网下载模型）。

### 方式二：通过 Actions Artifacts 下载
1. 打开 GitHub 仓库顶部的 **Actions** 标签页。
2. 点开最近一次状态为绿色的 **Build Android APK** 运行记录（也可点击 **Run workflow** 手动触发）。
3. 在页面底部的 **Artifacts** 区域下载 `DialecticSelf-APKs` 压缩包，解压后即可获得 `.apk` 安装包。

## 可选：替换 Release 正式发布签名

默认工作流会在未配置自定义密钥时自动使用仓库固定签名生成可升级覆盖安装的 `DialecticSelf-release.apk`。如需绑定您自己的 Play 上架签名，可在 GitHub 仓库的 **Settings -> Secrets and variables -> Actions** 中添加以下三个 Secret：
- `RELEASE_KEYSTORE_BASE64`：正式 `.jks` 签名文件的 Base64 编码内容（`key_alias` 需为 `upload`）
- `STORE_PASSWORD`：签名库密码
- `KEY_PASSWORD`：密钥密码

## 本地构建命令

```bash
# 运行本地单元测试与 Robolectric 测试
gradle :app:testDebugUnitTest

# 构建 Debug APK
gradle :app:assembleDebug

# 构建 Release APK（需提供签名环境变量）
gradle :app:assembleRelease
```
