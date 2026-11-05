from fastapi import APIRouter
from app.api.v1.operators import router as operators_router
from app.api.v1.plans import router as plans_router
from app.api.v1.offers import router as offers_router
from app.api.v1.deals import router as deals_router
from app.api.v1.savings import router as savings_router

api_router = APIRouter()
api_router.include_router(operators_router, tags=["Operators & Circles"])
api_router.include_router(plans_router, tags=["Recharge Plans"])
api_router.include_router(offers_router, tags=["Offers"])
api_router.include_router(deals_router, tags=["Smart Deals Calculation"])
api_router.include_router(savings_router, tags=["Savings & History"])
