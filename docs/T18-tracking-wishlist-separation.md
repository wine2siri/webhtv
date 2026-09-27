# T18 追更与心愿管理解耦

## 目标与范围

完成句：电视与手机端把“追更/置顶/剧集展示顺序”放入独立原生菜单，心愿菜单只保留资源订阅、重搜、取消订阅和删除文件。

范围仅限：

- `app/src/main/java/com/fongmi/android/tv/ui/dialog/TrackingActionDialog.java`
- `app/src/main/java/com/fongmi/android/tv/ui/dialog/WishlistActionDialog.java`
- mobile/leanback 的 `TypeFragment.java` 与 `VideoActivity.java` 接线

仓库已有 `.artifacts/`、`app/.cxx/` 不属于本任务，必须保持不动。

## 设计评审

问题根因不是弹窗文案，而是两个生命周期被放进同一菜单：心愿管理回答“是否获取/删除资源”，追更回答“是否关注内容及如何看剧集”。取消资源订阅不应清除追更和播放偏好。

对比方案：

1. 不改：改动最小，但职责继续混杂，无法表达“追更但不订阅”。不采用。
2. 在原心愿弹窗继续追加追更项：兼容简单，但仍共享一个动作空间，用户容易把取消追更误解为取消订阅。不采用。
3. 新增窄化的原生追更弹窗，并复用现有 actionContent 回调链：不新增依赖、不改变播放器核心，只增加一种动作前缀；mobile/leanback 行为一致。采用。

本任务没有上游提交、协议、二进制、播放器依赖或性能算法变更，因此上游源码/PR/论文/基准证据不适用。可复用的成熟本地模式是已有 `WishlistActionDialog`、`ChoiceDialog` 与四处 action 拦截链；本地代码是决定性证据。安全边界由后端确认操作语义，客户端只展示选择与二次确认。

## 验收标准

- `injoy-follow:menu:` 在分类卡片和详情播放源入口均弹出追更菜单。
- 追更菜单支持加入/取消追更、置顶/取消置顶、顺序/倒序展示。
- 明确提示倒序只改变展示，自动连播始终按集数递增。
- 取消追更明确说明不取消订阅、不删除文件，并有二次确认。
- 心愿菜单不再出现剧集顺序。
- mobile 与 leanback Java 编译同时通过。

## 回滚

回滚本任务提交即可恢复旧心愿弹窗；后端继续兼容旧 `episode_order`，无需回滚用户数据。

## Recovery anchor

- 分支：`codex/navigation-pdca`
- 当前状态：实现与静态检查完成，task guard 已接管；本机已有 JDK 21，但没有 Android SDK，Gradle 在项目配置阶段明确停止，尚未进入源码编译。
- 已验证：后端/导航 219 项测试通过；仓库 `git diff --check` 通过；Gradle 能启动并定位 JDK，唯一阻塞为 `SDK location not found`。
- 未验证：Android 双端 Java 编译，提交推送后由 Android Release 工作流完成。
- 下一步：task guard 提交并创建 recovery tag，推送后观察 Android Release 编译结果。
