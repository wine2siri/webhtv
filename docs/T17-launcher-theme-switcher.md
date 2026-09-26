# T17 图标、TV Banner 与启动过场主题

## 结果

- 默认主题为 H3「曜石鎏金」：闭合圆环与播放三角均以同一中心点构造。
- 应用显示名统一为「影卓」。
- 支持六套主题：H3、深色胡桃木、曜黑玻璃、荷叶露珠、午夜大理石、月下水面。
- 手机端和 TV 端设置页均提供「图标与启动主题」入口。
- 六套主题现在同时驱动启动过场：材质背景、H3 标志与「影卓」字标会做 650ms 的缩放淡入，再以 260ms 淡出，不阻塞首页初始化。
- 冷启动不再先显示历史 WebHTV 位图；系统等待阶段只显示同色背景，应用首帧只播放一套“InJoy / 影卓智能追剧引擎”品牌过场。
- 手机端继续切换 Launcher 图标；TV 端固定 H3 桌面入口和 Banner，主题差异在启动过场中可见，以保证 Projectivy 卡片稳定。

## 实现

手机端仍通过指向 `HomeActivity` 的 `activity-alias` 切换桌面图标。TV 端恢复为稳定的 `HomeActivity` Launcher 组件，不再动态启停别名；`launcher_theme_changes_component` 使同一套设置逻辑在 TV 上只保存主题选择，在手机上同时更换组件。

`LauncherThemeTransition` 在首次创建首页时叠加一个轻量启动过场，直接读取 `launcher_theme` 选择的背景与图标资源。不创建额外 Splash Activity，因此不会在 Android 12+ 上产生「系统启动画面 + 专用 Activity」的双重闪屏。

自然材质只作为背景，圆环、播放符号与 Banner 字标均为确定性矢量，因此主题变化不会改变标志的几何中心。五张生成材质已标准化为 1280×720 WebP，合计约 0.7 MB。

对 TV 来说，桌面图标和 Banner 固定为默认 H3，这是 Projectivy 兼容性的明确取舍，不是用户设置限制。组件身份保持不变比每次切换后让用户重新固定卡片更稳定。

## 最佳实践与方案取舍（2026-09-25）

- Android 官方《Splash screens》说明 Android 12+ 的系统启动画面由启动主题控制，动画建议不超过 1000ms，且可用退出动画衔接应用内容：<https://developer.android.com/develop/ui/views/launch/splash-screen>
- Android 官方迁移指南明确警告，专用 Splash Activity 会在 Android 12+ 上造成双重启动画面：<https://developer.android.com/develop/ui/views/launch/splash-screen/migrate>
- 方案 A（不改）：动态别名在 Android 系统层生效，但 Projectivy 实机丢卡，不可发布。
- 方案 B（为每个主题建立独立启动 Activity）：能提前决定系统启动资源，但仍会改变桌面组件身份，且增加双重启动风险，否决。
- 方案 C（采用）：TV 稳定单一启动组件 + 应用内不阻塞主题过场；手机保留动态图标。兼容 Projectivy，启动过场总时长 910ms，不引入额外 Activity。
- 回滚：删除 `LauncherThemeTransition` 调用并恢复 TV 别名；偏好值无需迁移。

### 双品牌修正（2026-09-26）

- 实机观察到旧 `startup_logo.png` 的 WebHTV 品牌先于 InJoy 主题过场显示，根因是 legacy `windowBackground` 仍叠加历史全屏位图。
- Android 官方建议启动窗口使用单一不透明背景，并由系统 SplashScreen/应用首帧完成品牌衔接；也明确提醒旧自定义启动画面可能造成重复启动体验：<https://developer.android.com/develop/ui/views/launch/splash-screen>、<https://developer.android.com/develop/ui/views/launch/splash-screen/migrate>（访问于 2026-09-26）。
- 采用最窄修复：移除 legacy 启动窗口中的 WebHTV 位图，仅保留主题背景；继续复用已有非阻塞 InJoy 过场，不增加 Splash Activity。过场总时长 950ms，标志、字标、副标题统一淡入后退出。

## 验证

- `:app:assembleLeanbackArm64_v8aDebug`：通过。
- `:app:assembleMobileArm64_v8aDebug`：通过。
- APK 资源清单：应用标签为「影卓」，六个 Launcher 别名均打包，H3 默认启用。
- 本地 Debug APK 与电视当前公开 Beta 的签名不同，因此未卸载覆盖，避免清除电视端现有数据；真机覆盖验证应使用 CI 生成的正式签名 APK。

## 电视覆盖安装实测（2026-09-25）

- 使用私有 Actions 运行 `36113280379` 构建正式签名 Leanback ARM64 APK，不创建公开 Release。
- 新 APK 与电视现装版本证书 SHA-256 均为 `6e6f3fd89bdb3bc6f20ed911a1a61c6717aaf3e08d528b44fd3ff6e353ba0e19`。
- `adb install -r` 覆盖成功；UID、`dataDir` 与首次安装时间保持不变，首页历史数据仍在。
- H3 别名可以正常启动应用；设置页显示六个主题。切换到曜黑玻璃后系统唯一 Leanback 入口变为 `.launcher.Glass`，恢复 H3 后变回 `.launcher.H3`。
- **发布阻断项**：Projectivy Launcher 不能把 `activity-alias` 作为原应用卡片继续展示。强制停止桌面和完整重启电视后仍不出现影卓卡片；系统 `cmd package resolve-activity` 同时能正确解析 H3，证明问题位于第三方桌面对动态别名的兼容层。

在解决 Projectivy 固定卡片迁移前，不应把当前动态别名方案发布为公开 Beta。可选方向是 TV 固定使用稳定 H3 组件、仅手机动态换图标，或在主题切换后通过 Projectivy/Android 固定快捷方式接口重新固定当前主题卡片。
