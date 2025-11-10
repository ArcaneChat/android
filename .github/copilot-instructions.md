# GitHub Copilot Instructions for ArcaneChat Android

## Project Overview

ArcaneChat is a Delta Chat Android client built on top of the official Delta Chat client with several improvements. It is a messenger app that uses email infrastructure for secure communication.

**Technology Stack:**
- **Language:** Java (Java 8 compatibility)
- **Build System:** Gradle with Android Gradle Plugin 8.11.1
- **Min SDK:** 21 (Android 5.0)
- **Target SDK:** 35 (Android 15)
- **NDK Version:** 27.0.12077973
- **Native Components:** Rust (deltachat-core-rust submodule)
- **UI Framework:** Android SDK, Material Design Components
- **Testing:** JUnit 4, Espresso, Mockito, PowerMock, AssertJ

## Repository Structure

- `src/main/` - Main application source code
- `src/androidTest/` - Instrumented tests (UI tests, benchmarks)
- `src/gplay/` - Google Play flavor-specific code
- `src/foss/` - F-Droid/FOSS flavor-specific code
- `jni/deltachat-core-rust/` - Native Rust core library (submodule)
- `scripts/` - Build and helper scripts
- `docs/` - Documentation
- `fastlane/` - App store metadata and screenshots

## Build Instructions

### Prerequisites

1. **Initialize submodules:**
   ```bash
   git submodule update --init --recursive
   ```

2. **Build native libraries:**
   ```bash
   scripts/ndk-make.sh
   ```
   Note: First run may take significant time as it builds for all architectures (armeabi-v7a, arm64-v8a, x86, x86_64)

3. **Build APK:**
   ```bash
   ./gradlew assembleDebug
   ```

### Build Flavors

- **gplay:** Google Play version with Firebase Cloud Messaging (applicationId: `com.github.arcanechat`)
- **foss:** F-Droid version without proprietary services (applicationId: `chat.delta.lite`)

### Build Outputs

- Debug APKs: `build/outputs/apk/gplay/debug/` and `build/outputs/apk/fat/debug/`
- Release APKs require signing configuration in `~/.gradle/gradle.properties`

## Testing

### Running Unit Tests

```bash
./gradlew test
```

### Running Instrumented Tests

1. **Disable animations** on your device/emulator:
   - Developer Options → Set "Window animation scale", "Transition animation scale", and "Animator duration scale" to 0x

2. **Run tests:**
   ```bash
   ./gradlew connectedAndroidTest
   ```

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

### Working with Translations

- Translations managed via Transifex (not in repository)
- English source strings: `res/values/strings.xml`
- Don't mix string changes with refactoring

### Debugging Native Code

Decode crash symbols:
```bash
$ANDROID_NDK_ROOT/ndk-stack --sym obj/local/armeabi-v7a --dump crash.txt > decoded.txt
```

## WebXDC Support

ArcaneChat has extended WebXDC support:
- `window.webxdc.arcanechat` - Version detection
- `sendToChat()` - Extra properties: `subject`, `html`, `type` (sticker/image/audio/video/file)
- External link support in apps
- `manifest.toml` - `orientation` field for landscape mode

## Important Files

- `build.gradle` - Main build configuration
- `CONTRIBUTING.md` - Contribution guidelines
- `BUILDING.md` - Detailed build setup
- `RELEASE.md` - Release process
- `proguard-rules.pro` - ProGuard configuration
- `google-services.json` - Firebase configuration (gplay flavor)

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
