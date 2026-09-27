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
- Unverified runtime step: cleared-cache TV cold start after the signed Beta is installed.
- Next action: publish the signed Beta, install it on the TV, and verify a cleared-cache cold start.
