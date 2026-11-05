# System Architecture

## BestRecharge Architecture Overview

BestRecharge adopts a decoupled, offline-first native mobile architecture paired with a high-performance Python FastAPI backend microservice.

```mermaid
graph TD
    subgraph Client["Android Native Client (Java / MD3 / Jetpack)"]
        UI[Activity & Fragments / ViewBinding]
        VM[MVVM ViewModels]
        REPO[Repository Layer]
        ROOM[(Room SQLite Database)]
        ENGINE_LOCAL[Local Deterministic Calc Engine]
        MLKIT[Google ML Kit OCR Engine]
        NET_OKHTTP[Retrofit 2 / OkHttp3 Client]
        INTENTS[System Intent Dispatcher]
    end

    subgraph Backend["Backend Microservice (FastAPI / Python 3.13)"]
        API[FastAPI REST Controllers v1]
        CALC_SRV[Offer Calculation & Stacking Service]
        PARSER_SRV[Screenshot OCR Ingestion Service]
        CATALOG_SRV[Plan & Offer Catalog Engine]
        DB_ORM[(PostgreSQL / SQLite Storage)]
    end

    subgraph Providers["External Ecosystem"]
        PAYMENT_APPS[Paytm / PhonePe / BHIM / GPay Apps]
        OPERATOR_APPS[Vi / Jio / Airtel / BSNL Apps & Portals]
    end

    UI --> VM
    VM --> REPO
    REPO --> ROOM
    REPO --> ENGINE_LOCAL
    REPO --> MLKIT
    REPO --> NET_OKHTTP
    UI --> INTENTS
    INTENTS -.->|Opens App via URI/Intent| PAYMENT_APPS
    INTENTS -.->|Opens Portal/App| OPERATOR_APPS

    NET_OKHTTP -->|HTTPS REST| API
    API --> CALC_SRV
    API --> CATALOG_SRV
    API --> PARSER_SRV
    CATALOG_SRV --> DB_ORM
```

### Component Architecture

#### 1. Android Client Architecture
- **Language**: Java 17 (Pure Native Android).
- **Presentation Pattern**: Model-View-ViewModel (MVVM) with Android Jetpack Lifecycle (`ViewModel`, `LiveData`, `ViewBinding`).
- **Data Layer**:
  - `RoomDatabase` with SQLite caching for complete offline operation.
  - Data Access Objects (`PlanDao`, `OfferDao`, `SimDao`, `SavingsDao`, `PreferenceDao`).
  - Single Source of Truth `Repository` pattern.
- **Computation Engine**: Local Java implementation of the deterministic calculation engine (`DealCalculator`), ensuring that calculations work instantaneously with zero network latency or when disconnected.
- **Machine Learning**: Google ML Kit On-Device Vision Text Recognition (`com.google.android.gms:play-services-mlkit-text-recognition`).

#### 2. Backend Architecture
- **Framework**: FastAPI (Asynchronous Python 3.13).
- **ORM / Persistence**: SQLAlchemy 2.0 with PostgreSQL (and SQLite development fallback).
- **Calculation Layer**: Stateless, deterministic combinatoric evaluator evaluating:
  - Offer eligibility constraints.
  - Combination rules (e.g. 1 operator discount + 1 payment offer).
  - Effective cost formulas.
- **Provider Adapters**: Extensible adapter modules for standardizing disparate offer representations from Vi, Jio, Airtel, BSNL, Paytm, PhonePe, BHIM, and Google Pay.
