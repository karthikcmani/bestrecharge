import pytest
from datetime import datetime, timedelta
from app.calculators.engine import OfferCalculationEngine

class MockOffer:
    def __init__(self, **kwargs):
        for k, v in kwargs.items():
            setattr(self, k, v)

def test_scenario_1_single_guaranteed_discount():
    # Single operator instant discount: ₹15 off on ₹349
    op_offer = MockOffer(
        title="Vi Exclusive ₹15 Off",
        category="OPERATOR",
        discount_type="FLAT_DISCOUNT",
        discount_amount=15.0,
        cashback_amount=0.0,
        min_transaction_amount=299.0,
        expiry_date=None,
        is_stackable_with_operator=True
    )
    deal = OfferCalculationEngine.evaluate_deal(349.0, op_offer, None, None)
    assert deal is not None
    assert deal["original_mrp"] == 349.0
    assert deal["operator_discount"] == 15.0
    assert deal["immediate_payable"] == 334.0
    assert deal["guaranteed_cashback"] == 0.0
    assert deal["guaranteed_effective_cost"] == 334.0
    assert deal["total_guaranteed_savings"] == 15.0

def test_scenario_2_single_conditional_cashback():
    # Single BHIM "Up to ₹75" cashback on ₹349
    bhim_offer = MockOffer(
        title="BHIM Up to ₹75",
        provider="BHIM",
        category="UPI",
        discount_type="UPTO_CASHBACK",
        discount_amount=0.0,
        cashback_amount=75.0,
        min_transaction_amount=99.0,
        user_eligibility="ALL",
        expiry_date=None
    )
    deal = OfferCalculationEngine.evaluate_deal(349.0, None, bhim_offer, None)
    assert deal is not None
    assert deal["original_mrp"] == 349.0
    assert deal["immediate_payable"] == 349.0
    assert deal["guaranteed_cashback"] == 0.0  # Not guaranteed!
    assert deal["potential_cashback"] == 75.0  # Conditional lottery
    assert deal["guaranteed_effective_cost"] == 349.0
    assert deal["potential_effective_cost"] == 274.0

def test_scenario_3_two_compatible_offers():
    # Vi ₹15 instant discount + Paytm ₹125 assured cashback
    op_offer = MockOffer(
        title="Vi App ₹15 Off",
        category="OPERATOR",
        discount_type="FLAT_DISCOUNT",
        discount_amount=15.0,
        min_transaction_amount=299.0,
        expiry_date=None
    )
    paytm_offer = MockOffer(
        title="Paytm ₹125 Cashback",
        provider="Paytm",
        category="UPI",
        discount_type="FLAT_CASHBACK",
        cashback_amount=125.0,
        min_transaction_amount=179.0,
        user_eligibility="INACTIVE_60_DAYS",
        expiry_date=None
    )
    deal = OfferCalculationEngine.evaluate_deal(
        349.0, op_offer, paytm_offer, None, 
        user_prefs={"is_paytm_new_or_inactive_60d": True}
    )
    assert deal is not None
    assert deal["operator_discount"] == 15.0
    assert deal["immediate_payable"] == 334.0
    assert deal["guaranteed_cashback"] == 125.0
    # Effective cost = 334 - 125 = 209
    assert deal["guaranteed_effective_cost"] == 209.0
    assert deal["total_guaranteed_savings"] == 140.0

def test_scenario_4_two_mutually_exclusive_offers():
    # User cannot execute Paytm and BHIM concurrently in a single transaction
    # find_best_deals evaluates them in separate permutations and picks the best
    op_offer = MockOffer(
        id="vi_15",
        title="Vi App ₹15 Off",
        category="OPERATOR",
        applicable_operators="VI",
        discount_type="FLAT_DISCOUNT",
        discount_amount=15.0,
        min_transaction_amount=299.0,
        expiry_date=None,
        is_stackable_with_operator=True
    )
    paytm_offer = MockOffer(
        id="paytm_125",
        title="Paytm Assured ₹125",
        provider="Paytm",
        category="UPI",
        applicable_operators="VI",
        discount_type="FLAT_CASHBACK",
        cashback_amount=125.0,
        min_transaction_amount=179.0,
        user_eligibility="ALL",
        expiry_date=None
    )
    bhim_offer = MockOffer(
        id="bhim_75",
        title="BHIM Up to ₹75",
        provider="BHIM",
        category="UPI",
        applicable_operators="VI",
        discount_type="UPTO_CASHBACK",
        cashback_amount=75.0,
        min_transaction_amount=99.0,
        user_eligibility="ALL",
        expiry_date=None
    )

    deals = OfferCalculationEngine.find_best_deals(
        plan_price=349.0,
        operator_code="VI",
        offers=[op_offer, paytm_offer, bhim_offer],
        user_prefs=None
    )
    # Paytm gives guaranteed effective cost 209; BHIM gives potential 259
    # The best deal ranked first should be Paytm!
    assert len(deals) >= 2
    best = deals[0]
    assert best["recommended_payment_provider"] == "Paytm"
    assert best["guaranteed_effective_cost"] == 209.0

