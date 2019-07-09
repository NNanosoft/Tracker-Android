package com.adsamcik.signalcollector.tracker.data.collection

import android.annotation.SuppressLint
import android.net.wifi.ScanResult
import android.os.Build
import android.telephony.*
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import com.adsamcik.signalcollector.common.Reporter
import com.adsamcik.signalcollector.common.Time
import com.adsamcik.signalcollector.common.data.*
import com.adsamcik.signalcollector.common.data.CellInfo
import com.squareup.moshi.JsonClass
import java.util.*

/**
 * Object containing raw collection data.
 * Data in here might have been reordered to different objects
 * but they have not been modified in any way.
 */
@JsonClass(generateAdapter = true)
data class MutableCollectionData(
		override val time: Long = Time.nowMillis,
		override var location: Location? = null,
		override var activity: ActivityInfo? = null,
		override var cell: CellData? = null,
		override var wifi: WifiData? = null) : CollectionData {


	/**
	 * Sets collection location.
	 *
	 * @param location location
	 * @return this
	 */
	fun setLocation(location: android.location.Location) {
		this.location = Location(location)
	}

	/**
	 * Sets wifi and time of wifi collection.
	 *
	 * @param data data
	 * @param time time of collection
	 * @return this
	 */
	fun setWifi(location: android.location.Location, time: Long, data: Array<ScanResult>?) {
		if (data != null && time > 0) {
			val scannedWifi = data.map { scanResult -> WifiInfo(scanResult) }
			this.wifi = WifiData(Location(location), time, scannedWifi)
		}
	}

	fun addCell(telephonyManager: TelephonyManager) {
		val networkOperator = telephonyManager.networkOperator
		if (networkOperator.isNotEmpty()) {
			val mcc = networkOperator.substring(0, 3)
			val mnc = networkOperator.substring(3)

			val registeredOperator = RegisteredOperator(mcc, mnc, telephonyManager.networkOperatorName)

			addCell(telephonyManager, listOf(registeredOperator))
		}
	}

	@RequiresApi(22)
	@RequiresPermission(android.Manifest.permission.READ_PHONE_STATE)
	fun addCell(telephonyManager: TelephonyManager, subscriptionManager: SubscriptionManager) {
		val list = mutableListOf<RegisteredOperator>()
		subscriptionManager.activeSubscriptionInfoList.forEach {
			val mcc: String?
			val mnc: String?

			if (Build.VERSION.SDK_INT >= 29) {
				mcc = it.mccString
				mnc = it.mncString
			} else {
				@Suppress("deprecation")
				mcc = it.mcc.toString()
				@Suppress("deprecation")
				mnc = it.mnc.toString()
			}

			if (mcc != null && mnc != null) {
				list.add(RegisteredOperator(mcc, mnc, it.carrierName.toString()))
			}
		}

		addCell(telephonyManager, list)
	}

	private fun addCell(telephonyManager: TelephonyManager, registeredOperators: List<RegisteredOperator>) {
		//Annoying lint bug CoarseLocation permission is not required when android.permission.ACCESS_FINE_LOCATION is present
		@SuppressLint("MissingPermission") val cellInfo = telephonyManager.allCellInfo ?: return

		val phoneCount = if (Build.VERSION.SDK_INT >= 23) telephonyManager.phoneCount else 1
		val registeredCells = ArrayList<CellInfo>(phoneCount)

		for (ci in cellInfo) {
			if (ci.isRegistered) {
				convertToCellInfo(ci, registeredOperators)?.let {
					if (registeredCells.size == phoneCount - 1) return
					registeredCells.add(it)
				}
			}
		}


		this.cell = CellData(registeredCells.toTypedArray(), cellInfo.size)
	}

	private fun convertToCellInfo(cellInfo: android.telephony.CellInfo, registeredOperator: List<RegisteredOperator>): CellInfo? {
		return if (cellInfo is CellInfoLte) {
			val operator = registeredOperator.find { it.sameNetwork(cellInfo) }
			if (operator != null) {
				CellInfo.newInstance(cellInfo, operator.name)
			} else {
				CellInfo.newInstance(cellInfo, null)
			}
		} else if (Build.VERSION.SDK_INT >= 29 && cellInfo is CellInfoNr) {
			val operator = registeredOperator.find { it.sameNetwork(cellInfo) }
			if (operator != null) {
				CellInfo.newInstance(cellInfo, operator.name)
			} else {
				CellInfo.newInstance(cellInfo, null)
			}
		} else if (cellInfo is CellInfoGsm) {
			val operator = registeredOperator.find { it.sameNetwork(cellInfo) }
			if (operator != null) {
				CellInfo.newInstance(cellInfo, operator.name)
			} else {
				CellInfo.newInstance(cellInfo, null)
			}
		} else if (cellInfo is CellInfoWcdma) {
			val operator = registeredOperator.find { it.sameNetwork(cellInfo) }
			if (operator != null) {
				CellInfo.newInstance(cellInfo, operator.name)
			} else {
				CellInfo.newInstance(cellInfo, null)
			}
		} else if (cellInfo is CellInfoCdma) {
			val operator = registeredOperator.find { it.sameNetwork(cellInfo) }
			if (operator != null) {
				CellInfo.newInstance(cellInfo, operator.name)
			} else {
				CellInfo.newInstance(cellInfo, null)
			}
		} else {
			Reporter.report(Throwable("UNKNOWN CELL TYPE ${cellInfo.javaClass.simpleName}"))
			null
		}
	}

	private class RegisteredOperator(val mcc: String, val mnc: String, val name: String) {
		fun sameNetwork(info: CellInfoLte): Boolean {
			val identity = info.cellIdentity
			return if (Build.VERSION.SDK_INT >= 28) {
				identity.mncString == mnc && identity.mccString == mcc
			} else {
				@Suppress("deprecation")
				identity.mnc.toString() == mnc && identity.mcc.toString() == mcc
			}
		}

		fun sameNetwork(info: CellInfoCdma): Boolean {
			//todo add cdma network matching (can be done if only 1 cell is registered)
			return false
		}

		fun sameNetwork(info: CellInfoGsm): Boolean {
			val identity = info.cellIdentity
			return if (Build.VERSION.SDK_INT >= 28) {
				identity.mncString == mnc && identity.mccString == mcc
			} else {
				@Suppress("deprecation")
				identity.mnc.toString() == mnc && identity.mcc.toString() == mcc
			}
		}

		fun sameNetwork(info: CellInfoWcdma): Boolean {
			val identity = info.cellIdentity
			return if (Build.VERSION.SDK_INT >= 28) {
				identity.mncString == mnc && identity.mccString == mcc
			} else {
				@Suppress("deprecation")
				identity.mnc.toString() == mnc && identity.mcc.toString() == mcc
			}
		}

		@RequiresApi(29)
		fun sameNetwork(info: CellInfoNr): Boolean {
			val identity = info.cellIdentity as CellIdentityNr
			return identity.mncString == mnc && identity.mccString == mcc
		}
	}
}