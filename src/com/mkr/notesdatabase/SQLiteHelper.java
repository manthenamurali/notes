package com.mkr.notesdatabase;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class SQLiteHelper extends SQLiteOpenHelper {

	private static final String DATABASE_NAME = "notes.db";
	private static final int DATABASE_VERSION = 1;

	public static final String TABLE_NAME = "notesInfo";

	public static final String COLUMN_CREATED_TIME = "created_time";
	public static final String COLUMN_MODIFIED_TIME = "modified_time";
	public static final String COLUMN_NOTE_TITLE = "title";
	public static final String COLUMN_NOTE_PATH = "note_path";
	public static final String COLUMN_NOTE_LABEL = "note_label";

	public SQLiteHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL("CREATE TABLE "+ TABLE_NAME + " (" 
				+ COLUMN_CREATED_TIME + " datetime primary key ,"
				+ COLUMN_MODIFIED_TIME + " datetime,"
				+ COLUMN_NOTE_TITLE + " VARCHAR,"
				+ COLUMN_NOTE_PATH + " VARCHAR," 
				+ COLUMN_NOTE_LABEL + " VARCHAR);");
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
	    onCreate(db);
	}
}
