package com.adsamcik.tracker.tracker.component

import android.content.Context
import androidx.annotation.CallSuper
import androidx.lifecycle.Observer
import com.adsamcik.androidcomponents.common_preferences.observer.PreferenceObserver
import com.adsamcik.tracker.tracker.data.collection.MutableCollectionTempData

internal abstract class TrackerDataProducerComponent(private val changeReceiver: TrackerDataProducerObserver) {
	private val observer = Observer<Boolean> { changeReceiver.onStateChange(it, this) }

	protected abstract val keyRes: Int
	protected abstract val defaultRes: Int

	var isEnabled: Boolean = false
		private set

	fun onAttach(context: Context) {
		com.adsamcik.androidcomponents.common_preferences.observer.PreferenceObserver.observe(
				context,
				keyRes = keyRes,
				defaultRes = defaultRes,
				observer = observer
		)
	}

	fun onDetach(context: Context) {
		com.adsamcik.androidcomponents.common_preferences.observer.PreferenceObserver.removeObserver(context, keyRes, observer)
		if (isEnabled) onDisable(context)
	}

	@CallSuper
	open fun onEnable(context: Context) {
		isEnabled = true
	}

	@CallSuper
	open fun onDisable(context: Context) {
		isEnabled = false
	}

	abstract fun onDataRequest(tempData: MutableCollectionTempData)
}
