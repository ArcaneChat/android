# GitHub Copilot Instructions for ArcaneChat Android

## Project Overview

ArcaneChat is a Delta Chat Android client built on top of the official Delta Chat client with several improvements. It is a messenger app that uses email infrastructure for secure communication.

**Technology Stack:**
- **Language:** Java (Java 8 compatibility)
- **Build System:** Gradle with Android Gradle Plugin 8.11.1
- **Min SDK:** 21 (Android 5.0)
- **Target SDK:** 36 (Android 15)
- **NDK Version:** 27.0.12077973
- **Native Components:** Rust (deltachat-core-rust submodule)
- **UI Framework:** Android SDK, Material Design Components
- **Testing:** JUnit 4, Espresso, Mockito, PowerMock, AssertJ

## Repository Structure

- `src/main/` - Main application source code
  - `src/main/java/org/thoughtcrime/securesms/` - Main UI components
  - `src/main/java/com/b44t/messenger/` - Delta Chat core integration
  - `src/main/java/chat/delta/rpc/` - JSON-RPC bindings (generated, don't edit manually)
  - `src/main/res/` - Android resources (layouts, strings, drawables)
- `src/androidTest/` - Instrumented tests (UI tests, benchmarks)
  - `src/androidTest/java/com/b44t/messenger/uitests/` - UI tests
  - `src/androidTest/java/com/b44t/messenger/uibenchmarks/` - Performance benchmarks
- `src/gplay/` - Google Play flavor-specific code
- `src/foss/` - F-Droid/FOSS flavor-specific code
- `jni/deltachat-core-rust/` - Native Rust core library (submodule, **don't edit directly**)
- `scripts/` - Build and helper scripts
  - `scripts/ndk-make.sh` - Build native libraries
  - `scripts/install-toolchains.sh` - Install Rust cross-compilation toolchains
  - `scripts/generate-rpc-bindings.sh` - Generate JSON-RPC bindings
- `docs/` - Documentation
- `fastlane/` - App store metadata and screenshots
- `.github/workflows/` - CI/CD workflows (GitHub Actions)

## Build Instructions

### Prerequisites

1. **Initialize submodules:**
   ```bash
   git submodule update --init --recursive
   ```
   This MUST be done first before any build attempts.

2. **Set up environment variables:**
   ```bash
   export ANDROID_NDK_ROOT=/path/to/ndk/27.0.12077973
   export PATH=${PATH}:${ANDROID_NDK_ROOT}/toolchains/llvm/prebuilt/linux-x86_64/bin/:${ANDROID_NDK_ROOT}
   ```
   Note: Path format varies by OS (linux-x86_64, darwin-x86_64, etc.)

3. **Install Rust toolchains:**
   ```bash
   scripts/install-toolchains.sh
   ```
   Required for building the native Rust components.

4. **Build native libraries:**
   ```bash
   scripts/ndk-make.sh
   ```
   **IMPORTANT:** First run takes 30-60 minutes as it builds for all architectures (armeabi-v7a, arm64-v8a, x86, x86_64).
   For faster development builds, build for a single architecture:
   ```bash
   scripts/ndk-make.sh armeabi-v7a
   ```

5. **Build APK:**
   ```bash
   ./gradlew assembleDebug
   ```
   Build time: ~2-5 minutes after native libraries are built.

### Build Flavors

- **gplay:** Google Play version with Firebase Cloud Messaging (applicationId: `com.github.arcanechat`)
- **foss:** F-Droid version without proprietary services (applicationId: `chat.delta.lite`)

### Build Outputs

- Debug APKs: `build/outputs/apk/gplay/debug/` and `build/outputs/apk/fat/debug/`
- Release APKs require signing configuration in `~/.gradle/gradle.properties`

### Common Build Issues

1. **Missing NDK or incorrect version:**
   - Error: `ANDROID_NDK_ROOT not set` or native library missing
   - Solution: Install NDK 27.0.12077973 and set ANDROID_NDK_ROOT environment variable

2. **Submodules not initialized:**
   - Error: Missing deltachat-core-rust files
   - Solution: Run `git submodule update --init --recursive`

3. **Gradle wrapper validation:**
   - Always validate gradle wrapper before building: `./gradlew wrapper --gradle-version=current`
   - Wrapper is validated in CI via `gradle/actions/wrapper-validation@v4`

4. **Clean build issues:**
   - If build fails, try: `./gradlew clean && scripts/ndk-make.sh && ./gradlew assembleDebug`
   - Remove `build/` directory if clean doesn't work

## Testing

### Running Unit Tests

```bash
./gradlew test
```
Expected duration: 1-3 minutes

### Running Instrumented Tests

1. **Disable animations** on your device/emulator:
   - Developer Options → Set "Window animation scale", "Transition animation scale", and "Animator duration scale" to 0x
   - **CRITICAL:** Tests will fail if animations are enabled

2. **Run tests:**
   ```bash
   ./gradlew connectedAndroidTest
   ```
   Expected duration: 10-30 minutes depending on device/emulator

### Online Tests

Some tests require real email credentials. Configure in `~/.gradle/gradle.properties`:
```properties
TEST_ADDR=youraccount@yourdomain.org
TEST_MAIL_PW=yourpassword
```

### UI Tests and Benchmarks

- Located in `src/androidTest/java/com/b44t/messenger/`
- Test categories: `uitests/online/`, `uitests/offline/`, `uibenchmarks/`
- Run via Android Studio: Run → Edit Configurations → Android Instrumented Test

## Coding Conventions

### General Guidelines

1. **Embrace existing style:** Match the coding style of the file you're editing
2. **Minimize changes:** Don't refactor or rename in the same PR as bug fixes/features
3. **Readable over paradigmatic:** Favor readability over strict Java patterns
4. **Avoid premature optimization:** Keep things simple and on point
5. **No excessive abstraction:** Avoid unnecessary factories, one-liner functions, or abstraction layers
6. **Comments:** Only add comments if they match existing style or explain complex logic

### Architecture

- **UI/Model Separation:** Delta Chat Core (Rust) handles the model layer
- **High-level interface:** Core provides data in UI-ready form; avoid additional transformations in UI layer
- **Direct approach:** Prefer direct implementation over excessive class hierarchies

### Key Principles

- Work hard to avoid options and up-front choices
- Avoid speaking about keys/encryption in primary UI
- App must work offline and with poor network
- Users don't read much text
- Consistency matters
- Primary UI should only show highly useful features

## Common Development Tasks

### Adding New Features

1. Consider the UX philosophy (minimal options, offline-first, simplicity)
2. Check if core library changes are needed before implementing in UI
3. Match existing code style in modified files
4. Add instrumented tests for UI changes when appropriate
5. Update relevant documentation

### Modifying Core Integration

- Core library is in `jni/deltachat-core-rust/` submodule
- Java bindings are in `src/main/java/com/b44t/messenger/Dc*.java`
- JSON-RPC bindings in `chat.delta.rpc.*` package (generated via dcrpcgen)

### Generating JSON-RPC Bindings

To regenerate JSON-RPC bindings after core changes:
```bash
./scripts/generate-rpc-bindings.sh
```
**Note:** Requires Rust tooling and [dcrpcgen tool](https://github.com/chatmail/dcrpcgen) installed

### Working with Translations

- Translations managed via Transifex (not in repository)
- English source strings: `res/values/strings.xml`
- Don't mix string changes with refactoring

### Debugging Native Code

Decode crash symbols:
```bash
$ANDROID_NDK_ROOT/ndk-stack --sym obj/local/armeabi-v7a --dump crash.txt > decoded.txt
```

## Validation and Quality Checks

### Pre-commit Checks

Before committing changes, always run:

1. **Gradle wrapper validation:**
   ```bash
   ./gradlew wrapper --gradle-version=current
   ```

2. **Build verification:**
   ```bash
   ./gradlew assembleDebug
   ```

3. **Unit tests:**
   ```bash
   ./gradlew test
   ```

4. **Code style:** Match existing code style in modified files (no automatic formatter configured)

### When to Rebuild Native Libraries

Rebuild native libraries (`scripts/ndk-make.sh`) when:
- Updating deltachat-core-rust submodule
- Modifying anything in `jni/` directory
- Changing NDK version
- After `git clean -fdx` or fresh clone

**DO NOT** rebuild native libraries for:
- Pure Java/Kotlin code changes
- Resource file changes
- Gradle configuration changes (unless changing native library linking)
- Documentation updates

## WebXDC Support

ArcaneChat has extended WebXDC support:
- `window.webxdc.arcanechat` - Version detection
- `sendToChat()` - Extra properties: `subject`, `html`, `type` (sticker/image/audio/video/file)
- External link support in apps
- `manifest.toml` - `orientation` field for landscape mode

## Important Files

- `build.gradle` - Main build configuration (Android Gradle Plugin 8.11.1, Java 8 compatibility)
- `CONTRIBUTING.md` - Contribution guidelines
- `BUILDING.md` - Detailed build setup instructions
- `RELEASE.md` - Release process
- `proguard-rules.pro` - ProGuard configuration (enabled for both debug and release)
- `google-services.json` - Firebase configuration (gplay flavor only)
- `settings.gradle` - Gradle settings
- `.github/workflows/` - CI/CD configuration

## Dependencies and Constraints

- **Java Version:** Java 8 compatibility (do not use Java 9+ features)
- **Gradle:** Use wrapper (`./gradlew`) to ensure correct Gradle version
- **NDK:** Must use version 27.0.12077973 (specified in build.gradle)
- **Min SDK:** 21 (Android 5.0) - code must be compatible
- **Target SDK:** 36 (Android 15) - test on this API level when possible
- **ProGuard:** Always enabled - ensure ProGuard rules are correct for new dependencies
- **Multi-dex:** Enabled - app exceeds 65k method limit

## Package Structure

- `org.thoughtcrime.securesms.*` - Main UI components (legacy namespace from Signal)
- `com.b44t.messenger.*` - Delta Chat core integration
- `chat.delta.rpc.*` - JSON-RPC bindings (generated)

## Notes for AI Assistants

- This is a fork of Delta Chat Android with ArcaneChat-specific improvements
- Maintain compatibility with Delta Chat core library
- Test on both gplay and foss flavors when making changes
- Native library must be rebuilt after core changes
- ProGuard is enabled in both debug and release builds
- Multi-dex is enabled due to app size

## CI/CD Workflows

### Preview APK Workflow (.github/workflows/preview-apk.yml)

Runs on every pull request to build and upload a preview APK:

1. **Setup steps:**
   - Checks out repository with submodules
   - Validates Fastlane metadata
   - Sets up Rust cache (working-directory: jni/deltachat-core-rust)
   - Sets up Java 17 (Temurin distribution)
   - Sets up Android SDK
   - Caches Gradle dependencies
   - Sets up NDK r27

2. **Build process:**
   ```bash
   scripts/install-toolchains.sh && scripts/ndk-make.sh armeabi-v7a
   ./gradlew --no-daemon -PABI_FILTER=armeabi-v7a assembleFossDebug
   ```
   Note: Builds only armeabi-v7a for faster CI builds

3. **Output:** Uploads APK artifact to GitHub Actions

### Important CI Considerations

- Always validate Gradle wrapper before committing changes
- Fastlane metadata must be valid (validated in CI)
- Use `--no-daemon` flag for Gradle in CI environments
- CI builds use FOSS flavor to avoid Google Services dependencies
- Expected CI build time: 15-25 minutes for full workflow
