package com.adsamcik.signalcollector.utility

import android.app.Activity
import android.graphics.Color
import android.graphics.PointF
import android.support.v4.graphics.ColorUtils
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.core.content.edit
import com.adsamcik.draggable.DraggableImageButton
import com.adsamcik.signalcollector.R
import com.adsamcik.signalcollector.uitools.dpAsPx
import com.takusemba.spotlight.OnTargetStateChangedListener
import com.takusemba.spotlight.SimpleTarget
import com.takusemba.spotlight.Spotlight
import com.takusemba.spotlight.Target
import com.takusemba.spotlight.shapes.Circle
import com.takusemba.spotlight.shapes.RoundedRectangle

object Tips {
    const val HOME_TIPS = "home_tips"
    const val MAP_TIPS = "map_tips"

    var active = false

    fun getTipsPreferenceKey(key: String) = "tip:$key"

    fun showTips(activity: Activity, key: String) {
        val preferences = Preferences.getPref(activity)
        val tipsPreferenceKey = activity.getString(R.string.show_tips_key)
        if (preferences.getBoolean(tipsPreferenceKey, true) && !preferences.getBoolean(getTipsPreferenceKey(key), false)) {
            when (key) {
                HOME_TIPS -> showHomeTips(activity)
                MAP_TIPS -> showMapTips(activity)
                else -> throw RuntimeException("$key is not a valid tips key")
            }
        }
    }

    private fun showHomeTips(activity: Activity) {
        activity.run {
            val resources = resources
            val buttonData = SimpleTarget.ButtonData(resources.getString(R.string.next_part)) { _, spotlight ->
                spotlight.next()
            }

            val welcome = SimpleTarget.Builder(this)
                    .setTitle(resources.getString(R.string.tutorial_welcome_title))
                    .addButtonData(SimpleTarget.ButtonData(resources.getString(R.string.skip_tips)) { _, spotlight ->
                        spotlight.finishSpotlight()
                    })
                    .addButtonData(buttonData)
                    .setShape(Circle(PointF(0f, 0f), 0f))
                    .setDescription(resources.getString(R.string.tutorial_welcome_description)).build()

            //var radius = Math.sqrt(Math.pow(button_settings.height.toDouble(), 2.0) + Math.pow(button_settings.width.toDouble(), 2.0)) / 2
            // var point = PointF()
            var target = findViewById<View>(R.id.button_settings)
            val settingsButtonTarget = SimpleTarget.Builder(this)
                    .setPoint(target.x + target.pivotX, target.y + target.pivotY)
                    .setTitle(resources.getString(R.string.tutorial_settings_title))
                    .addButtonData(buttonData)
                    .setShape(Circle(target))
                    .setDescription(resources.getString(R.string.tutorial_settings_description))
                    .build()

            target = findViewById<View>(R.id.button_stats)
            //radius = Math.sqrt(Math.pow(button_stats.height.toDouble(), 2.0) + Math.pow(button_stats.width.toDouble(), 2.0)) / 2
            val statsButtonTarget = SimpleTarget.Builder(this)
                    .setPoint(target.x + target.pivotX, target.y + target.pivotY)
                    .setTitle(resources.getString(R.string.tutorial_stats_title))
                    .addButtonData(buttonData)
                    .setShape(Circle(target))
                    .setDescription(resources.getString(R.string.tutorial_stats_description))
                    .build()

            target = findViewById<View>(R.id.button_activity)
            //radius = Math.sqrt(Math.pow(button_activity.height.toDouble(), 2.0) + Math.pow(button_activity.width.toDouble(), 2.0)) / 2
            val activitiesButtonTarget = SimpleTarget.Builder(this)
                    .setPoint(target.x + target.pivotX, target.y + target.pivotY)
                    .setTitle(resources.getString(R.string.tutorial_activity_title))
                    .addButtonData(buttonData)
                    .setShape(Circle(target))
                    .setDescription(resources.getString(R.string.tutorial_activity_description))
                    .build()

            target = findViewById<View>(R.id.button_map)
            val mapButtonTarget = SimpleTarget.Builder(this)
                    .setPoint(target.x + target.pivotX, target.y + target.pivotY)
                    .setTitle(resources.getString(R.string.tips_map_overlay_title))
                    .addButtonData(buttonData)
                    .setShape(RoundedRectangle(target, 8.dpAsPx.toFloat(), target.height.toFloat()))
                    .setDescription(resources.getString(R.string.tutorial_map_description))
                    .build()

            Spotlight.with(this)
                    .setTargets(welcome, settingsButtonTarget, mapButtonTarget, statsButtonTarget, activitiesButtonTarget)
                    .setOverlayColor(ColorUtils.setAlphaComponent(Color.BLACK, 230))
                    .setAnimation(AccelerateDecelerateInterpolator())
                    .setOnSpotlightEndedListener {
                        Preferences.getPref(this).edit {
                            putBoolean(getTipsPreferenceKey(HOME_TIPS), true)
                        }
                        active = false
                    }
                    .start()

            active = true
        }
    }

