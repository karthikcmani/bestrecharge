# Database Schema

## Relational Schema (PostgreSQL & SQLite Room)

The schema is synchronized between the backend database and the Android Room local database to ensure offline-first synchronization.

```mermaid
erDiagram
    OPERATOR ||--o{ RECHARGE_PLAN : offers
    CIRCLE ||--o{ RECHARGE_PLAN : scoped_to
    OPERATOR ||--o{ OFFER : applies_to
    SIM_PROFILE ||--o{ RECHARGE_HISTORY : records
    USER ||--o{ SIM_PROFILE : owns
    USER ||--o{ USER_PREFERENCE : has
    OFFER ||--o{ RECHARGE_HISTORY : utilized_in

    OPERATOR {
        string id PK
        string name
        string code
        string logo_url
    }

    CIRCLE {
        string id PK
        string name
        string code
    }

    RECHARGE_PLAN {
        string id PK
        string operator_id FK
        string circle_id FK
        float price
        int validity_days
        float daily_data_gb
        float total_data_gb
        string voice_benefit
        int sms_per_day
        string ott_benefits
        string plan_category
        string description
        string source
        datetime last_verified
        boolean is_active
    }

    OFFER {
        string id PK
        string title
        string provider
        string category
        string description
        string discount_type
        float discount_value
        float max_discount
        float min_transaction_amount
        string eligibility_rule
        string payment_method_required
        string applicable_operators
        datetime start_date
        datetime expiry_date
        string terms_and_conditions
        string source_url
        string verification_status
        int confidence_score
        boolean is_stackable_with_operator
    }

    SIM_PROFILE {
        string id PK
        string mobile_number
        string operator_id FK
        string circle_id FK
        string sim_slot
        boolean is_prepaid
        boolean is_primary
    }

    RECHARGE_HISTORY {
        string id PK
        string sim_profile_id FK
        string plan_id FK
        float original_amount
        float operator_discount
        float payable_amount
        string payment_provider
        string applied_offer_id FK
        float expected_cashback
        float received_cashback
        string status
        datetime transaction_date
    }
```
