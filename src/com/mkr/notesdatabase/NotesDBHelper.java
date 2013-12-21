package com.mkr.notesdatabase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.mkr.notes.Note;

public class NotesDBHelper {

	private static NotesDBHelper mNotesDBHelper;
	private static SQLiteHelper mSQLiteHelper;
	private SQLiteDatabase mSQLWritableDatabase;
	private final String[] allColumns = {
				SQLiteHelper.COLUMN_CREATED_TIME, 
				SQLiteHelper.COLUMN_MODIFIED_TIME,
				SQLiteHelper.COLUMN_NOTE_TITLE, 
				SQLiteHelper.COLUMN_NOTE_PATH,
				SQLiteHelper.COLUMN_NOTE_LABEL,
				SQLiteHelper.COLUMN_HAS_CUSTOM_TITLE
			};

	/**
	 * this is for temp use remove it later
	 */
	private static final Map<Long, Note> mNotesInfo = new HashMap<Long, Note>();
	
	//private constructor
	private NotesDBHelper() { }

	public static NotesDBHelper getInstance(final Context mContext) {
		if(mNotesDBHelper == null) {
			mNotesDBHelper = new NotesDBHelper();
			mSQLiteHelper = new SQLiteHelper(mContext);
		}
		return mNotesDBHelper;
	}
	
	public void open() {
		mSQLWritableDatabase = mSQLiteHelper.getWritableDatabase();
	}

	public void insertNewNote(final long createTime, final long modifiedTime, final String noteTitle,
			final String notePath, final String label, final int isTitleSelected) {

		final ContentValues values = new ContentValues();
		values.put(SQLiteHelper.COLUMN_CREATED_TIME, createTime);
		values.put(SQLiteHelper.COLUMN_MODIFIED_TIME, modifiedTime);
		values.put(SQLiteHelper.COLUMN_NOTE_TITLE, noteTitle);
		values.put(SQLiteHelper.COLUMN_NOTE_PATH, notePath);
		values.put(SQLiteHelper.COLUMN_NOTE_LABEL, label);
		//1 = true, 0= false
		values.put(SQLiteHelper.COLUMN_HAS_CUSTOM_TITLE, isTitleSelected);

		if(mSQLWritableDatabase.isOpen()) {
			final long rowID = mSQLWritableDatabase.insert(SQLiteHelper.TABLE_NAME, null, values);
			if(rowID == -1) {
				Log.e("mkr","FAILED TO INSERT THE NOTE");
			}
		}
	}

	public void deleteNote(final long createdTime) {
		int deletedRows = mSQLWritableDatabase.delete(SQLiteHelper.TABLE_NAME, SQLiteHelper.COLUMN_CREATED_TIME 
								+ " = " + createdTime, null);
		Log.e("mkr","Number of rows deleted = " + deletedRows);
		getAllSavedNotes();
	}
	
	public ArrayList<Note> getAllSavedNotes() {
		final ArrayList<Note> notesList = new ArrayList<Note>();
		final Cursor cursor = mSQLWritableDatabase.query(SQLiteHelper.TABLE_NAME, allColumns, 
				null, null, null, null, null);
		if(cursor != null) {
			while (cursor.moveToNext()) {
				final Note note = new Note();
				note.createDate = Long.valueOf(cursor.getString(cursor.getColumnIndex(SQLiteHelper.COLUMN_CREATED_TIME)));
				note.modifiedDate = Long.valueOf(cursor.getString(cursor.getColumnIndex(SQLiteHelper.COLUMN_MODIFIED_TIME)));
				note.title = cursor.getString(cursor.getColumnIndex(SQLiteHelper.COLUMN_NOTE_TITLE));
				note.NotePath = cursor.getString(cursor.getColumnIndex(SQLiteHelper.COLUMN_NOTE_PATH));
				note.NoteLabel = cursor.getString(cursor.getColumnIndex(SQLiteHelper.COLUMN_NOTE_LABEL));
				final int hasCustomTitle = Integer.valueOf(cursor.getInt(cursor.getColumnIndex(SQLiteHelper.COLUMN_HAS_CUSTOM_TITLE)));
				note.hasTitle = hasCustomTitle == 1 ? true : false; 
				notesList.add(note);
				
				mNotesInfo.put(note.createDate, note);
			}
		}

		/*Log.e("mkr","Total notes count ---->"+notesList.size());
		for (Note note : notesList) {
			Log.e("mkr","Title ---->"+note.title);
		}*/
		
		return notesList;
	}

	public static Map<Long, Note> getCurrentNotesMap() {
		return mNotesInfo;
	}

	public Map<String, Integer> getLabelsDetails() {
		final Cursor cursor = mSQLWritableDatabase.query(SQLiteHelper.TABLE_NAME, allColumns, 
				null, null, null, null, null);
		final Map<String, Integer> labelsMap = new HashMap<String, Integer>();
		if(cursor != null) {
			while (cursor.moveToNext()) {
				int counter = 0;
				final String noteLabel = cursor.getString(cursor.getColumnIndex(SQLiteHelper.COLUMN_NOTE_LABEL));
				if(labelsMap.containsKey(noteLabel)) {
					counter = labelsMap.get(noteLabel);
				}
				counter = counter + 1;
				labelsMap.put(noteLabel, counter);
			}
		}
		
		/*Log.e("mkr","labels info");
		Iterator<String> keysIterator = labelsMap.keySet().iterator();
		while (keysIterator.hasNext()) {
			String next = keysIterator.next();
			Log.e("mkr","for " + next + " lables are --> " + labelsMap.get(next));
		}*/
		
		return labelsMap;
	}
	
	public void close() {
		mSQLWritableDatabase.close();
	}
}
