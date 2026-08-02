# Clash Verge Rev TV

这是 Clash Verge Rev 的 Android TV 伴生版本，面向只使用遥控器的 Google TV / Android TV。桌面项目基于 Tauri，不能直接生成 Android APK，因此 TV 端复用 GPL-3.0 的 [ClashMetaForAndroid](https://github.com/MetaCubeX/ClashMetaForAndroid) 与 Mihomo 内核，并在本仓库中完成 TV 启动入口、十英尺主界面、D-pad 焦点反馈、独立包名与签名构建。

## 当前版本

- 应用名：Clash Verge Rev TV
- 包名：`io.github.clashvergerev.tv`
- 版本：`0.1.2`（versionCode 102）
- minSdk：21
- targetSdk / compileSdk：35
- Sony BRAVIA 8M2 国行实机：`armeabi-v7a`
- 已验证环境：Android 12 TV（API 31）ARM64 模拟器、Android 14 Sony BRAVIA 8M2 ARMv7 实机

## TV 改造

- 声明 `android.software.leanback`，提供 `LEANBACK_LAUNCHER` 入口，不要求触摸屏。
- 主 Activity 固定横屏，使用电视专属 `layout-television` 双列布局。
- 冷启动默认聚焦状态卡片；卡片获得焦点时有描边、缩放和高度反馈。
- 方向键可进入配置、代理、日志、设置和帮助页面。
- 新建配置支持从 URL、文件或二维码导入；项目和安装包不包含任何预置订阅。
- 使用 Android `VpnService` 和前台通知运行 Mihomo，配置格式与 Clash/Mihomo YAML 兼容。
- Release 包使用独立应用 ID 和本地 release key 签名，可与其他 Clash 客户端并存。

## 构建

需要 JDK 21、Android SDK 35、Build Tools 35.0.0、NDK 29.0.14206865、CMake 3.22.1 和 Go。

```bash
cd /Users/wendazhi/www/rust/clash-verge-rev/android-tv
export JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home
export ANDROID_HOME=/opt/homebrew/share/android-commandlinetools
export ANDROID_SDK_ROOT="$ANDROID_HOME"
./gradlew --no-daemon testMetaReleaseUnitTest app:lintMetaRelease app:assembleMetaRelease
```

构建产物位于 `app/build/outputs/apk/meta/release/`，只生成 `arm64-v8a` 与 `armeabi-v7a` 两种 APK。当前连接验证的国行 BRAVIA 8M2 仅报告 `armeabi-v7a`；其他型号请先通过 ADB 执行 `adb shell getprop ro.product.cpu.abilist` 确认架构。

签名配置保存在被 Git 忽略的 `signing.properties`，密钥库也未纳入版本控制。发布升级包时必须保留同一密钥，否则 Android 会拒绝覆盖安装。

## 验证记录

- Gradle 单元测试、Lint 和 Meta Release 构建通过。
- APK v1、v2 签名校验通过。
- Android 12 TV ARM64 冷启动、Leanback 入口和遥控器方向键导航通过。
- URL 配置导入、配置选中、系统 VPN 授权、`tun0` 建立、前台服务、流量状态和停止 VPN 全链路通过。

## 上游与许可

- 桌面项目：[clash-verge-rev/clash-verge-rev](https://github.com/clash-verge-rev/clash-verge-rev)
- Android 基础：[MetaCubeX/ClashMetaForAndroid](https://github.com/MetaCubeX/ClashMetaForAndroid)
- 代理内核：[MetaCubeX/mihomo](https://github.com/MetaCubeX/mihomo)

Android TV 端延续上游 GPL-3.0 许可。此软件只提供网络代理客户端能力，不包含订阅服务或代理节点。
