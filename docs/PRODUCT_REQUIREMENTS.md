# Product Requirements Document (PRD)

## BestRecharge: Intelligent Mobile Recharge, Cashback & Offer Optimization Platform

### 1. Executive Summary
BestRecharge is a native Android and microservice platform tailored for the Indian telecom market (Jio, Airtel, Vodafone Idea / Vi, BSNL). Rather than functioning as another payment gateway or recharge merchant, BestRecharge serves as an impartial decision-intelligence engine. It discovers, compares, evaluates, and calculates the true effective cost of mobile recharges by taking into account operator discounts, payment app cashbacks (Paytm, PhonePe, Google Pay, BHIM, Amazon Pay), bank cards, and coupon stacking rules.

### 2. Problem Statement
Every month, over 500 million prepaid mobile subscribers in India choose between dozens of recharge tiers and hundreds of dynamic payment promotions:
1. **Confusing promotional rules**: Offers often claim "Up to ₹125 Cashback" but bury restrictive terms (e.g., valid only on first recharge, minimum transaction ₹179, specific payment method like UPI Lite, or 60-day inactivity clause).
2. **Coupon incompatibility**: Users are rarely aware which discounts stack (e.g., a ₹15 direct operator discount + a ₹75 BHIM cashback) versus mutually exclusive payment offers.
3. **Hidden effective costs**: Payment apps advertise gross figures or non-cash reward points (e.g., gold purchase coupons or loyalty coins) as equivalent to direct cashback.
4. **Mobile Number Portability (MNP)**: Users frequently port their numbers (e.g., from Airtel to Jio), rendering standard prefix lookup tables obsolete.

### 3. Core Product Principles
- **Accuracy over Hype**: Clear differentiation between Guaranteed Effective Cost and Potential Effective Cost. Reward coins are never conflated with fiat cash.
- **Privacy First**: Sensitive data such as banking passwords, UPI PINs, or OTPs are never asked, logged, or processed. All screenshots are processed on-device via ML Kit.
- **Offline Resilience**: The Android application is fully operational offline using Room DB caching and a native Java calculation engine.
- **Transparency**: Every plan and offer explicitly cites its data source, verification timestamp, and confidence status.

### 4. Functional Scope & Modules

#### Module A: User Onboarding & SIM Profiles
- Multi-SIM support (Dual SIM default, expandable).
- Operator selection (Vi, Jio, Airtel, BSNL) and Telecom Circle selection (e.g., Andhra Pradesh, Maharashtra, Delhi, Tamil Nadu, etc.).
- Prepaid / Postpaid distinction.
- MNP manual operator confirmation.
- User preference settings (preferred payment apps, available bank credit/debit cards).

#### Module B: Recharge Plan Discovery & Comparison
- Category browsing: Unlimited, Daily Data (1GB, 1.5GB, 2GB, 3GB/day), Long Validity (84 days, 365 days), Data Add-ons, Talktime/Top-up, OTT Bundles (Disney+ Hotstar, SonyLIV, Zee5).
- Multi-dimensional sorting: Lowest Price, Highest Validity, Lowest Cost/Day, Best Data Value.
- Side-by-side plan comparator showing differences in voice, SMS, data caps, and bundled subscriptions.

#### Module C: Offer Discovery & Aggregation
- Multi-channel aggregation: Operator-exclusive app discounts (e.g. Vi App ₹15 instant deduction), UPI cashback offers (BHIM, Paytm, PhonePe, Google Pay, Amazon Pay), and bank cards.
- Comprehensive offer model with rule definitions: min transaction, max cap, cashback percentage, user condition (`NEW_USER`, `EXISTING_USER`, `INACTIVE_60_DAYS`, `ALL`), payment method requirement, and expiry.

#### Module D: Screenshot-to-Offer Intelligence (OCR)
- On-device optical character recognition using Google ML Kit Text Recognition.
- Heuristic parsing engine extracting operator, provider, cashback amount, minimum transaction amount, validity period, and terms.
- Review & Confirmation dialog enabling user validation before saving to local database.

#### Module E: Smart Deterministic Calculation Engine
- Calculation formula:
  $$\text{Effective Cost} = \text{Initial Amount Paid} - \text{Actual Cashback} - \text{Applicable Redeemed Rewards}$$
- Evaluates coupon combinations against compatibility matrix:
  - Mutual exclusivity.
  - Calculation base (whether cashback is computed against original MRP or post-operator-discount payable amount).
  - Minimum transaction threshold checks.
- Dual metrics: Guaranteed Savings (immediate payable discounts) vs. Potential Savings (conditional cashbacks).

#### Module F: Best Deal Recommendation Screen
- Unified summary card showing:
  - Plan MRP
  - Operator instant discount
  - Immediate payable amount
  - Recommended payment application
  - Expected / Potential cashback
  - Final effective cost
  - Full transparency breakdown and terms summary
- "Continue to Recharge" intent dispatcher opening the verified payment/operator application securely.

#### Module G: Offer Explorer
- Searchable directory of active deals with tag filters (UPI, Operator, Wallet, Bank).
- Offer status tracking: Interested, Used, Expired, Not Eligible.

#### Module H: Cashback & Savings Tracker
- Personal dashboard monitoring total expenditure and lifetime savings.
- Status management: Expected $\rightarrow$ Pending $\rightarrow$ Received / Rejected / Expired.
- Historical log of completed recharges.

#### Module I: Privacy & Data Freshness
- Local-first storage; optional account synchronization.
- Data freshness indicator: Officially Verified, Publicly Sourced, User Imported, Expired.
