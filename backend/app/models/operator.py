import enum
from datetime import datetime
from sqlalchemy import Column, String, Float, Integer, Boolean, DateTime, ForeignKey, Text, Enum
from sqlalchemy.orm import relationship
from app.core.database import Base

class Operator(Base):
    __tablename__ = "operators"

    id = Column(String(50), primary_key=True, index=True)
    name = Column(String(100), nullable=False)
    code = Column(String(20), unique=True, nullable=False)
    color = Column(String(20), default="#E60000")
    logo_symbol = Column(String(50), default="VI")
    is_active = Column(Boolean, default=True)

    plans = relationship("RechargePlan", back_populates="operator")

class Circle(Base):
    __tablename__ = "circles"

    id = Column(String(50), primary_key=True, index=True)
    name = Column(String(100), nullable=False)
    code = Column(String(20), unique=True, nullable=False)

    plans = relationship("RechargePlan", back_populates="circle")
