from datetime import datetime, timedelta
from app.core.database import SessionLocal, Base, engine
from app.models.operator import Operator, Circle
from app.models.plan import RechargePlan
from app.models.offer import Offer
from app.models.savings import RechargeRecord

def seed_all():
    Base.metadata.create_all(bind=engine)
    db = SessionLocal()

    # Clear existing seed data if any
    db.query(RechargeRecord).delete()
    db.query(RechargePlan).delete()
    db.query(Offer).delete()
    db.query(Circle).delete()
    db.query(Operator).delete()
    db.commit()

    print("Seeding operators...")
    operators = [
        Operator(id="op_vi", name="Vodafone Idea (Vi)", code="VI", color="#FF0033", logo_symbol="VI"),
        Operator(id="op_jio", name="Reliance Jio", code="JIO", color="#005BFF", logo_symbol="JIO"),
        Operator(id="op_airtel", name="Bharti Airtel", code="AIRTEL", color="#EE0000", logo_symbol="AIRTEL"),
        Operator(id="op_bsnl", name="BSNL", code="BSNL", color="#008000", logo_symbol="BSNL"),
    ]
    db.add_all(operators)

    print("Seeding telecom circles...")
    circles = [
        Circle(id="circ_dl", name="Delhi NCR", code="DL"),
        Circle(id="circ_mum", name="Mumbai", code="MUM"),
        Circle(id="circ_mh", name="Maharashtra & Goa", code="MH"),
        Circle(id="circ_ka", name="Karnataka", code="KA"),
        Circle(id="circ_tn", name="Tamil Nadu", code="TN"),
        Circle(id="circ_ap", name="Andhra Pradesh & Telangana", code="AP"),
        Circle(id="circ_gj", name="Gujarat", code="GJ"),
        Circle(id="circ_kl", name="Kerala", code="KL"),
        Circle(id="circ_pb", name="Punjab", code="PB"),
        Circle(id="circ_kol", name="Kolkata", code="KOL"),
        Circle(id="circ_wb", name="West Bengal", code="WB"),
        Circle(id="circ_upe", name="UP East", code="UPE"),
        Circle(id="circ_upw", name="UP West", code="UPW"),
        Circle(id="circ_bih", name="Bihar & Jharkhand", code="BH"),
        Circle(id="circ_mp", name="Madhya Pradesh", code="MP"),
        Circle(id="circ_raj", name="Rajasthan", code="RJ"),
        Circle(id="circ_hr", name="Haryana", code="HR"),
    ]
    db.add_all(circles)

    print("Seeding recharge plans...")
    now = datetime.utcnow()
    plans = [
        # Vi Plans
        RechargePlan(
            id="vi_349_hero",
            operator_id="op_vi",
            price=349.0,
            validity_days=28,
            daily_data_gb=1.5,
            total_data_gb=42.0,
            voice_benefit="Truly Unlimited Calls",
            sms_per_day=100,
            ott_benefits="Vi Movies & TV, Weekend Data Rollover, Binge All Night (12am-6am)",
            plan_category="UNLIMITED",
            description="Hero Unlimited: Unlimited Night Data 12am-6am, Weekend Rollover, 1.5GB/day for 28 days.",
            source="Official Vi Prepaid Tariff",
            last_verified=now
        ),
        RechargePlan(
            id="vi_479_hero",
            operator_id="op_vi",
            price=479.0,
            validity_days=48,
            daily_data_gb=1.5,
            total_data_gb=72.0,
            voice_benefit="Truly Unlimited Calls",
            sms_per_day=100,
            ott_benefits="Weekend Data Rollover, Binge All Night",
            plan_category="DAILY_DATA",
            description="1.5GB/day with Binge All Night unlimited night internet.",
            source="Official Vi Prepaid Tariff",
            last_verified=now
        ),
        RechargePlan(
            id="vi_859_hero",
            operator_id="op_vi",
            price=859.0,
            validity_days=84,
            daily_data_gb=1.5,
            total_data_gb=126.0,
            voice_benefit="Truly Unlimited Calls",
            sms_per_day=100,
            ott_benefits="Weekend Data Rollover, Binge All Night",
            plan_category="LONG_VALIDITY",
            description="84 Days 1.5GB/day Hero Unlimited package.",
            source="Official Vi Prepaid Tariff",
            last_verified=now
        ),
        RechargePlan(
            id="vi_199_data",
            operator_id="op_vi",
            price=199.0,
            validity_days=18,
            daily_data_gb=1.0,
            total_data_gb=18.0,
            voice_benefit="Truly Unlimited Calls",
            sms_per_day=100,
            plan_category="POPULAR",
            description="Affordable short validity unlimited calling and daily 1GB.",
            source="Official Vi Prepaid Tariff",
            last_verified=now
        ),
        RechargePlan(
            id="vi_3599_annual",
            operator_id="op_vi",
            price=3599.0,
            validity_days=365,
            daily_data_gb=2.0,
            total_data_gb=730.0,
            voice_benefit="Truly Unlimited Calls",
            sms_per_day=100,
            ott_benefits="Disney+ Hotstar 1 Year, Weekend Data Rollover",
            plan_category="ANNUAL",
            description="Full 365 Days 2GB/day with Disney+ Hotstar Mobile 1 Year included.",
            source="Official Vi Prepaid Tariff",
            last_verified=now
        ),

        # Jio Plans
        RechargePlan(
            id="jio_349_popular",
            operator_id="op_jio",
            price=349.0,
            validity_days=28,
            daily_data_gb=2.0,
            total_data_gb=56.0,
            voice_benefit="Truly Unlimited Calls",
            sms_per_day=100,
            ott_benefits="JioTV, JioCinema, JioCloud, Unlimited True 5G",
            plan_category="UNLIMITED",
            description="True 5G Unlimited, 2GB/day for 28 days with Jio apps.",
            source="Official Jio Portal",
            last_verified=now
        ),
        RechargePlan(
            id="jio_198_starter",
            operator_id="op_jio",
            price=198.0,
            validity_days=14,
            daily_data_gb=2.0,
            total_data_gb=28.0,
            voice_benefit="Truly Unlimited Calls",
            sms_per_day=100,
            ott_benefits="Unlimited True 5G, JioTV",
            plan_category="POPULAR",
            description="Budget 14-day 2GB/day pack with True 5G.",
            source="Official Jio Portal",
            last_verified=now
        ),
        RechargePlan(
            id="jio_899_quarterly",
            operator_id="op_jio",
            price=899.0,
            validity_days=90,
            daily_data_gb=2.0,
            total_data_gb=180.0,
            voice_benefit="Truly Unlimited Calls",
            sms_per_day=100,
            ott_benefits="Unlimited True 5G, JioTV, JioCinema",
            plan_category="LONG_VALIDITY",
            description="90 Days uninterrupted 2GB/day + 20GB extra data.",
            source="Official Jio Portal",
            last_verified=now
        ),
        RechargePlan(
            id="jio_3599_annual",
            operator_id="op_jio",
            price=3599.0,
            validity_days=365,
            daily_data_gb=2.5,
            total_data_gb=912.5,
            voice_benefit="Truly Unlimited Calls",
            sms_per_day=100,
            ott_benefits="Unlimited True 5G, JioCinema, JioTV",
            plan_category="ANNUAL",
            description="365 Days 2.5GB/day True 5G flagship package.",
            source="Official Jio Portal",
            last_verified=now
        ),

        # Airtel Plans
        RechargePlan(
            id="airtel_349_popular",
            operator_id="op_op_airtel" if False else "op_airtel",
            price=349.0,
            validity_days=28,
            daily_data_gb=1.5,
            total_data_gb=42.0,
            voice_benefit="Truly Unlimited Calls",
            sms_per_day=100,
            ott_benefits="Unlimited 5G Data, Airtel Xstream Play, Wynk Music",
            plan_category="UNLIMITED",
            description="Unlimited 5G, 1.5GB/day 28 days with Wynk and Apollo 24/7.",
            source="Official Airtel Tariff",
            last_verified=now
        ),
        RechargePlan(
            id="airtel_979_long",
            operator_id="op_airtel",
            price=979.0,
            validity_days=84,
            daily_data_gb=2.0,
            total_data_gb=168.0,
            voice_benefit="Truly Unlimited Calls",
            sms_per_day=100,
            ott_benefits="Airtel Xstream Play, Unlimited 5G",
            plan_category="LONG_VALIDITY",
            description="84 Days 2GB/day premium bundle with Unlimited 5G.",
            source="Official Airtel Tariff",
            last_verified=now
        ),

        # BSNL Plans
        RechargePlan(
            id="bsnl_199_value",
            operator_id="op_bsnl",
            price=199.0,
            validity_days=30,
            daily_data_gb=2.0,
            total_data_gb=60.0,
            voice_benefit="Unlimited Local & STD Calls",
            sms_per_day=100,
            plan_category="POPULAR",
            description="Affordable high-data 2GB/day for 30 full days.",
            source="Official BSNL Tariff",
            last_verified=now
        ),
        RechargePlan(
            id="bsnl_397_long",
            operator_id="op_bsnl",
            price=397.0,
            validity_days=150,
            daily_data_gb=2.0,
            total_data_gb=60.0,
            voice_benefit="Unlimited for 30 days, incoming valid 150 days",
            sms_per_day=100,
            plan_category="LONG_VALIDITY",
            description="Long validity SIM keeping pack: 150 days validity with 2GB/day for first 30 days.",
            source="Official BSNL Tariff",
            last_verified=now
        ),
    ]
    db.add_all(plans)

    print("Seeding real promotional offers...")
    expiry_end_month = datetime(2026, 11, 30, 23, 59, 59)
    offers = [
        # Vi App Exclusive Discount
        Offer(
            id="vi_app_15_off",
            title="Vi App Exclusive ₹15 Instant Discount",
            provider="Vi",
            category="OPERATOR",
            description="Direct instant deduction of ₹15 applied when recharging on Vi App.",
            discount_type="FLAT_DISCOUNT",
            discount_amount=15.0,
            cashback_amount=0.0,
            min_transaction_amount=299.0,
            max_cashback=15.0,
            user_eligibility="ALL",
            payment_method_required="ANY",
            applicable_operators="VI",
            is_stackable_with_operator=True,
            start_date=datetime(2026, 11, 1),
            expiry_date=expiry_end_month,
            terms_and_conditions="Instant ₹15 discount applied at checkout on Vi app for recharge >= ₹299.",
            source_url="https://www.myvi.in/offers",
            verification_status="OFFICIALLY_VERIFIED",
            confidence_score=100
        ),

        # Paytm UPI Offer 1: ₹125 Assured Cashback for inactive 60d
        Offer(
            id="paytm_upi_125_cb",
            title="Paytm UPI Assured ₹125 Cashback",
            provider="Paytm",
            category="UPI",
            description="Get assured ₹125 cashback for users who have not used Paytm anywhere in the last 60 days.",
            discount_type="FLAT_CASHBACK",
            discount_amount=0.0,
            cashback_amount=125.0,
            min_transaction_amount=179.0,
            max_cashback=125.0,
            user_eligibility="INACTIVE_60_DAYS",
            payment_method_required="Paytm UPI",
            applicable_operators="ALL",
            is_stackable_with_operator=True,
            start_date=datetime(2026, 11, 1),
            expiry_date=expiry_end_month,
            terms_and_conditions="Min assured cashback of ₹125. Applicable on min transaction value of ₹179 using Paytm UPI. Offer valid for Users who have not used Paytm anywhere in the last 60 days; will get flat ₹125 cashback + Gold Coins on every payment.",
            source_url="https://paytm.com/recharge-offers",
            verification_status="OFFICIALLY_VERIFIED",
            confidence_score=95
        ),

        # Paytm UPI Offer 2: ₹100 Assured Cashback (General)
        Offer(
            id="paytm_upi_100_cb",
            title="Paytm UPI Assured ₹100 Cashback",
            provider="Paytm",
            category="UPI",
            description="Get assured ₹100 cashback on min transaction of ₹179 via Paytm UPI.",
            discount_type="FLAT_CASHBACK",
            discount_amount=0.0,
            cashback_amount=100.0,
            min_transaction_amount=179.0,
            max_cashback=100.0,
            user_eligibility="ALL",
            payment_method_required="Paytm UPI",
            applicable_operators="ALL",
            is_stackable_with_operator=True,
            start_date=datetime(2026, 11, 1),
            expiry_date=expiry_end_month,
            terms_and_conditions="Valid once per user during offer period on min transaction value of Rs.179 using Paytm UPI. Eligible users receive cashback directly in UPI-linked bank account.",
            source_url="https://paytm.com/recharge-offers",
            verification_status="OFFICIALLY_VERIFIED",
            confidence_score=95
        ),

        # BHIM Payments App Offer: Up to ₹75 Cashback
        Offer(
            id="bhim_upi_75_cb",
            title="BHIM Payments App Up to ₹75 Cashback",
            provider="BHIM",
            category="UPI",
            description="Get assured cashback up to ₹75 on min transaction of ₹99 via BHIM payments app.",
            discount_type="UPTO_CASHBACK",
            discount_amount=0.0,
            cashback_amount=75.0,
            min_transaction_amount=99.0,
            max_cashback=75.0,
            user_eligibility="ALL",
            payment_method_required="BHIM Payments App",
            applicable_operators="ALL",
            is_stackable_with_operator=True,
            start_date=datetime(2026, 11, 1),
            expiry_date=expiry_end_month,
            terms_and_conditions="Offer valid from 1st to 30th November 2026. Applicable on payments using BHIM Payments App on min transaction value of ₹99. On eligible transactions cashback will be instantly credited to user's UPI linked bank account.",
            source_url="https://www.bhimupi.org.in",
            verification_status="OFFICIALLY_VERIFIED",
            confidence_score=98
        ),

        # PhonePe UPI Offer
        Offer(
            id="phonepe_flat_25",
            title="PhonePe Assured ₹25 Cashback",
            provider="PhonePe",
            category="UPI",
            description="Flat ₹25 cashback on mobile recharge of ₹199 or more using PhonePe UPI.",
            discount_type="FLAT_CASHBACK",
            discount_amount=0.0,
            cashback_amount=25.0,
            min_transaction_amount=199.0,
            max_cashback=25.0,
            user_eligibility="ALL",
            payment_method_required="PhonePe UPI",
            applicable_operators="ALL",
            is_stackable_with_operator=True,
            start_date=datetime(2026, 11, 1),
            expiry_date=expiry_end_month,
            terms_and_conditions="Cashback credited within 24 hours to PhonePe gift card or bank account.",
            source_url="https://phonepe.com",
            verification_status="PUBLICLY_SOURCED",
            confidence_score=90
        ),

        # Loyalty Reward Points (Non-fiat cash)
        Offer(
            id="vi_stars_25",
            title="Earn 25 Vi Stars Loyalty Points",
            provider="Vi",
            category="REWARD",
            description="Earn 25 Vi Stars points redeemable for partner coupons.",
            discount_type="REWARD_POINTS",
            discount_amount=0.0,
            cashback_amount=25.0, # in points
            min_transaction_amount=199.0,
            max_cashback=25.0,
            user_eligibility="ALL",
            payment_method_required="ANY",
            applicable_operators="VI",
            is_stackable_with_operator=True,
            start_date=datetime(2026, 11, 1),
            expiry_date=expiry_end_month,
            terms_and_conditions="Vi Stars points will be credited to user profile. Points cannot be redeemed for fiat cash.",
            source_url="https://www.myvi.in",
            verification_status="OFFICIALLY_VERIFIED",
            confidence_score=100
        )
    ]
    db.add_all(offers)

    print("Seeding sample recharge history for savings tracker...")
    records = [
        RechargeRecord(
            id="rec_oct_01",
            sim_mobile_number="9847837607",
            operator="Vi",
            circle="Delhi NCR",
            plan_name="Vi Hero Unlimited ₹349",
            original_mrp=349.0,
            operator_discount=15.0,
            amount_paid=334.0,
            payment_provider="Paytm UPI",
            applied_offer_title="Paytm UPI ₹125 Assured Cashback",
            expected_cashback=125.0,
            received_cashback=125.0,
            status="RECEIVED",
            effective_cost=209.0,
            transaction_date=datetime(2026, 10, 5, 14, 30),
            notes="Recharge successful. Received ₹125 in bank."
        ),
        RechargeRecord(
            id="rec_sep_01",
            sim_mobile_number="9847837607",
            operator="Vi",
            circle="Delhi NCR",
            plan_name="Vi Hero Unlimited ₹349",
            original_mrp=349.0,
            operator_discount=15.0,
            amount_paid=334.0,
            payment_provider="BHIM Payments App",
            applied_offer_title="BHIM Up to ₹75 Cashback",
            expected_cashback=75.0,
            received_cashback=45.0,
            status="RECEIVED",
            effective_cost=289.0,
            transaction_date=datetime(2026, 9, 7, 11, 15),
            notes="Received ₹45 lottery cashback via BHIM."
        )
    ]
    db.add_all(records)

    db.commit()
    db.close()
    print("Database seeding completed successfully!")

if __name__ == "__main__":
    seed_all()
