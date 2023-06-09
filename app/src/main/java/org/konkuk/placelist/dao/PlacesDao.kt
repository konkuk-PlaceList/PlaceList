package org.konkuk.placelist.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import org.konkuk.placelist.domain.Place

@Dao
interface PlacesDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(place: Place): Long

    @Query("select * from places")
    fun getAll(): List<Place>

    @Update
    fun update(place: Place)

    @Delete
    fun delete(place: Place)

    @Query("select * from places p where p.place_id = :placeId")
    fun findByPlaceId(placeId: Long): Place?
}