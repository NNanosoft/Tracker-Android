package com.adsamcik.signalcollector.activities

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.support.annotation.RequiresApi
import android.support.v4.content.ContextCompat
import com.adsamcik.signalcollector.services.TrackerService
import com.adsamcik.signalcollector.utility.Shortcuts
import com.adsamcik.signalcollector.utility.Shortcuts.ShortcutType
import com.crashlytics.android.Crashlytics

/**
 * ShortcutActivity is activity that handles shortcut actions, so no UI is shown.
 */
class ShortcutActivity : Activity() {

    @RequiresApi(api = Build.VERSION_CODES.N_MR1)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val intent = intent
        if (intent.action == Shortcuts.ACTION) {
            val value = intent.getIntExtra(Shortcuts.ACTION_STRING, -1)
            if (value >= 0 && value < ShortcutType.values().size) {
                val type = ShortcutType.values()[value]
                val serviceIntent = Intent(this, TrackerService::class.java)

                when (type) {
                    Shortcuts.ShortcutType.START_COLLECTION -> {
                        serviceIntent.putExtra("backTrack", false)
                        ContextCompat.startForegroundService(this, serviceIntent)
                    }
                    Shortcuts.ShortcutType.STOP_COLLECTION -> if (TrackerService.isServiceRunning.value)
                        stopService(serviceIntent)
                }
            } else {
                Crashlytics.logException(Throwable("Invalid value $value"))
            }
        }
        finishAffinity()
    }
}
