package com.adsamcik.signalcollector.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.adsamcik.signalcollector.data.DatabaseLocation

@Dao
interface LocationDataDao {
	@Query("SELECT * from location_data")
	fun getAll(): List<DatabaseLocation>

	@Insert(onConflict = OnConflictStrategy.ABORT)
	fun insert(locationData: DatabaseLocation)

	@Query("DELETE from location_data")
	fun deleteAll()

	@Query("SELECT * from location_data where time >= :from and time <= :to")
	fun getAllBetween(from: Long, to: Long): List<DatabaseLocation>

	@Query("SELECT * FROM location_data where lat >= :bottomLatitude and lon >= :leftLongitude and lat <= :topLatitude and lon <= :rightLongitude")
	fun getAllInside(topLatitude: Double, rightLongitude: Double, bottomLatitude: Double, leftLongitude: Double): List<DatabaseLocation>

	@Query("SELECT COUNT(id) FROM location_data")
	fun count(): LiveData<Int>

}