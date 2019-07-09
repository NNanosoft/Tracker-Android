package com.adsamcik.signalcollector.app

import android.os.Build
import com.adsamcik.signalcollector.AppModuleInitializer
import com.adsamcik.signalcollector.activity.service.ActivityWatcherService
import com.adsamcik.signalcollector.common.module.ModuleInitializer
import com.adsamcik.signalcollector.module.Module
import com.adsamcik.signalcollector.notification.NotificationChannels
import com.adsamcik.signalcollector.shortcut.Shortcuts
import com.adsamcik.signalcollector.tracker.locker.TrackerLocker
import com.google.android.play.core.splitcompat.SplitCompatApplication

class Application : SplitCompatApplication() {
	private fun initializeModules() {
		AppModuleInitializer().initialize(this)

		val activeModules = Module.getActiveModuleInfo(this)
		activeModules.forEach {
			try {
				val initializer = it.module.loadClass<ModuleInitializer>("${it.module.moduleName.capitalize()}${ModuleInitializer::class.java.simpleName}")
				initializer.newInstance().initialize(this)
			} catch (e: ClassNotFoundException) {
				//it's fine, do nothing
			}
		}
	}

	private fun initializeClasses() {
		if (Build.VERSION.SDK_INT >= 25) Shortcuts.initializeShortcuts(this)
		if (Build.VERSION.SDK_INT >= 26) NotificationChannels.prepareChannels(this)

		ActivityWatcherService.poke(this)
		TrackerLocker.initializeFromPersistence(this)
	}

	override fun onCreate() {
		super.onCreate()

		initializeClasses()
		initializeModules()
	}

}