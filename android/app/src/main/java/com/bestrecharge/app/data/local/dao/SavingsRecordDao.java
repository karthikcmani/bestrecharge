package com.bestrecharge.app.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.bestrecharge.app.data.local.entities.SavingsRecordEntity;

import java.util.List;

@Dao
public interface SavingsRecordDao {
    @Query("SELECT * FROM savings_records ORDER BY transactionDate DESC")
    LiveData<List<SavingsRecordEntity>> getAllRecordsLiveData();

    @Query("SELECT * FROM savings_records ORDER BY transactionDate DESC")
    List<SavingsRecordEntity> getAllRecords();

    @Query("SELECT * FROM savings_records WHERE id = :recordId LIMIT 1")
    SavingsRecordEntity getRecordById(String recordId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(SavingsRecordEntity record);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<SavingsRecordEntity> records);

    @Query("UPDATE savings_records SET status = :status, receivedCashback = :receivedAmount, effectiveCost = :effectiveCost WHERE id = :recordId")
    void updateRecordStatus(String recordId, String status, double receivedAmount, double effectiveCost);

    @Query("DELETE FROM savings_records WHERE id = :recordId")
    void delete(String recordId);
}
