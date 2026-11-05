package com.bestrecharge.app.data.local.entities;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "user_preferences")
public class UserPreferenceEntity {
    @PrimaryKey
    @NonNull
    public String key;
    public String value;

    public UserPreferenceEntity(@NonNull String key, String value) {
        this.key = key;
        this.value = value;
    }
}
