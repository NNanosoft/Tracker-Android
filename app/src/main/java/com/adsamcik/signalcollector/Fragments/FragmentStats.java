package com.adsamcik.signalcollector.fragments;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.util.JsonReader;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.adsamcik.signalcollector.DataStore;
import com.adsamcik.signalcollector.Network;
import com.adsamcik.signalcollector.R;
import com.adsamcik.signalcollector.Setting;
import com.adsamcik.signalcollector.Table;
import com.adsamcik.signalcollector.data.Stat;
import com.adsamcik.signalcollector.data.StatData;
import com.adsamcik.signalcollector.data.StatDay;
import com.adsamcik.signalcollector.interfaces.ITabFragment;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import cz.msebera.android.httpclient.Header;

public class FragmentStats extends Fragment implements ITabFragment {
	private static final String GENERAL_STAT_FILE = "general_stats_cache_file";
	private static final String USER_STAT_FILE = "user_stats_cache_file";
	private static long lastRequest = 0;
	private final AsyncHttpClient client = new AsyncHttpClient();
	private View view;

	private Table weeklyStats;

	//todo add user stats
	//todo add last day stats

	//todo Improve stats updating
	private final AsyncHttpResponseHandler generalStatsResponseHandler = new AsyncHttpResponseHandler() {
		@Override
		public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
			if (responseBody != null && responseBody.length > 0)
				try {
					String data = new String(responseBody);
					DataStore.saveString(GENERAL_STAT_FILE, data);
					GenerateStatsTable(readJsonStream(new ByteArrayInputStream(responseBody)));
				} catch (IOException e) {
					Log.e("Error", e.getMessage());
				}
		}

		@Override
		public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {

		}
	};

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_stats, container, false);

		//todo show notification when offline to let know that the stats might be outdated
		this.view = view;
		long time = System.currentTimeMillis();
		time -= time % 600000;
		time += 120000;
		long diff = time - lastRequest;

		if (diff > 600000) {
			client.get(Network.URL_STATS, null, generalStatsResponseHandler);
			lastRequest = time;
		} else if (DataStore.exists(GENERAL_STAT_FILE)) {
			String data = DataStore.loadString(GENERAL_STAT_FILE);
			try {
				GenerateStatsTable(readJsonStream(new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8))));
			} catch (IOException e) {
				Log.e("Error", e.getMessage());
			}
		}

		weeklyStats = new Table(getContext(), 4, false);
		((LinearLayout) view.findViewById(R.id.statsLayout)).addView(weeklyStats.getLayout(), 0);

		return view;
	}

	private void GenerateStatsTable(List<Stat> stats) {
		Context c = getContext();
		LinearLayout ll = (LinearLayout) view.findViewById(R.id.statsLayout);
		for (int i = 0; i < stats.size(); i++) {
			Stat s = stats.get(i);
			Table table = new Table(c, s.statData.size(), s.showPosition);
			table.setTitle(s.name);
			for (int y = 0; y < s.statData.size(); y++) {
				StatData sd = s.statData.get(y);
				table.addRow().addData(sd.id, sd.value);
			}
			ll.addView(table.getLayout());
		}
	}

	@Override
	public boolean onEnter(Activity activity, FloatingActionButton fabOne, FloatingActionButton fabTwo) {
		if(weeklyStats == null)
			return false;
		//todo check if up to date
		fabOne.hide();
		fabTwo.hide();

		Setting.checkStatsDay(activity);

		Resources r = activity.getResources();

		weeklyStats.clear();
		weeklyStats.setTitle(r.getString(R.string.stats_weekly_title));
		StatDay weekStats = Setting.countStats(activity);
		weeklyStats.addRow().addData(r.getString(R.string.stats_weekly_collected_location), String.valueOf(weekStats.getLocations()));
		weeklyStats.addRow().addData(r.getString(R.string.stats_weekly_collected_wifi), String.valueOf(weekStats.getWifi()));
		weeklyStats.addRow().addData(r.getString(R.string.stats_weekly_collected_cell), String.valueOf(weekStats.getCell()));

		return true;
	}

	@Override
	public void onLeave() {

	}

	private List<Stat> readJsonStream(InputStream in) throws IOException {
		try (JsonReader reader = new JsonReader(new InputStreamReader(in, "UTF-8"))) {
			return readStatDataArray(reader);
		}
	}

	private List<Stat> readStatDataArray(JsonReader reader) throws IOException {
		List<Stat> l = new ArrayList<>();
		reader.beginArray();
		while (reader.hasNext())
			l.add(new Stat(reader));
		reader.endArray();
		return l;
	}
}

