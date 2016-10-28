package com.adsamcik.signalcollector.fragments;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.adsamcik.signalcollector.utility.Assist;
import com.adsamcik.signalcollector.utility.NetworkLoader;
import com.adsamcik.signalcollector.utility.Preferences;
import com.adsamcik.signalcollector.R;
import com.adsamcik.signalcollector.activities.RecentUploadsActivity;
import com.adsamcik.signalcollector.utility.DataStore;
import com.adsamcik.signalcollector.utility.Network;
import com.adsamcik.signalcollector.utility.Success;
import com.adsamcik.signalcollector.utility.Table;
import com.adsamcik.signalcollector.data.UploadStats;
import com.adsamcik.signalcollector.data.Stat;
import com.adsamcik.signalcollector.data.StatData;
import com.adsamcik.signalcollector.data.StatDay;
import com.adsamcik.signalcollector.interfaces.ITabFragment;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class FragmentStats extends Fragment implements ITabFragment {
	private Table weeklyStats;
	private Table lastUpload;
	private ArrayList<Table> publicStats = null;
	private View view;

	private SwipeRefreshLayout refreshLayout;

	//todo add user stats

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Context context = getContext();
		weeklyStats = new Table(context, 4, false, ContextCompat.getColor(context, R.color.textPrimary));
	}

	@Override
	public void onDestroyView() {
		((LinearLayout) view.findViewById(R.id.statsLayout)).removeAllViews();
		if (publicStats != null)
			publicStats.clear();
		super.onDestroyView();
	}

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		view = inflater.inflate(R.layout.fragment_stats, container, false);
		Resources r = getResources();

		boolean lastUploadAvailable = false;

		UploadStats us = DataStore.loadLastObjectJsonArrayAppend(DataStore.RECENT_UPLOADS_FILE, UploadStats.class);
		if (us != null && Assist.getAgeInDays(us.time) < 30) {
			lastUpload = RecentUploadsActivity.GenerateTableForUploadStat(us, (LinearLayout) view.findViewById(R.id.statsLayout), getContext(), getResources().getString(R.string.most_recent_upload));
			lastUpload.addButton(getString(R.string.more_uploads), v -> {
				Intent intent = new Intent(getContext(), RecentUploadsActivity.class);
				startActivity(intent);
			});
			lastUploadAvailable = true;
		} else {
			if (lastUpload != null)
				lastUpload.destroy(getActivity());
		}

		new Thread(DataStore::removeOldRecentUploads).start();

		Preferences.checkStatsDay(getActivity());

		weeklyStats.clear();
		weeklyStats.addTitle(r.getString(R.string.stats_weekly_title));
		StatDay weekStats = Preferences.countStats(getActivity());
		weeklyStats.addRow().addData(r.getString(R.string.stats_weekly_minutes), String.valueOf(weekStats.getMinutes()));
		weeklyStats.addRow().addData(r.getString(R.string.stats_weekly_uploaded), Assist.humanReadableByteCount(weekStats.getUploaded(), true));
		weeklyStats.addRow().addData(r.getString(R.string.stats_weekly_collected_location), String.valueOf(weekStats.getLocations()));
		weeklyStats.addRow().addData(r.getString(R.string.stats_weekly_collected_wifi), String.valueOf(weekStats.getWifi()));
		weeklyStats.addRow().addData(r.getString(R.string.stats_weekly_collected_cell), String.valueOf(weekStats.getCell()));
		weeklyStats.addToViewGroup((LinearLayout) view.findViewById(R.id.statsLayout), lastUploadAvailable ? 1 : 0, false, 0);

		updateStats();
		refreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.statsSwipeRefresh);
		refreshLayout.setOnRefreshListener(this::updateStats);
		refreshLayout.setColorSchemeResources(R.color.colorPrimary);
		return view;
	}

	private void updateStats() {
		Activity activity = getActivity();
		NetworkLoader.loadString(Network.URL_STATS, Assist.DAY_IN_MINUTES, getContext(), Preferences.GENERAL_STATS, value -> {
			if (refreshLayout != null && refreshLayout.isRefreshing())
				activity.runOnUiThread(() -> refreshLayout.setRefreshing(false));
			if (value != null)
				generateStats(value, activity);
		});
	}

	private void generateStats(String json, Activity activity) {
		List<Stat> stats = new Gson().fromJson(json, new TypeToken<List<Stat>>() {
		}.getType());
		if (publicStats != null) {
			for (Table t : publicStats) {
				t.destroy(activity);
			}
		}
		activity.runOnUiThread(() -> publicStats = generateStatsTable(stats));
	}

	private void getUserStats() {
		OkHttpClient client = new OkHttpClient();
		Request request = new Request.Builder()
				.url(Network.URL_USER_STATS)
				.build();
		Callback c = new Callback() {
			@Override
			public void onFailure(Call call, IOException e) {

			}

			@Override
			public void onResponse(Call call, Response response) throws IOException {
				String body = response.body().string();
				if (body.startsWith("[")) {
					List<Stat> stats = new Gson().fromJson(body, new TypeToken<List<Stat>>() {
					}.getType());
					getActivity().runOnUiThread(() -> generateStatsTable(stats));
				}
				response.close();
			}
		};
		client.newCall(request).enqueue(c);
	}

	/**
	 * Generates table from List of stats
	 *
	 * @param stats list of stats
	 */
	private ArrayList<Table> generateStatsTable(List<Stat> stats) {
		ArrayList<Table> items = new ArrayList<>();
		Context c = getContext();
		LinearLayout ll = (LinearLayout) view.findViewById(R.id.statsLayout);
		int color = ContextCompat.getColor(c, R.color.textPrimary);
		for (int i = 0; i < stats.size(); i++) {
			Stat s = stats.get(i);
			if (s.data != null) {
				Table table = new Table(c, s.data.size(), s.showPosition, color);
				table.addTitle(s.name);
				for (int y = 0; y < s.data.size(); y++) {
					StatData sd = s.data.get(y);
					table.addRow().addData(sd.id, sd.value);
				}
				table.addToViewGroup(ll, true, (i + 1) * 150);
				items.add(table);
			}
		}
		return items;
	}

	@Override
	public Success<String> onEnter(FragmentActivity activity, FloatingActionButton fabOne, FloatingActionButton fabTwo) {
		//todo check if up to date
		return new Success<>();
	}

	@Override
	public void onLeave() {
		if (refreshLayout != null && refreshLayout.isRefreshing())
			refreshLayout.setRefreshing(false);
	}

	@Override
	public void onPermissionResponse(int requestCode, boolean success) {

	}

	@Override
	public ITabFragment newInstance() {
		return new FragmentStats();
	}
}

