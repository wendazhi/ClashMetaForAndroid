# Sony BRAVIA 8 II（XR80M2 / 8M2）安装 Clash Verge TV

本教程对应 Sony BRAVIA 8 II（例如 K-55XR80M2、K-65XR80M2）。当前连接验证的国行 BRAVIA 8M2 运行 Android 14，并且系统只报告 ARMv7 32 位 ABI；不同地区和固件的架构可能不同。

## 准备

1. 国行 BRAVIA 8M2 使用 `clash-verge-tv-0.1.7-meta-armeabi-v7a-release.apk`。其他型号可先用 ADB 执行 `adb shell getprop ro.product.cpu.abilist`：包含 `arm64-v8a` 就选 64 位包，只包含 `armeabi-v7a` 就选 32 位包。
2. 安装前使用随包提供的 `SHA256SUMS.txt` 核对完整性。
3. 准备一个 U 盘，推荐 FAT32 或 exFAT；将 APK 复制到 U 盘根目录。
4. 电视连接网络，并切换到完整 Google TV 模式。Sony 说明“基本电视模式”不能安装新应用。

## 方法一：U 盘安装（推荐）

1. 在 BRAVIA 首页进入“应用”，安装一个能打开 APK 的文件管理器，例如 X-plore File Manager 或 File Commander。Sony 官方说明 Google TV 可从首页“应用”标签搜索并安装兼容应用。
2. 将 U 盘插入电视的 USB 口，打开刚安装的文件管理器，允许其访问 U 盘。
3. 找到 APK 并按遥控器中间的确认键。
4. 第一次侧载时系统会阻止安装。选择“设置”，在“安装未知应用”页面仅允许当前文件管理器。常见路径是“设置 → 应用 → 安全与限制 → 安装未知应用”；若界面不同，可在设置中搜索“未知应用”。
5. 返回文件管理器，再次打开 APK，选择“安装”。
6. 完成后选择“打开”，或回到首页“应用 → 查看全部应用 → Clash Verge TV”。
7. 安装成功后建议回到“安装未知应用”，关闭文件管理器的授权。

## 方法二：电脑通过 ADB 安装

如果 U 盘界面找不到 APK，可使用 ADB：

1. 电视进入“设置 → 系统 → 关于 → Android TV 操作系统版本/Build”，对“Build”连续按确认键 7 次，直到提示开发者模式已开启。
2. 返回“设置 → 系统 → 开发者选项”，开启“网络调试”或“无线调试”。电视固件只显示“USB 调试”时也先开启它。
3. 在“设置 → 网络和互联网”查看电视 IP，确保电脑与电视在同一局域网。
4. 电脑安装 Android Platform Tools 后执行：

```bash
adb connect 电视IP:5555
adb install -r clash-verge-tv-0.1.7-meta-armeabi-v7a-release.apk
```

5. 电视出现调试授权时选择“始终允许”并确认。若固件使用“无线调试”配对码，先按电视显示的地址执行 `adb pair IP:配对端口`，再执行电视显示的连接地址。
6. 安装后关闭网络/无线调试。

## 首次配置和启动

1. 打开 Clash Verge TV，首次焦点会落在最上方“已停止”卡片。
2. 方向键向下进入“配置”，按确认键。
3. 选择右上角“+”新建配置，选择“URL”，输入自己的订阅地址并保存。项目和安装包不附带任何订阅。
4. 回到配置列表，确认刚导入的配置左侧为蓝色选中状态，然后按返回键回到主页。
5. 选中顶部状态卡片并确认启动。Google TV 首次会显示“连接请求”，选择“确定”。
6. 状态变为“运行中”即成功；“代理”页面可选节点，“日志”页面可排查订阅或网络错误。
7. 再次按顶部状态卡片即可停止 VPN。

## 常见问题

- **解析软件包或 ABI 不兼容**：重新核对 SHA-256，并按电视的 `ro.product.cpu.abilist` 选择 APK；当前国行 BRAVIA 8M2 应使用 `armeabi-v7a`。
- **应用不显示在首页**：进入“应用 → 查看全部应用”；本包已包含 Google TV Leanback 启动入口，不需要 Sideload Launcher。
- **覆盖安装提示签名不一致**：旧包不是同一签名。先导出/记下订阅，再卸载旧包并安装本包。卸载会清除本地配置。
- **点击启动后立即停止**：先确认已选择并成功更新配置，再查看“日志”；订阅返回网页、证书异常或 YAML 错误都会导致启动失败。
- **遥控器不方便输入 URL**：可给电视配对蓝牙键盘，或使用 Google TV 手机上的虚拟遥控器输入。
- **重启电视后未自动连接**：在应用设置中开启“开机启动”；也可在 Google TV 的 VPN 设置中检查该应用是否允许始终开启。不同地区固件可能没有后一个选项。

## 安全说明

只导入你信任的订阅。VPN 应用能接触设备网络流量；不要从不明网站下载安装包。该项目为开源客户端，不提供订阅、节点或绕过当地法律的保证，请遵守所在地法律及网络服务条款。

## 官方参考

- [Sony BRAVIA 8 II K-65XR80M2 规格（Google TV）](https://www.sony.com/electronics/support/televisions-projectors-oled-tvs-android-/k-65xr80m2/specifications)
- [Sony：如何在 Google TV / Android TV 安装应用](https://www.sony.com/electronics/support/articles/00147386)
- [Sony：Google TV 可安装哪些应用](https://www.sony.com/electronics/support/articles/00114472)
- [Google：Android TV 下载或删除应用](https://support.google.com/androidtv/answer/6121463?hl=zh-Hans)
