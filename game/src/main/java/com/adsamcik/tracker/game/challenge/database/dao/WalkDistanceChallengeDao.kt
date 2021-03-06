package com.adsamcik.tracker.game.challenge.database.dao

import androidx.room.Dao
import androidx.room.Query
import com.adsamcik.tracker.game.challenge.data.entity.WalkDistanceChallengeEntity
import com.adsamcik.tracker.shared.base.database.dao.BaseDao

@Dao
interface WalkDistanceChallengeDao : BaseDao<WalkDistanceChallengeEntity> {
	@Query("SELECT * FROM challenge_walk_distance WHERE id == :id")
	fun get(id: Long): WalkDistanceChallengeEntity

	@Query("SELECT * FROM challenge_walk_distance WHERE entry_id == :entryId")
	fun getByEntry(entryId: Long): WalkDistanceChallengeEntity

	@Query("DELETE FROM challenge_walk_distance")
	fun deleteAll()
}
