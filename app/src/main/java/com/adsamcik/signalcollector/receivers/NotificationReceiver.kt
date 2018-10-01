package com.adsamcik.signalcollector.receivers


import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import com.adsamcik.signalcollector.services.TrackerService
import com.adsamcik.signalcollector.utility.Constants
import com.adsamcik.signalcollector.utility.FirebaseAssist
import com.adsamcik.signalcollector.utility.TrackingLocker
import com.google.firebase.analytics.FirebaseAnalytics

class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val value = intent.getIntExtra(ACTION_STRING, -1)
        val params = Bundle()
        params.putString(FirebaseAssist.PARAM_SOURCE, "notification")
        when (value) {
            STOP_TRACKING_ACTION -> {
                FirebaseAnalytics.getInstance(context).logEvent(FirebaseAssist.STOP_EVENT, params)
                context.stopService(Intent(context, TrackerService::class.java))
            }
            LOCK_RECHARGE_ACTION -> {
                TrackingLocker.lockUntilRecharge(context)
                FirebaseAnalytics.getInstance(context).logEvent(FirebaseAssist.STOP_TILL_RECHARGE_EVENT, params)
            }
            LOCK_TIME_ACTION -> {
                val minutes = intent.getIntExtra(STOP_MINUTES_EXTRA, -1)
                if (minutes > 0)
                    TrackingLocker.lockTimeLock(context, Constants.MINUTE_IN_MILLISECONDS * minutes)
            }
            else -> Log.w(TAG, "Unknown value $value")
        }
    }

    companion object {
        private const val TAG = "SignalsReceiver"
        const val ACTION_STRING = "action"
        const val STOP_MINUTES_EXTRA = "stopForMinutes"

        const val STOP_TRACKING_ACTION = 0
        const val LOCK_RECHARGE_ACTION = 1
        const val LOCK_TIME_ACTION = 2
    }
}
