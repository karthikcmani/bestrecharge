from typing import List
from fastapi import APIRouter, Depends
from sqlalchemy.orm import Session
from app.core.database import get_db
from app.models.operator import Operator, Circle
from app.schemas.schemas import OperatorResponse, CircleResponse

router = APIRouter()

@router.get("/operators", response_model=List[OperatorResponse])
def get_operators(db: Session = Depends(get_db)):
    return db.query(Operator).filter(Operator.is_active == True).all()

@router.get("/circles", response_model=List[CircleResponse])
def get_circles(db: Session = Depends(get_db)):
    return db.query(Circle).all()
