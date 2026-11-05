from datetime import datetime
from typing import List, Dict, Any, Optional, Tuple

class OfferCalculationEngine:
    """
    Deterministic Financial Offer Calculation Engine for BestRecharge.
    Strictly calculates initial payable amounts, verified guaranteed savings,
    conditional cashback potentials, and true effective costs.
    """

    @staticmethod
    def is_offer_expired(offer_expiry: Optional[datetime]) -> bool:
        if offer_expiry is None:
            return False
        return offer_expiry < datetime.utcnow()

    @staticmethod
    def evaluate_user_eligibility(
        offer_user_eligibility: str,
        provider: str,
        user_prefs: Optional[Dict[str, Any]]
    ) -> Tuple[bool, str]:
        """
        Returns (is_eligible, eligibility_label)
        """
        if offer_user_eligibility == "ALL":
            return True, "ELIGIBLE_ALL"
        
        if not user_prefs:
            return False, "UNKNOWN"

        provider_norm = provider.upper()
        if "PAYTM" in provider_norm:
            if offer_user_eligibility in ["NEW_USER", "INACTIVE_60_DAYS"]:
                is_eligible = bool(user_prefs.get("is_paytm_new_or_inactive_60d", False))
                return is_eligible, "ELIGIBLE_NEW_OR_INACTIVE_60D" if is_eligible else "INELIGIBLE_EXISTING_ACTIVE"
        
        if "BHIM" in provider_norm:
            if offer_user_eligibility == "NEW_USER":
                is_eligible = bool(user_prefs.get("is_bhim_new_user", True))
                return is_eligible, "ELIGIBLE_BHIM_NEW" if is_eligible else "INELIGIBLE_BHIM_REPEAT"

        if "PHONEPE" in provider_norm:
            if offer_user_eligibility == "NEW_USER":
                is_eligible = bool(user_prefs.get("is_phonepe_new_user", False))
                return is_eligible, "ELIGIBLE_PHONEPE_NEW" if is_eligible else "INELIGIBLE_REPEAT"

        return True, "ELIGIBILITY_STANDARD"

    @staticmethod
    def compute_single_offer_value(
        offer: Any,
        base_amount: float
    ) -> Tuple[float, float, float]:
        """
        Calculates:
        (immediate_discount, guaranteed_cashback, potential_cashback)
        """
        d_type = getattr(offer, "discount_type", "FLAT_CASHBACK")
        d_amount = max(0.0, float(getattr(offer, "discount_amount", 0.0) or 0.0))
        cb_amount = max(0.0, float(getattr(offer, "cashback_amount", 0.0) or 0.0))
        cb_percent = max(0.0, float(getattr(offer, "cashback_percentage", 0.0) or 0.0))
        max_cb = max(0.0, float(getattr(offer, "max_cashback", 0.0) or 0.0))

        immediate_discount = 0.0
        guaranteed_cb = 0.0
        potential_cb = 0.0

        if d_type == "FLAT_DISCOUNT":
            immediate_discount = d_amount
        elif d_type == "PERCENTAGE_DISCOUNT":
            calc_val = (cb_percent / 100.0) * base_amount
            if max_cb > 0:
                calc_val = min(calc_val, max_cb)
            immediate_discount = calc_val
        elif d_type == "FLAT_CASHBACK":
            guaranteed_cb = cb_amount
        elif d_type == "PERCENTAGE_CASHBACK":
            calc_val = (cb_percent / 100.0) * base_amount
            if max_cb > 0:
                calc_val = min(calc_val, max_cb)
            guaranteed_cb = calc_val
        elif d_type == "UPTO_CASHBACK":
            # "Up to ₹75" means conditional / scratch card lottery:
            # Min guaranteed is 0 or base, potential is cb_amount
            guaranteed_cb = 0.0
            potential_cb = cb_amount
        elif d_type == "ASSURED_UPTO":
            # Assured cashback up to X with minimum guaranteed
            guaranteed_cb = min(d_amount, cb_amount) if d_amount > 0 else 0.0
            potential_cb = max(0.0, cb_amount - guaranteed_cb)
        elif d_type == "REWARD_POINTS":
            # Loyalty points are not direct fiat cash
            immediate_discount = 0.0
            guaranteed_cb = 0.0
            potential_cb = 0.0

        return immediate_discount, guaranteed_cb, potential_cb

    @classmethod
    def evaluate_deal(
        cls,
        plan_price: float,
        operator_offer: Optional[Any],
        payment_offer: Optional[Any],
        reward_offer: Optional[Any],
        user_prefs: Optional[Dict[str, Any]] = None
    ) -> Optional[Dict[str, Any]]:
        """
        Combines up to 1 operator offer + 1 payment offer + 1 secondary reward offer
        and deterministically computes financial breakdown.
        """
        original_mrp = plan_price
        op_discount = 0.0
        stacked_titles = []

        # 1. Operator Discount Evaluation
        if operator_offer:
            if cls.is_offer_expired(getattr(operator_offer, "expiry_date", None)):
                return None
            min_txn = float(getattr(operator_offer, "min_transaction_amount", 0.0) or 0.0)
            if original_mrp < min_txn:
                return None # Plan MRP doesn't meet minimum
            
            op_imm, _, _ = cls.compute_single_offer_value(operator_offer, original_mrp)
            op_discount = op_imm
            stacked_titles.append(getattr(operator_offer, "title", "Operator Discount"))

        # Amount payable after operator discount
        immediate_payable = max(0.0, original_mrp - op_discount)

        # 2. Payment Offer Evaluation
        pay_discount = 0.0
        pay_guar_cb = 0.0
        pay_pot_cb = 0.0
        provider_name = "Standard Payment"
        eligibility_status = "ALL"
        confidence = "HIGH"

        if payment_offer:
            if cls.is_offer_expired(getattr(payment_offer, "expiry_date", None)):
                return None
            
            provider_name = getattr(payment_offer, "provider", "UPI")
            
            # Check user eligibility
            is_elig, elig_label = cls.evaluate_user_eligibility(
                getattr(payment_offer, "user_eligibility", "ALL"),
                provider_name,
                user_prefs
            )
            if not is_elig:
                return None
            eligibility_status = elig_label

            # Check min transaction threshold
            min_txn = float(getattr(payment_offer, "min_transaction_amount", 0.0) or 0.0)
            applies_to_discounted = getattr(payment_offer, "applies_to_discounted_value", False)
            eval_amount = immediate_payable if applies_to_discounted else original_mrp

            if eval_amount < min_txn:
                return None # Ineligible due to transaction amount

            pay_imm, pay_g_cb, pay_p_cb = cls.compute_single_offer_value(payment_offer, eval_amount)
            pay_discount = pay_imm
            pay_guar_cb = pay_g_cb
            pay_pot_cb = pay_p_cb
            stacked_titles.append(getattr(payment_offer, "title", "Payment Offer"))

        # 3. Secondary Loyalty Rewards
        reward_title = None
        reward_val = 0.0
        if reward_offer:
            reward_title = getattr(reward_offer, "title", "Loyalty Reward")
            reward_val = float(getattr(reward_offer, "cashback_amount", 0.0) or 0.0)
            stacked_titles.append(reward_title)

        # Final Financial Accounting
        # Out of pocket paid at payment gateway
        out_of_pocket = max(0.0, immediate_payable - pay_discount)
        
        # Guaranteed Effective Cost = Out of pocket - Guaranteed Cash
        guaranteed_effective_cost = max(0.0, out_of_pocket - pay_guar_cb)
        
        # Potential Effective Cost = Guaranteed Effective Cost - Potential/Conditional Cash
        potential_effective_cost = max(0.0, guaranteed_effective_cost - pay_pot_cb)

        total_guaranteed_savings = (original_mrp - guaranteed_effective_cost)
        total_potential_savings = (original_mrp - potential_effective_cost)

        # Explanation generator
        explanation_parts = []
        if op_discount > 0:
            explanation_parts.append(f"₹{op_discount:.0f} instant operator discount")
        if pay_discount > 0:
            explanation_parts.append(f"₹{pay_discount:.0f} payment discount")
        if pay_guar_cb > 0:
            explanation_parts.append(f"₹{pay_guar_cb:.0f} assured cashback via {provider_name}")
        if pay_pot_cb > 0:
            explanation_parts.append(f"up to ₹{pay_pot_cb:.0f} scratch-card cashback via {provider_name}")
        if reward_title:
            explanation_parts.append(f"plus {reward_title}")

        explanation = " + ".join(explanation_parts) if explanation_parts else "Standard recharge price with no additional promotional discounts."

        # Action URI
        action_intent_uri = "https://www.myvi.in"
        action_label = "Continue to Recharge"
        provider_lower = provider_name.lower()
        if "paytm" in provider_lower:
            action_intent_uri = "paytmmp://pay?pa=vi@upi"
            action_label = "Pay with Paytm UPI"
        elif "bhim" in provider_lower:
            action_intent_uri = "bhim://upi/pay"
            action_label = "Pay with BHIM UPI"
        elif "phonepe" in provider_lower:
            action_intent_uri = "phonepe://pay"
            action_label = "Pay with PhonePe"
        elif "gpay" in provider_lower or "google" in provider_lower:
            action_intent_uri = "tez://upi/pay"
            action_label = "Pay with Google Pay"
        elif "amazon" in provider_lower:
            action_intent_uri = "amazonpay://pay"
            action_label = "Pay with Amazon Pay"

        return {
            "original_mrp": original_mrp,
            "operator_discount": op_discount,
            "immediate_payable": immediate_payable,
            "recommended_payment_provider": provider_name,
            "applied_payment_discount": pay_discount,
            "guaranteed_cashback": pay_guar_cb,
            "potential_cashback": pay_pot_cb,
            "reward_points_title": reward_title,
            "reward_points_value": reward_val,
            "guaranteed_effective_cost": guaranteed_effective_cost,
            "potential_effective_cost": potential_effective_cost,
            "total_guaranteed_savings": total_guaranteed_savings,
            "total_potential_savings": total_potential_savings,
            "user_eligibility_status": eligibility_status,
            "confidence_level": confidence,
            "stacked_offers": stacked_titles,
            "explanation": explanation,
            "action_intent_uri": action_intent_uri,
            "action_label": action_label
        }

    @classmethod
    def find_best_deals(
        cls,
        plan_price: float,
        operator_code: str,
        offers: List[Any],
        user_prefs: Optional[Dict[str, Any]] = None
    ) -> List[Dict[str, Any]]:
        """
        Evaluates all permutations of valid operator + payment + reward offers
        and ranks them deterministically.
        """
        # Separate offers by category
        op_offers = [
            o for o in offers 
            if getattr(o, "category", "") == "OPERATOR" 
            and (getattr(o, "applicable_operators", "ALL") in ["ALL", operator_code.upper()])
            and not cls.is_offer_expired(getattr(o, "expiry_date", None))
        ]
        
        pay_offers = [
            o for o in offers 
            if getattr(o, "category", "") in ["UPI", "WALLET", "BANK_CARD"] 
            and (getattr(o, "applicable_operators", "ALL") in ["ALL", operator_code.upper()])
            and not cls.is_offer_expired(getattr(o, "expiry_date", None))
        ]

        reward_offers = [
            o for o in offers 
            if getattr(o, "category", "") == "REWARD" 
            and (getattr(o, "applicable_operators", "ALL") in ["ALL", operator_code.upper()])
        ]

        # Consider null operator offer and null payment offer
        op_candidates = op_offers + [None]
        pay_candidates = pay_offers + [None]
        reward_candidates = reward_offers[:1] + [None]

        evaluated_deals = []

        for op in op_candidates:
            for pay in pay_candidates:
                # Compatibility check: if op offer does not allow stacking with payment
                if op and pay and not getattr(op, "is_stackable_with_operator", True):
                    continue
                for rew in reward_candidates:
                    deal = cls.evaluate_deal(plan_price, op, pay, rew, user_prefs)
                    if deal:
                        evaluated_deals.append(deal)

        if not evaluated_deals:
            # Baseline deal with no discounts
            baseline = cls.evaluate_deal(plan_price, None, None, None, user_prefs)
            if baseline:
                evaluated_deals.append(baseline)

        # Deterministic Ranking:
        # 1. Highest Guaranteed Savings
        # 2. Lowest Guaranteed Effective Cost
        # 3. Highest Potential Savings
        # 4. Lowest Potential Effective Cost
        evaluated_deals.sort(
            key=lambda d: (
                -d["total_guaranteed_savings"],
                d["guaranteed_effective_cost"],
                -d["total_potential_savings"],
                d["potential_effective_cost"]
            )
        )

        # Deduplicate deals with identical financial breakdown and provider
        unique_deals = []
        seen = set()
        for d in evaluated_deals:
            key = (
                d["guaranteed_effective_cost"],
                d["potential_effective_cost"],
                d["recommended_payment_provider"],
                d["operator_discount"]
            )
            if key not in seen:
                seen.add(key)
                unique_deals.append(d)

        return unique_deals
