import uuid
from typing import List, Optional
from datetime import datetime
from fastapi import APIRouter, Depends, Query, HTTPException
from sqlalchemy.orm import Session
from app.core.database import get_db
from app.models.offer import Offer
from app.schemas.schemas import OfferResponse, OfferImportRequest
from app.services.ocr_parser_service import ScreenshotOfferParser

router = APIRouter()

@router.get("/offers", response_model=List[OfferResponse])
def get_offers(
    provider: Optional[str] = Query(None),
    category: Optional[str] = Query(None),
    operator_code: Optional[str] = Query(None),
    valid_only: bool = Query(True),
    db: Session = Depends(get_db)
):
    query = db.query(Offer).filter(Offer.is_active == True)

    if provider:
        query = query.filter(Offer.provider.ilike(f"%{provider}%"))

    if category:
        query = query.filter(Offer.category == category.upper())

    if operator_code:
        # Check if applicable_operators contains operator_code or ALL
        query = query.filter(
            (Offer.applicable_operators.ilike(f"%{operator_code}%")) | 
            (Offer.applicable_operators == "ALL")
        )

    if valid_only:
        now = datetime.utcnow()
        query = query.filter((Offer.expiry_date == None) | (Offer.expiry_date >= now))

    return query.all()

@router.get("/offers/{offer_id}", response_model=OfferResponse)
def get_offer(offer_id: str, db: Session = Depends(get_db)):
    offer = db.query(Offer).filter(Offer.id == offer_id).first()
    if not offer:
        raise HTTPException(status_code=404, detail="Offer not found")
    return offer

@router.post("/offers/import", response_model=OfferResponse)
def import_offer(req: OfferImportRequest, db: Session = Depends(get_db)):
    offer_id = f"imported_{uuid.uuid4().hex[:8]}"
    new_offer = Offer(
        id=offer_id,
        title=req.title,
        provider=req.provider,
        category=req.category,
        description=f"User imported offer for {req.provider}",
        discount_type=req.discount_type,
        discount_amount=req.discount_amount,
        cashback_amount=req.cashback_amount,
        cashback_percentage=req.cashback_percentage,
        min_transaction_amount=req.min_transaction_amount,
        max_cashback=req.max_cashback,
        user_eligibility=req.user_eligibility,
        payment_method_required=req.payment_method_required,
        applicable_operators=req.applicable_operators,
        terms_and_conditions=req.terms_and_conditions,
        verification_status=req.verification_status,
        confidence_score=90,
        is_active=True
    )
    db.add(new_offer)
    db.commit()
    db.refresh(new_offer)
    return new_offer

@router.post("/offers/parse-ocr")
def parse_ocr_text(payload: dict):
    raw_text = payload.get("text", "")
    if not raw_text:
        raise HTTPException(status_code=400, detail="Text field cannot be empty")
    return ScreenshotOfferParser.parse_extracted_text(raw_text)
