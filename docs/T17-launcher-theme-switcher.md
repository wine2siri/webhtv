# T17 桌面图标与 TV Banner 主题切换

## 结果

- 默认主题为 H3「曜石鎏金」：闭合圆环与播放三角均以同一中心点构造。
- 应用显示名统一为「影卓」。
- 支持六套主题：H3、深色胡桃木、曜黑玻璃、荷叶露珠、午夜大理石、月下水面。
- 手机端和 TV 端设置页均提供「桌面图标主题」入口。
- TV 端主题同时切换 Launcher 图标与 16:9 Banner；手机端切换 Launcher 图标。

## 实现

Android Manifest 为每套主题声明一个指向 `HomeActivity` 的 `activity-alias`。首次安装仅启用 H3；切换时先启用目标别名，再禁用其余别名，避免中途中断造成桌面入口全部消失。选择结果保存在 `launcher_theme` 偏好中。

自然材质只作为背景，圆环、播放符号与 Banner 字标均为确定性矢量，因此主题变化不会改变标志的几何中心。五张生成材质已标准化为 1280×720 WebP，合计约 0.7 MB。

部分第三方桌面会缓存图标或 Banner。切换完成后设置页会提示返回主页；仍未刷新时需重启桌面进程或设备。别名本身已携带独立的 `android:icon` 与 `android:banner`，不依赖应用级默认资源。

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
