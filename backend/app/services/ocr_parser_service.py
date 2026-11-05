import re
from datetime import datetime, timedelta
from typing import Dict, Any, Optional

class ScreenshotOfferParser:
    """
    Heuristic rule-based extractor that converts OCR text from telecom/UPI recharge
    screenshots into structured BestRecharge Offer entities.
    """

    @classmethod
    def parse_extracted_text(cls, raw_text: str) -> Dict[str, Any]:
        text = raw_text.replace("\n", " ").replace("₹", " Rs ").replace("Rs.", " Rs ")
        text_lower = text.lower()

        # 1. Identify Provider
        provider = "UPI"
        if "paytm" in text_lower:
            provider = "Paytm"
        elif "bhim" in text_lower:
            provider = "BHIM"
        elif "phonepe" in text_lower:
            provider = "PhonePe"
        elif "google pay" in text_lower or "gpay" in text_lower:
            provider = "Google Pay"
        elif "amazon pay" in text_lower:
            provider = "Amazon Pay"
        elif "vi upi" in text_lower or "app exclusive" in text_lower:
            provider = "Vi"

        # 2. Identify Category
        category = "UPI"
        if provider == "Vi":
            category = "OPERATOR"
        elif "credit card" in text_lower or "rupay" in text_lower or "debit" in text_lower:
            category = "BANK_CARD"

        # 3. Extract Cashback / Discount Amounts
        # Check for "saved 15" or "save 15"
        instant_save_match = re.search(r'(?:saved|save|off)\s*(?:rs|rs\.)?\s*(\d+)', text_lower)
        instant_discount = float(instant_save_match.group(1)) if instant_save_match else 0.0

        # Check for cashback amounts: "cashback of rs 125", "cashback upto rs 75", "assured rs 100 cashback"
        cb_match = re.search(r'(?:cashback|cash back)[^\d]{0,20}(?:rs|rs\.)?\s*(\d+)', text_lower)
        if not cb_match:
            cb_match = re.search(r'(?:rs|rs\.)?\s*(\d+)\s*(?:cashback|cash back)', text_lower)
        
        cashback_amount = float(cb_match.group(1)) if cb_match else 0.0

        # Check if "up to" / "upto" is mentioned
        is_upto = "upto" in text_lower or "up to" in text_lower
        discount_type = "FLAT_CASHBACK"
        if instant_discount > 0 and cashback_amount == 0:
            discount_type = "FLAT_DISCOUNT"
        elif is_upto:
            discount_type = "UPTO_CASHBACK"
        elif "min assured cashback" in text_lower or "assured" in text_lower:
            discount_type = "FLAT_CASHBACK"

        # 4. Extract Minimum Transaction Amount
        # e.g. "min transaction value of rs 179" or "min transaction of rs 99"
        min_txn_match = re.search(r'min(?:imum)?\s*transaction[^\d]{0,20}(?:rs|rs\.)?\s*(\d+)', text_lower)
        if not min_txn_match:
            min_txn_match = re.search(r'(?:min|min\.)\s*(?:rs|rs\.)?\s*(\d+)', text_lower)
        min_transaction = float(min_txn_match.group(1)) if min_txn_match else 0.0

        # 5. Extract User Eligibility
        user_eligibility = "ALL"
        if "last 60 days" in text_lower or "not used paytm anywhere in the last 60 days" in text_lower:
            user_eligibility = "INACTIVE_60_DAYS"
        elif "first time" in text_lower or "new user" in text_lower:
            user_eligibility = "NEW_USER"

        # 6. Extract Payment Method Required
        payment_method = "ANY"
        if "paytm upi lite" in text_lower:
            payment_method = "Paytm UPI Lite"
        elif "paytm upi" in text_lower:
            payment_method = "Paytm UPI"
        elif "bhim payments app" in text_lower or "bhim upi" in text_lower:
            payment_method = "BHIM Payments App"
        elif "rupay" in text_lower:
            payment_method = "RuPay Card"

        # 7. Expiry Date (e.g. "30th november 2026")
        expiry_date = None
        date_match = re.search(r'to\s*(\d{1,2})(?:st|nd|rd|th)?\s*([a-zA-Z]+)\s*(\d{4})', text_lower)
        if date_match:
            try:
                day = int(date_match.group(1))
                month_str = date_match.group(2)[:3]
                year = int(date_match.group(3))
                month_map = {
                    "jan": 1, "feb": 2, "mar": 3, "apr": 4, "may": 5, "jun": 6,
                    "jul": 7, "aug": 8, "sep": 9, "oct": 10, "nov": 11, "dec": 12
                }
                month = month_map.get(month_str, 11)
                expiry_date = datetime(year, month, day, 23, 59, 59)
            except Exception:
                expiry_date = None

        title = f"{provider} ₹{int(cashback_amount or instant_discount)} {'Cashback' if cashback_amount > 0 else 'Discount'}"
        if is_upto:
            title = f"{provider} Up to ₹{int(cashback_amount)} Cashback"

        return {
            "title": title,
            "provider": provider,
            "category": category,
            "discount_type": discount_type,
            "discount_amount": instant_discount,
            "cashback_amount": cashback_amount,
            "min_transaction_amount": min_transaction,
            "max_cashback": cashback_amount,
            "user_eligibility": user_eligibility,
            "payment_method_required": payment_method,
            "expiry_date": expiry_date,
            "terms_and_conditions": raw_text.strip(),
            "verification_status": "USER_CONFIRMATION_REQUIRED",
            "confidence_score": 85
        }
