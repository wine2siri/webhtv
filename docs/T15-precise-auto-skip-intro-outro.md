# T15 精准自动跳片头片尾：评估、接口与验收方案

## Recovery anchor

- 目标：在保留 Exo、MPV、IJK 三个现有内核和现有手工片头/片尾能力的前提下，设计由 Moonbox 聚合片段数据、WebHTV 在公共播放控制层执行的精准自动跳过能力。
- 权限：**assessment-only**。用户尚未批准实施；不得修改 `app/**`、`third_party/**`、锁文件、补丁、AAR、native 库或运行时行为。
- 工作区：独立 Worktree `H:\OKYS-worktrees\webhtv-t15`；分支 `codex/t15-auto-skip-assessment`；基线 `3fa46da8141bd62e5cbfcc0eb8dcabd77f8dab97`；评估开始时无脏文件。
- 文档所有权：本文件是 T15 唯一任务文档；总索引仅登记状态和链接。
- 当前结论：建议按“Moonbox 统一片段聚合与缓存 -> WebHTV 公共层验证/执行 -> 三内核无感复用”实施；第一阶段只允许高置信度、时间轴一致的 `intro`/`outro` 自动跳，其他结果仅提示或忽略。
- 未授权：Moonbox/WebHTV 生产代码、数据库迁移、播放器行为、第三方写入/投票、后台批量扫描、发布与推送。
- 唯一下一动作：用户审阅本方案并明确批准、修改或暂缓 T15；未批准前停止。

## 1. 决策包

- 任务编号和名称：`T15` 精准自动跳片头片尾。
- 所属分类：通用/App（公共播放策略），实施顺序 `Moonbox contract -> WebHTV common -> Exo/MPV/IJK 回归`；不属于任何单一内核升级。
- 要实现的实际能力：依据具体作品、季、集和当前文件时间轴取得片头/片尾区间；仅在数据足够可靠时自动 seek，失败时静默保留原播放，并允许用户继续用现有手工功能。
- 当前项目已有实现：`History.opening` 表示开场跳转目标毫秒数，`History.ending` 表示距媒体尾部的毫秒数；Mobile/Leanback 已在起播与进度回调中执行，且三内核共享上层控制逻辑。当前没有远端片段查询、片段来源/置信度、具体版本校验或自动策略。
- 涉及仓库及完整 commit ID：WebHTV `3fa46da8141bd62e5cbfcc0eb8dcabd77f8dab97`；AniSkip API `02bdc469c5acdd4c517244a22c49e9a0b333c6fa`；TheIntroDB TypeScript client `c2862cc71625faa3e17488f8fb457c8a5b351fe7`；Jellyfin server `208c278b75abd897aefa1e1175126eac5e4dbfaa`；Jellyfin Intro Skipper `efef91c0d9e1a9ff52a833fe9d59ce3cee2ddab5`。这些外部提交仅作为接口/实现证据，不提议合入。
- 收益：一次实现覆盖三内核；Moonbox 统一处理凭据、限流、缓存和来源变更；WebHTV 不直接依赖多家第三方；高置信度命中可自动跳，缺失或不匹配时不影响播放。
- 缺点与风险：社区时间戳可能缺失、错误或对应不同片源；季/集映射特别是动画分 cours 时可能错位；远端服务会限流/变更/不可用；片尾可能含彩蛋；未经验证的比例缩放会误跳正文。
- 与现有功能的关系：手工 `opening/ending` 保留且优先级最高；不删除按钮、不改历史数据含义；自动片段作为独立会话态，不把第三方结果覆盖进手工字段。
- 建议：**有条件实施**。先落地只读聚合接口和公共策略；默认仅高置信度自动跳片头，片尾先“提示/自动下一集可配置”，待真机误跳率达标再扩大默认自动化。
- 最小实施步骤：Moonbox 只读聚合/缓存 -> WebHTV DTO 与判定器单测 -> Mobile/Leanback 公共执行器 -> 三内核回归 -> 小流量开关；无 native/AAR/锁变更。
- 预计需要的验证：接口契约、来源故障/限流、身份与时长门槛、手工优先级、seek 幂等、续播/切源/切集/暂停、彩蛋片尾、三内核与双 UI、离线/超时，以及误跳/漏跳样本统计。

