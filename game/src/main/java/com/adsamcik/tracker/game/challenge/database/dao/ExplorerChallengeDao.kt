package com.adsamcik.tracker.game.challenge.database.dao

import androidx.room.Dao
import androidx.room.Query
import com.adsamcik.tracker.game.challenge.data.entity.ExplorerChallengeEntity
import com.adsamcik.tracker.shared.base.database.dao.BaseDao

@Dao
interface ExplorerChallengeDao : BaseDao<ExplorerChallengeEntity> {

	@Query("SELECT * FROM challenge_explorer WHERE id == :id")
	fun get(id: Long): ExplorerChallengeEntity

	@Query("SELECT * FROM challenge_explorer WHERE entry_id == :entryId")
	fun getByEntry(entryId: Long): ExplorerChallengeEntity

	@Query("DELETE FROM challenge_explorer")
	fun deleteAll()
}
