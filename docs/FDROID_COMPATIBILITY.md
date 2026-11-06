# F-Droid Compatibility & Open Source Compliance

## 1. Overview
BestRecharge is engineered as a free, open-source Android application targeted for distribution via **F-Droid** and standalone APK releases. This document specifies the dependency licenses, architecture, permissions, and build reproducibility requirements to guarantee strict adherence to the **F-Droid Free Software Criteria**.

## 2. Dependency License Audit

All dependencies included in `android/app/build.gradle` have been audited to ensure they contain **zero proprietary binary blobs** and rely strictly on standard open-source licenses:

| Dependency Artifact | Version | License | F-Droid Compatible? | Purpose |
|---|---|---|---|---|
| `androidx.appcompat:appcompat` | 1.7.0 | Apache 2.0 | Yes | Core UI compatibility |
| `com.google.android.material:material` | 1.12.0 | Apache 2.0 | Yes | Material Design 3 UI components |
| `androidx.constraintlayout:constraintlayout` | 2.2.0 | Apache 2.0 | Yes | Responsive constraint layouts |
| `androidx.recyclerview:recyclerview` | 1.3.2 | Apache 2.0 | Yes | List view rendering |
| `androidx.cardview:cardview` | 1.0.0 | Apache 2.0 | Yes | Card UI rendering |
| `androidx.lifecycle:lifecycle-viewmodel` | 2.8.7 | Apache 2.0 | Yes | MVVM state handling |
| `androidx.lifecycle:lifecycle-livedata` | 2.8.7 | Apache 2.0 | Yes | Observable reactive pipeline |
| `androidx.navigation:navigation-fragment` | 2.8.4 | Apache 2.0 | Yes | Native screen navigation |
| `androidx.navigation:navigation-ui` | 2.8.4 | Apache 2.0 | Yes | Navigation UI integration |
| `androidx.room:room-runtime` | 2.6.1 | Apache 2.0 | Yes | Local SQLite persistence |
| `androidx.room:room-compiler` | 2.6.1 | Apache 2.0 | Yes | Annotation processor (build-time) |
| `com.squareup.retrofit2:retrofit` | 2.11.0 | Apache 2.0 | Yes | Offline-first HTTP client |
| `com.squareup.retrofit2:converter-gson` | 2.11.0 | Apache 2.0 | Yes | JSON serialization |
| `com.squareup.okhttp3:logging-interceptor` | 4.12.0 | Apache 2.0 | Yes | HTTP network logging |
| `androidx.work:work-runtime` | 2.9.1 | Apache 2.0 | Yes | Background synchronization |
| `junit:junit` | 4.13.2 | EPL 1.0 | Yes | Unit test framework |
| `org.mockito:mockito-core` | 5.14.2 | MIT | Yes | Mocking test framework |

### Excluded / Prohibited Dependencies:
* **Google Play Services (`com.google.android.gms:*`)**: Excluded.
* **Google ML Kit Vision (`com.google.mlkit:*`)**: Excluded in F-Droid builds; replaced by open-source, local offline text analysis and screenshot parsers.
* **Firebase / Crashlytics / Analytics**: Zero usage.
* **Proprietary Advertising SDKs**: Zero usage.
* **Mandatory Cloud Accounts**: Zero usage.

## 3. Permissions Justification

| Permission | Protection Level | F-Droid Justification |
|---|---|---|
| `android.permission.INTERNET` | Normal | Sourcing publicly available telecom tariff updates. |
| `android.permission.ACCESS_NETWORK_STATE` | Normal | Verifying connectivity status before network requests. |
| `android.permission.READ_PHONE_STATE` | Dangerous | Reading active SIM card presence, slot index, and carrier metadata. |
| `android.permission.READ_PHONE_NUMBERS` | Dangerous (API 30+) | Reading hardware-reported MSISDN when available. Handled with explicit runtime consent. |
| `android.permission.BIND_ACCESSIBILITY_SERVICE` | Signature | Scoped specifically to `ProviderInspectionService` for user-initiated, guided inspection of telecom applications. |

## 4. Build Reproducibility
The project builds using standard Gradle commands without requiring proprietary Gradle plugins:
```bash
cd android
./gradlew clean assembleRelease
```
Source compatibility is configured for **Java 17**.

## 5. Offline-First Guarantee
Core features (plan comparison, offer stacking, financial calculations, savings logging, guided accessibility inspection, and manual number entry) function entirely offline using local Room database storage.