## 2. 权限、范围与不变量

### 2.1 本轮允许与禁止

本轮只修改本文件和 `docs/upstream-player-dependency-merge-assessment-2026-08-20.md`。以下均是后续实施建议，不是已完成行为。

必须保留的合同：

1. Exo、MPV、IJK 的选择、解码、Surface、网络、字幕与音频路径不变。
2. Mobile 与 Leanback 的现有手工片头/片尾按钮、±1 秒调整、清除、历史同步和下一集行为不变。
3. 手工值不被远端数据覆盖；用户显式清除/禁用必须胜过自动结果。
4. Moonbox 或任一第三方不可用时，起播不得被阻塞，也不得改变媒体 URL、清单、时长或内核。
5. 光盘导航、直播、未知时长、音频播放和无法可靠识别季集的内容默认不自动跳。

### 2.2 基线与本地可达性

| 项目 | 当前证据 | 结论 |
| --- | --- | --- |
| 手工数据 | `app/src/main/java/com/fongmi/android/tv/bean/History.java` 的 `opening`、`ending`，缺省 `C.TIME_UNSET` | 现有字段是用户历史合同，不应复用为第三方缓存 |
| 起播 | Mobile `VideoActivity.resolveInitialPlaybackPosition()`、Leanback `VideoActivity.setPosition()` 取 `max(opening, history.position)` | 片头跳转已位于公共控制层，但自动片段需防止把正常续播拉回或重复 seek |
| 片尾 | 两端 `onProgress` 在 `ending + position >= duration` 时进入 `checkEnded(false)` | 当前 `ending` 是“距尾部时长”，而第三方通常返回绝对区间，必须显式转换且保留彩蛋语义 |
| 手工编辑 | 两端 `setOpening` / `setEnding` 更新 History 并同步 | 手工覆盖应形成最高优先级，自动结果不得写回这两个字段 |
| 内核边界 | 上述逻辑调用公共 `player().seekTo()`/下一集控制 | 无需修改 Exo/MPV/IJK 内核即可覆盖三者 |
| Moonbox | 已有 TMDB、Emby 身份与 WebHTV 管理链路；未发现片段时间 API | Moonbox 适合做服务端适配/缓存，但需要新增版本化只读合同 |

## 3. 决策问题与结论

### 3.1 问题

**主假设**：Moonbox 聚合多来源并返回可审计的片段及匹配证据，WebHTV 只在当前媒体时间轴通过严格校验时执行，可在不改内核的情况下达到低误跳率。

**反假设**：作品/集级时间戳无法区分剪辑版本；即使来源返回成功，也不足以安全自动 seek，因此只能保留手工能力或本地逐文件检测。

**区分证据**：来源是否包含具体集身份、源媒体时长/文件身份、置信度与多片段语义；当前文件 duration 是否匹配；是否能在超时、切源与重复回调下做到幂等和无副作用。

### 3.2 结论

作品级社区时间戳可显著提高覆盖率，但单独不足以称为“精准”。推荐将结果分为三档：

- `exact`：媒体服务器/文件级标记，或 Moonbox 已绑定到具体 Emby item/media source 且时长一致；允许按用户开关自动执行。
- `verified`：TMDB/IMDb/MAL + S/E 精确，来源带置信度/投票，源时长与本地时长在严格阈值内；片头可自动，片尾默认提示。
- `suggested`：缺少源时长、仅模糊映射、需要明显比例缩放、来源冲突或单票；只显示“跳过”提示，不自动执行。

任何 `rejected`（身份冲突、越界、片段重叠、duration 不匹配、直播/光盘/未知时长）都视为无数据。**不得为了覆盖率把低置信度结果自动执行。**

## 4. 第三方数据源研究

访问日期均为 2026-09-25。网络请求通过正常 HTTPS 完成；未使用私有凭据。

### 4.1 数据源比较

