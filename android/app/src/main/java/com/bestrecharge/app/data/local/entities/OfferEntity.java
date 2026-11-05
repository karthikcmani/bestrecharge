package com.bestrecharge.app.data.local.entities;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "offers")
public class OfferEntity {
    @PrimaryKey
    @NonNull
    public String id;
    public String title;
    public String provider;             // Vi, Paytm, BHIM, PhonePe, Google Pay
    public String category;             // OPERATOR, UPI, WALLET, BANK_CARD, REWARD
    public String description;
    public String discountType;         // FLAT_DISCOUNT, FLAT_CASHBACK, UPTO_CASHBACK, REWARD_POINTS
    public double discountAmount;       // instant operator discount
    public double cashbackAmount;       // flat or max cashback
    public double minTransactionAmount;
    public double maxCashback;
    public String userEligibility;      // ALL, NEW_USER, INACTIVE_60_DAYS, REPEAT_USER
    public String paymentMethodRequired;// Paytm UPI, BHIM Payments App, PhonePe UPI, ANY
    public String applicableOperators;  // ALL, VI, JIO, AIRTEL, BSNL
    public boolean isStackableWithOperator;
    public String expiryDate;           // YYYY-MM-DD
    public String termsAndConditions;
    public String verificationStatus;   // OFFICIALLY_VERIFIED, USER_IMPORTED, PUBLICLY_SOURCED
    public String userTag;              // NONE, INTERESTED, USED, NOT_ELIGIBLE
    public boolean isActive;

    public OfferEntity(@NonNull String id, String title, String provider, String category,
                       String description, String discountType, double discountAmount,
                       double cashbackAmount, double minTransactionAmount, double maxCashback,
                       String userEligibility, String paymentMethodRequired,
                       String applicableOperators, boolean isStackableWithOperator,
                       String expiryDate, String termsAndConditions,
                       String verificationStatus, String userTag, boolean isActive) {
        this.id = id;
        this.title = title;
        this.provider = provider;
        this.category = category;
        this.description = description;
        this.discountType = discountType;
        this.discountAmount = discountAmount;
        this.cashbackAmount = cashbackAmount;
        this.minTransactionAmount = minTransactionAmount;
        this.maxCashback = maxCashback;
        this.userEligibility = userEligibility;
        this.paymentMethodRequired = paymentMethodRequired;
        this.applicableOperators = applicableOperators;
        this.isStackableWithOperator = isStackableWithOperator;
        this.expiryDate = expiryDate;
        this.termsAndConditions = termsAndConditions;
        this.verificationStatus = verificationStatus;
        this.userTag = userTag;
        this.isActive = isActive;
    }
}
