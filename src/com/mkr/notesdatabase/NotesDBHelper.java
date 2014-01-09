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
import com.mkr.notes.NotesActivity;

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
	 * load all the current notes
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

	/**
	 * insert a new note into the data base
	 * 
	 * @param createTime created time for this note
	 * @param modifiedTime modified time for this note 
	 * @param noteTitle title of the note
	 * @param notePath path for the note in the internal storage
	 * @param label label for this note
	 * @param isTitleSelected whether "set title" option is selected
	 */
	public void insertNewNote(final long createTime, final long modifiedTime, final String noteTitle,
			final String notePath, final String label, final int isTitleSelected) {

		final ContentValues values = new ContentValues();
		values.put(SQLiteHelper.COLUMN_MODIFIED_TIME, modifiedTime);
		values.put(SQLiteHelper.COLUMN_NOTE_TITLE, noteTitle);
		values.put(SQLiteHelper.COLUMN_NOTE_PATH, notePath);
		values.put(SQLiteHelper.COLUMN_NOTE_LABEL, label);
		//1 = true, 0= false
		values.put(SQLiteHelper.COLUMN_HAS_CUSTOM_TITLE, isTitleSelected);

		long rowID = -1;
		if(mSQLWritableDatabase.isOpen()) {
			if(!isRowAlreadyExists(createTime)) {
				//this is a new row so insert the created time as well, since this is the primary key
				values.put(SQLiteHelper.COLUMN_CREATED_TIME, createTime);
				rowID = mSQLWritableDatabase.insert(SQLiteHelper.TABLE_NAME, null, values);
			} else {
				//row already exists, so just update the row. since created time is the primary key
				//don't insert it again
				rowID = mSQLWritableDatabase.update(SQLiteHelper.TABLE_NAME, values, SQLiteHelper.COLUMN_CREATED_TIME 
						+ " = " + createTime, null);
			}
		}
		
		//failed to insert
		if(rowID == -1) {
			Log.e(NotesActivity.TAG, "FAILED TO INSERT THE NOTE");
		}
	}

	public void deleteNote(final long createdTime) {
		if(mSQLWritableDatabase == null) return; 
			
		mSQLWritableDatabase.delete(SQLiteHelper.TABLE_NAME, SQLiteHelper.COLUMN_CREATED_TIME 
								+ " = " + createdTime, null);
		getAllSavedNotes();
	}
	
	private boolean isRowAlreadyExists(final long createdTime) {
		if(mNotesInfo == null) return false; 
			
		return mNotesInfo.containsKey(createdTime);
	}
	
	/**
	 * get all the saved notes
	 * @return
	 */
	public ArrayList<Note> getAllSavedNotes() {
		if(mSQLWritableDatabase == null) return null; 
		
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
		return notesList;
	}

	public static Map<Long, Note> getCurrentNotesMap() {
		return mNotesInfo;
	}

	/**
	 * get the number of notes that are associated with each labels ie.. number of work labels, personal labels etc..<br>
	 * work label -- 2 notes<br>
	 * ideas label - 1 note <br>
	 * 
	 * @return the map labels and the number of notes associated with them
	 */
	public Map<String, Integer> getLabelsDetails() {
		if(mSQLWritableDatabase == null) return null; 
		
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
		return labelsMap;
	}
	
	/**
	 * get all notes to the assciated labels
	 * @param label
	 * @return
	 */
	public ArrayList<Note> getNotesForLabel(final String label) {
		if(mSQLWritableDatabase == null) return null;
		
		final String searchQuery = "select * from " + SQLiteHelper.TABLE_NAME + " where " + SQLiteHelper.COLUMN_NOTE_LABEL
						+ " = '"+label+"' ";
		final ArrayList<Note> notesList = new ArrayList<Note>();
		final Cursor cursor = mSQLWritableDatabase.rawQuery(searchQuery, null);
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
			}
		}
		return notesList;
	}
	
	/**
	 * get the search result for the search sting
	 * 
	 * @param searchString
	 * @return
	 */
	public ArrayList<Note> getSearchNotes(final String searchString) {
		if(mSQLWritableDatabase == null) return null;
		
		final String searchQuery = "select * from " + SQLiteHelper.TABLE_NAME + " where " + SQLiteHelper.COLUMN_NOTE_TITLE
						+ " like '%"+searchString+"%' ";
		final ArrayList<Note> notesList = new ArrayList<Note>();
		final Cursor cursor = mSQLWritableDatabase.rawQuery(searchQuery, null);
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
			}
		}
		return notesList;
	}
	
	public void close() {
		if(mSQLWritableDatabase != null) {
			mSQLWritableDatabase.close();
		}
	}
}
