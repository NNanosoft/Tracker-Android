package com.adsamcik.tracker.tracker.component.timer

import android.Manifest
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import com.adsamcik.tracker.common.Time
import com.adsamcik.tracker.common.extension.locationManager
import com.adsamcik.tracker.common.preference.Preferences
import com.adsamcik.tracker.tracker.R
import com.adsamcik.tracker.tracker.component.TrackerTimerErrorData
import com.adsamcik.tracker.tracker.component.TrackerTimerErrorSeverity
import com.adsamcik.tracker.tracker.component.TrackerTimerReceiver

internal class AndroidLocationTrackerTimer : LocationTrackerTimer() {
	override val requiredPermissions: Collection<String>
		get() = listOf(Manifest.permission.ACCESS_FINE_LOCATION)

	private val locationListener: LocationListener = object : LocationListener {
		override fun onLocationChanged(location: Location) {
			onNewData(listOf(location))
		}

		override fun onStatusChanged(p0: String?, p1: Int, p2: Bundle?) = Unit

		override fun onProviderEnabled(p0: String?) = Unit

		override fun onProviderDisabled(p0: String?) {
			val errorData = TrackerTimerErrorData(TrackerTimerErrorSeverity.NOTIFY_USER,
					R.string.notification_looking_for_gps)
			receiver?.onError(errorData)
		}
	}

	override fun onEnable(context: Context, receiver: TrackerTimerReceiver) {
		super.onEnable(context, receiver)

		val preferences = Preferences.getPref(context)
		val minUpdateDelayInSeconds = preferences.getIntRes(R.string.settings_tracking_min_time_key,
				R.integer.settings_tracking_min_time_default)
		val minDistanceInMeters = preferences.getIntRes(R.string.settings_tracking_min_distance_key,
				R.integer.settings_tracking_min_distance_default)

		val locationManager = context.locationManager
		//It is checked by the component system
		@Suppress("missing_permission")
		locationManager.requestLocationUpdates(
				LocationManager.GPS_PROVIDER,
				minUpdateDelayInSeconds * Time.SECOND_IN_MILLISECONDS,
				minDistanceInMeters.toFloat(),
				locationListener,
				Looper.getMainLooper())
	}

	override fun onDisable(context: Context) {
		super.onDisable(context)

		context.locationManager.removeUpdates(locationListener)
	}
}

