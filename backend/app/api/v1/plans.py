from typing import List, Optional
from fastapi import APIRouter, Depends, Query, HTTPException
from sqlalchemy.orm import Session
from app.core.database import get_db
from app.models.plan import RechargePlan
from app.models.operator import Operator
from app.schemas.schemas import RechargePlanResponse

router = APIRouter()

@router.get("/plans", response_model=List[RechargePlanResponse])
def get_plans(
    operator_code: Optional[str] = Query(None, description="VI, JIO, AIRTEL, BSNL"),
    circle_code: Optional[str] = Query(None),
    category: Optional[str] = Query(None, description="UNLIMITED, DAILY_DATA, LONG_VALIDITY, POPULAR, etc."),
    min_price: Optional[float] = Query(None),
    max_price: Optional[float] = Query(None),
    min_validity: Optional[int] = Query(None),
    min_data: Optional[float] = Query(None),
    sort_by: Optional[str] = Query("price_asc", description="price_asc, price_desc, validity_desc, data_desc"),
    db: Session = Depends(get_db)
):
    query = db.query(RechargePlan).filter(RechargePlan.is_active == True)

    if operator_code:
        op = db.query(Operator).filter(Operator.code == operator_code.upper()).first()
        if op:
            query = query.filter(RechargePlan.operator_id == op.id)
        else:
            return []

    if category:
        query = query.filter(RechargePlan.plan_category == category.upper())

    if min_price is not None:
        query = query.filter(RechargePlan.price >= min_price)
    if max_price is not None:
        query = query.filter(RechargePlan.price <= max_price)
    if min_validity is not None:
        query = query.filter(RechargePlan.validity_days >= min_validity)
    if min_data is not None:
        query = query.filter(RechargePlan.daily_data_gb >= min_data)

    if sort_by == "price_desc":
        query = query.order_by(RechargePlan.price.desc())
    elif sort_by == "validity_desc":
        query = query.order_by(RechargePlan.validity_days.desc())
    elif sort_by == "data_desc":
        query = query.order_by(RechargePlan.daily_data_gb.desc())
    else:
        query = query.order_by(RechargePlan.price.asc())

    plans = query.all()
    # Enrich with operator info
    enriched = []
    for p in plans:
        resp = RechargePlanResponse.model_validate(p)
        if p.operator:
            resp.operator_name = p.operator.name
            resp.operator_code = p.operator.code
        enriched.append(resp)
    return enriched

@router.get("/plans/{plan_id}", response_model=RechargePlanResponse)
def get_plan(plan_id: str, db: Session = Depends(get_db)):
    plan = db.query(RechargePlan).filter(RechargePlan.id == plan_id).first()
    if not plan:
        raise HTTPException(status_code=404, detail="Recharge plan not found")
    resp = RechargePlanResponse.model_validate(plan)
    if plan.operator:
        resp.operator_name = plan.operator.name
        resp.operator_code = plan.operator.code
    return resp

@router.post("/plans/compare", response_model=List[RechargePlanResponse])
def compare_plans(plan_ids: List[str], db: Session = Depends(get_db)):
    plans = db.query(RechargePlan).filter(RechargePlan.id.in_(plan_ids)).all()
    enriched = []
    for p in plans:
        resp = RechargePlanResponse.model_validate(p)
        if p.operator:
            resp.operator_name = p.operator.name
            resp.operator_code = p.operator.code
        enriched.append(resp)
    return enriched
