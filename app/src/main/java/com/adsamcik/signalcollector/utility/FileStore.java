package com.adsamcik.signalcollector.utility;

import android.support.annotation.NonNull;

import com.google.firebase.crash.FirebaseCrash;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.security.InvalidParameterException;

public class FileStore {

	/**
	 * Checks if file exists
	 *
	 * @param fileName file name
	 * @return existence of file
	 */
	public static File file(@NonNull String parent, @NonNull String fileName) {
		return new File(parent, fileName);
	}

	/**
	 * Checks if file exists
	 *
	 * @param fileName file name
	 * @return existence of file
	 */
	public static File file(@NonNull File parent, @NonNull String fileName) {
		return new File(parent, fileName);
	}


	/**
	 * Saves string to file
	 *
	 * @param file file
	 * @param data string data
	 */
	public static boolean saveString(@NonNull File file, @NonNull String data) {
		try (FileOutputStream outputStream = new FileOutputStream(file)) {
			outputStream.getChannel().lock();
			OutputStreamWriter osw = new OutputStreamWriter(outputStream);
			osw.write(data);
			osw.close();
		} catch (Exception e) {
			FirebaseCrash.report(e);
			e.printStackTrace();
			return false;
		}
		return true;
	}

	public static boolean saveStringAppend(@NonNull File parent, @NonNull String fileName, @NonNull String data) {
		File file = new File(parent, fileName);
		try (FileOutputStream outputStream = new FileOutputStream(file)) {
			outputStream.getChannel().lock();
			OutputStreamWriter osw = new OutputStreamWriter(outputStream);
			osw.write(data);
			osw.close();
		} catch (Exception e) {
			FirebaseCrash.report(e);
			return false;
		}
		return true;
	}


	/**
	 * Load string file as StringBuilder
	 *
	 * @param fileName file name
	 * @return content of file as StringBuilder
	 */
	public static StringBuilder loadStringAsBuilder(@NonNull File file) {
		if (!file.exists())
			return null;

		try {
			FileInputStream fis = new FileInputStream(file);
			InputStreamReader isr = new InputStreamReader(fis);
			BufferedReader br = new BufferedReader(isr);
			String receiveString;
			StringBuilder stringBuilder = new StringBuilder();

			while ((receiveString = br.readLine()) != null)
				stringBuilder.append(receiveString);

			isr.close();
			return stringBuilder;
		} catch (Exception e) {
			FirebaseCrash.report(e);
			return null;
		}
	}

	/**
	 * Converts loadStringAsBuilder to string and handles nulls
	 *
	 * @param fileName file name
	 * @return content of file (empty if file has no content or does not exists)
	 */
	public static String loadString(@NonNull File file) {
		StringBuilder sb = loadStringAsBuilder(file);
		if (sb != null)
			return sb.toString();
		else
			return null;
	}

	/**
	 * Loads json array that was saved with append method
	 *
	 * @param fileName file name
	 * @return proper json array
	 */
	public static String loadJsonArrayAppend(@NonNull File file) {
		StringBuilder sb = loadStringAsBuilder(file);
		if (sb != null && sb.length() != 0) {
			if (sb.charAt(sb.length() - 1) != ']')
				sb.append(']');
			return sb.toString();
		}
		return null;
	}

	/**
	 * Tries to delete file 5 times.
	 * After every unsuccessfull try there is 50ms sleep so you should ensure that this function does not run on UI thread.
	 *
	 * @param file file to delete
	 * @return true if file was deleted, false otherwise
	 */
	public static boolean retryDelete(File file) {
		return retryDelete(file, 5);
	}

	/**
	 * Tries to delete file multiple times based on {@code maxRetryCount}.
	 * After every unsuccessfull try there is 50ms sleep so you should ensure that this function does not run on UI thread.
	 *
	 * @param file          file to delete
	 * @param maxRetryCount maximum retry count
	 * @return true if file was deleted, false otherwise
	 */
	public static boolean retryDelete(File file, int maxRetryCount) {
		if (file == null)
			throw new InvalidParameterException("file is null");

		int retryCount = 0;
		for (; ; ) {
			if (!file.exists() || file.delete())
				return true;

			if (++retryCount < maxRetryCount)
				return false;

			try {
				Thread.sleep(50);
			} catch (InterruptedException e) {
				// Restore the interrupted done
				Thread.currentThread().interrupt();
			}
		}
	}
}
