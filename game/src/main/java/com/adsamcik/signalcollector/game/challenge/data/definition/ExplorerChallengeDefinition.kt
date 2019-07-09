package com.adsamcik.signalcollector.game.challenge.data.definition

import android.content.Context
import com.adsamcik.signalcollector.common.Time
import com.adsamcik.signalcollector.game.R
import com.adsamcik.signalcollector.game.challenge.data.ChallengeDefinition
import com.adsamcik.signalcollector.game.challenge.data.ChallengeType
import com.adsamcik.signalcollector.game.challenge.data.builder.ExplorerChallengeBuilder
import com.adsamcik.signalcollector.game.challenge.data.instance.ExplorerChallengeInstance

class ExplorerChallengeDefinition : ChallengeDefinition<ExplorerChallengeInstance>(
		R.string.challenge_explorer_title,
		R.string.challenge_explorer_description,
		Time.WEEK_IN_MILLISECONDS) {
	val defaultLocationCount: Int = 1000

	override val type: ChallengeType = ChallengeType.Explorer

	override fun newInstance(context: Context, startAt: Long): ExplorerChallengeInstance {
		return ExplorerChallengeBuilder(this).build(context, startAt)
	}

}