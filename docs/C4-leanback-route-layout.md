# C4：Leanback 线路切换与布局冲突修复

## 决策与边界

- 状态：用户于 2026-09-28 确认实施；本阶段只处理 Leanback 线路选择，不合入 E11 音频内核变更。
- 分支：`codex/c4-leanback-route-layout`。
- 本地基线：`c9db6898f94d146fb87d0a981ab63023b5109cce`。
- 上游依据：`fa09d226f9f3763bc0f50167d6485178438fad52`（完整提交 ID）。
- 影响范围：Leanback `VideoActivity`、独立线路选择监听器、对应 Robolectric 测试，以及测试专用依赖配置。
- 不影响：Mobile UI、播放器内核、解码、协议、ABI、原生库、生产 APK 依赖与包体。

## 问题与本地调用链

Leanback 的 `onChildViewHolderSelected` 允许在布局尚未完成时触发。本地旧补丁仅用一次 `View.post` 延迟 `onItemClick(Flag)`；如果布局跨帧仍在进行，或者期间线路列表/adapter 已替换，回调仍可能操作失效对象。

完整线路切换会间接触发 `FlagAdapter.setNextFocusDown()` 与 `FlagAdapter.toggle()`，二者都会调用 `notifyDataSetChanged()`。RecyclerView 在 `isComputingLayout()` 为 true 时禁止修改 adapter，并抛出 `Cannot call this method while RecyclerView is computing a layout or scrolling`。

## 证据与资料（访问日期：2026-09-28）

| 证据 | 结论 | 用途 |
| --- | --- | --- |
| [RecyclerView.isComputingLayout](https://developer.android.com/reference/androidx/recyclerview/widget/RecyclerView#isComputingLayout()) | 布局锁定期间更新 adapter 会抛异常；焦点回调中应延后变更 | 证明修复必要性与重试条件 |
| [OnChildViewHolderSelectedListener](https://developer.android.com/reference/androidx/leanback/widget/OnChildViewHolderSelectedListener) | 选择回调发生时位置尚未最终确定；`NO_POSITION` 合法 | 证明不能把一次选择回调直接当最终稳定状态 |
| [View.postOnAnimation](https://developer.android.com/reference/android/view/View#postOnAnimation(java.lang.Runnable)) | runnable 可安排到下一显示帧 | 用于布局仍在计算时逐帧重试 |
| [Robolectric Getting Started](https://robolectric.org/getting-started/) | Android 资源 JVM 测试需要 `includeAndroidResources` 和 Robolectric 测试依赖 | 证明测试配置；依赖仅进入测试 classpath |
| 上游提交 `fa09d226f9f3763bc0f50167d6485178438fad52` | 已覆盖布局临界区、快速切换、列表替换、adapter 替换和窗口退出 | 采用其经过验证的窄修复结构 |

论文、性能基准、安全通告、native/ABI 资料对本次纯 UI 时序缺陷不适用。Robolectric 只用于 JVM 测试，不进入运行时 APK，因此无需包体或原生兼容评估。

## 方案比较

1. 不改：保留已知崩溃窗口，不能接受。
2. 保留单次 `post`：只能避开部分同帧布局，不能处理跨帧布局、快速选择和陈旧回调。
3. 适配上游监听器（采用）：把整个线路切换延后；布局未完成时按帧重试；只保留最新选择；执行前验证视图附着、adapter 身份、当前位置和对象身份；页面退出时拒绝点击。

本地 `VideoActivity.onItemClick(Flag)` 还包含追更/心愿动作弹窗，因此没有整段覆盖上游方法，只加入生命周期保护并保留本地行为。

## 验收标准

- 真实 Leanback 布局回调发生在 `isComputingLayout=true` 时，线路切换必须等布局结束再执行。
- `setNextFocusDown` 与 `toggle` 不在布局临界区执行。
- 快速连续选择只执行最后一次。
- 无效位置、列表对象替换、adapter 替换、视图离开窗口均丢弃旧选择。
- Leanback ARM64 Release Java 编译通过。
- 不修改或清理既有 `.artifacts/`、`app/.cxx/`。

## 验证结果

2026-09-28 验证完成：

- `:app:testLeanbackArm64_v8aDebugUnitTest --tests com.fongmi.android.tv.ui.custom.FlagSelectionListenerTest`：8 项，0 失败、0 错误、0 跳过，测试用例耗时 14.437 秒。
- 同一 Gradle 运行中的 `:app:compileLeanbackArm64_v8aReleaseJavaWithJavac` 成功；总计 123 个任务，15 个执行、108 个已是最新，`BUILD SUCCESSFUL in 1m 45s`。
- 测试先用真实 RecyclerView adapter observer 复现布局临界区的异常，再确认完整线路切换等布局结束后才执行。
- 覆盖快速连续选择、无效位置、同名新对象替换、adapter 替换、页面离开窗口等陈旧回调场景。
- `git diff --check` 通过，仅有 Windows 工作区既有的 LF/CRLF 转换提示。
- 未触碰受保护的 `.artifacts/` 与 `app/.cxx/`。

## 回滚与恢复锚点

- 回滚粒度：C4 原子提交整体 revert。
- 回滚基线：`c9db6898f94d146fb87d0a981ab63023b5109cce`。
- 恢复标签前缀：`recovery/C4-leanback-route-layout/`。
- 实机边界：自动测试覆盖 Android 9/Leanback 布局时序；受影响电视上的多线路端到端播放仍需设备复测。
