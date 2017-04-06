package com.adsamcik.signalcollector.activities;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;
import android.util.MutableInt;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.adsamcik.signalcollector.NoiseTracker;
import com.adsamcik.signalcollector.R;

import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.util.ArrayList;

public class NoiseTestingActivity extends DetailActivity {

	private Button startStopButton;
	private NoiseGetter noiseGetter;
	private ArrayList<String> arrayList = new ArrayList<>();
	private ArrayAdapter<String> adapter;

	private MutableInt delayBetweenCollections = new MutableInt(3);

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		View v = getLayoutInflater().inflate(R.layout.fragment_noise_testing, createContentParent(false));
		startStopButton = (Button) findViewById(R.id.noiseTestStartStopButton);

		setTitle(R.string.settings_track_noise);

		TextView sampleIntervalTV = (TextView) v.findViewById(R.id.dev_text_noise_sample_size);

		SeekBar seekBar = (SeekBar) v.findViewById(R.id.dev_noise_sample_rate_seek_bar);
		seekBar.setMax(9);
		seekBar.incrementProgressBy(1);
		seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
				delayBetweenCollections.value = progress + 1;
				sampleIntervalTV.setText(getString(R.string.x_second_short, delayBetweenCollections.value));
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {

			}

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {

			}
		});

		seekBar.setProgress(delayBetweenCollections.value - 1);

		adapter = new ArrayAdapter(this, R.layout.spinner_item, arrayList);
		final ListView listView = ((ListView) v.findViewById(R.id.dev_noise_list_view));
		listView.setAdapter(adapter);

		startStopButton.setOnClickListener(view -> {
			if (noiseGetter == null) {
				noiseGetter = new NoiseGetter(this, adapter, listView, delayBetweenCollections);
				noiseGetter.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
				startStopButton.setText(getString(R.string.stop));
			} else {
				noiseGetter.cancel(false);
				noiseGetter = null;
				startStopButton.setText(getString(R.string.start));
			}
		});

		findViewById(R.id.dev_noise_clear_list).setOnClickListener((f) -> adapter.clear());

	}


	private class NoiseGetter extends AsyncTask<Void, Void, Void> {
		private final NoiseTracker noiseTracker;
		private final MutableInt delayBetweenSamples;
		private final ArrayAdapter<String> adapter;
		private final Activity activity;
		private final ListView listView;

		private NoiseGetter(@NonNull Activity activity, ArrayAdapter<String> adapter, ListView listView, MutableInt delayBetweenSamples) {
			this.delayBetweenSamples = delayBetweenSamples;
			noiseTracker = new NoiseTracker(activity);
			this.adapter = adapter;
			this.activity = activity;
			this.listView = listView;
		}

		@Override
		protected Void doInBackground(Void... params) {
			noiseTracker.start();
			while (delayBetweenSamples != null) {
				try {
					Thread.sleep(delayBetweenSamples.value * 1000);
					Log.d("TAG", delayBetweenSamples.value + " str");
				} catch (InterruptedException e) {
					e.printStackTrace();
				}

				if (isCancelled())
					break;

				activity.runOnUiThread(() -> {
					adapter.add(Integer.toString(noiseTracker.getSample(delayBetweenSamples.value)));
					listView.smoothScrollToPosition(adapter.getCount() - 1);
				});
			}
			noiseTracker.stop();
			return null;
		}
	}
}
