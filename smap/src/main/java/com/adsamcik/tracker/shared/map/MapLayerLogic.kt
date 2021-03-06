package com.adsamcik.tracker.shared.map

import android.content.Context
import androidx.lifecycle.LiveData
import com.google.android.gms.maps.GoogleMap

/**
 * Map layer logic definition
 */
interface MapLayerLogic {
	/**
	 * Supports automatic updating
	 */
	val supportsAutoUpdate: Boolean

	/**
	 * Data date range
	 */
	var dateRange: LongRange

	/**
	 * Quality of layer as value from 0 to 1.
	 * Can be resolution or point reduction.
	 */
	var quality: Float

	/**
	 * Available data range for layer.
	 */
	val availableRange: LongRange

	/**
	 * Number of tiles being generated by the layer.
	 */
	val tileCountInGeneration: LiveData<Int>

	/**
	 * Layer information.
	 */
	val layerInfo: MapLayerInfo

	/**
	 * List of main colors that this map layer uses.
	 * Layers can use other colors as interpolation between these colors.
	 */
	fun colorList(): List<Int>

	/**
	 * Layer data.
	 */
	fun layerData(): MapLayerData

	/**
	 * Called when layer is enabled.
	 */
	fun onEnable(context: Context, map: GoogleMap, quality: Float)

	/**
	 * Called when layer is disabled
	 */
	fun onDisable(map: GoogleMap)

	/**
	 * Called when layer should be updated.
	 */
	fun update(context: Context)
}
