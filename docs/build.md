# Build

This app keeps the Android build setup intentionally small because it is a single-module flashlight app built through GitHub Actions.

## Gradle

The project keeps the simple root `build.gradle` and `settings.gradle` layout because the repository has only one app module.

The root `build.gradle` defines the Android and Kotlin build plugins plus the shared repositories. The app module owns the Android app settings, version, dependencies, signing config, and release build type.

## Gradle Wrapper Strategy

The repository intentionally does not commit Gradle Wrapper files. The manual release workflow downloads the pinned Gradle `8.11.1` distribution and generates the wrapper inside the temporary GitHub Actions runner before building the app.

This keeps generated wrapper files out of the repository while preserving a deterministic release build. If this strategy changes, update this document, the workflow, and the documented toolchain together.

## Current toolchain

The repository currently pins:

- minimum SDK: `24`
- compile SDK: `36`
- target SDK: `36`
- Android Build Tools: `35.0.0`
- Android Gradle Plugin: `8.10.1`
- Gradle: `8.11.1`
- Kotlin: `2.2.21`
- JDK and Java/Kotlin bytecode target: `17`

Keep this section aligned with `app/build.gradle`, the root `build.gradle`, and `.github/workflows/android-release.yml` whenever the build toolchain changes.

## R8 and ProGuard

R8 and ProGuard rules are not currently needed because release minification and resource shrinking are disabled in `app/build.gradle`.

The app is small, has no reflection-heavy third-party SDKs, and does not need custom keep rules right now. Keeping an empty `proguard-rules.pro` file only adds confusion, so the file is intentionally absent.

If release minification is enabled later, add a new `app/proguard-rules.pro` file only when there are real keep rules to maintain.
