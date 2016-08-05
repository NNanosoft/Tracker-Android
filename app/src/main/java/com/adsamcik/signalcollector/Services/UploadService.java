package com.adsamcik.signalcollector.services;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.Context;
import android.os.Build;
import android.util.Log;

import com.adsamcik.signalcollector.BuildConfig;
import com.adsamcik.signalcollector.classes.DataStore;
import com.adsamcik.signalcollector.Extensions;
import com.adsamcik.signalcollector.Setting;
import com.adsamcik.signalcollector.classes.Network;
import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.firebase.crash.FirebaseCrash;

import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

public class UploadService extends JobService {
	private Thread thread;
	private static int queued = 0;

	private void checkQueue() {
		if(--queued == 0) {
			DataStore.cleanup();
			DataStore.recountDataSize();
			DataStore.onUpload();
		}
		else if(queued < 0)
			FirebaseCrash.report(new Throwable("queued is less than 0, how could it happen?"));
	}

	/**
	 * Uploads data to server.
	 *  @param data json array of Data
	 * @param name name of file where the data is saved (Function will clear the file afterwards)
	 * @param size size of data uploaded
	 */
	private void upload(final String data, final String name, final long size) {
		if (data.isEmpty()) {
			FirebaseCrash.report(new Exception("data are empty"));
			return;
		}
		final Context context = getApplicationContext();
		if (!Extensions.isInitialized())
			Extensions.initialize(context);

		final String serialized = "{\"imei\":" + Extensions.getImei() +
				",\"device\":\"" + Build.MODEL +
				"\",\"manufacturer\":\"" + Build.MANUFACTURER +
				"\",\"api\":" + Build.VERSION.SDK_INT +
				",\"version\":" + BuildConfig.VERSION_CODE + "," +
				"\"data\":" + data + "}";

		StringRequest postRequest = new StringRequest(Request.Method.POST, Network.URL_DATA_UPLOAD,
				response -> {
					deleteFile(name);
					TrackerService.approxSize -= size;
					DataStore.onUpload();
					checkQueue();
				},
				error -> {
					DataStore.requestUpload(context, true);
					checkQueue();
				}
		) {
			@Override
			protected Map<String, String> getParams() {
				Map<String, String> params = new HashMap<>();
				params.put("data", serialized);
				params.put("imei", Extensions.getImei());
				return params;
			}
		};

		queued++;
		Volley.newRequestQueue(context).add(postRequest);
	}

	/**
	 * @param background background upload
	 * @return true if started
	 */
	private boolean uploadAll(final boolean background) {
		DataStore.setContext(getApplicationContext());
		if (thread == null || !thread.isAlive()) {
			thread = new Thread(() -> {
				Setting.getPreferences().edit().putBoolean(Setting.SCHEDULED_UPLOAD, false).apply();
				String[] files = DataStore.getDataFileNames(!background);

				if (files == null || files.length == 0) {
					Log.e(DataStore.TAG, "No file names were entered");
					FirebaseCrash.report(new Throwable("No file names were entered"));
					return;
				}

				TrackerService.approxSize = DataStore.sizeOfData();

				for (String fileName : files) {
					if (!Thread.currentThread().isInterrupted()) {
						if (fileName == null || fileName.trim().length() == 0) {
							Log.e(DataStore.TAG, "Null or empty file name was in load and upload task. This should not happen.");
							FirebaseCrash.report(new Exception("Null or empty file name was in load and upload task. This should not happen."));
							continue;
						}

						StringBuilder builder = DataStore.loadStringAsBuilder(fileName);

						if (builder == null || builder.length() == 0) {
							String issue = builder == null ? "does not exist" : "is empty";
							Log.e(DataStore.TAG, "File " + fileName + " " + issue + ". This should not happen.");
							FirebaseCrash.report(new Exception("File " + fileName + " " + issue + ". This should not happen."));
							continue;
						} else {
							builder.setCharAt(0, '[');
							builder.append(']');
						}
						long size = builder.toString().getBytes(Charset.defaultCharset()).length;
						if (canStart(background))
							upload(builder.toString(), fileName, size);
						else
							break;
					} else
						break;
				}
			});
			thread.start();
			return true;
		}
		return false;
	}

	private boolean canStart(boolean background) {
		Context c = getApplicationContext();
		return Extensions.canUpload(c, background);
	}

	@Override
	public boolean onStartJob(JobParameters jobParameters) {
		return uploadAll(jobParameters.getExtras().getInt(DataStore.KEY_IS_AUTOUPLOAD) == 1);
	}

	@Override
	public boolean onStopJob(JobParameters jobParameters) {
		if (thread != null && thread.isAlive())
			thread.interrupt();
		DataStore.cleanup();
		return false;
	}
}
