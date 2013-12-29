package com.mkr.notes;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.widget.Toast;

/**
 * this is the service which saves the note in the internal storage
 * @author murali
 *
 */
public class SaveNoteService extends Service {

	private String mNotePath;
	private String mNotesText;

	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		if(intent != null) {
			mNotesText = intent.getStringExtra("text");
			mNotePath = intent.getStringExtra("filepath");
			saveNote(mNotesText);
			
			Toast.makeText(SaveNoteService.this, getString(R.string.saved), Toast.LENGTH_SHORT).show();
			
			stopSelf();
		}
		return 0;
	}
	
	/**
	 * save the note 
	 * @param body text to write into file
	 */
	public void saveNote(final String body) {
		BufferedWriter writer = null;
		try {
			writer = new BufferedWriter(new FileWriter(mNotePath));
			writer.write(body);
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (writer != null) {
				try {
					writer.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
}
