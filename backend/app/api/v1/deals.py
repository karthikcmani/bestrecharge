from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.orm import Session
from app.core.database import get_db
from app.models.plan import RechargePlan
from app.models.offer import Offer
from app.schemas.schemas import DealCalculateRequest, DealCalculateResponse, RechargePlanResponse, DealBreakdown
from app.calculators.engine import OfferCalculationEngine

router = APIRouter()

@router.post("/deals/calculate", response_model=DealCalculateResponse)
def calculate_deals(req: DealCalculateRequest, db: Session = Depends(get_db)):
    plan = db.query(RechargePlan).filter(RechargePlan.id == req.plan_id).first()
    if not plan:
        raise HTTPException(status_code=404, detail="Plan not found")

    operator_code = plan.operator.code if plan.operator else "VI"
    
    # Retrieve all active offers
    offers = db.query(Offer).filter(Offer.is_active == True).all()

    user_prefs_dict = req.user_preferences.model_dump() if req.user_preferences else None

    # Deterministic deal calculation
    deals = OfferCalculationEngine.find_best_deals(
        plan_price=plan.price,
        operator_code=operator_code,
        offers=offers,
        user_prefs=user_prefs_dict
    )

    if not deals:
        raise HTTPException(status_code=500, detail="Could not calculate deals for this plan")

    best_deal = deals[0]
    alternative_deals = deals[1:4] # Top 3 alternatives

    plan_resp = RechargePlanResponse.model_validate(plan)
    if plan.operator:
        plan_resp.operator_name = plan.operator.name
        plan_resp.operator_code = plan.operator.code

    return DealCalculateResponse(
        plan=plan_resp,
        best_deal=DealBreakdown(**best_deal),
        alternative_deals=[DealBreakdown(**d) for d in alternative_deals]
    )
