import uuid
from typing import List
from fastapi import APIRouter, Depends, HTTPException, Query
from sqlalchemy.orm import Session
from sqlalchemy import func
from app.core.database import get_db
from app.models.savings import RechargeRecord
from app.schemas.schemas import RechargeRecordCreate, RechargeRecordResponse, SavingsSummaryResponse

router = APIRouter()

@router.get("/savings/summary", response_model=SavingsSummaryResponse)
def get_savings_summary(db: Session = Depends(get_db)):
    records = db.query(RechargeRecord).all()
    if not records:
        return SavingsSummaryResponse(
            total_recharge_spent=0.0,
            total_guaranteed_discounts=0.0,
            total_cashback_expected=0.0,
            total_cashback_received=0.0,
            total_cashback_pending=0.0,
            total_effective_savings=0.0,
            total_transactions_count=0
        )

    total_spent = sum(r.amount_paid for r in records)
    total_discounts = sum(r.operator_discount for r in records)
    total_cb_expected = sum(r.expected_cashback for r in records)
    total_cb_received = sum(r.received_cashback for r in records if r.status == "RECEIVED")
    total_cb_pending = sum(r.expected_cashback for r in records if r.status in ["PENDING", "EXPECTED"])
    total_savings = total_discounts + total_cb_received

    return SavingsSummaryResponse(
        total_recharge_spent=round(total_spent, 2),
        total_guaranteed_discounts=round(total_discounts, 2),
        total_cashback_expected=round(total_cb_expected, 2),
        total_cashback_received=round(total_cb_received, 2),
        total_cashback_pending=round(total_cb_pending, 2),
        total_effective_savings=round(total_savings, 2),
        total_transactions_count=len(records)
    )

@router.get("/savings/records", response_model=List[RechargeRecordResponse])
def get_recharge_records(db: Session = Depends(get_db)):
    return db.query(RechargeRecord).order_by(RechargeRecord.transaction_date.desc()).all()

@router.post("/savings/records", response_model=RechargeRecordResponse)
def create_recharge_record(req: RechargeRecordCreate, db: Session = Depends(get_db)):
    record_id = f"rec_{uuid.uuid4().hex[:8]}"
    record = RechargeRecord(
        id=record_id,
        sim_mobile_number=req.sim_mobile_number,
        operator=req.operator,
        circle=req.circle,
        plan_name=req.plan_name,
        original_mrp=req.original_mrp,
        operator_discount=req.operator_discount,
        amount_paid=req.amount_paid,
        payment_provider=req.payment_provider,
        applied_offer_title=req.applied_offer_title,
        expected_cashback=req.expected_cashback,
        received_cashback=req.received_cashback,
        status=req.status,
        effective_cost=req.effective_cost,
        notes=req.notes
    )
    db.add(record)
    db.commit()
    db.refresh(record)
    return record

@router.patch("/savings/records/{record_id}/status", response_model=RechargeRecordResponse)
def update_recharge_status(
    record_id: str,
    status: str = Query(..., description="EXPECTED, PENDING, RECEIVED, REJECTED, EXPIRED"),
    received_amount: float = Query(0.0),
    db: Session = Depends(get_db)
):
    record = db.query(RechargeRecord).filter(RechargeRecord.id == record_id).first()
    if not record:
        raise HTTPException(status_code=404, detail="Recharge record not found")
    
    record.status = status.upper()
    if status.upper() == "RECEIVED":
        record.received_cashback = received_amount if received_amount > 0 else record.expected_cashback
        record.effective_cost = max(0.0, record.amount_paid - record.received_cashback)
    elif status.upper() in ["REJECTED", "EXPIRED"]:
        record.received_cashback = 0.0
        record.effective_cost = record.amount_paid

    db.commit()
    db.refresh(record)
    return record
