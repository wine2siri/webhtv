# T9 网盘 Cookie 归一化

## 结论

采用“官方登录页 WebView → Moonbox Cookie Hub → 电视端 CookieManager”的单向同步链路。Cookie Hub 继续作为唯一服务端数据源，不新增第二份数据库。

## 依据与取舍

- Android `CookieManager.getCookie(url)` 返回可直接作为 HTTP Cookie 请求头使用的字符串，适合从官方登录会话提取凭据。
- 登录 WebView 仅允许 HTTPS 且限制在对应厂商域名；关闭文件与内容访问，避免登录态接触本地文件。
- 普通 Cookie Hub 状态接口继续脱敏；原始凭据仅由局域网专用、`Cache-Control: no-store` 的同步接口返回。
- 未采用剪贴板或手工粘贴：容易截断、泄露，也无法形成自动续期闭环。
- 未引入 WorkManager：当前应用已有常驻后台服务启动点，进程内每 6 小时同步足以覆盖电视使用场景，并避免新增依赖与系统任务。

## 验收标准

1. 手机增强设置可进入夸克、UC、百度官方登录页，无需复制 Cookie。
2. 点击“登录完成并同步”后，Cookie 上传到 Cookie Hub；日志和 UI 不显示原文。
3. 电视启动立即同步，此后每 6 小时同步；状态页只显示最近时间和数量。
4. 非局域网来源无法读取原始凭据，响应禁止缓存。
5. JVM 单测、Android lint/编译与 Moonbox 交付自检通过。

## 回滚

回滚 WebHTV 的 T9 恢复标签，并移除 Moonbox `/api/cookie_hub/tv-sync` 路由即可；既有 Cookie Hub 数据格式不变，无需迁移数据。

## 验证记录（2026-09-24）

- `:app:testMobileArm64_v8aDebugUnitTest --tests com.fongmi.android.tv.drive.DriveCookieSyncTest`：通过。
- `:app:compileLeanbackArm64_v8aDebugJavaWithJavac`：通过。
- `:app:assembleLeanbackArm64_v8aDebug`：通过，103 个任务完成。
- Moonbox：155 项 pytest 通过，`pre_delivery_check.py` 全绿；生产 `/api/cookie_hub/tv-sync` 返回 200 且带 `Cache-Control: no-store, max-age=0`。
