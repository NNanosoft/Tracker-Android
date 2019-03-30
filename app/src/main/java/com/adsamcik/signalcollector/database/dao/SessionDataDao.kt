package com.adsamcik.signalcollector.database.dao

import androidx.room.*
import com.adsamcik.signalcollector.statistics.data.TrackerSessionSummary
import com.adsamcik.signalcollector.statistics.data.TrackerSessionTimeSummary
import com.adsamcik.signalcollector.tracker.data.TrackerSession

@Dao
interface SessionDataDao {
	@Insert
	fun insert(session: TrackerSession): Long

	@Update
	fun update(session: TrackerSession)

	@Delete
	fun delete(session: TrackerSession)

	@Query("SELECT * FROM tracking_session")
	fun getAll(): List<TrackerSession>

	@Query("SELECT SUM(`end` - start) as duration, SUM(steps) as steps, SUM(collections) as collections, SUM(distance) as distance FROM tracking_session")
	fun getSummary(): TrackerSessionSummary

	@Query("SELECT SUM(`end` - start) as duration, SUM(steps) as steps, SUM(collections) as collections, SUM(distance) as distance FROM tracking_session WHERE start >= :from AND start <= :to")
	fun getSummary(from: Long, to: Long): TrackerSessionSummary


	//(round(timestamp / 86400000.0 - 0.5) * 86400000.0) should round down to date
	@Query("SELECT ((start / 86400000) * 86400000) as time, SUM(`end` - start) as duration, SUM(steps) as steps, SUM(collections) as collections, SUM(distance) as distance FROM tracking_session WHERE start >= :from AND start <= :to GROUP BY ((start / 86400000) * 86400000) ORDER BY time DESC")
	fun getSummaryByDays(from: Long, to: Long): List<TrackerSessionTimeSummary>

	@Query("SELECT * FROM tracking_session WHERE datetime(start, 'start of day') == datetime(:day, 'start of day')")
	fun getForDay(day: Long): List<TrackerSession>

	@Query("SELECT * FROM tracking_session WHERE start >= :from AND start <= :to ORDER BY start DESC")
	fun getBetween(from: Long, to: Long): List<TrackerSession>

	@Query("SELECT COUNT(*) FROM tracking_session")
	fun count(): Long

}