| 来源 | 身份/单位 | 实测与能力 | 优点 | 局限与处置 | 建议 |
| --- | --- | --- | --- | --- | --- |
| TheIntroDB v3 | TMDB 或 IMDb；TV 带 S/E；毫秒数组 | `GET /v3/media?tmdb_id=1396&type=tv&season=1&episode=1` 实测 200，返回 intro `[228664,246143]` 与 credits `[3431000,null]`；响应头显示 30/10s、500/day/IP | 直接匹配 Moonbox 现有 TMDB；多片段；读无需用户 key | 社区数据、覆盖率和条款/长期可用性需持续核验；`null` 端点需明确语义 | 主社区源之一，经 Moonbox 缓存；不由 App 直连 |
| IntroDB (`introdb.app`) | IMDb；TV 带 S/E；秒和毫秒；带 confidence/submission_count | `GET /segments?imdb_id=tt0903747&season=1&episode=1` 实测 200，返回 outro 3431–3500 秒、confidence 1、submission_count 1 | 有置信度/票数、电影与 post-credits 类型；读无 key | 条款禁止批量下载/竞争服务；单票不可直接视为精确；依赖 IMDb 外部 ID | 辅助源；只按播放请求查询并缓存，不做数据库镜像 |
| AniSkip v2 | MAL ID + 绝对集号；秒；op/ed/recap | Steins;Gate MAL 9253 E1 实测 200；op 638.489–728.489、ed 1331.713–1421.713；各结果的 `episodeLength` 不同；限流头 120 | 动画覆盖和类型清晰；MIT API 源码 | Moonbox 当前以 TMDB 为主，TMDB→MAL/cour/绝对集映射复杂；不同贡献的时长可不一致 | 仅动画 fallback；映射不确定时降为 suggested |
| Jellyfin Media Segments | 具体服务器 item；ticks/片段类型 | 官方定义 Intro/Outro/Recap/Preview/Commercial，服务端 provider 生成，客户端决定动作 | 可绑定具体库存文件，最接近 exact；标准化片段类型 | 仅 Jellyfin 10.10+ 且需 provider/扫描；Moonbox 当前连接也可能是 Emby，不可假设均支持 | 若目标服务器支持，优先级最高 |
| Emby markers/chapters | 具体 Emby item/media source；chapter/marker | Emby 模型存在 IntroStart 等 marker 类型；当前 Moonbox 已有 Emby item 身份 | 可绑定当前库文件 | 版本/插件能力分散，必须运行时探测；普通固定间隔 chapter 不能冒充 intro | 能力探测后作为 exact；无类型章节点不自动用 |
| Jellyfin Intro Skipper | 服务端本地文件的音频指纹/静音/黑帧 | 项目在固定 Jellyfin/FFmpeg 环境分析本地文件，并明确可能误检/漏检 | 不依赖社区覆盖，可产生文件级片段 | 计算重、需 FFmpeg Chromaprint、服务端集成复杂；不应移植到 Android 播放线程 | 作为 Moonbox/媒体服务器后续离线 provider，不纳入 T15 v1 |
| Plex | 文件级 intro/credits marker | 官方支持本地分析与 credits 云哈希复用 | 产品证明“文件级分析 + 客户端动作”模式成熟 | Plex Pass/账号与 Plex 私有服务绑定；无适合 Moonbox 的开放公共按作品查询接口 | 仅作为架构佐证，不集成 |
| 内嵌章节 | 当前文件 chapter 名称 | Jellyfin 官方 Chapter Segments Provider 会把具名章节转为媒体片段 | 零外部网络、天然绑定文件 | 章节名语言/质量不统一；普通自动章节可能每 N 分钟生成 | 严格名称白名单可作 exact/verified 来源 |

### 4.2 证据等级

