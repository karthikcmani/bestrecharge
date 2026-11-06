# BestRecharge – Privacy Policy & Security Principles

## 1. Core Privacy Philosophy
BestRecharge is engineered as a privacy-respecting, open-source decision intelligence application. It does not operate as a payment intermediary and **never** collects, transmits, or stores user financial credentials.

## 2. Accessibility Service Privacy Commitments
The `ProviderInspectionService` adheres to non-negotiable privacy boundaries:
1. **Explicit User Initiation:** The service only processes events when the user explicitly triggers an inspection from within the BestRecharge application.
2. **Strict Package Whitelist:** The service listens only to four verified telecom apps (`com.mventus.selfcare.activity`, `com.jio.myjio`, `com.myairtelapp`, `in.co.bsnl`). Events from all other applications (messaging, social media, banking, browsers) are discarded immediately.
3. **Zero Credential Access:** The application includes automated filters that detect password fields, OTP inputs, MPINs, and UPI PINs. Upon encountering any authentication or payment gate, the inspection pauses immediately.
4. **No Financial Transactions:** BestRecharge cannot and does not initiate payments, authorize debits, or submit UPI transactions.
5. **Zero Background Surveillance:** The application does not monitor user activity after inspections complete, nor does it start automatically on device boot.

## 3. Telephony & SIM Privacy
- Hardware SIM metadata (carrier, MCC/MNC, slot) is retrieved strictly through Android standard telephony APIs.
- Mobile numbers are never fabricated, inferred, or uploaded.
- If Android telephony does not report the MSISDN, the application displays "Number Unavailable" and allows the user to manually enter and confirm their number.
- Manual confirmations are stored locally on device in SQLite and are never sent to external servers.

## 4. Local Data Storage & No Tracking
- All extracted plans, offers, terms, and savings logs reside exclusively in the local Room SQLite database on your device.
- BestRecharge contains **zero analytics SDKs, zero crash trackers (Firebase/Crashlytics), and zero advertising SDKs**.
- No account or sign-in is required to use any feature.
