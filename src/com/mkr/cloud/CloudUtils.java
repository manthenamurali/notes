package com.mkr.cloud;

import java.io.File;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.os.AsyncTask;

import com.mkr.notes.NotesActivity;
import com.mkr.notes.R;

public class CloudUtils {

	public static final int STORAGE_DROPBOX = 0;
	private Context mContext;
	
	
	private static CloudUtils mCloudUtils;
	private static Dropbox mDropbox; 
	
	private static boolean mIsDropboxLoginStarted = false;
	
	public static CloudUtils getInstance() {
		if(mCloudUtils == null) {
			mCloudUtils = new CloudUtils();
		}
		return mCloudUtils;
	}

	public void init(final Context context) {
		mContext = context;
		
		mDropbox = Dropbox.getInstance();
		mDropbox.init(context);
	}
	
	public void loginIntoDropBox(final NotesActivity activity) {
		if(mDropbox.isAlreadyLogged()) {
			//Toast.makeText(mContext, mContext.getResources().getString(R.string.already_logged_in), Toast.LENGTH_LONG).show();
			displayAlertDialog(mContext.getResources().getString(R.string.dropbox_title));
		} else {
			setDropboxLoginState(true);
			mDropbox.Login();
		}
	}
	
	public static void finishDropboxAuthentication() {
		mDropbox.finishAuthentication();
	}
	
	public String[] getAllInstalledCloudOptions() {
		if(mDropbox.isAlreadyLogged()) {
			return new String[]{mContext.getResources().getString(R.string.dropbox_title)};
		} else {
			return null;
		}
		
		/*final StringBuffer sb = new StringBuffer();
		final String delimiter = ";;";
		if(mDropbox.isAlreadyLogged()) {
			sb.append(mContext.getResources().getString(R.string.dropbox_title));
			sb.append(delimiter);
		}
		
		String[] arr = sb.toString().split(delimiter);
		for (String str : arr) {
			Log.e("mkr","storage name-->"+str);
		}
		return arr;*/
	}
	
	public static boolean isDropboxLoginStarted() {
		return mIsDropboxLoginStarted;
	}

	public static void setDropboxLoginState(boolean state) {
		mIsDropboxLoginStarted = state;
	}

	public static void uploadFiles(final File[] files, final int storageOption) {
		UploadFilesToStorage uploadTask = new UploadFilesToStorage(storageOption, files);
		uploadTask.execute();
	}

	public void displayAlertDialog(final String whichAccount) {
		final AlertDialog.Builder builder = new AlertDialog.Builder(mContext); 
		
		final Resources res = mContext.getResources(); 
		builder.setTitle(res.getString(R.string.already_logged));
		if(whichAccount.equalsIgnoreCase(res.getString(R.string.dropbox_title))) {
			builder.setMessage(res.getString(R.string.remove_added_dropbox_account));
		}
		builder.setPositiveButton(res.getString(R.string.logout), new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				mDropbox.logOut();
			}
		});
		builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				
			}
		});
		builder.create().show();
	}
	
	static class UploadFilesToStorage extends AsyncTask<Void, Void, Void> {
		
		private final int storageType;
		private final File[] filesToUpload;
		
		UploadFilesToStorage(final int cloudType, final File[] files) {
			storageType = cloudType;
			filesToUpload = files;
		}
		
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
		}
		
		@Override
		protected Void doInBackground(Void... params) {
			switch (storageType) {
			case R.string.dropbox_title:
				
				for (int i = 0; i < filesToUpload.length; i++) {
					mDropbox.uploadFile(filesToUpload[i]);
				}
				
				break;
			default:
				break;
			}
			return null;
		}
		
		@Override
		protected void onPostExecute(Void result) {
			super.onPostExecute(result);
			
		}
		
	}
	
}
