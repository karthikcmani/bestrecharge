# API Documentation

## Base URL
`http://localhost:8000/api/v1`

All responses follow standard JSON serialization and HTTP status codes.

---

### Endpoints

#### 1. Operators & Circles
- **`GET /operators`**: List supported telecom operators (Vi, Jio, Airtel, BSNL).
- **`GET /circles`**: List all 22 Indian telecom circles with circle codes.

#### 2. Recharge Plans
- **`GET /plans`**: Query plans with filters:
  - Query params: `operator_code`, `circle_code`, `category`, `min_price`, `max_price`, `min_validity`, `min_data`.
- **`GET /plans/{id}`**: Get full details for a specific plan.
- **`POST /plans/compare`**: Compare 2 or more plan IDs side-by-side.

#### 3. Offers Catalog
- **`GET /offers`**: Query available offers:
  - Query params: `provider`, `category`, `operator_code`, `valid_only`.
- **`GET /offers/{id}`**: Detailed offer terms and eligibility rules.
- **`POST /offers/import`**: Ingest an offer parsed from screenshot or user submission.

#### 4. Smart Deal Calculation Engine
- **`POST /deals/calculate`**:
  - Request:
    ```json
    {
      "plan_id": "vi_349_hero",
      "user_preferences": {
        "installed_payment_apps": ["PAYTM", "BHIM", "PHONEPE", "GPAY"],
        "is_paytm_new_or_inactive_60d": true,
        "is_bhim_new_user": true
      }
    }
    ```
  - Response:
    ```json
    {
      "plan": { "id": "vi_349_hero", "operator": "Vi", "price": 349.0 },
      "best_deal": {
        "deal_id": "deal_vi_paytm_opt",
        "operator_discount": 15.0,
        "initial_payable": 334.0,
        "recommended_payment_provider": "Paytm UPI",
        "guaranteed_cashback": 125.0,
        "conditional_cashback": 0.0,
        "guaranteed_effective_cost": 209.0,
        "potential_effective_cost": 209.0,
        "stacked_offers": ["vi_app_15_off", "paytm_upi_125_cb"],
        "explanation": "App exclusive ₹15 instant discount combined with Paytm UPI ₹125 assured cashback for users with no transaction in last 60 days.",
        "confidence": "HIGH"
      },
      "alternative_deals": [...]
    }
    ```

#### 5. Savings Tracking
- **`GET /savings/summary`**: Get aggregate savings, pending cashbacks, and breakdown.
- **`POST /savings/record`**: Log a completed recharge transaction.
- **`PATCH /savings/record/{id}`**: Update cashback claim status (`EXPECTED`, `PENDING`, `RECEIVED`, `REJECTED`, `EXPIRED`).
