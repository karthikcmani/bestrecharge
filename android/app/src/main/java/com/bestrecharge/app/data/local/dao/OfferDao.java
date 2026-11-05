package com.bestrecharge.app.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.bestrecharge.app.data.local.entities.OfferEntity;

import java.util.List;

@Dao
public interface OfferDao {
    @Query("SELECT * FROM offers WHERE isActive = 1 ORDER BY cashbackAmount DESC, discountAmount DESC")
    LiveData<List<OfferEntity>> getAllOffersLiveData();

    @Query("SELECT * FROM offers WHERE isActive = 1 ORDER BY cashbackAmount DESC, discountAmount DESC")
    List<OfferEntity> getAllOffers();

    @Query("SELECT * FROM offers WHERE category = :category AND isActive = 1 ORDER BY cashbackAmount DESC")
    LiveData<List<OfferEntity>> getOffersByCategoryLiveData(String category);

    @Query("SELECT * FROM offers WHERE (applicableOperators LIKE '%' || :operatorCode || '%' OR applicableOperators = 'ALL') AND isActive = 1")
    List<OfferEntity> getOffersForOperator(String operatorCode);

    @Query("SELECT * FROM offers WHERE id = :offerId LIMIT 1")
    OfferEntity getOfferById(String offerId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<OfferEntity> offers);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(OfferEntity offer);

    @Query("UPDATE offers SET userTag = :tag WHERE id = :offerId")
    void updateOfferTag(String offerId, String tag);

    @Query("DELETE FROM offers")
    void deleteAll();
}