| 主张 | 来源 | 等级 | 对 WebHTV 的影响 |
| --- | --- | ---: | --- |
| Jellyfin 片段有 begin/end/type，客户端决定动作 | [Jellyfin Media segments](https://jellyfin.org/docs/general/server/metadata/media-segments/) | A（官方文档） | 支持 Moonbox 标准化 DTO、WebHTV 自主执行 |
| TheIntroDB 返回按 TMDB/IMDb + S/E 的毫秒数组，并允许多个同类区间 | [TheIntroDB client](https://github.com/TheIntroDB/theintrodb-npm) @ `c2862cc71625faa3e17488f8fb457c8a5b351fe7` 与本轮实测 | A/B（源码客户端 + 可复现请求） | 能直接利用 Moonbox TMDB 身份，但需处理 null/多段/限流 |
| IntroDB 读 API 无 key、限流，且禁止整库抓取/再分发 | [API 文档](https://introdb.app/docs/api)、[条款](https://introdb.app/docs/terms) 与本轮实测 | A（服务官方） | Moonbox 只能按需缓存，不能镜像全库；数据源条款是实施 gate |
| AniSkip 以 MAL+集号返回 op/ed/recap 秒区间 | [AniSkip OpenAPI](https://api.aniskip.com/api-docs)、[源码](https://github.com/aniskip/aniskip-api) @ `02bdc469c5acdd4c517244a22c49e9a0b333c6fa` 与本轮实测 | A | 适合动画 fallback，但映射层是主要风险 |
| 本地音频指纹能产生文件级 intro，但有计算/误检成本 | [Jellyfin Intro Skipper](https://github.com/intro-skipper/intro-skipper) @ `efef91c0d9e1a9ff52a833fe9d59ce3cee2ddab5` | B（成熟相关项目） | 应在服务端离线做，不进入三播放器内核或起播关键路径 |
| Plex 通过服务器分析产生 marker，客户端提供 Skip UI；credits 可用文件哈希云复用 | [Plex Credits Detection](https://support.plex.tv/articles/credits-detection/) | A（官方产品文档） | 佐证“服务端分析/聚合，客户端策略执行”边界 |

### 4.3 不适用证据类别

- 学术模型：存在 CLIP 等片头片尾检测研究，但 T15 v1 不训练或部署模型；其设备/数据集不能决定当前接口合同，故不作为采用依据。
- 上游播放器 commit/PR：T15 不改 Media3、mpv 或 IJK；没有要 cherry-pick 的播放器提交。相关外部仓库只作为数据/架构证据。
- 二进制/ABI：不改 AAR/native/锁，当前评估无 ABI 与制品重建范围。

## 5. 方案比较

| 方案 | 正确性 | 起播/资源 | 维护与隐私 | 结论 |
| --- | --- | --- | --- | --- |
| A. 不改，继续纯手工 | 用户设置后最可靠 | 零新增成本 | 无外部依赖 | 安全但不能满足自动化目标 |
| B. WebHTV 直连每个第三方 | 可快速试验；身份/冲突逻辑散落客户端 | 每设备重复请求，离线差 | API/限流/条款变化需发版；扩大网络面 | 拒绝作为正式架构 |
| C. Moonbox 聚合 + WebHTV 公共策略（推荐） | 可集中做来源优先级、缓存、审计；App 再校验当前 duration | 一次请求，有 stale cache；不阻塞起播 | 凭据不下发 App；可逐源熔断 | 推荐，最小且可逆 |
| D. Moonbox/Jellyfin 本地指纹检测 | 文件级最精确，覆盖无社区数据内容 | 扫描 CPU/IO 高、需 FFmpeg/Chromaprint | 运维复杂，需样本与任务队列 | 后续 Phase 3，不纳入 v1 |
| E. 修改三个内核分别识别/跳过 | 与内核时间轴耦合 | 重复开发、回归面最大 | 三套生命周期/seek 语义 | 拒绝；当前公共层已足够 |

## 6. 推荐架构与数据流

```text
WebHTV 播放身份
  (tmdb/imdb + S/E + media item/source + duration)
          |
          v
Moonbox GET /api/v1/playback/segments
  1. 手工/用户覆盖元数据（不覆盖 WebHTV History）
  2. 当前 Emby/Jellyfin 文件级 marker/具名 chapter
  3. TheIntroDB / IntroDB
  4. 动画专用 AniSkip fallback
  -> 归一化、冲突判定、缓存、审计、confidence/match
          |
          v
WebHTV SegmentPolicy（公共 App 层）
  身份 + duration + 状态 + 用户策略二次校验
          |
          +--> exact/verified: 一次性 seek/next
          +--> suggested: Skip 按钮提示
          +--> rejected/unavailable: 原样播放
          |
          v
Exo / MPV / IJK（保持不变）
```

## 7. Moonbox 接口设计

### 7.1 查询

`GET /api/v1/playback/segments`

必需参数：

- `media_type=tv|movie`
- `duration_ms`：播放器拿到可信 duration 后查询；未知时可预取，但只返回 suggested，不可自动执行。

身份参数至少满足一种：

- TV 首选：`tmdb_id + season + episode`
- 电影：`tmdb_id`
- 可补充：`imdb_id`、`mal_id`、`absolute_episode`
- 文件级优先：`emby_server_id + emby_item_id + media_source_id`

可选：`edition`、`release_group`、`file_size`、`media_fingerprint`。URL、cookie、播放 token、完整文件路径不得发送给第三方，日志必须脱敏。

### 7.2 成功响应（草案）

```json
{
  "schema": "moonbox.playback-segments.v1",
  "request_id": "uuid",
  "status": "matched",
  "identity": {
    "media_type": "tv",
    "tmdb_id": 1396,
    "imdb_id": "tt0903747",
    "season": 1,
    "episode": 1,
    "duration_ms": 3500000,
    "match": "tmdb-season-episode-duration"
  },
  "segments": [
    {
      "id": "stable-source-id",
      "kind": "intro",
      "start_ms": 228664,
      "end_ms": 246143,
      "source": "theintrodb",
      "source_revision": "remote-record-or-updated-at",
      "source_duration_ms": 3500000,
      "confidence": 0.96,
      "match_level": "verified",
      "action": "auto_skip_allowed"
    },
    {
      "id": "stable-source-id-2",
      "kind": "outro",
      "start_ms": 3431000,
      "end_ms": 3500000,
      "source": "introdb",
      "source_duration_ms": 3500000,
      "confidence": 0.70,
      "match_level": "suggested",
      "action": "prompt_only"
    }
  ],
  "cache": {"state": "fresh", "fetched_at": "RFC3339", "expires_at": "RFC3339"},
  "warnings": []
}
```

约束：

- `start_ms >= 0`，`end_ms > start_ms`；服务端先 clamp，但客户端仍验证。
- `credits/outro` 的远端 `end=null` 只能在本地 duration 已知时转换为 duration；不得用 0 表示结尾。
- 多段保留为数组；不得只取第一段后丢失 recap/post-credits 语义。
- Moonbox 给出 `action` 是建议，WebHTV 保留最终否决权。
- 不返回原始第三方 key、账号 token 或未脱敏 URL。

### 7.3 无命中与错误

- `200 status=no_match segments=[]`：有效身份但无数据；负缓存 6–24 小时。
- `200 status=ambiguous`：多个身份/来源冲突；只可提示，不自动。
- `400`：请求字段/范围错误；不重试。
- `401/403`：Moonbox 自身鉴权失败；不回退为 App 直连第三方。
- `429`：聚合接口限流；使用未过安全上限的 stale cache，否则原样播放。
- `502/503`：上游不可用；不得阻塞起播，指数退避。

### 7.4 来源归一化与选择

优先级：用户针对具体版本的 Moonbox 覆盖 > 文件级 Emby/Jellyfin typed segment/具名 chapter > 多来源一致 > 单一高置信社区源 > AniSkip 精确映射 > 模糊建议。

冲突规则：同类片段端点差异 `> 2 秒` 或超出预设容忍即标为 ambiguous，不做平均；不同来源不能简单投票掩盖版本差异。Moonbox 应保存原始来源、抓取时间、source duration、转换步骤和拒绝原因，便于诊断。

缓存键建议：

```text
file-level: provider + server + item + mediaSource + duration
episode-level: mediaType + tmdb/imdb/mal + season + episode/absoluteEpisode + roundedDuration
```

社区源正缓存建议 7 天、负缓存 12 小时、错误缓存 1–5 分钟；实际 TTL 可配置。仅播放按需查询，不预抓整库，遵守第三方条款。

## 8. WebHTV 接口与状态设计

### 8.1 独立模型，不污染 History

新增的建议模型应类似：

```text
PlaybackSegmentPlan
  generation / mediaIdentity / durationMs
  segments[] / matchLevel / source / expiresAt
  manualOpeningMs / manualEndingMs（只读快照）
  executedSegmentIds / rejectedReasons
```

`History.opening/ending` 继续只表示现有手工设置。自动结果只在当前播放 generation 内生效，可另做有 TTL 的应用缓存，但不得同步成手工值。

### 8.2 优先级与行为

1. 手工 opening > 自动 intro。手工 opening 存在时不执行任何自动 intro。
2. 手工 ending > 自动 outro。手工 ending 存在时保持现有下一集语义。
3. 正常续播位置若已在片段之后，不回跳；若落在片段内部，只允许前跳到其 end。
4. 每个 `generation + segment.id` 最多自动执行一次；用户主动 seek 回片段后不再次强跳。
5. 暂停时不自动 seek；恢复后仍在片段内再按策略处理。
6. 切源、切集、解析重试、内核切换时取消旧请求并递增 generation；迟到响应必须丢弃。
7. 直播、光盘菜单、未知 duration、媒体时长变化、无精确季集身份时拒绝自动。
8. 片尾含 post-credits 或来源只给“credits 到文件尾”时，默认显示按钮；只有明确 final outro 且用户开启自动下一集才执行。

### 8.3 精准门槛（实施前需用样本校准）

推荐初始 gate：

- 当前 duration 与 `source_duration_ms` 差值 `<= max(2000 ms, 0.2%)` 才可 `verified` 自动执行。
- 只知道作品/集、不知道 source duration：最高 `suggested`。
- 不自动做线性缩放。缩放可能修正帧率/片头 logo 差异，也可能把广告版/导演剪辑版错误映射；如未来启用，必须作为单独实验策略和验收阶段。
- intro 需位于前 50% 且长度在 5 秒–5 分钟；outro 需位于后 40% 且长度在 5 秒–20 分钟。边界只用于拒绝异常数据，不用来凭空生成片段。
- seek target 应留 `0–250 ms` 可配置安全余量并由真机校准；不得因关键帧 seek 提前落入正文前后造成循环触发。

这些数值是方案初值，不是已验证事实；实施时先用验收集冻结阈值，再写入代码。

### 8.4 开关与回滚

建议独立开关：`自动跳片头`、`片尾行为=提示/自动下一集/关闭`、`仅高置信度`。首次发布默认：片头仅 exact/verified 自动，片尾提示，suggested 提示；总开关可立即关闭且不需重建播放器。

回滚只需关闭 T15 总开关/移除 Moonbox endpoint 使用；三内核、History 字段和手工按钮无需回滚。网络请求失败自动等价于“关闭”。

## 9. 分阶段实施建议（待批准）

### Phase A：Moonbox 只读 contract 与适配器

- 新增 DTO、输入验证、缓存、来源适配器和诊断日志；先接 TheIntroDB/IntroDB，Jellyfin/Emby typed marker 能力探测。
- AniSkip 只有在已有可靠 MAL/绝对集映射后启用；否则返回 suggested 或 no_match。
- 不做提交/投票、不批量抓库、不改媒体服务器数据。
- 产出契约测试、录制的脱敏 fixtures、限流/超时/冲突测试。

### Phase B：WebHTV 公共判定器与提示

- 纯 Java 策略/DTO/缓存；公共层查询，不改内核。
- 先只显示可跳按钮与诊断来源，不自动 seek；Mobile/Leanback 一致。
- 保留手工按钮与优先级单测。

### Phase C：高置信自动片头

- exact/verified + duration gate + generation/idempotency 后开启。
- 分别回归 Exo/MPV/IJK 的起播、续播、seek、暂停、切源、切集和内核切换。
- 片尾仍提示；收集误跳/拒绝原因，不记录私有 URL。

### Phase D：片尾与本地检测（另行批准）

- 有彩蛋语义/多 credits 段后再决定自动下一集。
- 本地指纹检测只考虑 Moonbox/Jellyfin 离线任务，独立评估 CPU/IO、许可、样本、任务队列和存储；不放进 Android 播放线程。

## 10. 验收矩阵

### 10.1 接口与策略

| ID | 场景 | 输入/故障 | 期望 |
| --- | --- | --- | --- |
| A01 | 精确 TV 集 | TMDB+S/E+duration 命中 exact | 返回规范区间、来源、match、缓存信息 |
| A02 | 电影 | TMDB+duration 命中 outro/post-credits | 类型不混淆，post-credits 不被当 final outro |
| A03 | 无数据 | 上游 404/空数组 | 200 no_match；负缓存；播放不阻塞 |
| A04 | 限流 | 上游 429 | 使用安全 stale 或空；不让 App 改为直连 |
| A05 | 超时/坏 JSON | 连接超时、超大/畸形响应 | 有界超时/大小；记录来源错误；不影响其他源 |
| A06 | 来源冲突 | 同类端点差 >2s | ambiguous/prompt_only，不平均、不自动 |
| A07 | null 端点 | credits end=null | duration 已知才转换；未知时 prompt/reject |
| A08 | 多段 | 多 recap/credits/post-credits | 全部保留，稳定排序且不合并语义不同段 |
| A09 | 身份错位 | TMDB 对、S/E 错或 MAL cour 错 | 拒绝/降级 suggested；不得自动 |
| A10 | 条款/隐私 | 请求第三方 | 不含本地 URL/token/path；不批量镜像；日志脱敏 |

### 10.2 WebHTV 公共行为

| ID | 场景 | Exo | MPV | IJK | 通过标准 |
| --- | ---: | :---: | :---: | :---: | --- |
| W01 | 片头 exact，从 0 起播 | ✓ | ✓ | ✓ | 仅一次跳至 end；无重复/闪回 |
| W02 | 正常续播已越过片头 | ✓ | ✓ | ✓ | 不回跳、不重置 history |
| W03 | 续播落在片头内部 | ✓ | ✓ | ✓ | 一次前跳；位置单调 |
| W04 | 手工 opening 已设 | ✓ | ✓ | ✓ | 只执行手工合同；自动计划不覆盖 History |
| W05 | 手工 ending 已设 | ✓ | ✓ | ✓ | 保持现有 checkEnded/下一集行为 |
| W06 | suggested/时长不匹配 | ✓ | ✓ | ✓ | 只提示或无动作，绝不自动 seek |
| W07 | 暂停在片段内 | ✓ | ✓ | ✓ | 暂停期间无 seek；恢复按一次性策略 |
| W08 | 用户 seek 回片头 | ✓ | ✓ | ✓ | 已执行 segment 不二次强跳 |
| W09 | 切源/解析重试 | ✓ | ✓ | ✓ | 旧响应 generation 丢弃；新 duration 重判 |
| W10 | 切集/连播 | ✓ | ✓ | ✓ | 上集计划清空；新 S/E 查询；无串集 |
| W11 | 播放中切内核 | ✓ | ✓ | ✓ | 计划不重复执行，手工状态保留 |
| W12 | Moonbox 离线/慢 | ✓ | ✓ | ✓ | 首帧/控制不等待；超时后原样播放 |
| W13 | 未知时长/直播 | ✓ | ✓ | ✓ | 自动关闭；不猜测、不缩放 |
| W14 | Blu-ray/DVD 菜单 | n/a | ✓ | n/a | 自动关闭，不破坏菜单/标题时间轴 |
| W15 | 片尾含彩蛋 | ✓ | ✓ | ✓ | 只提示；不直接下一集，除非 final marker+用户配置 |
| W16 | Mobile/Leanback | ✓ | ✓ | ✓ | 文案、焦点、遥控/触控与开关一致 |

### 10.3 性能、可靠性和发布门槛

- Moonbox 查询不在首帧关键路径；WebHTV 可并行请求，迟到计划仅在仍匹配当前 generation 时采用。
- 建议服务端 p95（缓存命中）<100 ms，外部冷查有界超时每源 1.5–2.0 s、总预算 <=2.5 s；这些是实施目标，需测量后确认。
- App 请求/解析不得造成主线程网络或 ANR；计划/缓存应有条目与响应体上限。
- 冻结至少 30 集验收集：不同来源、动画/真人、片头在 0/中段、无片头、多 recap、多 credits、彩蛋、不同片源时长。高置信自动集合要求 **0 次误跳正文**；漏跳可接受并记录。出现一次误跳即停止扩大 rollout、降级为提示并复盘阈值/来源。
- 每个内核至少覆盖 Mobile ARM64 和 Leanback 目标 ABI 的公共 Java 编译/单测；本任务不要求 native 重建。真实设备需覆盖至少一个触屏设备和一个遥控设备。
- 观察指标：matched/no_match/ambiguous/rejected、来源、duration delta 桶、自动执行/用户撤销、请求耗时/缓存命中；不得上传标题外的私有播放 URL/token。

## 11. 风险、回滚与待用户决定

### 11.1 主要风险及缓解

1. **不同剪辑版本误跳**：严格 duration/file-level 身份；不默认缩放；不满足即提示。
2. **季集映射错误**：TMDB S/E 优先；AniSkip 需 MAL+cour+absolute episode 可证明映射。
3. **片尾彩蛋**：区分 outro/credits/post-credits/final；v1 不默认自动下一集。
4. **服务依赖**：Moonbox 适配、缓存、熔断；App 不直连 fallback；失败等价无数据。
5. **社区数据投毒/单票**：来源置信、提交数、多源一致与异常边界；用户可关闭/手工覆盖。
6. **手工能力回归**：自动计划独立存储；手工优先；验收矩阵 W04/W05 为阻断项。

### 11.2 回滚

- 运行时：远端总开关/本地开关关闭，立即停止新自动动作；当前播放已执行 seek 无法“反 seek”，因此必须在执行前保守判定。
- 代码：后续每个 Phase 独立提交；先撤 WebHTV 自动执行，再撤 DTO/查询，最后可撤 Moonbox endpoint/cache。History 与三个内核不在回滚集合。
- 数据：缓存可丢弃重建；不把自动结果写入手工 History，也不要求数据库不可逆迁移。

### 11.3 审批项

请用户选择：

1. 批准推荐的 Phase A+B（Moonbox 只读聚合 + WebHTV 提示/公共判定，不自动 seek），验收后再批 Phase C；或
2. 批准 A+B+C 一次实施，但仍按三次独立可回滚提交推进；或
3. 修改来源优先级、默认片尾策略、精准阈值后再评估；或
4. 暂缓/忽略 T15。

在收到明确批准前，本任务保持 `pending approval`，不修改播放器或 Moonbox 代码。

## 12. Checkpoint 1：2026-09-25 方案评估完成

- Completed：本地手工功能与三内核公共边界核对；Moonbox 身份能力核对；TheIntroDB、IntroDB、AniSkip 可复现 API 查询；Jellyfin/Plex/Intro Skipper 架构与条款研究；接口与验收矩阵完成。
- Source identities：WebHTV `3fa46da8141bd62e5cbfcc0eb8dcabd77f8dab97`；AniSkip API `02bdc469c5acdd4c517244a22c49e9a0b333c6fa`；TheIntroDB client `c2862cc71625faa3e17488f8fb457c8a5b351fe7`；Jellyfin `208c278b75abd897aefa1e1175126eac5e4dbfaa`；Intro Skipper `efef91c0d9e1a9ff52a833fe9d59ce3cee2ddab5`。
- Decisions/evidence：不改内核；Moonbox 聚合；手工优先；不默认比例缩放；片尾彩蛋保守；外部失败无副作用。
- Workspace：`codex/t15-auto-skip-assessment`，基线同上；guard `T15-precise-auto-skip-intro-outro` / `assessment`。
- Files changed：仅本文件和总评估索引（待校验）。
- Validation：`git diff --check` 通过；`verify_upstream_checkpoint.sh` 通过（0 error、0 warning，受保护依赖/二进制路径无变化）；task guard finish 待执行。无代码/构建/设备验证，因为尚未授权实施。
- Rollback anchor：基线 `3fa46da8141bd62e5cbfcc0eb8dcabd77f8dab97`；文档提交可单独 revert。
- Unresolved：用户审批范围；Moonbox 实际可用 Emby/Jellyfin marker 能力；发布前阈值样本校准；第三方条款的持续可用性。
- Next action：用户决定 11.3 的实施范围。
