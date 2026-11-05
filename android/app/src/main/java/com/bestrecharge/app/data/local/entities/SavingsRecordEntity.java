package com.bestrecharge.app.data.local.entities;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "savings_records")
public class SavingsRecordEntity {
    @PrimaryKey
    @NonNull
    public String id;
    public String mobileNumber;
    public String operator;
    public String circle;
    public String planName;
    public double originalMrp;
    public double operatorDiscount;
    public double amountPaid;
    public String paymentProvider;
    public String appliedOfferTitle;
    public double expectedCashback;
    public double receivedCashback;
    public String status; // EXPECTED, PENDING, RECEIVED, REJECTED, EXPIRED
    public double effectiveCost;
    public String transactionDate;
    public String notes;

    public SavingsRecordEntity(@NonNull String id, String mobileNumber, String operator,
                               String circle, String planName, double originalMrp,
                               double operatorDiscount, double amountPaid,
                               String paymentProvider, String appliedOfferTitle,
                               double expectedCashback, double receivedCashback,
                               String status, double effectiveCost,
                               String transactionDate, String notes) {
        this.id = id;
        this.mobileNumber = mobileNumber;
        this.operator = operator;
        this.circle = circle;
        this.planName = planName;
        this.originalMrp = originalMrp;
        this.operatorDiscount = operatorDiscount;
        this.amountPaid = amountPaid;
        this.paymentProvider = paymentProvider;
        this.appliedOfferTitle = appliedOfferTitle;
        this.expectedCashback = expectedCashback;
        this.receivedCashback = receivedCashback;
        this.status = status;
        this.effectiveCost = effectiveCost;
        this.transactionDate = transactionDate;
        this.notes = notes;
    }
}