def test_scenario_5_cashback_below_minimum_transaction():
    # Minimum transaction is ₹400, plan is ₹349 -> offer disqualified
    offer = MockOffer(
        title="Min ₹400 Required",
        provider="PhonePe",
        category="UPI",
        discount_type="FLAT_CASHBACK",
        cashback_amount=50.0,
        min_transaction_amount=400.0,
        user_eligibility="ALL",
        expiry_date=None
    )
    deal = OfferCalculationEngine.evaluate_deal(349.0, None, offer, None)
    assert deal is None

def test_scenario_6_new_user_only_offer():
    # User is existing, offer requires NEW_USER -> ineligible
    offer = MockOffer(
        title="New User ₹50",
        provider="Paytm",
        category="UPI",
        discount_type="FLAT_CASHBACK",
        cashback_amount=50.0,
        min_transaction_amount=100.0,
        user_eligibility="NEW_USER",
        expiry_date=None
    )
    deal = OfferCalculationEngine.evaluate_deal(
        349.0, None, offer, None,
        user_prefs={"is_paytm_new_or_inactive_60d": False}
    )
    assert deal is None

def test_scenario_7_expired_offer():
    # Offer expired yesterday -> disqualified
    yesterday = datetime.utcnow() - timedelta(days=1)
    offer = MockOffer(
        title="Expired Offer",
        category="OPERATOR",
        discount_type="FLAT_DISCOUNT",
        discount_amount=20.0,
        min_transaction_amount=100.0,
        expiry_date=yesterday
    )
    deal = OfferCalculationEngine.evaluate_deal(349.0, offer, None, None)
    assert deal is None

def test_scenario_8_unknown_eligibility():
    # Without user preferences, offer with condition is safely not assumed
    offer = MockOffer(
        title="Inactive 60d ₹125",
        provider="Paytm",
        category="UPI",
        discount_type="FLAT_CASHBACK",
        cashback_amount=125.0,
        min_transaction_amount=179.0,
        user_eligibility="INACTIVE_60_DAYS",
        expiry_date=None
    )
    deal = OfferCalculationEngine.evaluate_deal(349.0, None, offer, None, user_prefs=None)
    assert deal is None

def test_scenario_9_cashback_percentage_with_cap():
    # 10% cashback up to max ₹30 on ₹349 -> 34.9 capped at 30.0
    offer = MockOffer(
        title="10% Cashback",
        provider="Google Pay",
        category="UPI",
        discount_type="PERCENTAGE_CASHBACK",
        cashback_percentage=10.0,
        max_cashback=30.0,
        min_transaction_amount=100.0,
        user_eligibility="ALL",
        expiry_date=None
    )
    deal = OfferCalculationEngine.evaluate_deal(349.0, None, offer, None)
    assert deal is not None
    assert deal["guaranteed_cashback"] == 30.0
    assert deal["guaranteed_effective_cost"] == 319.0

def test_scenario_10_cashback_on_discounted_transaction_value():
    # Operator discount ₹15 makes amount payable ₹334.
    # Payment offer computes 10% on discounted value = ₹33.4
    op_offer = MockOffer(
        title="Vi ₹15 Off",
        category="OPERATOR",
        discount_type="FLAT_DISCOUNT",
        discount_amount=15.0,
        min_transaction_amount=100.0,
        expiry_date=None
    )
    pay_offer = MockOffer(
        title="10% on Net Payable",
        provider="UPI",
        category="UPI",
        discount_type="PERCENTAGE_CASHBACK",
        cashback_percentage=10.0,
        max_cashback=50.0,
        min_transaction_amount=100.0,
        user_eligibility="ALL",
        applies_to_discounted_value=True,
        expiry_date=None
    )
    deal = OfferCalculationEngine.evaluate_deal(349.0, op_offer, pay_offer, None)
    assert deal is not None
    assert deal["immediate_payable"] == 334.0
    assert round(deal["guaranteed_cashback"], 2) == 33.40
    assert round(deal["guaranteed_effective_cost"], 2) == 300.60

