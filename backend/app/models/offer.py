from datetime import datetime
from sqlalchemy import Column, String, Float, Integer, Boolean, DateTime, Text
from app.core.database import Base

class Offer(Base):
    __tablename__ = "offers"

    id = Column(String(100), primary_key=True, index=True)
    title = Column(String(200), nullable=False)
    provider = Column(String(100), nullable=False) # Vi, Paytm, PhonePe, BHIM, Google Pay, Amazon Pay, HDFC, SBI
    category = Column(String(50), nullable=False) # OPERATOR, UPI, WALLET, BANK_CARD, REWARD
    description = Column(Text, default="")
    
    # Financial fields
    discount_type = Column(String(50), default="FLAT_CASHBACK") # FLAT_DISCOUNT, PERCENTAGE_DISCOUNT, FLAT_CASHBACK, UPTO_CASHBACK, REWARD_POINTS
    discount_amount = Column(Float, default=0.0) # immediate discount
    cashback_amount = Column(Float, default=0.0) # flat or max cashback
    cashback_percentage = Column(Float, default=0.0) # if percentage based
    min_transaction_amount = Column(Float, default=0.0)
    max_cashback = Column(Float, default=0.0)
    
    # Eligibility & constraints
    user_eligibility = Column(String(50), default="ALL") # ALL, NEW_USER, INACTIVE_60_DAYS, REPEAT_USER, UNKNOWN
    payment_method_required = Column(String(100), default="ANY") # Paytm UPI, BHIM UPI, PhonePe UPI, RuPay, Credit Card, etc.
    applicable_operators = Column(String(100), default="ALL") # ALL, VI, JIO, AIRTEL, BSNL
    
    # Stacking logic
    is_stackable_with_operator = Column(Boolean, default=True) # Payment offer can stack with operator instant discount
    applies_to_discounted_value = Column(Boolean, default=False) # whether min txn is evaluated post operator discount
    
    # Validity & Provenance
    start_date = Column(DateTime, default=datetime.utcnow)
    expiry_date = Column(DateTime, nullable=True)
    terms_and_conditions = Column(Text, default="")
    source_url = Column(String(255), default="")
    verification_status = Column(String(50), default="OFFICIALLY_VERIFIED") # OFFICIALLY_VERIFIED, PUBLICLY_SOURCED, USER_IMPORTED, EXPIRED
    confidence_score = Column(Integer, default=100) # 0-100
    is_active = Column(Boolean, default=True)
