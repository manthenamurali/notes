package com.mkr.notes;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.CheckedTextView;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.mkr.notes.labels.LabelUtils;
import com.mkr.notesdatabase.NotesDBHelper;

public class CreateEditNoteActivity extends Activity implements ActionBar.OnNavigationListener {

	private final int MAX_TITLE_LEN = 25;
	
	//whether this a create or edit type note
	private int mNoteActionType = NotesActivity.NOTE_CREATE;

	private long mNoteCreationtime;
	private long mNoteModifiedtime;
	private String mNotePath;

	private EditText mTitleEditText;
	private CustomEditText mSubjectEditText;

	private ProgressBar mProgressBar;

	private ArrayList<String> mValuesList;

	private int mItemPosition;

	private int mShouldInsertCustomTitle = 0;

	private Menu mMenu;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.create_note_layout);
		
		mTitleEditText = (EditText) findViewById(R.id.create_edit_note_edittext_title);
		mSubjectEditText = (CustomEditText) findViewById(R.id.create_edit_note_edittext_body);
		
		final Intent intent = getIntent();
		String noteTitle = null;
		String noteLabel = null;
		boolean hasTitle = false;
		
		if(intent != null) {
			mNoteActionType = intent.getIntExtra(NotesActivity.INTENT_KEY_NOTE_TYPE, NotesActivity.NOTE_CREATE);
			mNoteCreationtime = intent.getLongExtra(NotesActivity.INTENT_KEY_CREATE_TIME, System.currentTimeMillis());
			mNotePath = intent.getStringExtra(NotesActivity.INTENT_KEY_NOTE_PATH);
			noteTitle = intent.getStringExtra(NotesActivity.INTENT_KEY_NOTE_TITLE);
			noteLabel = intent.getStringExtra(NotesActivity.INTENT_KEY_NOTE_LABEL);
			hasTitle = intent.getBooleanExtra(NotesActivity.INTENT_KEY_HAS_TITLE, false);
		}

		final ActionBar actionBar = getActionBar();
		if(actionBar != null) {
			actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_HOME /*| ActionBar.DISPLAY_SHOW_TITLE*/);
			actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
			actionBar.setDisplayHomeAsUpEnabled(false);

			View layout = getLayoutInflater().inflate(R.layout.create_note_actionbar, null);
			mProgressBar = (ProgressBar) layout.findViewById(R.id.create_note_progressbar);
			actionBar.setCustomView(layout);
		}

		final Map<String, ?> labels = LabelUtils.getAllSavedLables();
		final Iterator<String> keysIterator = labels.keySet().iterator();
		mValuesList = new ArrayList<String>();
		while (keysIterator.hasNext()) {
			mValuesList.add(keysIterator.next()); 
		}
		
		mItemPosition = mValuesList.indexOf(noteLabel);
		
		final String[] values = mValuesList.toArray(new String[mValuesList.size()]);;
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActionBar().getThemedContext(),
				android.R.layout.simple_spinner_item, android.R.id.text1,
				values);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		actionBar.setListNavigationCallbacks(adapter, this);
		actionBar.setSelectedNavigationItem(mItemPosition);	    
		
		if(mNoteActionType == NotesActivity.NOTE_EDIT) {
			if(hasTitle) {
				mTitleEditText.setVisibility(View.VISIBLE);
				mTitleEditText.setText(noteTitle);
			}
			
			mSubjectEditText.setFocusable(false);
			mTitleEditText.setFocusable(false);
			
			DisplaySavedNote displayNote = new DisplaySavedNote();
			displayNote.execute(mNotePath);
			
			Toast.makeText(CreateEditNoteActivity.this, getString(R.string.tap_icon_to_edit), Toast.LENGTH_LONG).show();
		} else {
			mSubjectEditText.setFocusable(true);
			mSubjectEditText.requestFocus();
		}
	}

	@Override
	protected void onResume() {
		super.onResume();

		mSubjectEditText.applytheme();
		mSubjectEditText.setTypeface(Utils.getInstance().getNoteFont());
		mSubjectEditText.setTextSize(Utils.getInstance().getNoteFontSize());
		
		mTitleEditText.setTypeface(Utils.getInstance().getNoteFont());
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflator = getMenuInflater();
		inflator.inflate(R.menu.create_edit_menu, menu);
		
		mMenu = menu;
		
		final MenuItem editMenu = menu.findItem(R.id.menu_edit_note); 
		final MenuItem titleMenu = menu.findItem(R.id.menu_title);
		
		if(mNoteActionType == NotesActivity.NOTE_EDIT) {
			editMenu.setVisible(true);
			titleMenu.setEnabled(false);
		} else {
			editMenu.setVisible(false);
			titleMenu.setEnabled(true);
		}
		
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			final Intent i = new Intent(CreateEditNoteActivity.this, NotesActivity.class);
			i.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
			startActivity(i);
			break;
		case R.id.menu_edit_note:
			
			mTitleEditText.setFocusable(true);
			mTitleEditText.setFocusableInTouchMode(true);
			
			mSubjectEditText.setFocusable(true);
			mSubjectEditText.setFocusableInTouchMode(true);
			mSubjectEditText.requestFocus();
			
			final InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
			imm.toggleSoftInput(InputMethodManager.SHOW_FORCED,0);
			
			final MenuItem editMenu = mMenu.findItem(R.id.menu_edit_note); 
			final MenuItem titleMenu = mMenu.findItem(R.id.menu_title);

			editMenu.setVisible(false);
			editMenu.setEnabled(false);
			
			titleMenu.setEnabled(true);

			break;
		case R.id.menu_title:
			if(mTitleEditText.getVisibility() != View.VISIBLE) {
				mTitleEditText.setVisibility(View.VISIBLE);
				mTitleEditText.requestFocus();
			}
			break;
		case R.id.menu_font:
			displayFonts();
			break;
		case R.id.menu_textsize:
			displayTextSizes();
			break;
		case R.id.menu_theme:
			displayThemes();
			break;
		case R.id.menu_discard:
			finish();
			break;
		}
		return true;
	}

	private void displayFonts() {
		final AlertDialog.Builder builder = new AlertDialog.Builder(CreateEditNoteActivity.this);
		builder.setTitle(R.string.select_font);
		
		final int noteFont = Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(CreateEditNoteActivity.this).
										getString(SettingsActivity.PREF_TEXT_FONT, ""+SettingsActivity.TEXT_FONT_SANS));
		final ListAdapter adapter = new ArrayAdapter<String>(CreateEditNoteActivity.this, 
									android.R.layout.simple_list_item_single_choice, 
									getResources().getStringArray(R.array.text_font_entries)) {
			@Override
			public View getView(int position, View convertView, ViewGroup parent) {
				View v = super.getView(position, convertView, parent);
				if(position == noteFont) {
					((CheckedTextView)v).setChecked(true);
				} else {
					((CheckedTextView)v).setChecked(false);
				}
				return v;
			}
		};
		
		builder.setAdapter(adapter, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				Utils.getInstance().updateFont(which);
				Utils.getInstance().loadNoteFont(which);
				mSubjectEditText.setTypeface(Utils.getInstance().getNoteFont());
				mSubjectEditText.invalidate();
				
				mTitleEditText.setTypeface(Utils.getInstance().getNoteFont());
			}
		});
		builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) { }
		});
		builder.create().show();
	}
	
	private void displayTextSizes() {
		final AlertDialog.Builder builder = new AlertDialog.Builder(CreateEditNoteActivity.this);
		builder.setTitle(R.string.select_text_size);
		
		final int textSize = Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(CreateEditNoteActivity.this).
				getString(SettingsActivity.PREF_TEXT_SIZE, ""+SettingsActivity.TEXT_SIZE_MEDIUM));
		final ListAdapter adapter = new ArrayAdapter<String>(CreateEditNoteActivity.this, 
									android.R.layout.simple_list_item_single_choice, 
									getResources().getStringArray(R.array.text_size_entries)) {
				@Override
				public View getView(int position, View convertView, ViewGroup parent) {
					View v = super.getView(position, convertView, parent);
					if(position == textSize) {
						((CheckedTextView)v).setChecked(true);
					} else {
						((CheckedTextView)v).setChecked(false);
					}
					return v;
				}
			
		};
		builder.setAdapter(adapter, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				Utils.getInstance().updateTextSize(which);
				Utils.getInstance().loadNoteFontSize(which);
				mSubjectEditText.setTextSize(Utils.getInstance().getNoteFontSize());
				mSubjectEditText.invalidate();
			}
		});
		builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) { }
		});
		builder.create().show();
	}
	
	private void displayThemes() {
		final AlertDialog.Builder builder = new AlertDialog.Builder(CreateEditNoteActivity.this);
		builder.setTitle(R.string.select_theme);
		
		final int theme = Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(CreateEditNoteActivity.this).
				getString(SettingsActivity.PREF_THEME, ""+SettingsActivity.THEME_PLAIN));
		final ListAdapter adapter = new ArrayAdapter<String>(CreateEditNoteActivity.this, 
									android.R.layout.simple_list_item_single_choice, 
									getResources().getStringArray(R.array.theme_entries)) {
				@Override
				public View getView(int position, View convertView, ViewGroup parent) {
					View v = super.getView(position, convertView, parent);
					if(position == theme) {
						((CheckedTextView)v).setChecked(true);
					} else {
						((CheckedTextView)v).setChecked(false);
					}
					return v;
				}
			
		};
		builder.setAdapter(adapter, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				Utils.getInstance().updateTheme(which);
				mSubjectEditText.applytheme();
				mSubjectEditText.invalidate();
			}
		});
		builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) { }
		});
		builder.create().show();
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if(keyCode == KeyEvent.KEYCODE_BACK) {
			saveNoteAndEnterIntoDB();
		} 
		return super.onKeyDown(keyCode, event);
	}

	private void saveNoteAndEnterIntoDB() {
		final Editable editabletext = mSubjectEditText.getEditableText();
		if(editabletext != null) {
			final String text = editabletext.toString();
			if(!TextUtils.isEmpty(text)) {

				saveToDatabase();

				final Intent serviceIntent = new Intent(CreateEditNoteActivity.this, SaveNoteService.class);
				serviceIntent.putExtra("text", text);
				serviceIntent.putExtra("filepath", mNotePath);
				startService(serviceIntent);
			} else {
				final File fileToDelete = new File(mNotePath);
				if(fileToDelete != null && fileToDelete.exists()) {
					fileToDelete.delete();
				}
			}
		}
	}

	private void saveToDatabase() {
		mNoteModifiedtime = System.currentTimeMillis();
		final String title = getNoteTitle();
		NotesDBHelper dbHelper = NotesDBHelper.getInstance(CreateEditNoteActivity.this);
		dbHelper.insertNewNote(mNoteCreationtime, mNoteModifiedtime, title, mNotePath, mValuesList.get(mItemPosition), mShouldInsertCustomTitle);
	}

	public String getNoteTitle() {
		mShouldInsertCustomTitle = 0;
		String title = null; 
		final Editable titleText = mTitleEditText.getText();
		if(titleText != null) {
			final String str = titleText.toString();
			if(!TextUtils.isEmpty(str)) {
				mShouldInsertCustomTitle = 1;
				title = str;
			}  else {
				final String text = mSubjectEditText.getText().toString();
				if(text.length() <= MAX_TITLE_LEN) {
					title = text;
				} else {
					title = text.substring(0, MAX_TITLE_LEN);
				}
			}
		} else {
			final String text = mSubjectEditText.getText().toString();
			if(text.length() <= MAX_TITLE_LEN) {
				title = text;
			} else {
				title = text.substring(0, MAX_TITLE_LEN);
			}
		}
		return title;
	}

	class DisplaySavedNote extends AsyncTask<String, Void, Void> {

		private String toDisplay;
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			mProgressBar.setVisibility(View.VISIBLE);
		}

		@Override
		protected Void doInBackground(String... params) {
			toDisplay = readFile(params[0]);
			return null;
		}
		
		private String readFile(final String fileToRead) {
			BufferedReader br = null;
			StringBuffer sb = new StringBuffer();
			try {
				String str;
				br = new BufferedReader(new FileReader(fileToRead));
				while ((str = br.readLine()) != null) {
					sb.append(str);
					sb.append("\n");
				}
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				try {
					if (br != null)br.close();
				} catch (IOException ex) {
					ex.printStackTrace();
				}
			}
			return sb.toString();
		}

		@Override
		protected void onPostExecute(Void result) {
			super.onPostExecute(result);
			mProgressBar.setVisibility(View.INVISIBLE);
			mSubjectEditText.setText(toDisplay);
		}
	}

	@Override
	public boolean onNavigationItemSelected(int itemPosition, long itemId) {
		mItemPosition = itemPosition;
		return false;
	}
}
