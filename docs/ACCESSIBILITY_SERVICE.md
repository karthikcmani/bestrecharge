# AccessibilityService Architecture & Security Specification

## 1. Overview
BestRecharge employs an explicitly scoped Android `AccessibilityService` (`ProviderInspectionService`) to provide **user-initiated, guided automatic provider inspection** across installed Indian telecom applications:
- Vodafone Idea (`com.mventus.selfcare.activity`)
- Reliance Jio (`com.jio.myjio`)
- Bharti Airtel (`com.myairtelapp`)
- BSNL (`in.co.bsnl`)

The service operates exclusively under user direction to discover accessible recharge tariffs, discount coupons, and payment terms & conditions, eliminating manual screenshotting.

## 2. Platform Configuration

### 2.1 Manifest Declaration
Declared in `AndroidManifest.xml` with signature-level permission protection:
```xml
<service
    android:name=".inspection.ProviderInspectionService"
    android:exported="true"
    android:permission="android.permission.BIND_ACCESSIBILITY_SERVICE"
    android:label="@string/inspect_provider_offers">
    <intent-filter>
        <action android:name="android.accessibilityservice.AccessibilityService" />
    </intent-filter>
    <meta-data
        android:name="android.accessibilityservice"
        android:resource="@xml/accessibility_service_config" />
</service>
```

### 2.2 Service Configuration (`res/xml/accessibility_service_config.xml`)
```xml
<accessibility-service xmlns:android="http://schemas.android.com/apk/res/android"
    android:description="@string/accessibility_service_description"
    android:packageNames="com.mventus.selfcare.activity,com.jio.myjio,com.myairtelapp,in.co.bsnl"
    android:accessibilityEventTypes="typeWindowStateChanged|typeWindowContentChanged|typeViewScrolled|typeViewClicked"
    android:accessibilityFlags="flagDefault|flagRetrieveInteractiveWindows|flagReportViewIds"
    android:accessibilityFeedbackType="feedbackGeneric"
    android:notificationTimeout="100"
    android:canRetrieveWindowContent="true"
    android:canPerformGestures="true" />
```

## 3. Strict Security & Privacy Boundaries

### 3.1 Narrowly Scoped Package Allowlist
`android:packageNames` restricts event delivery exclusively to verified telecom applications. In addition, `AccessibilityEventProcessor` enforces runtime package verification:
```java
if (!session.targetPackage.equals(currentPkg)) {
    InspectionStateManager.getInstance().updateStage(session,
        ProviderInspectionSession.Status.PAUSED_SENSITIVE_SCREEN,
        "Inspection paused (outside " + session.providerCode + " app)",
        session.progressPercent);
    return;
}
```
If the user navigates away or switches to another app (messaging, banking, browser), the service **immediately pauses and ignores all non-whitelisted events**.

### 3.2 Sensitive Screen & Credential Protection
The extraction engine continuously scans for sensitive elements:
- Password fields (`node.isPassword() == true`)
- OTP inputs (`enter otp`, `verification code`)
- UPI PIN & MPIN (`upi pin`, `enter mpin`)
- Banking & Card fields (`cvv`, `card expiry`, `netbanking password`)

Upon detection of any sensitive element:
1. Extraction is **immediately paused**.
2. No input fields are touched or read.
3. The user is prompted to complete or dismiss the screen manually.
4. BestRecharge **never** initiates financial transactions or enters credentials.

### 3.3 Active Session Requirement
The service is strictly active only when a user explicitly taps **"Inspect Offers"** in BestRecharge. In the background or when no session is active, all accessibility events are dropped immediately.

## 4. Node Lifecycle & Memory Management
To prevent memory leaks in the Android Accessibility IPC bridge:
- Maximum recursion depth is capped at 35 levels.
- Maximum extracted elements per snapshot is capped at 300.
- All intermediate `AccessibilityNodeInfo` objects are recycled via `recycle()` in `finally` blocks.
