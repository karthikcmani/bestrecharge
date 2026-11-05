from typing import List, Optional
from datetime import datetime
from pydantic import BaseModel, Field

# Operators & Circles
class OperatorBase(BaseModel):
    id: str
    name: str
    code: str
    color: str = "#E60000"
    logo_symbol: str = "VI"
    is_active: bool = True

class OperatorResponse(OperatorBase):
    model_config = {"from_attributes": True}

class CircleBase(BaseModel):
    id: str
    name: str
    code: str

class CircleResponse(CircleBase):
    model_config = {"from_attributes": True}

# Recharge Plans
class RechargePlanBase(BaseModel):
    id: str
    operator_id: str
    circle_id: Optional[str] = None
    price: float
    validity_days: int
    daily_data_gb: float = 0.0
    total_data_gb: float = 0.0
    voice_benefit: str = "Truly Unlimited Calls"
    sms_per_day: int = 100
    ott_benefits: str = ""
    plan_category: str = "UNLIMITED"
    description: str = ""
    source: str = "Official Telecom Portal"
    last_verified: Optional[datetime] = None
    is_active: bool = True

class RechargePlanResponse(RechargePlanBase):
    operator_name: Optional[str] = None
    operator_code: Optional[str] = None
    model_config = {"from_attributes": True}

# Offers
class OfferBase(BaseModel):
    id: str
    title: str
    provider: str
    category: str
    description: str = ""
    discount_type: str = "FLAT_CASHBACK"
    discount_amount: float = 0.0
    cashback_amount: float = 0.0
    cashback_percentage: float = 0.0
    min_transaction_amount: float = 0.0
    max_cashback: float = 0.0
    user_eligibility: str = "ALL"
    payment_method_required: str = "ANY"
    applicable_operators: str = "ALL"
    is_stackable_with_operator: bool = True
    applies_to_discounted_value: bool = False
    start_date: Optional[datetime] = None
    expiry_date: Optional[datetime] = None
    terms_and_conditions: str = ""
    source_url: str = ""
    verification_status: str = "OFFICIALLY_VERIFIED"
    confidence_score: int = 100
    is_active: bool = True

class OfferResponse(OfferBase):
    model_config = {"from_attributes": True}

class OfferImportRequest(BaseModel):
    title: str
    provider: str
    category: str = "UPI"
    discount_type: str = "FLAT_CASHBACK"
    discount_amount: float = 0.0
    cashback_amount: float = 0.0
    cashback_percentage: float = 0.0
    min_transaction_amount: float = 0.0
    max_cashback: float = 0.0
    user_eligibility: str = "ALL"
    payment_method_required: str = "ANY"
    applicable_operators: str = "ALL"
    terms_and_conditions: str = ""
    verification_status: str = "USER_IMPORTED"

# Calculation & Smart Deals
class UserDealPreferences(BaseModel):
    installed_payment_apps: List[str] = Field(default_factory=lambda: ["PAYTM", "BHIM", "PHONEPE", "GPAY", "AMAZON_PAY"])
    is_paytm_new_or_inactive_60d: bool = True
    is_bhim_new_user: bool = True
    is_phonepe_new_user: bool = False
    has_rupay_credit_card: bool = False
    has_vi_upi: bool = False

class DealCalculateRequest(BaseModel):
    plan_id: str
    user_preferences: Optional[UserDealPreferences] = None

class DealBreakdown(BaseModel):
    original_mrp: float
    operator_discount: float
    immediate_payable: float
    recommended_payment_provider: str
    applied_payment_discount: float
    guaranteed_cashback: float
    potential_cashback: float
    reward_points_title: Optional[str] = None
    reward_points_value: float = 0.0
    guaranteed_effective_cost: float
    potential_effective_cost: float
    total_guaranteed_savings: float
    total_potential_savings: float
    user_eligibility_status: str
    confidence_level: str
    stacked_offers: List[str]
    explanation: str
    action_intent_uri: str
    action_label: str

class DealCalculateResponse(BaseModel):
    plan: RechargePlanResponse
    best_deal: DealBreakdown
    alternative_deals: List[DealBreakdown]

# Savings & Tracking
class RechargeRecordCreate(BaseModel):
    sim_mobile_number: str
    operator: str
    circle: str = "Delhi"
    plan_name: str
    original_mrp: float
    operator_discount: float = 0.0
    amount_paid: float
    payment_provider: str
    applied_offer_title: str
    expected_cashback: float = 0.0
    received_cashback: float = 0.0
    status: str = "PENDING"
    effective_cost: float
    notes: str = ""

class RechargeRecordResponse(RechargeRecordCreate):
    id: str
    transaction_date: datetime
    model_config = {"from_attributes": True}

class SavingsSummaryResponse(BaseModel):
    total_recharge_spent: float
    total_guaranteed_discounts: float
    total_cashback_expected: float
    total_cashback_received: float
    total_cashback_pending: float
    total_effective_savings: float
    total_transactions_count: int
