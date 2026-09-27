# Navigation loader and updater hotfix

## Objective

Keep the InJoy navigation usable after a canceled first Python download, and ensure in-app update checks use only the `wine2siri/webhtv` release stream.

## Acceptance criteria

- A transient or interrupted Python spider construction is not cached for the process lifetime.
- A later request for the same site key can initialize and cache the real spider.
- Stable and beta release discovery, manifest lookup, and GitHub APK fallback target `wine2siri/webhtv`.
- A cleared-cache TV cold start loads the InJoy navigation without `Unable to download python script` leaving a poisoned site entry.

## Scope and rollback

- Scope: `PyLoader`, its focused regression test, and GitHub release endpoint ownership.
- Rollback: revert the atomic task commit and reinstall the preceding signed APK.

## Recovery anchor

- Branch: `codex/navigation-pdca`.
- Protected pre-existing paths: `.artifacts/`, `app/.cxx/`.
- Implemented: retryable Python spider initialization and fork-owned GitHub update endpoints.
- Verified: `:app:testLeanbackArm64_v8aDebugUnitTest --tests com.fongmi.android.tv.api.loader.PyLoaderTest` and `:app:compileLeanbackArm64_v8aReleaseJavaWithJavac` passed together on 2026-09-28 (112 tasks, 2m56s).
- Published: GitHub Actions run `36356903312` produced public pre-release `v5.6.7-beta-202609280657` from commit `6d0af0783c2ca0d3d68b13c752737133051e493c`; the Leanback ARM64 APK matched manifest SHA-256 `f03851422f910423567649d6b52cc8ddf9144ebcd23aa3c9c2e4e308b5d1d767`.
- TV verification: the signed APK installed successfully on `192.168.0.230:5555`. A canceled first Python download returned a one-shot failure, the same process retried instead of retaining `SpiderNull`, and the UI then displayed the InJoy navigation categories, recent-watch posters, episode labels, and update row without a fatal exception.
- Runtime caveat: Android release permissions do not allow ADB to delete only the private Python cache. The focused regression test covers the empty-cache transient-failure contract; device verification exercised the actual transient interruption and in-process recovery without clearing application data.
- Next action: keep this Beta as the rollback-tested navigation hotfix baseline.
