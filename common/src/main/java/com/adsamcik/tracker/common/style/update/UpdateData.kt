package com.adsamcik.tracker.common.style.update

import androidx.annotation.ColorInt

data class UpdateData(
		@ColorInt
		val fromColor: Int,
		@ColorInt
		val toColor: Int,
		val duration: Long,
		var progress: Long
)