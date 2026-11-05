from datetime import datetime
from sqlalchemy import Column, String, Float, DateTime
from app.core.database import Base

class RechargeRecord(Base):
    __tablename__ = "recharge_records"

    id = Column(String(100), primary_key=True, index=True)
    sim_mobile_number = Column(String(20), nullable=False)
    operator = Column(String(50), nullable=False)
    circle = Column(String(50), default="Delhi")
    plan_name = Column(String(100), default="")
    
    # Financial reconciliation
    original_mrp = Column(Float, nullable=False)
    operator_discount = Column(Float, default=0.0)
    amount_paid = Column(Float, nullable=False)
    payment_provider = Column(String(100), default="UPI")
    applied_offer_title = Column(String(200), default="")
    
    expected_cashback = Column(Float, default=0.0)
    received_cashback = Column(Float, default=0.0)
    status = Column(String(50), default="PENDING") # EXPECTED, PENDING, RECEIVED, REJECTED, EXPIRED
    effective_cost = Column(Float, nullable=False)
    
    transaction_date = Column(DateTime, default=datetime.utcnow)
    notes = Column(String(255), default="")
