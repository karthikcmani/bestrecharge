# Security & Privacy Policy

## 1. Zero-Credential Architecture
BestRecharge is strictly an intelligence, discovery, and recommendation platform:
- **NO PAYMENT EXECUTION**: BestRecharge never handles credit/debit card PAN numbers, CVVs, netbanking passwords, UPI PINs, or OTPs.
- **NO ACCESSIBILITY SERVICE EXPLOITATION**: BestRecharge does not inspect third-party app windows or automate keystrokes.
- **OFFICIAL INTENT REDIRECTION ONLY**: To complete a recharge, BestRecharge creates an Android Intent dispatching directly to the official payment app (e.g. Paytm, BHIM, PhonePe, Google Pay, MyVi app) where the user authenticates directly with their bank.

---

## 2. On-Device Screenshot Privacy
- Screenshots imported by the user are processed **locally on the device** via Google ML Kit Text Recognition.
- Raw screenshot bitmaps are kept strictly in transient app cache and never transmitted to external servers without explicit user confirmation.

---

## 3. Data Protection & Cryptography
- **Network Transport**: Mandatory TLS 1.3 / HTTPS for all backend communication with strict certificate validation.
- **Local Persistence**: User preferences and SIM profiles stored in Android Room SQLite within app-private storage sandbox (`/data/data/com.bestrecharge.app`).
- **No Analytics Fingerprinting**: No IMEI, MAC address, or IMSI collection. Telecom detection relies on standard, non-sensitive Android Telephony APIs requiring minimal permissions (`READ_PHONE_STATE` is optional and strictly user-granted).