    private fun showMapTips(activity: Activity) {
        activity.run {
            val buttonData = SimpleTarget.ButtonData(getString(R.string.next_part)) { _, spotlight ->
                spotlight.next()
            }

            var target = findViewById<View>(R.id.layout_map_controls)
            val settingsButtonTarget = SimpleTarget.Builder(this)
                    .setPoint(target.x + target.pivotX, target.y + target.pivotY)
                    .setTitle(getString(R.string.tips_map_search_title))
                    .addButtonData(SimpleTarget.ButtonData(resources.getString(R.string.skip_tips)) { _, spotlight ->
                        spotlight.finishSpotlight()
                    })
                    .addButtonData(buttonData)
                    .setShape(RoundedRectangle(target, 8.dpAsPx.toFloat(),8.dpAsPx.toFloat()))
                    .setDescription(getString(R.string.tips_map_search_description)).build()

            target = findViewById<View>(R.id.button_map_my_location)
            //radius = Math.sqrt(Math.pow(button_stats.height.toDouble(), 2.0) + Math.pow(button_stats.width.toDouble(), 2.0)) / 2
            val statsButtonTarget = SimpleTarget.Builder(this)
                    .setPoint(target.x + target.pivotX, target.y + target.pivotY)
                    .setTitle(getString(R.string.tips_map_my_location_title))
                    .addButtonData(buttonData)
                    .setShape(Circle(target))
                    .setDescription(getString(R.string.tips_map_my_location_description)).build()

            target = findViewById<View>(R.id.map_menu_button)
            //radius = Math.sqrt(Math.pow(button_activity.height.toDouble(), 2.0) + Math.pow(button_activity.width.toDouble(), 2.0)) / 2
            val activitiesButtonTarget = SimpleTarget.Builder(this)
                    .setPoint(target.x + target.pivotX, target.y + target.pivotY)
                    .setTitle(getString(R.string.tips_map_overlay_title))
                    .addButtonData(buttonData)
                    .setShape(RoundedRectangle(target, 8.dpAsPx.toFloat(),8.dpAsPx.toFloat()))
                    .setDescription(getString(R.string.tips_map_overlay_description))
                    .setOnSpotlightStartedListener(object : OnTargetStateChangedListener {
                        override fun onEnded(target: Target) {
                            val mapMenuButton = findViewById<DraggableImageButton>(R.id.map_menu_button)
                            if(mapMenuButton != null && mapMenuButton.payloads.isEmpty()) {
                                mapMenuButton.visibility = View.GONE
                            }
                        }

                        override fun onStarted(target: Target) {
                            val mapMenuButton = findViewById<DraggableImageButton>(R.id.map_menu_button)
                            if(mapMenuButton != null) {
                                mapMenuButton.visibility = View.VISIBLE
                            }
                        }
                    })
                    .build()

            Spotlight.with(this)
                    .setTargets(settingsButtonTarget, statsButtonTarget, activitiesButtonTarget)
                    .setOverlayColor(ColorUtils.setAlphaComponent(Color.BLACK, 230))
                    .setAnimation(AccelerateDecelerateInterpolator())
                    .setOnSpotlightEndedListener {
                        Preferences.getPref(this).edit {
                            putBoolean(getTipsPreferenceKey(MAP_TIPS), true)
                        }
                        active = false
                    }
                    .start()

            active = true
        }
    }
}