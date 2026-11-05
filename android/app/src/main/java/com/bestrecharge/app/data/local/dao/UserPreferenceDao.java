package com.bestrecharge.app.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.bestrecharge.app.data.local.entities.UserPreferenceEntity;

@Dao
public interface UserPreferenceDao {
    @Query("SELECT value FROM user_preferences WHERE key = :key LIMIT 1")
    String getPreference(String key);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void setPreference(UserPreferenceEntity preference);
}
