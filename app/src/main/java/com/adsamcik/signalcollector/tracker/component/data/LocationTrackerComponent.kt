package com.adsamcik.signalcollector.tracker.component.data

import android.content.Context
import android.location.Location
import com.adsamcik.signalcollector.R
import com.adsamcik.signalcollector.tracker.component.PreferenceDataTrackerComponent
import com.adsamcik.signalcollector.tracker.data.MutableCollectionData
import com.google.android.gms.location.LocationResult

class LocationTrackerComponent : PreferenceDataTrackerComponent() {
	override val enabledKeyRes: Int
		get() = R.string.settings_location_enabled_key
	override val enabledDefaultRes: Int
		get() = R.string.settings_location_enabled_default

	override fun onDestroy(context: Context) {}

	override fun onLocationUpdated(locationResult: LocationResult, previousLocation: Location?, distance: Float, collectionData: MutableCollectionData) {
		TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
	}

}