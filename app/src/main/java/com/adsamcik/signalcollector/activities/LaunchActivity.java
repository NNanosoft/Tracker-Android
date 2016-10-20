package com.adsamcik.signalcollector.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import com.adsamcik.signalcollector.utility.Preferences;

public class LaunchActivity extends Activity {


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (Preferences.get(this).getBoolean(Preferences.HAS_BEEN_LAUNCHED, false))
			startActivity(new Intent(this, MainActivity.class));
		else
			startActivity(new Intent(this, IntroActivity.class));
		overridePendingTransition(0, 0);
		finish();
	}
}