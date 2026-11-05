from datetime import datetime
from sqlalchemy import Column, String, Float, Integer, Boolean, DateTime, ForeignKey, Text
from sqlalchemy.orm import relationship
from app.core.database import Base

class RechargePlan(Base):
    __tablename__ = "recharge_plans"

    id = Column(String(100), primary_key=True, index=True)
    operator_id = Column(String(50), ForeignKey("operators.id"), nullable=False)
    circle_id = Column(String(50), ForeignKey("circles.id"), nullable=True) # None = Pan India

    price = Column(Float, nullable=False)
    validity_days = Column(Integer, nullable=False)
    daily_data_gb = Column(Float, default=0.0)
    total_data_gb = Column(Float, default=0.0)
    voice_benefit = Column(String(100), default="Truly Unlimited Calls")
    sms_per_day = Column(Integer, default=100)
    ott_benefits = Column(String(255), default="")
    plan_category = Column(String(50), default="UNLIMITED") # POPULAR, UNLIMITED, DAILY_DATA, LONG_VALIDITY, ANNUAL, DATA_ONLY, TALKTIME, OTT
    description = Column(Text, default="")
    source = Column(String(100), default="Official Telecom Portal")
    last_verified = Column(DateTime, default=datetime.utcnow)
    is_active = Column(Boolean, default=True)

    operator = relationship("Operator", back_populates="plans")
    circle = relationship("Circle", back_populates="plans")
