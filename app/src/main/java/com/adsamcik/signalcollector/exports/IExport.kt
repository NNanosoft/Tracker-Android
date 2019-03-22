package com.adsamcik.signalcollector.exports

import com.adsamcik.signalcollector.database.data.DatabaseLocation
import java.io.File

interface IExport {
	fun export(locationData: List<DatabaseLocation>, destinationDirectory: File, desiredName: String): ExportResult
}