# Smart Offer Calculation Engine Specification

## 1. Mathematical Formulation & Definitions

The BestRecharge calculation engine is completely deterministic and strictly follows financial accounting principles:

### Core Definitions
- **Original Plan Price ($P_{orig}$)**: The telecom operator's listed plan MRP (e.g., ₹349).
- **Operator Discount ($D_{op}$)**: Immediate reduction provided by the telecom operator's app or portal (e.g. ₹15 instant deduction in the Vi App).
- **Initial Payable Amount ($A_{pay}$)**:
  $$A_{pay} = \max(0, P_{orig} - D_{op})$$
- **Guaranteed Payment Discount ($D_{pay}$)**: Direct discount applied immediately at the payment checkout screen before fund transfer.
- **Immediate Out-of-Pocket Expense ($E_{immediate}$)**:
  $$E_{immediate} = \max(0, A_{pay} - D_{pay})$$
- **Expected Cashback ($CB_{expected}$)**: Fiat cashback credited to the user's UPI-linked bank account or wallet within a defined settlement window.
  - *Guaranteed Cashback ($CB_{guar}$)*: Flat cashback or assured minimum cashback (e.g., "Min assured cashback ₹125").
  - *Conditional / Up-to Cashback ($CB_{cond}$)*: Variable lottery/scratch-card cashback (e.g., "Up to ₹75 cashback").
- **Reward Points / Non-Cash Value ($R_{val}$)**: Value of loyalty coins (e.g. Vi Stars, SuperCoins, Gold Coins). **Rule**: Reward points that cannot be redeemed as direct fiat cash against this recharge are NOT subtracted from the guaranteed effective cost.
- **Guaranteed Effective Cost ($C_{eff, guar}$)**:
  $$C_{eff, guar} = E_{immediate} - CB_{guar}$$
- **Potential Effective Cost ($C_{eff, pot}$)**:
  $$C_{eff, pot} = E_{immediate} - CB_{guar} - CB_{cond}$$

---

## 2. Offer Combination & Stacking Rules

Offers are categorized into distinct non-conflicting tiers:

| Tier | Type | Examples | Max Allowed per Deal |
|------|------|----------|----------------------|
| **Tier 1** | Operator App Discount | Vi Exclusive ₹15 off, JioMart Cashback | 1 |
| **Tier 2** | Payment Method / App Offer | Paytm UPI cashback, BHIM UPI cashback, PhonePe scratch card | 1 |
| **Tier 3** | Bank Card Instant Offer | HDFC Credit Card 5% off | 1 (mutually exclusive with Tier 2 UPI) |
| **Tier 4** | Loyalty Rewards | Vi Stars, Flipkart SuperCoins | Stacks with all tiers as secondary benefit |

### Incompatibility Constraints
1. **Payment App Mutual Exclusivity**: A transaction can only be routed through one payment application (e.g., Paytm UPI and BHIM UPI cannot be executed simultaneously on the same checkout).
2. **Transaction Base Calculation**:
   - Some payment offers compute eligibility against $P_{orig}$ (e.g., "Min transaction ₹179" evaluated against ₹349).
   - If an offer states "Cashback calculated on net payable value", percentage cashback is calculated on $A_{pay}$ instead of $P_{orig}$.
3. **Threshold Check**: If $A_{pay} < \text{MinTransactionAmount}$, the payment offer is disqualified.

---

## 3. Eligibility Matrix
- `ALL`: Open to every user.
- `NEW_USER`: Valid only for first-time app users.
- `INACTIVE_60_DAYS`: Valid for users with no transactions on the payment platform in the last 60 days (e.g., Paytm ₹125 assured cashback).
- `REPEAT_USER`: Existing users who receive alternative perks (e.g., 5% gold discount coupon).
- `UNKNOWN`: When user's historical state with that payment provider is not configured. Handled by flagging deal as "Eligibility: Unverified / Action Required" rather than assuming guaranteed savings.
