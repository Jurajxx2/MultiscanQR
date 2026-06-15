# Multiscan QR

A QR-code scanner for Android and iOS, built with **Kotlin Multiplatform** and
**Compose Multiplatform**. The UI, state management (MVI), domain logic and
persistence (SQLDelight) are shared; the camera, QR decoding, permissions and
SQLite drivers live behind `expect`/`actual` seams implemented natively on each
platform.

| | Android | iOS |
|---|---|---|
| UI | Compose Multiplatform (shared) | Compose Multiplatform (shared) |
| Camera preview | CameraX `PreviewView` via `AndroidView` | `AVCaptureVideoPreviewLayer` via `UIKitView` |
| QR decoding | ML Kit barcode scanning (on-device, bundled model) | `AVCaptureMetadataOutput` (built into the OS) |
| Permissions | Activity Result API + ON_RESUME re-check | `AVCaptureDevice.requestAccess` |
| Persistence | SQLDelight `AndroidSqliteDriver` | SQLDelight `NativeSqliteDriver` |

## Building and running

Requirements: JDK 21 for Gradle/Android builds, Android Studio (or the Android
SDK + an emulator/device), and for iOS a Mac with Xcode 16+.

### Android

```bash
./gradlew :androidApp:assembleDebug
# or install directly on a connected device/emulator:
./gradlew :androidApp:installDebug
```

If Gradle does not pick up JDK 21 automatically, export `JAVA_HOME` first:

```bash
export JAVA_HOME=/path/to/jdk-21
```

Or open the project in Android Studio, set the Gradle JDK to JDK 21, and run
the `androidApp` configuration. The project still targets Java 11 bytecode;
JDK 21 is only the Gradle launcher/runtime.

### iOS

```bash
open iosApp/iosApp.xcodeproj
```

Select the `iosApp` scheme and a simulator (or a device — set your `TEAM_ID` in
`iosApp/Configuration/Config.xcconfig` for signing), then Run. The Xcode build
invokes Gradle to compile and embed the shared framework, so the first build
takes a few minutes.

> Simulators have no camera hardware: on the simulator you can test the
> permission flow, history and navigation, but the preview stays empty. Use a
> physical device to scan.

### Tests

```bash
./gradlew :shared:testAndroidHostTest      # shared tests on the JVM
./gradlew :shared:iosSimulatorArm64Test      # same tests, Kotlin/Native on a simulator
```

Covered: QR content classification, the scanner's MVI state machine
(detection → pause → debounce → resume) against a fake repository, and the
relative-time formatter.

## Architecture

```
shared/src/commonMain          ── everything that has no platform reason to differ
 ├─ scanner/, history/         MVI features: UiState + Event + Effect + ViewModel + Screen
 ├─ domain/                    ScanResult, content classifier (pure, unit-tested)
 ├─ data/                      ScanHistoryRepository (SQLDelight), DatabaseDriverFactory (expect)
 ├─ camera/                    CameraPreview + permission controller (expect)
 └─ di/                        Koin module — small app composition root

shared/src/androidMain         ── actuals: CameraX + ML Kit, ActivityResult permissions,
                                  AndroidSqliteDriver, system clipboard
shared/src/iosMain             ── actuals: AVFoundation capture + decode, AVCaptureDevice
                                  permissions, NativeSqliteDriver, UIPasteboard
androidApp/                    ── Android entry point (Application-scoped Koin, MainActivity)
iosApp/                        ── iOS entry point (SwiftUI host wrapping ComposeUIViewController)
```

### The platform seam

The contract common code owns is deliberately small:

```kotlin
@Composable
expect fun CameraPreview(isScanning: Boolean, onQrDetected: (String) -> Unit, modifier: Modifier)

@Composable
expect fun rememberCameraPermissionController(): CameraPermissionController

expect class DatabaseDriverFactory { fun createDriver(): SqlDriver }
```

Decoding intentionally happens **on the platform side** of the seam and the
contract traffics only in decoded strings. Android decodes `ImageAnalysis`
frames with ML Kit; iOS doesn't need any decoder dependency because
`AVCaptureMetadataOutput` decodes QR codes in the OS. Pushing raw frames
through the seam to decode in common code would have meant copying pixel
buffers across the interop boundary for no benefit.

No turnkey cross-platform scanner library is used — CameraX, ML Kit and
AVFoundation are each platform's native tooling.

### MVI

Each screen is one immutable `UiState` (single `StateFlow`), a sealed set of
`Event`s flowing in, and one-shot `Effect`s (navigation/clipboard/URL opening)
delivered through a `Channel` — exactly-once, buffered, so effects can't be
dropped or replayed on configuration change. Screens are split into a stateful
wrapper (wires the ViewModel) and a stateless content composable that is a pure
function of state. The camera is injected into the content as a slot so the
screen renders in previews/tests without hardware.

ViewModels are the multiplatform `androidx.lifecycle.ViewModel`, scoped to the
Activity on Android (they survive rotation) and to the `ComposeUIViewController`
on iOS.

Koin is used narrowly as the composition root: it creates the SQLDelight-backed
repository and the two ViewModels, while platform behavior stays behind
`expect`/`actual` contracts. Navigation is the standard Compose Multiplatform
`navigation-compose` API with a tiny two-route graph (`scanner`, `history`) and
no nested graphs, arguments or deep links.
