package com.adsamcik.signalcollector.activities;

import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.adsamcik.signalcollector.utility.DataStore;

public class DebugFileActivity extends DetailActivity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		String fileName = getIntent().getStringExtra("fileName");
		setTitle(fileName);
		DataStore.setContext(getApplicationContext());
		TextView tv = new TextView(this);
		LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		tv.setLayoutParams(layoutParams);
		tv.setText(DataStore.loadString(fileName));
		getLayout().addView(tv);
	}
}