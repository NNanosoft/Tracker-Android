package com.adsamcik.tracker.map.heatmap.creators

import com.adsamcik.tracker.common.database.data.Database2DLocationWeightedMinimal
import com.adsamcik.tracker.common.extension.isPowerOfTwo
import com.adsamcik.tracker.commonmap.CoordinateBounds
import com.adsamcik.tracker.map.heatmap.HeatmapColorScheme
import com.adsamcik.tracker.map.heatmap.HeatmapStamp
import com.adsamcik.tracker.map.heatmap.HeatmapTile

typealias InsideAndBetween = (from: Long,
                              to: Long,
                              topLatitude: Double,
                              rightLongitude: Double,
                              bottomLatitude: Double,
                              leftLongitude: Double) -> List<Database2DLocationWeightedMinimal>

typealias Inside = (topLatitude: Double,
                    rightLongitude: Double,
                    bottomLatitude: Double,
                    leftLongitude: Double) -> List<Database2DLocationWeightedMinimal>

internal interface HeatmapTileCreator {
	val getAllInsideAndBetween: InsideAndBetween

	val getAllInside: Inside

	val availableRange: LongRange

	val weightNormalizationValue: Double

	fun createHeatmapConfig(heatmapSize: Int, maxHeat: Float): HeatmapConfig

	fun generateStamp(heatmapSize: Int): HeatmapStamp

	fun scaleStampSize(baseSize: Float, quality: Float): Float = baseSize * quality

	fun getHeatmap(data: HeatmapData, from: Long, to: Long): HeatmapTile {
		return createHeatmap(data) { topLatitude, rightLongitude, bottomLatitude, leftLongitude ->
			getAllInsideAndBetween(from, to, topLatitude, rightLongitude, bottomLatitude, leftLongitude)
		}
	}

	fun getHeatmap(data: HeatmapData): HeatmapTile {
		return createHeatmap(data) { topLatitude, rightLongitude, bottomLatitude, leftLongitude ->
			getAllInside(topLatitude, rightLongitude, bottomLatitude, leftLongitude)
		}
	}

	private inline fun createHeatmap(data: HeatmapData,
	                          getLocations: Inside
	): HeatmapTile {
		assert(data.heatmapSize.isPowerOfTwo())
		assert(data.area.left < data.area.right)
		assert(data.area.bottom < data.area.top)

		val extendLatitude = data.area.height * (data.stamp.height.toDouble() / data.heatmapSize.toDouble())
		val extendLongitude = data.area.width * (data.stamp.width.toDouble() / data.heatmapSize.toDouble())

		assert(extendLatitude > 0)
		assert(extendLongitude > 0)

		val allInside = getLocations.invoke(
				data.area.top + extendLatitude,
				data.area.right + extendLongitude,
				data.area.bottom - extendLatitude,
				data.area.left - extendLongitude)
		//val allInside = dao.getAllInside(area.top, area.right, area.bottom, area.left)

		if (weightNormalizationValue != 0.0) {
			val weightNormalizationValue = weightNormalizationValue
			allInside.forEach {
				it.normalize(weightNormalizationValue)
			}
		}

		//todo add dynamic heat (most things are ready for it, just find use for it)
		val heatmap = HeatmapTile(data)
		heatmap.addAll(allInside.sortedWith(compareBy({ it.longitude }, { it.latitude })))
		return heatmap
	}
}

internal data class HeatmapConfig(
		val colorScheme: HeatmapColorScheme,
		val maxHeat: Float,
		val dynamicHeat: Boolean = false,
		val mergeFunction: (current: Float, input: Float, weight: Float) -> Float
)

internal data class HeatmapData(
		val config: HeatmapConfig,
		val stamp: HeatmapStamp,
		val heatmapSize: Int,
		val x: Int,
		val y: Int,
		val zoom: Int,
		val area: CoordinateBounds
)
