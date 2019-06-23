package com.adsamcik.signalcollector.preference.pages

import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceFragmentCompat
import com.adsamcik.signalcollector.R
import com.adsamcik.signalcollector.common.Time
import com.adsamcik.signalcollector.common.dialog.ConfirmDialog
import com.adsamcik.signalcollector.common.misc.extension.startActivity
import com.adsamcik.signalcollector.common.preference.Preferences
import com.adsamcik.signalcollector.debug.activity.ActivityRecognitionActivity
import com.adsamcik.signalcollector.debug.activity.StatusActivity
import com.adsamcik.signalcollector.notification.Notifications
import com.adsamcik.signalcollector.preference.findPreference
import com.adsamcik.signalcollector.preference.setOnClickListener
import java.util.*

class DebugPage : PreferencePage {
	override fun onExit(caller: PreferenceFragmentCompat) {}

	override fun onEnter(caller: PreferenceFragmentCompat) {
		with(caller) {
			caller.setOnClickListener(R.string.settings_debug_activity_key) {
				startActivity<ActivityRecognitionActivity> { }
			}

			caller.setOnClickListener(R.string.settings_activity_status_key) {
				startActivity<StatusActivity> { }
			}

			caller.findPreference(R.string.settings_hello_world_key).setOnPreferenceClickListener {
				val context = it.context
				val helloWorld = getString(R.string.dev_notification_dummy)
				val color = ContextCompat.getColor(context, R.color.color_primary)
				val rng = Random(Time.nowMillis)
				val facts = resources.getStringArray(R.array.lorem_ipsum_facts)
				val notificationBuilder = NotificationCompat.Builder(context, getString(R.string.channel_other_id))
						.setSmallIcon(R.drawable.ic_signals)
						.setTicker(helloWorld)
						.setColor(color)
						.setLights(color, 2000, 5000)
						.setContentTitle(getString(R.string.did_you_know))
						.setContentText(facts[rng.nextInt(facts.size)])
						.setWhen(Time.nowMillis)
				val notificationManager = it.context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
				notificationManager.notify(Notifications.uniqueNotificationId(), notificationBuilder.build())
				false
			}

			caller.findPreference(R.string.settings_clear_preferences_key).setOnPreferenceClickListener { pref ->
				val context = pref.context
				ConfirmDialog.create(context, pref.title.toString()) {
					Preferences.getPref(context).edit {
						clear()
					}
				}

				false
			}
		}
	}

}