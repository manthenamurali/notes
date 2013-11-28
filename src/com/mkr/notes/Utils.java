package com.mkr.notes;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.preference.PreferenceManager;
import android.util.Log;

public class Utils {

	public static final String DELIMITER =";;";
	
	private static Utils mUtils;
	private Context mContext;

	private Typeface mRobotoSlabFont;
	private Typeface mNoteFont; 

	private float mNoteFontSize; 
	
	private static int mDateTypeToDisplay;
	private SharedPreferences mPrefs;
	private static DateFormat mDateFormat;
	private static Date mDate;

	private Utils() { }

	public static Utils getInstance() {
		if(mUtils == null) {
			mUtils = new Utils();
			mDateFormat = new SimpleDateFormat("MMM dd,yyyy");
			mDate  = new Date();
		}
		return mUtils;
	}

	public void setContext(final Context context) {
		mContext = context;
	}

	public void init() {
		mRobotoSlabFont = Typeface.createFromAsset(mContext.getAssets(), "fonts/RobotoSlab-Regular.ttf");

		mPrefs = PreferenceManager.getDefaultSharedPreferences(mContext);
		mDateTypeToDisplay = Integer.parseInt(mPrefs.getString(SettingsActivity.PREF_DATE_TO_DISPLAY, ""+SettingsActivity.DATE_TYPE_MODIFIED));
		setDefaultThemesToSharedPref();

		final int noteFont = Integer.parseInt(mPrefs.getString(SettingsActivity.PREF_TEXT_FONT, ""+SettingsActivity.TEXT_FONT_SANS));
		final int noteFontSize = Integer.parseInt(mPrefs.getString(SettingsActivity.PREF_TEXT_SIZE, ""+SettingsActivity.TEXT_SIZE_MEDIUM));
		
		loadNoteFont(noteFont);
		loadNoteFontSize(noteFontSize);
		loadTheme();
		
	}

	public void setDefaultThemesToSharedPref() {
		final SharedPreferences sharedPref = mContext.getSharedPreferences(SettingsActivity.THEMES_SHARED_PREF, Context.MODE_PRIVATE);
		final Map<String, ?> savedThemes = sharedPref.getAll(); 
		if(savedThemes != null && savedThemes.size()  == 0) {
			final Resources res = mContext.getResources();
			final int themesInSharedPref = savedThemes.size();  
			final int themesInRes = res.getInteger(R.integer.total_Default_themes);
			if(themesInSharedPref != themesInRes) {
				final Editor edit = sharedPref.edit();
				
				final String plainTheme = String.valueOf(res.getColor(R.color.plain_theme_text_color)) + DELIMITER +
						String.valueOf(res.getColor(R.color.plain_theme_line_color)) + DELIMITER +
						String.valueOf(res.getColor(R.color.plain_theme_background_color));
				
				final String yellowTheme = String.valueOf(res.getColor(R.color.yellow_theme_text_color)) + DELIMITER +
						String.valueOf(res.getColor(R.color.yellow_theme_line_color)) + DELIMITER +
						String.valueOf(res.getColor(R.color.yellow_theme_background_color));
				
				Log.e("mkr","plain theme-->"+plainTheme);
				Log.e("mkr","yellow theme-->"+yellowTheme);
				
				edit.putString(SettingsActivity.PREF_THEME_PLAIN, plainTheme);
				edit.putString(SettingsActivity.PREF_THEME_YELLOW, yellowTheme);
				edit.commit();
			}
		}
	}
	
	public String loadTheme() {
		final int selectedTheme = Integer.parseInt(mPrefs.getString(SettingsActivity.PREF_THEME, ""+SettingsActivity.THEME_PLAIN));
		final SharedPreferences sharedPref = mContext.getSharedPreferences(SettingsActivity.THEMES_SHARED_PREF, Context.MODE_PRIVATE);
		String themeValue = null;
		switch (selectedTheme) {
		case SettingsActivity.THEME_PLAIN:
			themeValue = sharedPref.getString(SettingsActivity.PREF_THEME_PLAIN, null);
			break;
		case SettingsActivity.THEME_YELLOW:
			themeValue = sharedPref.getString(SettingsActivity.PREF_THEME_YELLOW, null);
			break;
		}  
		
		if(themeValue == null) {
			final Resources res = mContext.getResources();
			themeValue = String.valueOf(res.getColor(R.color.plain_theme_text_color)) + DELIMITER +
					String.valueOf(res.getColor(R.color.plain_theme_line_color)) + DELIMITER +
					String.valueOf(res.getColor(R.color.plain_theme_background_color));
		}
		
		return themeValue;
	}
	
	public static String getReadableTime(final long millisec) {
		mDate.setTime(millisec);		
		return mDateFormat.format(mDate);
	}

	public Typeface getRobotoSlabFontTypeface() {
		return Typeface.SERIF;
		//return mRobotoSlabFont;
	}

	public static int getDateTypeToDisplay() {
		return mDateTypeToDisplay;
	}
	
	public void updateFont(final int font) {
		mPrefs.edit().putString(SettingsActivity.PREF_TEXT_FONT, ""+font).commit();
	}
	
	public void updateTextSize(final int textSize) {
		mPrefs.edit().putString(SettingsActivity.PREF_TEXT_SIZE, ""+textSize).commit();
	}
	
	public void updateTheme(final int theme) {
		mPrefs.edit().putString(SettingsActivity.PREF_THEME, ""+theme).commit();
	}
	
	public void loadNoteFont(final int noteFont) {
		switch (noteFont) {
		case SettingsActivity.TEXT_FONT_SANS:
			mNoteFont = Typeface.SANS_SERIF;
			break;
		case SettingsActivity.TEXT_FONT_SANS_MONO:
			mNoteFont = Typeface.MONOSPACE;
			break;
		case SettingsActivity.TEXT_FONT_SERIF:
			mNoteFont = Typeface.SERIF;
			break;
		case SettingsActivity.TEXT_FONT_ROBOTO_SLAB:
			mNoteFont = mRobotoSlabFont;
			break;
		}
	}
	
	public Typeface getNoteFont() {
		return mNoteFont;
	}
	
	public void loadNoteFontSize(final int noteFontSize) {
		final Resources res = mContext.getResources();
		switch (noteFontSize) {
		case SettingsActivity.TEXT_SIZE_SMALL:
			mNoteFontSize = res.getDimension(R.dimen.edit_text_size_small);
			break;
		case SettingsActivity.TEXT_SIZE_MEDIUM:
			mNoteFontSize = res.getDimension(R.dimen.edit_text_size_normal);
			break;
		case SettingsActivity.TEXT_SIZE_LARGE:
			mNoteFontSize = res.getDimension(R.dimen.edit_text_size_large);
			break;
		}
	}
	
	public float getNoteFontSize() {
		return mNoteFontSize;
	}
}
