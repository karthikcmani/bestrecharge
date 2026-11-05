package com.bestrecharge.app.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.bestrecharge.app.data.local.entities.SimProfileEntity;

import java.util.List;

@Dao
public interface SimProfileDao {
    @Query("SELECT * FROM sim_profiles ORDER BY isPrimary DESC, id ASC")
    LiveData<List<SimProfileEntity>> getAllSimProfilesLiveData();

    @Query("SELECT * FROM sim_profiles ORDER BY isPrimary DESC, id ASC")
    List<SimProfileEntity> getAllSimProfiles();

    @Query("SELECT * FROM sim_profiles WHERE isPrimary = 1 LIMIT 1")
    LiveData<SimProfileEntity> getPrimarySimProfileLiveData();

    @Query("SELECT * FROM sim_profiles WHERE isPrimary = 1 LIMIT 1")
    SimProfileEntity getPrimarySimProfile();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertOrUpdate(SimProfileEntity simProfile);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<SimProfileEntity> simProfiles);

    @Delete
    void delete(SimProfileEntity simProfile);

    @Query("UPDATE sim_profiles SET isPrimary = 0")
    void resetAllPrimary();

    @Query("UPDATE sim_profiles SET isPrimary = 1 WHERE id = :simId")
    void setPrimary(String simId);

    @Query("DELETE FROM sim_profiles WHERE id LIKE 'sim_default_%'")
    void clearDummySims();

    @Query("DELETE FROM sim_profiles")
    void deleteAllSims();
}
