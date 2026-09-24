# T14 内容来源聚合

## 目标

在搜索/发现结果的“全部”标签中，将同一影片或剧集的多个站点结果聚合为一张卡片。卡片显示来源数量；确认多来源卡片时从右侧打开来源选择面板，选择后沿用原有详情/播放链路。单站标签与单来源卡片维持原行为。

## 用户约束

- 聚合维度是内容，不是服务器。
- UI 必须直观、简洁，避免连续重复卡片。
- “超速”不得参与搜索或搜索建议；它可作为已有内容的播放来源。

## 当前实现与根因

- `CollectActivity.setCollect()` 与 `CollectFragment.setCollect()` 把各站点返回的 `Vod` 直接追加到“全部”列表。
- `SearchAdapter` 逐条渲染 `Vod`，因此同名内容在秋月、皓月等来源各占一张卡片。
- 站点级筛选已通过 `Site.isSearchable()` 执行。当前 `homedia/api.json`、模板和设备配置快照均将超速设置为 `searchable: 0`、`quickSearch: 0`。

## 设计依据（2026-09-24）

- Android TV 官方导航指南：遥控器以 D-pad 和确认键为主；导航应高效、可预测、直观，并以最少页面/点击到达内容。来源：<https://developer.android.com/training/tv/get-started/navigation>
- Android TV 官方焦点指南：焦点是 TV 的关键交互状态，所有可操作元素需要清晰、连续的焦点路径。来源：<https://developer.android.com/design/ui/tv/guides/styles/focus-system>
- Android TV 官方布局指南：焦点路径应与信息层级和决策逻辑一致，避免复杂嵌套。来源：<https://developer.android.com/design/ui/tv/guides/styles/layouts>
- 项目已有 `QuickSearchDialog`/`EpisodeListDialog` 的右侧全高面板模式，可复用其窗口、焦点和返回键行为，不引入新的 UI 依赖。

## 方案比较

### A. 不改

- 优点：零风险。
- 缺点：同一内容连续重复，来源差异凌驾于内容浏览任务之上。

### B. 每个服务器聚合

- 优点：实现简单。
- 缺点：仍会出现相同内容的多张卡片，不能解决用户问题。

### C. 内容聚合 + 卡片内独立来源按钮

- 优点：来源入口可见。
- 缺点：一张 TV 卡片产生两个焦点目标，D-pad 路径复杂，小角标不适合作为主要遥控器目标。

### D. 内容聚合 + 卡片确认后右侧来源面板（采用）

- 单来源：确认后直接进入详情。
- 多来源：卡片站点徽标改为“N 个来源”，确认后打开右侧面板；面板首项自动聚焦，选择后进入原详情链路。
- 优点：列表保持一内容一卡片；卡片仍只有一个焦点；来源选择只在必要时出现。
- 代价：多来源内容比原来多一次确认键。

## 聚合规则

1. 标题使用 Unicode NFKC、转小写并移除空白与标点后比较。
2. 年份双方都有值且不同则不合并；一方缺年可与标题相同项合并。
3. 内容粗类型双方都能识别且电影/剧集冲突时不合并。
4. 同一 `siteKey + vodId` 不重复加入来源。
5. 保留搜索任务到达顺序；现有 `SiteHealthStore` 排序后的首个来源为默认/推荐来源。
6. 聚合只应用于“全部”标签，单站标签仍展示原始结果。

## 验收标准

- 三个站点返回同名、同年、同类型内容时，“全部”列表只有一张卡片，来源数为 3。
- 同名不同年或明确电影/剧集冲突时不合并。
- 重复的同站点同资源不会增加来源数。
- 单来源点击直接进入；多来源点击打开右侧来源面板，选择后使用该来源的 `siteKey/vodId`。
- 单站标签不聚合。
- 超速配置为不可搜索时，不会进入搜索任务、联想或结果聚合。
- 聚合单元测试、leanback 与 mobile Java 编译通过。

## 回滚

回退本任务提交/恢复标签即可。没有数据库迁移、配置格式变化或持久化数据变更。

## Recovery anchor

- Branch: `codex/content-source-aggregation`
- Base: `3e03f661bdf8d64d734c6963933da8cb9b60a97e`
- Status: implementation complete; focused unit tests plus leanback/mobile arm64 Debug Java compilation pass
- Files changed: `Vod` source grouping metadata/copy, `ContentSourceAggregator`, right-side `ContentSourceDialog`, leanback/mobile Collect and Search adapters, localized strings, focused unit tests
- T9 closure completed separately: GitHub prerelease `v5.6.0-beta-202609242125` and same-signature TV overwrite install succeeded
- Allowed paths: task guard `T14-CONTENT-SOURCE-AGGREGATION` scope
- Verification: `:app:testLeanbackArm64_v8aDebugUnitTest --tests com.fongmi.android.tv.search.ContentSourceAggregatorTest`, `:app:compileLeanbackArm64_v8aDebugJavaWithJavac`, and `:app:compileMobileArm64_v8aDebugJavaWithJavac` completed with `BUILD SUCCESSFUL` on 2026-09-24.
- SDK setup: user authorized Google Android SDK licenses; SDK 37.0, Build Tools 37/36, Platform Tools, NDK 29, and CMake 4.1.2 are installed under `H:\OKYS\.android-sdk`.
- Next action: finish the task guard to create the scoped commit and annotated recovery tag; push only after explicit authorization.
