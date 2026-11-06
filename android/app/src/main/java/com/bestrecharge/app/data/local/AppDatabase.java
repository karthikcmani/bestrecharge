package com.bestrecharge.app.data.local;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.bestrecharge.app.data.local.dao.OfferDao;
import com.bestrecharge.app.data.local.dao.RechargePlanDao;
import com.bestrecharge.app.data.local.dao.SavingsRecordDao;
import com.bestrecharge.app.data.local.dao.SimProfileDao;
import com.bestrecharge.app.data.local.dao.UserPreferenceDao;
import com.bestrecharge.app.data.local.entities.OfferEntity;
import com.bestrecharge.app.data.local.entities.RechargePlanEntity;
import com.bestrecharge.app.data.local.entities.SavingsRecordEntity;
import com.bestrecharge.app.data.local.entities.SimProfileEntity;
import com.bestrecharge.app.data.local.entities.UserPreferenceEntity;

@Database(entities = {
        SimProfileEntity.class,
        RechargePlanEntity.class,
        OfferEntity.class,
        SavingsRecordEntity.class,
        UserPreferenceEntity.class
}, version = 2, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    private static volatile AppDatabase INSTANCE;

    public abstract SimProfileDao simProfileDao();
    public abstract RechargePlanDao rechargePlanDao();
    public abstract OfferDao offerDao();
    public abstract SavingsRecordDao savingsRecordDao();
    public abstract UserPreferenceDao userPreferenceDao();

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class,
                            "bestrecharge_local.db"
                    ).fallbackToDestructiveMigration().build();
                }
            }
        }
        return INSTANCE;
    }
}
