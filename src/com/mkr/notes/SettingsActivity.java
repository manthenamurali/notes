package com.mkr.notes;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.view.MenuItem;
import android.view.View;

public class SettingsActivity extends PreferenceActivity{

	public static final String THEMES_SHARED_PREF       =  "themes";
	public static final String PREF_THEME_PLAIN      	=  "theme_plain";
	public static final String PREF_THEME_YELLOW	    =  "theme_yellow";
	public static final String PREF_THEME_GREEN		    =  "theme_green";
	
	public static final int THEME_PLAIN      			=  0;
	public static final int THEME_YELLOW	        	=  1;
	public static final int THEME_GREEN	        		=  2;
	
	public static final String PREF_TEXT_SIZE 			= "text_size";
	public static final String PREF_TEXT_FONT 			= "text_font";
	public static final String PREF_THEME	 			= "theme";

	public static final int TEXT_SIZE_SMALL 		= 0;
	public static final int TEXT_SIZE_MEDIUM  		= 1;
	public static final int TEXT_SIZE_LARGE			= 2;

	public static final int TEXT_FONT_SANS 			= 0;
	public static final int TEXT_FONT_SANS_MONO  	= 1;
	public static final int TEXT_FONT_SERIF  		= 2;
	public static final int TEXT_FONT_ROBOTO_SLAB  	= 3;
	
	public static final int DATE_TYPE_MODIFIED  	= 0;
	public static final int DATE_TYPE_CREATED 		= 1;
	
	public static final String PREF_FIRST_LAUNCH   	= "first_launch";
	
	//public static final String PREF_NOTE_OPEN_OPTIONS   	= "notes_open_type";

	@SuppressWarnings("deprecation")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		final ActionBar actionBar = getActionBar();
		if(actionBar != null) {
			actionBar.setDisplayHomeAsUpEnabled(true);
	        actionBar.setHomeButtonEnabled(true);
		}
        
		addPreferencesFromResource(R.xml.prefs);
		findPreference("about_msg").setOnPreferenceClickListener(new OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				displayAboutDialog();
				return false;
			}
		});
		
		findPreference("rate_me").setOnPreferenceClickListener(new OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				int storeType = getResources().getInteger(R.integer.app_store_type);
				Intent intent = new Intent(Intent.ACTION_VIEW);
				if(storeType == 0) {
					try { 
						intent.setData(Uri.parse("market://details?id=com.mkr.notes"));
						startActivity(intent);
					} catch (Exception e) { //google play app is not installed
						intent.setData(Uri.parse("https://play.google.com/store/apps/details?id=com.mkr.notes"));
					}
				} else if(storeType == 1) {
					intent.setData(Uri.parse("http://www.amazon.com/gp/mas/dl/android?p=com.mkr.notes"));
				}
				startActivity(intent);
				return false;
			}
		});
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			final Intent i = new Intent(SettingsActivity.this, NotesActivity.class);
			i.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
			startActivity(i);
			break;
		};
		return true;
	}
	
	/**
	 * display the about dialog in settings
	 */
	private void displayAboutDialog() {
		final AlertDialog.Builder builder = new AlertDialog.Builder(SettingsActivity.this);
		builder.setTitle(getString(R.string.app_name));
		
		View view = getLayoutInflater().inflate(R.layout.about, null);
		builder.setView(view);
		
		builder.setPositiveButton(android.R.string.ok, new OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
			}
		});
		
		builder.create().show();
	}
}
