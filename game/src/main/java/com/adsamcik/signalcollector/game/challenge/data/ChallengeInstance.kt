package com.adsamcik.signalcollector.game.challenge.data

import com.adsamcik.signalcollector.common.data.TrackerSession
import com.adsamcik.signalcollector.game.challenge.ChallengeDifficulty
import com.adsamcik.signalcollector.game.challenge.database.data.ChallengeEntry
import com.adsamcik.signalcollector.game.challenge.database.data.ChallengeEntryExtra

abstract class ChallengeInstance<ExtraData : ChallengeEntryExtra>(
		val data: ChallengeEntry,
		val title: String,
		protected val descriptionTemplate: String,
		val extra: ExtraData) {

	val startTime: Long get() = data.startTime

	val endTime: Long get() = data.endTime

	val difficulty: ChallengeDifficulty get() = data.difficulty

	/**
	 * Duration of the challenge
	 */
	val duration: Long get() = data.endTime - data.startTime

	abstract val description: String

	abstract val progress: Double

	protected abstract fun checkCompletionConditions(): Boolean

	/**
	 * Runs a batch process on a specified session
	 */
	protected abstract fun processSession(session: TrackerSession)

	fun process(session: TrackerSession, onChallengeCompletedListener: (ChallengeInstance<*>) -> Unit) {
		if (extra.isCompleted) return

		processSession(session)
		if (checkCompletionConditions()) {
			extra.isCompleted = true
			onChallengeCompletedListener.invoke(this)
		}
	}
}