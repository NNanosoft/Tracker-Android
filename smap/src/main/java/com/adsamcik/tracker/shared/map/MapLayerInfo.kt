package com.adsamcik.tracker.shared.map

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class MapLayerInfo(
		val type: Class<out MapLayerLogic>,
		val nameRes: Int
) : Parcelable
