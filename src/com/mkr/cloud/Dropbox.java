package com.mkr.cloud;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.util.Log;
import android.widget.Toast;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.DropboxAPI.Entry;
import com.dropbox.client2.android.AndroidAuthSession;
import com.dropbox.client2.exception.DropboxException;
import com.dropbox.client2.session.AccessTokenPair;
import com.dropbox.client2.session.AppKeyPair;
import com.dropbox.client2.session.Session.AccessType;
import com.dropbox.client2.session.TokenPair;
import com.mkr.notes.NotesActivity;
import com.mkr.notes.R;

public class Dropbox {

	private static final String APP_KEY = "a7zsxo6a4osnne4";
	private static final String APP_SECRET = "by27ckiotjiz704";
	private static final AccessType ACCESS_TYPE = AccessType.DROPBOX;

	private static Dropbox mDropbox;

	private static DropboxAPI<AndroidAuthSession> mDBApi;

	private static boolean mIsAlreadyLoggedIn = false;
	private Context mContext;

	private static final String ACCOUNT_PREFS_NAME = "prefs";
	private static final String ACCESS_KEY_NAME = "ACCESS_KEY";
	private static final String ACCESS_SECRET_NAME = "ACCESS_SECRET";

	private static final String DROPBOX_FOLDER_NAME = "SyncNotes";
	
	private Dropbox() { }

	public static Dropbox getInstance() {
		if(mDropbox == null) {
			mDropbox = new Dropbox();
		}
		return mDropbox;
	}

	public void init(final Context appContext) {
		mContext = appContext;

		AndroidAuthSession session = buildSession();
		mDBApi = new DropboxAPI<AndroidAuthSession>(session);

		setLoggedIn(mDBApi.getSession().isLinked());
	}

	private AndroidAuthSession buildSession() {
		final AppKeyPair appKeyPair = new AppKeyPair(APP_KEY, APP_SECRET);
		final AndroidAuthSession session;

		final String[] stored = getKeys();
		if (stored != null) {
			final AccessTokenPair accessToken = new AccessTokenPair(stored[0], stored[1]);
			session = new AndroidAuthSession(appKeyPair, ACCESS_TYPE, accessToken);
		} else {
			session = new AndroidAuthSession(appKeyPair, ACCESS_TYPE);
		}

		return session;
	}

	private void setLoggedIn(boolean loggedIn) {
		mIsAlreadyLoggedIn = loggedIn;
	}

	/**
	 * Shows keeping the access keys returned from Trusted Authenticator in a local
	 * store, rather than storing user name & password, and re-authenticating each
	 * time (which is not to be done, ever).
	 *
	 * @return Array of [access_key, access_secret], or null if none stored
	 */
	private String[] getKeys() {
		final SharedPreferences prefs = mContext.getSharedPreferences(ACCOUNT_PREFS_NAME, 0);
		final String key = prefs.getString(ACCESS_KEY_NAME, null);
		final String secret = prefs.getString(ACCESS_SECRET_NAME, null);
		if (key != null && secret != null) {
			final String[] ret = new String[2];
			ret[0] = key;
			ret[1] = secret;
			return ret;
		} else {
			return null;
		}
	}

	public void finishAuthentication() {
		AndroidAuthSession session = mDBApi.getSession();

		// The next part must be inserted in the onResume() method of the
		// activity from which session.startAuthentication() was called, so
		// that Dropbox authentication completes properly.
		if (session.authenticationSuccessful()) {
			try {
				// Mandatory call to complete the auth
				session.finishAuthentication();

				// Store it locally in our app for later use
				TokenPair tokens = session.getAccessTokenPair();
				storeKeys(tokens.key, tokens.secret);
				setLoggedIn(true);
				
				Toast.makeText(mContext, mContext.getResources().getString(R.string.add_account_succesful), Toast.LENGTH_SHORT).show();
				CloudUtils.setDropboxLoginState(false);
				
			} catch (IllegalStateException e) {
				e.printStackTrace();
			}
		}
	}

	public boolean isAlreadyLogged() {
		return mIsAlreadyLoggedIn;
	}

	public void login() {
		mDBApi.getSession().startAuthentication(mContext);
	}

	public void logOut() {
		mDBApi.getSession().unlink();
		clearKeys();
		setLoggedIn(false);
		Toast.makeText(mContext, mContext.getResources().getString(R.string.remove_account_succesful), Toast.LENGTH_SHORT).show();
	}

	private void storeKeys(String key, String secret) {
		// Save the access key for later
		SharedPreferences prefs = mContext.getSharedPreferences(ACCOUNT_PREFS_NAME, 0);
		Editor edit = prefs.edit();
		edit.putString(ACCESS_KEY_NAME, key);
		edit.putString(ACCESS_SECRET_NAME, secret);
		edit.commit();
	}

	private void clearKeys() {
		final SharedPreferences prefs = mContext.getSharedPreferences(ACCOUNT_PREFS_NAME, 0);
		final Editor edit = prefs.edit();
		edit.clear();
		edit.commit();
	}

	public void uploadFile(final File file, final String title) {
		//create a folder to save files
		try {
			mDBApi.createFolder(DROPBOX_FOLDER_NAME);
		} catch (DropboxException e) {
			//folder alreay exists
			//e.printStackTrace();
		}
		
		try {
			final FileInputStream inputStream = new FileInputStream(file);
			Entry response = mDBApi.putFile(DROPBOX_FOLDER_NAME+ "/" + title, inputStream, file.length(), null, null); 
			Log.e(NotesActivity.TAG,"Dropbox Response-->"+response.path);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (DropboxException e) {
			e.printStackTrace();
		}
	}

}
