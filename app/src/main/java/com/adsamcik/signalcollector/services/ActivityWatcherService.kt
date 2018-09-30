package com.adsamcik.signalcollector.services

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import com.adsamcik.signalcollector.R
import com.adsamcik.signalcollector.activities.LaunchActivity
import com.adsamcik.signalcollector.enums.ResolvedActivities
import com.adsamcik.signalcollector.utility.Constants
import com.adsamcik.signalcollector.utility.Preferences
import com.adsamcik.signalcollector.utility.TrackingLocker

/**
 * Service used to keep device and ActivityService alive while automatic tracking might launch
 */
class ActivityWatcherService : LifecycleService() {
    private var notificationManager: NotificationManager? = null
    private var thread: Thread? = null

    private var activityInfo = ActivityService.lastActivity

    override fun onCreate() {
        super.onCreate()

        instance = this

        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        startForeground(NOTIFICATION_ID, updateNotification())

        ActivityService.requestAutoTracking(this, javaClass)

        thread = Thread {
            //Is not supposed to quit while, until service is stopped
            while (!Thread.currentThread().isInterrupted) {
                try {
                    Thread.sleep((500 + Preferences.getPref(this).getInt(Preferences.PREF_ACTIVITY_UPDATE_RATE, Preferences.DEFAULT_ACTIVITY_UPDATE_RATE * Constants.SECOND_IN_MILLISECONDS.toInt())).toLong())
                    val newActivityInfo = ActivityService.lastActivity
                    if (newActivityInfo != activityInfo) {
                        activityInfo = newActivityInfo
                        notificationManager!!.notify(NOTIFICATION_ID, updateNotification())
                    }
                } catch (e: InterruptedException) {
                    break
                }

            }

        }
        thread!!.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        ActivityService.removeActivityRequest(this, javaClass)
        instance = null
        thread!!.interrupt()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        return START_REDELIVER_INTENT
    }

    private fun updateNotification(): Notification {
        val intent = Intent(this, LaunchActivity::class.java)
        val builder = NotificationCompat.Builder(this, getString(R.string.channel_activity_watcher_id))
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setTicker(getString(R.string.notification_tracker_active_ticker))  // the done text
                .setWhen(System.currentTimeMillis())  // the time stamp
                .setVibrate(null)
                .setContentIntent(PendingIntent.getActivity(this, 0, intent, 0)) // The intent to send when the entry is clicked
                .setColor(ContextCompat.getColor(this, R.color.color_accent))

        builder.setContentTitle(getString(R.string.settings_activity_watcher_title))
        builder.setContentText(getString(R.string.notification_activity_watcher_info, activityInfo.getResolvedActivityName(this), activityInfo.confidence))
        when (activityInfo.resolvedActivity) {
            ResolvedActivities.IN_VEHICLE -> builder.setSmallIcon(R.drawable.ic_directions_car_white_24dp)
            ResolvedActivities.ON_FOOT -> builder.setSmallIcon(R.drawable.ic_directions_walk_white_24dp)
            ResolvedActivities.STILL -> builder.setSmallIcon(R.drawable.ic_accessibility_white_24dp)
            ResolvedActivities.UNKNOWN -> builder.setSmallIcon(R.drawable.ic_help_white_24dp)
        }

        return builder.build()
    }

    companion object {
        private const val NOTIFICATION_ID = -568465

        private var instance: ActivityWatcherService? = null

        /**
         * Returns preference whether this service should run
         */
        fun getServicePreference(context: Context) =
                Preferences.getPref(context).getBoolean(Preferences.PREF_ACTIVITY_WATCHER_ENABLED, Preferences.DEFAULT_ACTIVITY_WATCHER_ENABLED)

        /**
         * Checks if current [ActivityWatcherService] state is the one it should be in right now.
         *
         * @param context context
         */
        @Synchronized
        fun pokeWithCheck(context: Context) {
            val preference = getServicePreference(context)
            pokeWithCheck(context, preference)
        }

        /**
         * Pokes Activity Waker Service which checks if it should run
         * Ignores preference
         * Uses desired state instead of preference
         *
         * @param context context
         * @param desiredState desired service state
         */
        fun pokeWithCheck(context: Context, desiredState: Boolean) {
            poke(context, desiredState && !TrackerService.isServiceRunning.value && !TrackingLocker.isLocked.value)
        }

        /**
         * Pokes Activity Waker Service which checks if it should run
         *
         * @param context context
         */
        @Synchronized
        fun poke(context: Context, desiredState: Boolean) {
            if (desiredState) {
                if (instance == null)
                    ContextCompat.startForegroundService(context, Intent(context, ActivityWatcherService::class.java))
            } else if (instance != null) {
                instance!!.stopSelf()
            }
        }
    }
}