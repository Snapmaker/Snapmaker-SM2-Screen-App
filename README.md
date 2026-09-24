# FabScreen

Android touchscreen control application for Snapmaker 3-in-1 3D printers (SM 2.0 series).

FabScreen provides a native Android interface to control 3D printing, laser engraving, and CNC machining workflows directly from the device touchscreen.

## Supported Devices

- **Snapmaker 2.0** (A350, A250) — 3D printing, laser engraving, CNC machining

## Features

- 3D printing, laser engraving, and CNC machining control
- Wi-Fi and USB file transfer
- Real-time machine status monitoring
- Firmware over-the-air (OTA) updates
- Laser camera and auto-focus support (via Bluetooth)
- Modular tool head detection and management
- Multi-language UI support
- Orca Slicer remote connect (HTTP API)

## Project Structure

```
fabscreen/                Main application module
fabscreen-updating/       OTA updater app
libraries/
  fabscreen-legacy/       Core library (business logic, serial protocol, HTTP server, UI widgets)
features/
  about/                  About screen feature module
buildSrc/                 Gradle dependency version catalog
```

## Dependencies

Essential third-party libraries:

- [ButterKnife](https://github.com/JakeWharton/butterknife) — view binding
- [RxJava](https://github.com/ReactiveX/RxJava) — reactive state management
- [RxAndroid](https://github.com/ReactiveX/RxAndroid) — Android main thread scheduler
- [Okio](https://github.com/square/okio) — byte array and I/O processing
- [AndServer](https://github.com/yanzhenjie/AndServer) — embedded HTTP server
- [Retrofit 2](https://github.com/square/retrofit) — HTTP API client
- [ARouter](https://github.com/alibaba/ARouter) — in-app routing
- [Firebase Crashlytics](https://firebase.google.com/products/crashlytics) — crash reporting
- [Glide](https://github.com/bumptech/glide) — image loading

## Prerequisites

- **Android Studio** 3.5+
- **JDK** 1.8
- **Android NDK** 22.1.7171670
- **Gradle** 5.4.1+ (wrapper included)

## Getting Started

### 1. Clone the repository

```bash
git clone https://github.com/Snapmaker/Snapmaker-SM2-Screen-App.git
cd Snapmaker-SM2-Screen-App
```

### 2. Configure Firebase (optional)

Firebase Crashlytics and Analytics are used by default. If you want to use Firebase services, copy the example template and fill in your own Firebase project credentials:

```bash
cp fabscreen/google-services.json.example fabscreen/google-services.json
```

If you do not need Firebase, remove the `com.google.gms.google-services` and `com.google.firebase.crashlytics` plugins from `fabscreen/build.gradle`, and remove the Firebase dependency entries.

### 3. Configure signing (optional)

Debug builds do not require signing configuration. For release builds, create a keystore and configure signing credentials via environment variables or `gradle.properties`:

```bash
export KEYSTORE_PATH=/path/to/your.keystore
export KEYSTORE_PASSWORD=your_store_password
export KEY_ALIAS=your_key_alias
export KEY_PASSWORD=your_key_password
```

Alternatively, add these to your `~/.gradle/gradle.properties` or project-level `local.properties`:

```properties
KEYSTORE_PATH=/path/to/your.keystore
KEYSTORE_PASSWORD=your_store_password
KEY_ALIAS=your_key_alias
KEY_PASSWORD=your_key_password
```

### 4. Build

```bash
# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease
```

## Style Guide

```java
class FooFragment extends BaseFragment {

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // initialize
    }

    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_foo;
    }

    private void initView() {

    }

    private void initData() {

    }

    private void handleBar() {

    }

    @OnClick(R.id.btn_baz)
    void onClickBaz() {

    }
}
```

## Author

Snapmaker Software Team

## License

TBD
