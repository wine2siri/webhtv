# T16 Android TV Banner 品牌统一

## 问题

Android TV 主屏仍显示白底旧立方体。普通启动图标虽然已经换成钛金属六边形、香槟金播放键和 `YZ` 字标，但 Leanback 清单使用的是独立的 `@mipmap/ic_banner`，旧资源从未同步更新。

## 实现

- Android 8.0 及以上的自适应 Banner 复用主图标深空背景，并使用同一套钛金属/香槟金 `YZ` 前景。
- 为 Android 7.x 增加非自适应 `mipmap-anydpi` Banner，避免低版本缺少对应资源或继续显示旧视觉。
- 不改应用包名、Activity、播放器或业务逻辑。

## 验收

- `assembleLeanbackArm64_v8aDebug` 构建通过。
- 发布后覆盖安装到电视，主屏 Banner 应从白底旧立方体变为深色钛金属/香槟金 `YZ` 品牌图；以电视截图作为最终视觉证据。
