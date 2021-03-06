package com.mkr.cloud;

import java.io.File;
import java.util.Map;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.widget.Toast;

import com.mkr.notes.Note;
import com.mkr.notes.NotesActivity;
import com.mkr.notes.R;
import com.mkr.notes.Utils;
import com.mkr.notesdatabase.NotesDBHelper;

/**
 * this is the utile class to the clout storage options like dropbox, drive etcc..
 * @author murali
 *
 */
public class CloudUtils {

	public static final int STORAGE_DROPBOX = 0;
	private Context mContext;
	
	
	private static CloudUtils mCloudUtils;
	private static Dropbox mDropbox;
	//private static GoogleDrive mGoogleDrive; 
	
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
		
		//mGoogleDrive = GoogleDrive.getInstance();
		//mGoogleDrive.init(mContext);
	}
	
	public void loginIntoDropBox(final NotesActivity activity) {
		if(mDropbox.isAlreadyLogged()) {
			displayLogoutDialog(mContext.getResources().getString(R.string.dropbox_title));
		} else {
			if(isNetworkConnected()) {
				setDropboxLoginState(true);
				mDropbox.login();
			} else {
				Toast.makeText(mContext, mContext.getResources().getString(R.string.no_network), Toast.LENGTH_LONG).show();
			}
		}
	}
	
	public static void finishDropboxAuthentication() {
		mDropbox.finishAuthentication();
	}
	
	public String[] getAllInstalledCloudOptions() {
		
		StringBuffer sb = new StringBuffer();
		final String delimiter = ";;";
		if(mDropbox.isAlreadyLogged()) {
			sb.append(mContext.getResources().getString(R.string.dropbox_title));
			sb.append(delimiter);
		}
		
		if(isGoogleDriveLoggedIn()) {
			sb.append(mContext.getResources().getString(R.string.googledrive_title));
		}
		
		String[] arr = null;
		if(sb.toString() != "" && sb.toString().length() > 0) {
			arr = sb.toString().split(delimiter);
		}
		return arr;
	}
	
	public static boolean isDropBoxLoggedIn() {
		return mDropbox.isAlreadyLogged();
	}
	
	public static boolean isDropboxLoginStarted() {
		return mIsDropboxLoginStarted;
	}

	public static void setDropboxLoginState(boolean state) {
		mIsDropboxLoginStarted = state;
	}

	public void uploadFiles(final File[] files, final int storageOption) {
		UploadFilesToStorage uploadTask = new UploadFilesToStorage(storageOption, files);
		uploadTask.execute();
	}

	public void displayLogoutDialog(final String whichAccount) {
		final AlertDialog.Builder builder = new AlertDialog.Builder(mContext); 
		
		final Resources res = mContext.getResources(); 
		builder.setTitle(res.getString(R.string.already_logged));
		if(whichAccount.equalsIgnoreCase(res.getString(R.string.dropbox_title))) {
			builder.setMessage(res.getString(R.string.remove_added_dropbox_account));
		} else if(whichAccount.equalsIgnoreCase(res.getString(R.string.googledrive_title))) {
			builder.setMessage(res.getString(R.string.remove_added_googledrive_account));
		}
		
		builder.setPositiveButton(res.getString(R.string.logout), new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				if(whichAccount.equalsIgnoreCase(res.getString(R.string.dropbox_title))) {
					mDropbox.logOut();
				} else if(whichAccount.equalsIgnoreCase(res.getString(R.string.googledrive_title))) {
					//mGoogleDrive.disconnect();
				}
			}
		});
		builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				
			}
		});
		builder.create().show();
	}
	
	class UploadFilesToStorage extends AsyncTask<Void, Void, Void> {
		
		private final int storageType;
		private final File[] filesToUpload;
		
		UploadFilesToStorage(final int cloudType, final File[] files) {
			storageType = cloudType;
			filesToUpload = files;
		}
		
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			
			Toast.makeText(mContext, mContext.getResources().getString(R.string.cloud_upload_started), Toast.LENGTH_SHORT).show();
		}
		
		@Override
		protected Void doInBackground(Void... params) {
			switch (storageType) {
			case R.string.dropbox_title:
				final int length = filesToUpload.length;
				final Map<Long, Note> notesMap = NotesDBHelper.getCurrentNotesMap();
				
				for (int i = 0; i < length; i++) {
					final String fileNameWithoutExt = Utils.getOnlyFileName(filesToUpload[i].getName());
					final Note note = notesMap.get(Long.valueOf(fileNameWithoutExt));
					String title = filesToUpload[i].getName(); 
					if(note != null && note.title != null) {
						title = note.title +".txt";
					}
					mDropbox.uploadFile(filesToUpload[i], title);
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
			
			Toast.makeText(mContext, mContext.getResources().getString(R.string.cloud_upload_finish_dropbox), Toast.LENGTH_SHORT).show();
		}
	}
	
	public boolean isNetworkConnected() {
		ConnectivityManager cm = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo ni = cm.getActiveNetworkInfo();
		if (ni == null) {
			// There are no active networks.
			return false;
		} else
			return true;
	}
	
		//-------------Google Drive-------------
	
	public static boolean isGoogleDriveLoggedIn() {
		return false;
		//return mGoogleDrive.isConnected();
	}
	
	/*public void loginIntoGoogleDrive() {
		if(mGoogleDrive.isConnected()) {
			displayLogoutDialog(mContext.getResources().getString(R.string.googledrive_title));
		} else {
			if(isNetworkConnected()) {
				mGoogleDrive.connect();
			} else {
				Toast.makeText(mContext, mContext.getResources().getString(R.string.no_network), Toast.LENGTH_LONG).show();
			}
		}
	}
	
	public static void connectToGoogleDrive() {
		mGoogleDrive.connect();
	}
	
	public static void getDriveId(final Intent data) {
		mGoogleDrive.getCurrentDriveIdFromDrive(data);
	}
	
	public static void displayDriveDialog(File[] filesToUpload) {
		mGoogleDrive.openDriveDefaultDialog(filesToUpload);
	}*/
}
