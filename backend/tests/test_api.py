import pytest
from fastapi.testclient import TestClient
from app.main import app
from seed_data import seed_all

client = TestClient(app)

@pytest.fixture(scope="session", autouse=True)
def setup_database():
    seed_all()

def test_api_root():
    response = client.get("/")
    assert response.status_code == 200
    assert response.json()["app"] == "BestRecharge"

def test_get_operators():
    response = client.get("/api/v1/operators")
    assert response.status_code == 200
    operators = response.json()
    assert len(operators) >= 4
    codes = [op["code"] for op in operators]
    assert "VI" in codes
    assert "JIO" in codes
    assert "AIRTEL" in codes
    assert "BSNL" in codes

def test_get_plans():
    response = client.get("/api/v1/plans?operator_code=VI")
    assert response.status_code == 200
    plans = response.json()
    assert len(plans) >= 1
    # Check vi_349_hero
    plan_349 = next((p for p in plans if p["id"] == "vi_349_hero"), None)
    assert plan_349 is not None
    assert plan_349["price"] == 349.0
    assert plan_349["validity_days"] == 28

def test_get_offers():
    response = client.get("/api/v1/offers")
    assert response.status_code == 200
    offers = response.json()
    assert len(offers) >= 3

def test_calculate_deal_vi_349():
    payload = {
        "plan_id": "vi_349_hero",
        "user_preferences": {
            "installed_payment_apps": ["PAYTM", "BHIM", "PHONEPE"],
            "is_paytm_new_or_inactive_60d": True,
            "is_bhim_new_user": True
        }
    }
    response = client.post("/api/v1/deals/calculate", json=payload)
    assert response.status_code == 200
    data = response.json()
    assert data["plan"]["id"] == "vi_349_hero"
    best = data["best_deal"]
    # With Vi ₹15 off + Paytm ₹125 assured cashback:
    # MRP: ₹349, Payable: ₹334, Cashback: ₹125, Guaranteed Effective Cost: ₹209
    assert best["operator_discount"] == 15.0
    assert best["immediate_payable"] == 334.0
    assert best["guaranteed_cashback"] == 125.0
    assert best["guaranteed_effective_cost"] == 209.0

def test_ocr_parser():
    raw_text = """
    terms & conditions
    - Offer valid from 1st to 30th November 2026
    - Offer applicable on payments using BHIM Payments App
    - Applicable on min transaction value of ₹99
    - On eligible transactions the cashback will be instantly credited
    """
    response = client.post("/api/v1/offers/parse-ocr", json={"text": raw_text})
    assert response.status_code == 200
    parsed = response.json()
    assert parsed["provider"] == "BHIM"
    assert parsed["min_transaction_amount"] == 99.0

def test_savings_summary_and_record():
    # Fetch summary
    res_summary = client.get("/api/v1/savings/summary")
    assert res_summary.status_code == 200
    summary = res_summary.json()
    assert summary["total_transactions_count"] >= 2
    assert summary["total_recharge_spent"] > 0
