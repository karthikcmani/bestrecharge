# BestRecharge

**Intelligent Mobile Recharge, Cashback & Offer Optimization Platform**

*Recharge smarter. Save more.*

---

## 1. Executive Summary

**BestRecharge** is an open-source, native Android application and microservice platform designed for the Indian telecom market (Vodafone Idea / Vi, Reliance Jio, Bharti Airtel, and BSNL). 

Unlike standard recharge apps and digital wallets that act as payment merchants, BestRecharge is an independent decision-intelligence platform. Its mission is to **Discover, Compare, Calculate, Explain, and Recommend** the most mathematically optimal combination of mobile recharge plans and promotional payment cashbacks.

---

## 2. The Problem BestRecharge Solves

When recharging a mobile number in India:
1. **Promotional Complexity**: Users are presented with dozens of offers across payment gateways (Paytm, BHIM, PhonePe, Google Pay, Amazon Pay, bank cards).
2. **Hidden Fine Print**: Offers advertise misleading figures like "Up to ₹125 Cashback", while hiding strict conditions:
   - Valid only for users inactive on that payment app for 60+ days.
   - Minimum transaction amount thresholds (e.g., ₹179 or ₹99).
   - Scratch-card lottery rewards vs. assured fiat bank deposits.
   - Non-cash loyalty coins (e.g., gold discount coupons, Vi Stars).
3. **Coupon Incompatibility & Stacking**: Users don't know whether operator discounts (e.g. ₹15 instant deduction in the Vi App) can stack with external UPI cashbacks.
4. **Mobile Number Portability (MNP)**: Phone number prefixes cannot reliably identify the current carrier.

BestRecharge resolves this by computing:
$$\text{Effective Cost} = \text{Immediate Payable Amount} - \text{Guaranteed Cashback} - \text{Conditional Cashback}$$
and presenting a transparent, deterministic breakdown with dual metrics (**Guaranteed Savings** vs. **Potential Savings**).

---

## 3. Core Architecture & Highlights

- **Pure Native Android**: Built in **Java 17** using Android Jetpack MVVM, Room SQLite Database, Retrofit 2, OkHttp 3, and Material Design 3.
- **Offline-First Resilience**: All plan catalogs, offer rules, and calculation engines run completely on-device without requiring an internet connection.
- **On-Device Vision OCR**: Uses **Google ML Kit Text Recognition** to extract provider, cashback amounts, minimum transaction values, and terms directly from screenshot images.
- **Zero-Credential Security**: BestRecharge never handles UPI PINs, banking passwords, CVVs, or OTPs. Recharges are executed by redirecting users to verified official payment apps via secure Android Intents.
- **Python FastAPI Backend**: High-performance REST microservice with SQLAlchemy ORM, deterministic stacking calculator, and automated test suite.

---

## 4. Repository Structure

```
bestrecharge/
│
├── android/                             # Native Android Java Application
│   ├── app/
│   │   ├── src/main/java/com/bestrecharge/app/
│   │   │   ├── data/local/              # Room Database, DAOs, Entities
│   │   │   ├── data/repository/         # Single-source-of-truth Repositories
│   │   │   ├── engine/                  # On-device Deterministic Calculation Engine
│   │   │   ├── ocr/                     # Google ML Kit Text Recognition & Parser
│   │   │   └── ui/                      # MVVM Activities, Fragments, Adapters
│   │   │       ├── main/                # Bottom Navigation host
│   │   │       ├── home/                # Active SIM card & hero recommendation
│   │   │       ├── compare/             # Plan filters & side-by-side comparator
│   │   │       ├── offers/              # Aggregated promotional offers explorer
│   │   │       ├── savings/             # Lifetime savings tracker & history
│   │   │       ├── profile/             # SIM profiles & payment app toggles
│   │   │       ├── deals/               # The Best Deal recommendation screen
│   │   │       └── ocr/                 # Screenshot import & structured review
│   │   └── src/test/java/               # Android Unit Tests
│   ├── build.gradle
│   ├── settings.gradle
│   └── gradlew.bat
│
├── backend/                             # Python FastAPI Microservice
│   ├── app/
│   │   ├── api/v1/                      # REST API Endpoints (Plans, Offers, Deals, Savings)
│   │   ├── calculators/                 # Deterministic Stacking Engine
│   │   ├── core/                        # Configuration & Database setup
│   │   ├── models/                      # SQLAlchemy Relational Models
│   │   ├── schemas/                     # Pydantic v2 Request/Response Schemas
│   │   └── services/                    # Heuristic OCR extraction service
│   ├── tests/                           # Unit tests covering 15+ calculation scenarios
│   ├── seed_data.py                     # Realistic telecom catalog seed script
│   └── requirements.txt
│
├── docs/                                # Architectural & Product Documentation
│   ├── PRODUCT_REQUIREMENTS.md
│   ├── SYSTEM_ARCHITECTURE.md
│   ├── DATABASE_SCHEMA.md
│   ├── API_DOCUMENTATION.md
│   ├── OFFER_ENGINE.md
│   ├── DATA_SOURCES.md
│   ├── SECURITY.md
│   ├── DEVELOPMENT_GUIDE.md
│   └── ROADMAP.md
│
├── LICENSE                              # Apache 2.0 License
└── README.md
```

---

## 5. Quickstart: Running the Backend

```bash
cd backend
python -m venv venv
.\venv\Scripts\activate      # On Windows
# source venv/bin/activate  # On Linux / macOS

pip install -r requirements.txt
python seed_data.py
python -m pytest            # Run backend automated tests (22 passing)
uvicorn app.main:app --reload --port 8000
```
Open interactive Swagger API documentation at: `http://localhost:8000/docs`

---

## 6. Quickstart: Building the Android App

```bash
cd android
# Run unit tests:
.\gradlew.bat testDebugUnitTest

# Assemble Debug APK:
.\gradlew.bat assembleDebug
```
The compiled debug APK will be generated at:
```
android/app/build/outputs/apk/debug/app-debug.apk
```

---

## 7. License

Licensed under the [Apache License, Version 2.0](LICENSE).
