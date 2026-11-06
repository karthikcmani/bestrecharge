# BestRecharge – Troubleshooting & Diagnostics Guide

## 1. SIM Detection Diagnostics

### Q: Why does the app say "Number Unavailable (Tap Change)"?
* **Cause:** Many Indian telecom SIM cards (especially newer 4G/5G SIMs from Jio, Vi, and Airtel) do not store the 10-digit MSISDN directly in the SIM's EF_MSISDN elementary file. Android's `SubscriptionManager` returns an empty string when the number is not written to the hardware card.
* **Resolution:** Tap the **"Change"** button on the SIM card card to enter and confirm your 10-digit number. Once entered, BestRecharge saves it locally in Room as `USER_CONFIRMED`.

### Q: My number was auto-allocated to one operator, but I ported to another via MNP.
* **Resolution:** Under the Department of Telecommunications (DoT) National Numbering Plan, numbers belong to original series allocations. If you ported your number via Mobile Number Portability (MNP), tap **"Change"**, select your ported operator radio button (e.g., Vi, Jio, Airtel, or BSNL) and your current circle, then tap **"Apply & Compare"**. Your choice is permanently saved as user-confirmed.

---

## 2. Guided Provider Inspection Diagnostics

### Q: Why does the app ask for Accessibility Permission?
* **Cause:** Android requires the `AccessibilityService` permission for an application to read accessible on-screen text and navigate supported buttons within other installed apps.
* **Resolution:** In **Provider Intelligence**, tap **"Enable Accessibility Service"**. You will be taken directly to Android Accessibility Settings. Look for **BestRecharge** under "Downloaded Apps" or "Installed Services" and toggle it **ON**.

### Q: Accessibility Service turns off automatically after some time.
* **Cause:** Aggressive battery optimization engines in certain OEM Android skins (Nothing OS, OxygenOS, MIUI, ColorOS) periodically kill background services.
* **Resolution:** Go to **Android Settings $\rightarrow$ Apps $\rightarrow$ BestRecharge $\rightarrow$ Battery $\rightarrow$ Unrestricted**.

### Q: The inspection paused with "Sensitive credential / payment field detected".
* **Cause:** BestRecharge's security guard detected an authentication gate (OTP, password, MPIN) or a payment screen. By design, BestRecharge will **never** read or touch credentials.
* **Resolution:** Complete the login or dismiss the prompt manually in the provider app. Then switch back to BestRecharge to resume or finalize the inspection.

---

## 3. F-Droid Build & Offline Operation

### Q: Does BestRecharge work without Google Play Services or microG?
* **Yes.** All core features—including SIM detection, guided accessibility inspection, offer parsing, 3-tier financial optimization, and savings logging—work 100% offline without any proprietary Google libraries.

### Q: How do I build the APK from source?
* Open terminal in the repository root and run:
  ```bash
  cd android
  ./gradlew testDebugUnitTest assembleDebug
  ```
* The generated APK will be located at:
  `android/app/build/outputs/apk/debug/app-debug.apk`