def test_scenario_11_cashback_on_original_transaction_value():
    # Payment offer computes 10% on original MRP = ₹34.9
    op_offer = MockOffer(
        title="Vi ₹15 Off",
        category="OPERATOR",
        discount_type="FLAT_DISCOUNT",
        discount_amount=15.0,
        min_transaction_amount=100.0,
        expiry_date=None
    )
    pay_offer = MockOffer(
        title="10% on MRP",
        provider="UPI",
        category="UPI",
        discount_type="PERCENTAGE_CASHBACK",
        cashback_percentage=10.0,
        max_cashback=50.0,
        min_transaction_amount=100.0,
        user_eligibility="ALL",
        applies_to_discounted_value=False,
        expiry_date=None
    )
    deal = OfferCalculationEngine.evaluate_deal(349.0, op_offer, pay_offer, None)
    assert deal is not None
    assert round(deal["guaranteed_cashback"], 2) == 34.90

def test_scenario_12_reward_points_cannot_be_redeemed_as_cash():
    # 25 Vi Stars reward points must NOT be subtracted from guaranteed effective cost
    reward_offer = MockOffer(
        title="25 Vi Stars",
        category="REWARD",
        discount_type="REWARD_POINTS",
        cashback_amount=25.0,
        expiry_date=None
    )
    deal = OfferCalculationEngine.evaluate_deal(349.0, None, None, reward_offer)
    assert deal is not None
    assert deal["reward_points_title"] == "25 Vi Stars"
    assert deal["guaranteed_cashback"] == 0.0
    assert deal["guaranteed_effective_cost"] == 349.0  # NOT 324!

def test_scenario_13_multiple_recharge_plans_different_benefits():
    # Comparing 28-day ₹349 vs 84-day ₹859
    deal_28d = OfferCalculationEngine.evaluate_deal(349.0, None, None, None)
    deal_84d = OfferCalculationEngine.evaluate_deal(859.0, None, None, None)
    # Cost per day
    cpd_28 = deal_28d["guaranteed_effective_cost"] / 28.0
    cpd_84 = deal_84d["guaranteed_effective_cost"] / 84.0
    assert cpd_84 < cpd_28  # Long validity provides lower cost per day!

def test_scenario_14_negative_or_invalid_discount_values():
    # If discount is 0 or negative, it's safely clamped
    offer = MockOffer(
        title="Bad Discount",
        category="OPERATOR",
        discount_type="FLAT_DISCOUNT",
        discount_amount=-50.0,
        min_transaction_amount=100.0,
        expiry_date=None
    )
    deal = OfferCalculationEngine.evaluate_deal(349.0, offer, None, None)
    assert deal is not None
    assert deal["immediate_payable"] == 349.0
    assert deal["guaranteed_effective_cost"] == 349.0

def test_scenario_15_multiple_payment_providers_ranking():
    # Provider A offers ₹50 flat cashback
    # Provider B offers ₹100 flat cashback
    # Provider C offers up to ₹150 conditional cashback
    p_a = MockOffer(
        id="pa", title="A ₹50", provider="ProviderA", category="UPI",
        applicable_operators="VI", discount_type="FLAT_CASHBACK",
        cashback_amount=50.0, min_transaction_amount=100.0, user_eligibility="ALL", expiry_date=None
    )
    p_b = MockOffer(
        id="pb", title="B ₹100", provider="ProviderB", category="UPI",
        applicable_operators="VI", discount_type="FLAT_CASHBACK",
        cashback_amount=100.0, min_transaction_amount=100.0, user_eligibility="ALL", expiry_date=None
    )
    p_c = MockOffer(
        id="pc", title="C Up to ₹150", provider="ProviderC", category="UPI",
        applicable_operators="VI", discount_type="UPTO_CASHBACK",
        cashback_amount=150.0, min_transaction_amount=100.0, user_eligibility="ALL", expiry_date=None
    )

    ranked = OfferCalculationEngine.find_best_deals(349.0, "VI", [p_a, p_b, p_c])
    # Guaranteed ₹100 from ProviderB is ranked #1 because guaranteed savings takes highest priority!
    assert ranked[0]["recommended_payment_provider"] == "ProviderB"
    assert ranked[0]["guaranteed_effective_cost"] == 249.0
