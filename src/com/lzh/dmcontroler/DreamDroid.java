package com.lzh.dmcontroler;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;

import com.lzh.dmcontroler.helpers.Profile;
import com.lzh.dmcontroler.helpers.ProfileChangedListener;

import android.preference.PreferenceManager;
import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

public class DreamDroid extends Application
{
	public static final String LOG_TAG = "net.reichholf.dreamdroid";
	
	private static Profile sProfile;
	private static ProfileChangedListener sCurrentProfileChangedListener = null;
	
	private static boolean sFeaturePostRequest = true;
	private static boolean sFeatureNowNext = false;
	
	public static boolean DATE_LOCALE_WO;
	
	@Override
	public void onCreate()
	{
	}
	
	public static void disableNowNext() {
		sFeatureNowNext = false;
	}

	public static void enableNowNext() {
		sFeatureNowNext = true;
	}

	public static boolean featureNowNext() {
		return sFeatureNowNext;
	}
	
	public static boolean featurePostRequest() {
		return sFeaturePostRequest;
	}

	public static void setFeaturePostRequest(boolean enabled) {
		sFeaturePostRequest = enabled;
	}
	
	public static Profile getCurrentProfile() {
		return sProfile;
	}
	
	public static void loadCurrentProfile(Context context) {
		// the profile-table is initial - let's migrate the current config as
		// default Profile
		SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
		DatabaseHelper dbh = DatabaseHelper.getInstance(context);
		ArrayList<Profile> profiles = dbh.getProfiles();
		if (profiles.isEmpty()) {
			String host = sp.getString("host", "dm8000");
			String streamHost = sp.getString("host", "");

			int port = Integer.valueOf(sp.getString("port", "80"));
			String user = sp.getString("user", "root");
			String pass = sp.getString("pass", "dreambox");

			boolean login = sp.getBoolean("login", false);
			boolean ssl = sp.getBoolean("ssl", false);

			Profile p = new Profile("Default", host, streamHost, port, 8001, 80, login, user, pass, ssl, false, false,
					false, false);
			dbh.addProfile(p);
			SharedPreferences.Editor editor = sp.edit();
			editor.remove("currentProfile");
			editor.commit();
		}

		int profileId = sp.getInt("currentProfile", 1);
		if (!setCurrentProfile(context, profileId)) {
			// However we got here... we're creating an
			// "do-not-crash-default-profile now
			sProfile = new Profile("Default", "dm8000", "", 80, 8001, 80, false, "", "", false, false, false, false,
					false);
		}
	}

	public static boolean setCurrentProfile(Context context, int id) {
		return setCurrentProfile(context, id, false);
	}

	public static boolean setCurrentProfile(Context context, int id, boolean forceEvent) {
		Profile oldProfile = sProfile;
		if (oldProfile == null)
			oldProfile = new Profile();

		DatabaseHelper dbh = DatabaseHelper.getInstance(context);
		sProfile = dbh.getProfile(id);
		if (sProfile.getId() == id) {
			SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
			editor.putInt("currentProfile", id);
			editor.commit();
			if (!sProfile.equals(oldProfile) || forceEvent) {
				activeProfileChanged();
			}
			return true;
		}
		return false;
	}
	
	public static void profileChanged(Context context, Profile p) {
		if (p.getId() == sProfile.getId()) {
			reloadCurrentProfile(context);
		}
	}

	private static void activeProfileChanged() {
		if (sCurrentProfileChangedListener != null) {
			sCurrentProfileChangedListener.onProfileChanged(sProfile);
		}
	}
	
	public static boolean reloadCurrentProfile(Context ctx) {
		return setCurrentProfile(ctx, sProfile.getId(), true);
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static void scheduleBackup(Context context)
	{
		Log.d(LOG_TAG, "Scheduling backup");
		try {
			Class managerClass = Class.forName("android.app.backup.BackupManager");
			Constructor managerConstructor = managerClass.getConstructor(Context.class);
			Object manager = managerConstructor.newInstance(context);
			Method m = managerClass.getMethod("dataChanged");
			m.invoke(manager);
			Log.d(LOG_TAG, "Backup requested");
		} catch (ClassNotFoundException e) {
			Log.d(LOG_TAG, "No backup manager found");
		} catch (Throwable t) {
			Log.d(LOG_TAG, "Scheduling backup failed " + t);
			t.printStackTrace();
		}
	}
}
