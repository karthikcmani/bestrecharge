package com.bestrecharge.app.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.bestrecharge.app.data.local.entities.RechargePlanEntity;

import java.util.List;

@Dao
public interface RechargePlanDao {
    @Query("SELECT * FROM recharge_plans WHERE isActive = 1 ORDER BY price ASC")
    LiveData<List<RechargePlanEntity>> getAllPlansLiveData();

    @Query("SELECT * FROM recharge_plans WHERE isActive = 1 ORDER BY price ASC")
    List<RechargePlanEntity> getAllPlans();

    @Query("SELECT * FROM recharge_plans WHERE operatorCode = :operatorCode AND isActive = 1 ORDER BY price ASC")
    LiveData<List<RechargePlanEntity>> getPlansByOperatorLiveData(String operatorCode);

    @Query("SELECT * FROM recharge_plans WHERE operatorCode = :operatorCode AND isActive = 1 ORDER BY price ASC")
    List<RechargePlanEntity> getPlansByOperator(String operatorCode);

    @Query("SELECT * FROM recharge_plans WHERE id = :planId LIMIT 1")
    RechargePlanEntity getPlanById(String planId);

    @Query("SELECT * FROM recharge_plans WHERE id = :planId LIMIT 1")
    LiveData<RechargePlanEntity> getPlanByIdLiveData(String planId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<RechargePlanEntity> plans);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(RechargePlanEntity plan);

    @Query("DELETE FROM recharge_plans")
    void deleteAll();
}
