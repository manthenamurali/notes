package com.mkr.cloud;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Map;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.IntentSender.SendIntentException;
import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.drive.Contents;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveApi.ContentsResult;
import com.google.android.gms.drive.DriveFile;
import com.google.android.gms.drive.DriveId;
import com.google.android.gms.drive.DriveResource.MetadataResult;
import com.google.android.gms.drive.MetadataChangeSet;
import com.google.android.gms.drive.OpenFileActivityBuilder;
import com.mkr.notes.Note;
import com.mkr.notes.R;
import com.mkr.notes.Utils;
import com.mkr.notesdatabase.NotesDBHelper;

public class GoogleDrive implements GoogleApiClient.ConnectionCallbacks, 
GoogleApiClient.OnConnectionFailedListener {

	/**
	 * Request code for auto Google Play Services error resolution.
	 */
	public static final int REQUEST_CODE_RESOLUTION = 1;
	
	public static final int REQUEST_CODE_CREATOR = 2;

	private static GoogleDrive mGoogleDrive;
	private GoogleApiClient mGoogleApiClient;
	private Context mContext;

	private DriveId mCurrentDriveId;
	private File[] mFilesToUpload;
	
	private GoogleDrive() {
		//private constructor
	}

	public static GoogleDrive getInstance() {
		if(mGoogleDrive == null) {
			mGoogleDrive = new GoogleDrive();
		}
		return mGoogleDrive;
	} 

	public void init(final Context context) {
		mContext = context;
		mGoogleApiClient = new GoogleApiClient.Builder(context)
			.addApi(Drive.API).addScope(Drive.SCOPE_FILE)
			.addConnectionCallbacks(this)
			.addOnConnectionFailedListener(this).build();
	}

	public void connect() {
		if(mGoogleApiClient == null) {
			init(mContext);
		}
		mGoogleApiClient.connect();
	}

	public boolean isConnected() {
		if(mGoogleApiClient != null) {
			return mGoogleApiClient.isConnected();
		}
		return false;
	}

	public void disconnect() {
		if(mGoogleApiClient != null) {
			mGoogleApiClient.disconnect();
			Toast.makeText(mContext, mContext.getResources().getString(R.string.remove_account_succesful), Toast.LENGTH_SHORT).show();
		}
	}

	@Override
	public void onConnected(Bundle arg0) {
		Toast.makeText(mContext, mContext.getResources().getString(R.string.add_account_succesful), Toast.LENGTH_SHORT).show();
	}

	@Override
	public void onConnectionSuspended(int arg0) {
		Toast.makeText(mContext, mContext.getResources().getString(R.string.add_account_failed), Toast.LENGTH_SHORT).show();
	}
	
	@Override
	public void onConnectionFailed(ConnectionResult result) {
		/*if (!result.hasResolution()) {
			GooglePlayServicesUtil.getErrorDialog(result.getErrorCode(), (Activity) mContext, 0).show();
			return;
		}*/
		try {
			result.startResolutionForResult((Activity)mContext, REQUEST_CODE_RESOLUTION);
		} catch (SendIntentException e) {
			e.printStackTrace();
		}
	}
	
	public void openDriveDefaultDialog(final File[] filesToUpload) {
		mFilesToUpload = filesToUpload;
		

        ResultCallback<ContentsResult> onContentsCallback =
                new ResultCallback<ContentsResult>() {
            @Override
            public void onResult(ContentsResult result) {
                MetadataChangeSet metadataChangeSet = new MetadataChangeSet.Builder()
                        .setMimeType("text/plain" ).build();
                IntentSender createIntentSender = Drive.DriveApi
                        .newCreateFileActivityBuilder()
                        .setInitialMetadata(metadataChangeSet)
                        .setInitialContents(result.getContents())
                        .build(mGoogleApiClient);
                try {
                	((Activity)mContext).startIntentSenderForResult(createIntentSender, REQUEST_CODE_CREATOR, null,
                        0, 0, 0);
                } catch (SendIntentException e) {
                	e.printStackTrace();
                }
            }
        };
        Drive.DriveApi.newContents(mGoogleApiClient).setResultCallback(onContentsCallback);
	}
	
	public void getCurrentDriveIdFromDrive(final Intent data) {
		mCurrentDriveId = (DriveId) data.getParcelableExtra(
				OpenFileActivityBuilder.EXTRA_RESPONSE_DRIVE_ID);

		final int length = mFilesToUpload.length;
		final Map<Long, Note> notesMap = NotesDBHelper.getCurrentNotesMap();

		for (int i = 0; i < length; i++) {
			final String fileNameWithoutExt = Utils.getOnlyFileName(mFilesToUpload[i].getName());
			final Note note = notesMap.get(Long.valueOf(fileNameWithoutExt));
			String title = mFilesToUpload[i].getName(); 
			if(note != null && note.title != null) {
				title = note.title +".txt";
			}
			uploadFile(mFilesToUpload[i], title);
		}
	}
	
	public void uploadFile(final File file, final String title) {
		if (mCurrentDriveId == null) {
			return;
		}
		new EditDriveFileAsyncTask(mGoogleApiClient) {
			@Override
			public Changes edit(Contents contents) {
				MetadataChangeSet metadataChangeSet = new MetadataChangeSet.Builder()
				.setTitle(title).build();
				RandomAccessFile f = null;
				try {
					f = new RandomAccessFile(file, "r");
					byte[] b = new byte[(int)f.length()];
					f.read(b);
					contents.getOutputStream().write(b);
					
					f.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
				return new Changes(metadataChangeSet, contents);
			}

			@Override
			protected void onPostExecute(com.google.android.gms.common.api.Status status) {
				if (!status.getStatus().isSuccess()) {
					Toast.makeText(mContext, mContext.getResources().getString(R.string.cloud_upload_failed), Toast.LENGTH_SHORT).show();
					return;
				}
				Toast.makeText(mContext, mContext.getResources().getString(R.string.cloud_upload_finish_googledrive), Toast.LENGTH_SHORT).show();
			}
		}.execute(mCurrentDriveId);
	}
	
	/**
	 * An async task to open, make changes to and close a file.
	 */
	public abstract class EditDriveFileAsyncTask extends
	        AsyncTask<DriveId, Boolean, com.google.android.gms.common.api.Status> {

	    private final GoogleApiClient mClient;

	    /**
	     * Constructor.
	     * @param client A connected {@code GoogleApiClient} instance.
	     */
	    public EditDriveFileAsyncTask(GoogleApiClient client) {
	        mClient = client;
	    }

	    /**
	     * Handles the editing to file metadata and contents.
	     */
	    public abstract Changes edit(Contents contents);

	    /**
	     * Opens contents for the given file, executes the editing tasks, saves the
	     * metadata and content changes.
	     */
	    @Override
	    protected com.google.android.gms.common.api.Status doInBackground(DriveId... params) {
	    	 DriveFile file = Drive.DriveApi.getFile(mClient, params[0]);
	    	    PendingResult<ContentsResult> openContentsResult =
	    	        file.openContents(mClient, DriveFile.MODE_WRITE_ONLY, null);
	    	    openContentsResult.await();
	    	    if (!openContentsResult.await().getStatus().isSuccess()) {
	    	      return openContentsResult.await().getStatus();
	    	    }

	    	    Changes changes = edit(openContentsResult.await().getContents());
	    	    PendingResult<MetadataResult> metadataResult = null;
	    	    PendingResult<com.google.android.gms.common.api.Status>
	    	            closeContentsResult = null;

	    	    if (changes.getMetadataChangeSet() != null) {
	    	      metadataResult = file.updateMetadata(mClient, changes.getMetadataChangeSet());
	    	      metadataResult.await();
	    	      if (!metadataResult.await().getStatus().isSuccess()) {
	    	        return metadataResult.await().getStatus();
	    	      }
	    	    }

	    	    if (changes.getContents() != null) {
	    	      closeContentsResult = file.commitAndCloseContents(mClient, changes.getContents());
	    	      closeContentsResult.await();
	    	    }
	    	    return closeContentsResult.await().getStatus();
	    }

	    /**
	     * Represents the delta of the metadata changes and keeps a pointer to the
	     * file contents to be stored permanently.
	     */
	    public class Changes {
	        private final MetadataChangeSet mMetadataChangeSet;
	        private final Contents mContents;

	        public Changes(MetadataChangeSet metadataChangeSet, Contents contents) {
	            mMetadataChangeSet = metadataChangeSet;
	            mContents = contents;
	        }

	        public MetadataChangeSet getMetadataChangeSet() {
	            return mMetadataChangeSet;
	        }

	        public Contents getContents() {
	            return mContents;
	        }
	    }
	}
}
