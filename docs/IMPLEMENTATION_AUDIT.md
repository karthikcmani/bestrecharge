# BestRecharge – Implementation Audit Report

## 1. Executive Summary
This audit inspects the complete codebase of **BestRecharge**, an open-source Android recharge intelligence and offer optimization application designed for Indian prepaid telecom consumers (Vi, Jio, Airtel, BSNL). The application is targeted for distribution on **F-Droid** and direct APK release.

## 2. Technical Stack and Configuration
- **Programming Language:** Java (Java 17).
- **Target Platform:** Native Android (Min SDK 26, Target SDK 35, Compile SDK 35).
- **UI Framework:** Native Android XML with Material Design 3 and ViewBinding.
- **Architecture:** Jetpack MVVM (ViewModel, LiveData, MediatorLiveData).
- **Local Persistence:** Room Database (`bestrecharge_local.db`, SQLite).
- **Background Tasks:** WorkManager.
- **License:** Apache License 2.0.

## 3. Findings & Defect Root Causes

### 3.1 SIM Detection & Mobile Number Allocation
- **Previous Defect:** In early iterations, if `SubscriptionManager.getPhoneNumber()` returned empty (common on modern Android versions where operators do not write the MSISDN to the SIM card), the application displayed hardcoded fallback numbers or inferred operators without user confirmation. Furthermore, numbers were not cross-verified with Department of Telecommunications (DoT) allocations, resulting in cases like `98478` (a Vi Kerala series) being labeled as Reliance Jio.
- **Remediation Plan:**
  1. Implement explicit telephony states: `SIM_NOT_AVAILABLE`, `NUMBER_UNAVAILABLE`, `NUMBER_AVAILABLE`, `MANUAL_NUMBER_ENTRY`, `USER_CONFIRMED`, `OPERATOR_UNVERIFIED`, `PERMISSION_DENIED`.
  2. Never fabricate a mobile number. If Android telephony does not report the MSISDN, display "Number Unavailable" and prompt the user to confirm.
  3. Support full dual-SIM detection with slot IDs, carrier names, and SIM states.
  4. Allow users to confirm their operator and circle to account for Mobile Number Portability (MNP).

### 3.2 Provider Synchronization & Offer Data
- **Previous Defect:** The application displayed "Live sync active" or "Verified tariff active" based merely on whether the provider app package was installed, without actually inspecting or synchronizing live offers from inside the provider application.
- **Remediation Plan:**
  1. Build a genuine, user-authorized `AccessibilityService` (`ProviderInspectionService`).
  2. Implement guided automatic provider inspection where the user explicitly triggers an inspection of installed provider apps (starting with Vi).
  3. Extract accessible UI trees (recharge plans, offer cards, terms and conditions) using structured node extraction.
  4. Provide honest progress and state reporting. If an inspection fails or reaches an unmapped screen, report it truthfully without simulating results.

### 3.3 F-Droid Compatibility & Proprietary Dependencies
- **Previous Defect:** `build.gradle` included `com.google.android.gms:play-services-mlkit-text-recognition:19.0.1`, which depends on Google Play Services and proprietary binaries. This violates F-Droid Free Software guidelines.
- **Remediation Plan:**
  1. Remove proprietary Google Play Services and ML Kit dependencies.
  2. Implement a 100% open-source, offline OCR and screenshot parsing architecture.
  3. Verify that all runtime libraries are Apache 2.0, MIT, or compatible open-source licenses.

### 3.4 Recommendation & Calculation Pipeline
- **Status:** The underlying `RecommendationEngine` and `DealCalculator` correctly implement the 3-tier financial transparency model (Original MRP, Immediate Payable after Instant Discount, Potential Effective Cost after Guaranteed Cashback). 15 automated regression tests are passing.
- **Action:** Retain and expand the engine to dynamically consume offers extracted via the `ProviderInspectionService`.

---
*Generated: November 2026 as part of the Final Engineering Implementation Directive.*
