# Guided Automatic Provider Inspection

## 1. Architectural Workflow

```
[ User Initiates Inspection ]
              │
              ▼
[ Check Provider App Installed & A11y Enabled ]
              │
              ▼
[ Create ProviderInspectionSession & Launch App ]
              │
              ▼
[ AccessibilityEventProcessor (Package Whitelist Check) ]
              │
              ▼
[ AccessibilityNodeExtractor (Recursive Parsing + Auth Guard) ]
              │
              ▼
[ ProviderScreenClassifier (Identify Dashboard / Plans / Offers / T&C) ]
              │
              ▼
[ ProviderNavigationWorkflow (Vi / Jio / Airtel / BSNL Adapter) ]
              │
              ▼
[ ScreenContentParser (Rule-based Tariff & Cashback Extraction) ]
              │
              ▼
[ OfferExtractionCoordinator (Deduplicate & Persist to Room DB) ]
              │
              ▼
[ Return to BestRecharge & Recalculate Best Deals ]
```

## 2. Core Modules (`com.bestrecharge.app.inspection`)

| Component | Class Name | Responsibility |
|---|---|---|
| Central Controller | `ProviderInspectionManager` | Coordinates session lifecycles, user consent, and app launching. |
| Background Service | `ProviderInspectionService` | Android `AccessibilityService` receiving filtered accessibility events. |
| Session Telemetry | `ProviderInspectionSession` | Tracks progress percentage, current stage, visited screens, and counts. |
| State Machine | `InspectionStateManager` | Manages observable transitions (`CONNECTING`, `NAVIGATING`, `EXTRACTING`, `COMPLETED`, `PAUSED_SENSITIVE_SCREEN`). |
| Tree Extractor | `AccessibilityNodeExtractor` | Safely extracts text, content descriptions, and resource IDs with depth limits. |
| Event Processor | `AccessibilityEventProcessor` | Debounces rapid events and enforces strict package boundary checks. |
| Screen Classifier | `ProviderScreenClassifier` | Detects `HOME_DASHBOARD`, `RECHARGE_PLANS`, `OFFERS_SECTION`, `TERMS_AND_CONDITIONS`. |
| Content Parser | `ScreenContentParser` | Regex and heuristic extraction of prices, validity days, GB/day, cashbacks, and min txn. |
| Data Coordinator | `OfferExtractionCoordinator` | Deduplicates and persists verified tariffs & offers into Room database. |

## 3. Measurable Progress & Stage Indicators

The inspection progress interface displays real-time stages and measurable metrics:
* **Stage 1 (10% - 25%):** "Launching Vi application..." $\rightarrow$ "Locating Vi recharge section..."
* **Stage 2 (25% - 50%):** "Extracting Vi recharge plans & offers..." (Counts updated in real time)
* **Stage 3 (50% - 75%):** "Reading payment offers & discounts..." (Paytm, BHIM, PhonePe, Vi UPI)
* **Stage 4 (75% - 90%):** "Extracting promotional terms & conditions..."
* **Stage 5 (100%):** "Inspection completed successfully. Returning to BestRecharge."

## 4. User Controls
* **Pause / Resume:** Automatically triggered if an unmapped dialog or sensitive screen appears.
* **Stop Inspection:** Prominently available via `btn_stop_inspection` at any stage to immediately abort and release resources.
