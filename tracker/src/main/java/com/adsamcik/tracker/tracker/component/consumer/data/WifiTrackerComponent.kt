package com.adsamcik.tracker.tracker.component.consumer.data

import android.content.Context
import android.location.Location
import android.net.wifi.WifiManager
import com.adsamcik.tracker.shared.base.data.MutableCollectionData
import com.adsamcik.tracker.shared.base.extension.LocationExtensions
import com.adsamcik.tracker.shared.base.extension.wifiManager
import com.adsamcik.tracker.tracker.component.DataTrackerComponent
import com.adsamcik.tracker.tracker.component.TrackerComponentRequirement
import com.adsamcik.tracker.tracker.data.collection.CollectionTempData
import com.adsamcik.tracker.tracker.data.collection.WifiScanData
import kotlin.math.abs

internal class WifiTrackerComponent : DataTrackerComponent {

	private var wifiManager: WifiManager? = null

	override val requiredData: Collection<TrackerComponentRequirement> = mutableListOf(
			TrackerComponentRequirement.WIFI
	)


	override suspend fun onDataUpdated(
			tempData: CollectionTempData,
			collectionData: MutableCollectionData
	) {
		val scanData = tempData.getWifiData(this)
		val locationData = tempData.tryGetLocationData()
		if (locationData != null) {
			val location = locationData.lastLocation
			val locations = locationData.locations
			if (locations.size >= 2) {
				val nearestLocation = locations.sortedBy {
					abs(scanData.relativeTimeNanos - it.elapsedRealtimeNanos)
				}
						.take(2)
				val firstIndex = if (nearestLocation[0].time < nearestLocation[1].time) 0 else 1

				val first = nearestLocation[firstIndex]
				val second = nearestLocation[(firstIndex + 1).rem(2)]
				setWifi(scanData, collectionData, first, second, first.distanceTo(second))
			} else {
				val previousLocation = locationData.previousLocation
				val distance = locationData.distance
				if (previousLocation != null && distance != null) {
					setWifi(scanData, collectionData, previousLocation, location, distance)
				}
			}
		} else {
			setWifi(scanData, collectionData)
		}
	}

	private fun setWifi(scanData: WifiScanData, collectionData: MutableCollectionData) {
		collectionData.setWifi(
				null,
				scanData.timeMillis,
				scanData.data,
				requireNotNull(wifiManager)
		)
	}

	private fun setWifi(
			scanData: WifiScanData,
			collectionData: MutableCollectionData,
			firstLocation: Location,
			secondLocation: Location,
			distanceBetweenFirstAndSecond: Float
	) {
		val timeDelta = (scanData.relativeTimeNanos - firstLocation.elapsedRealtimeNanos).toDouble() /
				(secondLocation.elapsedRealtimeNanos - firstLocation.elapsedRealtimeNanos).toDouble()
		val wifiDistance = distanceBetweenFirstAndSecond * timeDelta
		if (wifiDistance <= MAX_DISTANCE_TO_WIFI) {
			val interpolatedLocation = LocationExtensions.interpolateLocation(
					firstLocation,
					secondLocation, timeDelta
			)
			collectionData.setWifi(
					interpolatedLocation,
					scanData.timeMillis,
					scanData.data,
					requireNotNull(wifiManager)
			)
		}
	}

	override suspend fun onEnable(context: Context) {
		wifiManager = context.wifiManager
	}

	override suspend fun onDisable(context: Context) {
		wifiManager = null
	}


	companion object {
		private const val MAX_DISTANCE_TO_WIFI = 100
	}
}

