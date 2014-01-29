package com.mkr.notes;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.app.Service;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.style.ImageSpan;
import android.view.ActionMode;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.mkr.cloud.CloudUtils;
import com.mkr.cloud.GoogleDrive;
import com.mkr.notes.NotesAdapter.Holder;
import com.mkr.notes.labels.LabelUtils;
import com.mkr.notes.labels.LabelsActivity;
import com.mkr.notesdatabase.NotesDBHelper;

public class NotesActivity extends Activity implements OnSharedPreferenceChangeListener, TextWatcher, OnClickListener {

	public static final String TAG = "Sync Notes";
	
	public static final int NOTE_CREATE = 0;
	public static final int NOTE_EDIT = 1;
	
	public static final int DISPLAY_FILTER_ALL 		= 0;
	public static final int DISPLAY_FILTER_SEARCH 	= 1;
	public static final int DISPLAY_FILTER_LABLES 	= 2;
	
	public static final int MSG_DELETE_NOTES 			 = 0;
	
	public static final String INTENT_KEY_NOTE_TYPE      =  "note_type";
	public static final String INTENT_KEY_NOTE_TITLE     =  "note_title";
	public static final String INTENT_KEY_CREATE_TIME    =  "creation_time";
	public static final String INTENT_KEY_MODIFIED_TIME  =  "modified_time";
	public static final String INTENT_KEY_NOTE_PATH  	 =  "note_path";
	public static final String INTENT_KEY_NOTE_LABEL  	 =  "note_label";
	public static final String INTENT_KEY_HAS_TITLE  	 =  "note_has_title";

	public static final String NOTES_PARENT_DIR_NAME = "notes";

	public static String INTERNAL_STORAGE_PATH;
	
	private NotesAdapter mNotesAdapter;
	private NotesDBHelper mDBHelper;

	private LinearLayout mLabelHeaderLinearLayout;
	private ListView mNotesListView;
	private ProgressBar mProgressBar;
	private TextView mTextview;
	private ActionMode mActionMode;

	private List<Long> mSelectedItemsList;
	private DrawerLayout mDrawerLayout;
	private ActionBarDrawerToggle mDrawerToggle;
	
	private AlertDialog mHeplpTutorialAlertDialog;
	private int mHelpTutorialPage;
	
	private ActionBar mActionBar;
	private MenuItem mSearchMenuItem; 
	private int mCurrentNotesDisplayType = DISPLAY_FILTER_ALL;
	
	private AdView mAdView;
	
	private String mSelectedFilterLabel = null;
	
	Handler handler = new Handler() {
		public void handleMessage(Message msg) {
			int which = msg.what;
			switch (which) {
			case MSG_DELETE_NOTES:
				final File[] files = (File[]) msg.obj;
				
				for (File file : files) {
					String creationTime = Utils.getOnlyFileName(file.getName());
					mDBHelper.deleteNote(Long.valueOf(creationTime));
					
					//also delete the file from internal storage
					if(file.exists()) {
						file.delete();
					}
				}
				
				if(mCurrentNotesDisplayType == DISPLAY_FILTER_SEARCH) {
					removeCustomSearchView();
				} else {
					DisplaySavedLists displaySavedListsTask = new DisplaySavedLists();
					displaySavedListsTask.setStringString(null);
					displaySavedListsTask.execute();
				}
				break;
			}
		};
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_main);
		mSelectedItemsList = new ArrayList<Long>();

		mCurrentNotesDisplayType = DISPLAY_FILTER_ALL;
		INTERNAL_STORAGE_PATH = getFilesDir().getPath() +"/" + NOTES_PARENT_DIR_NAME;
		
		final Utils utils = Utils.getInstance();
		utils.init(NotesActivity.this);
		
		final LabelUtils labelUtils = LabelUtils.getInstance();
		labelUtils.init(NotesActivity.this);
		
		final CloudUtils cloudUtils = CloudUtils.getInstance();
		cloudUtils.init(NotesActivity.this);
		
		mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);
		 
        mActionBar = getActionBar();
        mActionBar.setDisplayHomeAsUpEnabled(true);
        mActionBar.setHomeButtonEnabled(true);
        mActionBar.setCustomView(R.layout.action_bar_search);
        final EditText mSearchEditText = (EditText) mActionBar.getCustomView().findViewById(R.id.search_view);
        mSearchEditText.addTextChangedListener(this);
        
		mDBHelper = NotesDBHelper.getInstance(NotesActivity.this);
		mDBHelper.open();

		mProgressBar = (ProgressBar) findViewById(R.id.notes_progress_bar);
		mTextview = (TextView) findViewById(R.id.notes_message_textview);
		mNotesListView = (ListView) findViewById(R.id.notes_list_view);
		mLabelHeaderLinearLayout = (LinearLayout) findViewById(R.id.labels_header);
		mLabelHeaderLinearLayout.setVisibility(View.GONE);
		findViewById(R.id.labelfilter_navigate_back).setOnClickListener(this);
		
		mTextview.setTypeface(utils.getFontTypefaceForTitles());
		mNotesListView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
				final Holder holder = (Holder) arg1.getTag();

				if(mActionMode != null) {
					if(mSelectedItemsList.contains(holder.mCreationDate)) {
						mSelectedItemsList.remove(holder.mCreationDate);
					} else {
						mSelectedItemsList.add(holder.mCreationDate);
					}

					if(mSelectedItemsList.size() == 0) {
						//refresh the ui list
						DisplaySavedLists displaySavedListsTask = new DisplaySavedLists();
						displaySavedListsTask.setStringString(null);
						displaySavedListsTask.execute();
						mActionMode.finish();
						return;
					}
					
					mActionMode.setTitle(""+mSelectedItemsList.size());
					mNotesAdapter.setSelectedLists(mSelectedItemsList);
					mActionMode.invalidate();
					return;
				}

				final Intent i = new Intent(NotesActivity.this, CreateEditNoteActivity.class);
				i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				i.putExtra(INTENT_KEY_NOTE_TYPE, NOTE_EDIT);
				i.putExtra(INTENT_KEY_NOTE_TITLE, holder.mTitle.getText());
				i.putExtra(INTENT_KEY_CREATE_TIME, holder.mCreationDate);
				i.putExtra(INTENT_KEY_MODIFIED_TIME, -1);
				i.putExtra(INTENT_KEY_NOTE_PATH, holder.mNotePath);
				i.putExtra(INTENT_KEY_NOTE_LABEL, holder.mNoteLabel);
				i.putExtra(INTENT_KEY_HAS_TITLE, holder.mHasTitle);
				startActivity(i);
			}
		});

		mNotesListView.setOnItemLongClickListener(new OnItemLongClickListener() {
			@Override
			public boolean onItemLongClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
				final Holder holder = (Holder) arg1.getTag();

				if(mSelectedItemsList.contains(holder.mCreationDate)) {
					mSelectedItemsList.remove(holder.mCreationDate);
				} else {
					mSelectedItemsList.add(holder.mCreationDate);
				}

				if(mSelectedItemsList.size() == 0) {
					//refresh the ui list
					DisplaySavedLists displaySavedListsTask = new DisplaySavedLists(); ;
					displaySavedListsTask.setStringString(null);
					displaySavedListsTask.execute();
					mActionMode.finish();
					return true;
				}
				
				mNotesAdapter.setSelectedLists(mSelectedItemsList);
				if(mActionMode == null) {
					mActionMode = NotesActivity.this.startActionMode(ActionModeCallback);
				} else {
					mActionMode.invalidate();
				}
				mActionMode.setTitle(""+mSelectedItemsList.size());
				return true;
			}
		});

		mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, R.drawable.ic_drawer,  
				R.string.drawer_open, R.string.drawer_close ) {
			public void onDrawerClosed(View view) {
				invalidateOptionsMenu(); 
			}
			public void onDrawerOpened(View drawerView) {
				updateSidepaneHandler.sendEmptyMessage(0);
				if(!mSearchMenuItem.isVisible()) {
					removeCustomSearchView();
				}
				invalidateOptionsMenu(); 
			}
		};
		mDrawerLayout.setDrawerListener(mDrawerToggle);
	        
		createNotesFolder();
		
		final SharedPreferences sPref = PreferenceManager.getDefaultSharedPreferences(NotesActivity.this); 
		sPref.registerOnSharedPreferenceChangeListener(this);
		
		updateSidepaneHandler.sendEmptyMessage(0);
		
		mAdView = (AdView)this.findViewById(R.id.adView);
	    final AdRequest adRequest = new AdRequest.Builder().build();
	    mAdView.loadAd(adRequest);
	
		final boolean shouldDispChangeLog = sPref.getBoolean(SettingsActivity.PREF_FIRST_LAUNCH, true);
		if(shouldDispChangeLog) {
			sPref.edit().putBoolean(SettingsActivity.PREF_FIRST_LAUNCH, false).commit();
			displayWatsNewDialog();
		}
	}

	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
		mDrawerToggle.syncState();
	}
	
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		mDrawerToggle.onConfigurationChanged(newConfig);
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		
		mAdView.resume();
		if(mCurrentNotesDisplayType == DISPLAY_FILTER_SEARCH) { 
			removeCustomSearchView();
		} else {
			//load all the saved notes and display in the activity
			DisplaySavedLists displaySavedListsTask = new DisplaySavedLists();
			displaySavedListsTask.setStringString(null);
			displaySavedListsTask.execute();
		}
		if(CloudUtils.isDropboxLoginStarted()) {
			CloudUtils.finishDropboxAuthentication();
		}
	}

	@Override
	protected void onPause() {
		mAdView.pause();
		super.onPause();
	}
	
	/**
	 * create a notes folder in the internal storage in which all the notes will be saved
	 */
	private void createNotesFolder() {
		File file = new File(INTERNAL_STORAGE_PATH);
		if(!file.exists()) {
			file.mkdir();
		}
	}

	/**
	 * create a new note in the internal storage
	 * @param creationTime
	 * @return
	 */
	private String createNewNoteFile(final long creationTime) {
		final String fileToCreatePath = INTERNAL_STORAGE_PATH + "/" + creationTime+".txt";
		final File fileToCreate = new File(fileToCreatePath);
		try {
			fileToCreate.createNewFile();
		} catch (IOException e) {
			//e.printStackTrace();
		}
		return fileToCreatePath;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.action_bar_menu_options, menu);
		
		mSearchMenuItem = menu.findItem(R.id.menu_search); 
		if(mCurrentNotesDisplayType == DISPLAY_FILTER_SEARCH) {
			mSearchMenuItem.setVisible(false);
		}
		return true;
	} 

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (mDrawerToggle.onOptionsItemSelected(item)) {
			return true;
		}
		switch (item.getItemId()) {
		case R.id.menu_search:

			//close the drawer
			mDrawerLayout.closeDrawers();
			
			mCurrentNotesDisplayType = DISPLAY_FILTER_SEARCH;
			mSearchMenuItem.setVisible(false);
			mActionBar.setDisplayShowCustomEnabled(true);
			
			final EditText searchEdit = (EditText) mActionBar.getCustomView().findViewById(R.id.search_view);
			searchEdit.setText("");
			searchEdit.requestFocus();
			
			break;
		case R.id.menu_add_new_note:
			final Intent i = new Intent(NotesActivity.this, CreateEditNoteActivity.class);
			i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			i.putExtra(INTENT_KEY_NOTE_TYPE, NOTE_CREATE);
			final long creationTime = System.currentTimeMillis();
			i.putExtra(INTENT_KEY_CREATE_TIME, creationTime);
			i.putExtra(INTENT_KEY_MODIFIED_TIME, -1);
			i.putExtra(INTENT_KEY_NOTE_PATH,createNewNoteFile(creationTime));
			startActivity(i);
			break;
		case R.id.menu_settings:
			final Intent intent = new Intent(NotesActivity.this, SettingsActivity.class);
			intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			startActivity(intent);
			break;
		case R.id.menu_help:
			displayHelpDialog(0);
			break;
		case R.id.menu_feedback:
			final Intent emailIntent = new Intent(android.content.Intent.ACTION_SEND);
			emailIntent.setType("plain/text");
			emailIntent.putExtra(android.content.Intent.EXTRA_EMAIL, new String[] { "manthena.android@gmail.com" });
			emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "SyncNotes : New feature/Suggestion");
			startActivity(Intent.createChooser(emailIntent,"Send your feedback"));
			break;
		}
		return true;
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if(keyCode == KeyEvent.KEYCODE_BACK) { 
			if(mCurrentNotesDisplayType == DISPLAY_FILTER_SEARCH) {
				removeCustomSearchView();
				return true;
			} else if(mCurrentNotesDisplayType == DISPLAY_FILTER_LABLES) {
				backPressedFromFilterLabels();
				return true;
			}
		}
		return super.onKeyDown(keyCode, event);
	}
	
	private void backPressedFromFilterLabels() {
		mCurrentNotesDisplayType = DISPLAY_FILTER_ALL;
		mSelectedFilterLabel = null;
		
		//load all the saved notes and display in the activity
		DisplaySavedLists displaySavedListsTask = new DisplaySavedLists();
		displaySavedListsTask.setStringString(null);
		displaySavedListsTask.execute();
		
		mLabelHeaderLinearLayout.setVisibility(View.GONE);
	}
	
	private void removeCustomSearchView() {
		final EditText searchEdit = (EditText) mActionBar.getCustomView().findViewById(R.id.search_view);
		final InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(searchEdit.getWindowToken(), 0);
		
		mActionBar.setDisplayShowCustomEnabled(false);
		mSearchMenuItem.setVisible(true);
		mCurrentNotesDisplayType = DISPLAY_FILTER_ALL;
		
		//load all the saved notes and display in the activity
		DisplaySavedLists displaySavedListsTask = new DisplaySavedLists();
		displaySavedListsTask.setStringString(null);
		displaySavedListsTask.execute();
	}
	
	private ActionMode.Callback ActionModeCallback = new ActionMode.Callback() {
		@Override
		public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
			 MenuItem item = menu.findItem(R.id.item_share);
			 if(mSelectedItemsList.size() > 1) {
				 item.setVisible(false);
			 } else {
				 item.setVisible(true);
			 }
			return true;
		}

		@Override
		public void onDestroyActionMode(ActionMode mode) {
			mActionMode = null;
			mSelectedItemsList.clear();
			mNotesAdapter.notifyDataSetChanged();
		}

		@Override
		public boolean onCreateActionMode(ActionMode mode, Menu menu) {
			MenuInflater inflator = getMenuInflater();
			inflator.inflate(R.menu.long_pressed, menu);
			return true;
		}

		@Override
		public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
			
			final int selectedFilesLength = mSelectedItemsList.size();
			final File[] files = new File[selectedFilesLength];
			for (int i = 0; i < selectedFilesLength; i++) {
				final String path = INTERNAL_STORAGE_PATH + "/" + mSelectedItemsList.get(i) + ".txt";
				files[i] = new File(path);
			}
			
			switch (item.getItemId()) {
			case R.id.item_delete:
				deleteNote(files);
				break;
			case R.id.item_save:
				backupSelectedNotes(files);
				break;
			case R.id.item_share:
				shareNotes();
				break;
			case R.id.item_cloud:
				displaySelectCloudDialog(files);
				break;
			case R.id.item_info:
				displayNotesInfo(files);
				break;
			default:
				break;
			}
			
			if(mCurrentNotesDisplayType == DISPLAY_FILTER_SEARCH) {
				removeCustomSearchView();
			} else {
				//refresh the ui list
				DisplaySavedLists displaySavedListsTask = new DisplaySavedLists(); ;
				displaySavedListsTask.setStringString(null);
				displaySavedListsTask.execute();
			}
			
			mode.finish();
			return true;
		}
	};

	private void displayNotesInfo(final File[] files) {
		final AlertDialog.Builder builder = new AlertDialog.Builder(NotesActivity.this);
		builder.setTitle(getString(R.string.info));	
		builder.setMessage(getMessageStringForInfoDialog(files));
		builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				
			}
		});
		builder.create().show();
	}
	
	private String getMessageStringForInfoDialog(final File[] files) {
		final StringBuffer sb = new StringBuffer(); 
		final Map<Long, Note> notesInfo = NotesDBHelper.getCurrentNotesMap();
		if(files.length == 1) {
			final File file = files[0];
			sb.append(" Name : " + notesInfo.get(mSelectedItemsList.get(0)).title + ".txt" + "\n");
			sb.append(" Path : "+ file.getParent() + "\n");
			sb.append(" Size : "+ Utils.readableFileSize(file.length()) + " ("+file.length() +" bytes)" + "\n\n");
			sb.append(" Modified : " + Utils.getReadableTime(file.lastModified())+"\n");
		} else {
			sb.append(" Contains : " + files.length + " Items\n");
			sb.append(" Path : "+ files[0].getParent() + "\n");
			
			long totalSize = 0; 
			for(final File f : files) {
				totalSize += f.length();
			}
			sb.append(" Total Size : " +  Utils.readableFileSize(totalSize));
		}
		
		return sb.toString();
	}
	
	private void displaySelectCloudDialog(final File[] filesToUpload) {
		
		final AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(getString(R.string.select_cloud_options));
		final String[] options = CloudUtils.getInstance().getAllInstalledCloudOptions();
		//if no cloud storage are selected yet display a user asking to login to atleast one account
		final ListView optionsListView = new ListView(NotesActivity.this);
		if(options == null) {
			builder.setMessage(getString(R.string.login_into));
		} else {
			
			optionsListView.setCacheColorHint(Color.TRANSPARENT);
			optionsListView.setAdapter(new ArrayAdapter<String>(NotesActivity.this, 
					android.R.layout.simple_list_item_single_choice,
                    android.R.id.text1, options));
			optionsListView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
			builder.setView(optionsListView);
			
			/*builder.setSingleChoiceItems(options, 0, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
				}
			});*/
		}
		
		builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				if(options == null) {
					//display the side panel so that the user can login into one cloud storage
					mDrawerLayout.openDrawer(Gravity.LEFT);
				} else {
					if(CloudUtils.getInstance().isNetworkConnected()) {
						final int checkedPosition = optionsListView.getCheckedItemPosition();
						if(checkedPosition != -1) {
							if(getString(R.string.dropbox_title).equals(options[checkedPosition])) {
								CloudUtils.getInstance().uploadFiles(filesToUpload, R.string.dropbox_title);
							} else if(getString(R.string.googledrive_title).equals(options[checkedPosition])) {
								//CloudUtils.getInstance().uploadFiles(filesToUpload, R.string.googledrive_title);
								CloudUtils.displayDriveDialog(filesToUpload);
							} else {
								//nothing
							}
						}
					} else {
						Toast.makeText(NotesActivity.this, getString(R.string.no_network), Toast.LENGTH_LONG).show();
					}
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
	
	private void updateSidePaneLayout() {
		final RelativeLayout dropBoxView = (RelativeLayout) findViewById(R.id.side_pane_item_dropbox);
		final RelativeLayout googleDriveView = (RelativeLayout) findViewById(R.id.side_pane_item_googledrive);
		
		((TextView) dropBoxView.findViewById(R.id.side_pane_item_title)).setText(getString(R.string.dropbox_title));
		((TextView) googleDriveView.findViewById(R.id.side_pane_item_title)).setText(getString(R.string.googledrive_title));
		
		((ImageView) dropBoxView.findViewById(R.id.side_pane_item_icon)).setImageResource(R.drawable.dropbox);
		((ImageView) googleDriveView.findViewById(R.id.side_pane_item_icon)).setImageResource(R.drawable.googledrive);
		
		if(!CloudUtils.isDropBoxLoggedIn()) {
			((TextView) dropBoxView.findViewById(R.id.side_pane_item_summary)).setText(getString(R.string.subtitle_login));
		} else {
			((TextView) dropBoxView.findViewById(R.id.side_pane_item_summary)).setText(getString(R.string.subtitle_logout));
		}
		
		if(!CloudUtils.isGoogleDriveLoggedIn()) {
			((TextView) googleDriveView.findViewById(R.id.side_pane_item_summary)).setText(getString(R.string.subtitle_login));
		} else {
			((TextView) googleDriveView.findViewById(R.id.side_pane_item_summary)).setText(getString(R.string.subtitle_logout));
		}
		
		final LinearLayout labelsParent = (LinearLayout) findViewById(R.id.labels_parent_linear_layout);
		labelsParent.removeAllViews();
		
		final Map<String, ?> labels = LabelUtils.getAllSavedLables();
		final Iterator<String> keysIterator = labels.keySet().iterator();
		final LayoutInflater inflater = (LayoutInflater) getSystemService(Service.LAYOUT_INFLATER_SERVICE);
		
		final ArrayList<String> labelNamesList = new ArrayList<String>();
		labelNamesList.add(0, LabelsActivity.LABELS_DEFAULT_PERSONAL);
		labelNamesList.add(1, LabelsActivity.LABELS_DEFAULT_WORK);
		labelNamesList.add(2, LabelsActivity.LABELS_DEFAULT_IDEAS);
		
		while (keysIterator.hasNext()) {
			final String labelName = keysIterator.next();
			if(!(LabelsActivity.LABELS_DEFAULT_PERSONAL.equals(labelName) 
					|| LabelsActivity.LABELS_DEFAULT_WORK.equals(labelName) 
					|| LabelsActivity.LABELS_DEFAULT_IDEAS.equals(labelName))) {
				labelNamesList.add(labelName);
			}
		}
		
		final Map<String, Integer> lableDetails = mDBHelper.getLabelsDetails();
		
		for (String label : labelNamesList ) {
			final RelativeLayout lableLayout = (RelativeLayout) inflater.inflate(R.layout.label_item, null);
			lableLayout.setOnClickListener(this); 
			((TextView)lableLayout.findViewById(R.id.lable_name)).setText(label);
			((TextView)lableLayout.findViewById(R.id.lable_color)).setBackgroundColor((Integer) labels.get(label));
			
			int count = 0;
			if(lableDetails.containsKey(label)) {
				count = lableDetails.get(label);
			}
			((TextView)lableLayout.findViewById(R.id.lable_count)).setText("(" + count + ")");
			
			labelsParent.addView(lableLayout);
		}
		
		dropBoxView.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				mDrawerLayout.closeDrawers();
				CloudUtils.getInstance().loginIntoDropBox(NotesActivity.this);
			}
		});
		
		googleDriveView.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				mDrawerLayout.closeDrawers();
				CloudUtils.getInstance().loginIntoGoogleDrive();
			}
		});
		
		((ImageView) findViewById(R.id.edit_labels)).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				mDrawerLayout.closeDrawers();
				
				Intent i = new Intent(NotesActivity.this, LabelsActivity.class);
				i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				startActivity(i);
			}
		});
	}
	
	private void deleteNote(final File[] files) {
		
		AlertDialog.Builder builder = new AlertDialog.Builder(NotesActivity.this);
		builder.setTitle(R.string.confirm_delete_title);
		builder.setMessage(R.string.confirm_delete_msg);
		builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				Message msg = Message.obtain(handler, MSG_DELETE_NOTES, files);
				handler.sendMessage(msg);
			}
		});
		
		builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) { }
		});
		builder.create().show();
	}
	
	/**
	 * backup the selected files into the sdcard
	 * @param files
	 */
	private void backupSelectedNotes(final File[] files) {
		final String storageState = Environment.getExternalStorageState();
		if(!storageState.equals(Environment.MEDIA_MOUNTED)) {
			Toast.makeText(NotesActivity.this, getString(R.string.no_sdcard), Toast.LENGTH_LONG).show();
			return;
		}
		new BackupFiles().execute(files);
	}
	
	Handler updateSidepaneHandler = new Handler() {
		public void handleMessage(Message msg) {
			updateSidePaneLayout();
		};
	};
	
	class BackupFiles extends AsyncTask<File, Void, Void> {
		private ProgressDialog pd;
		
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			pd = ProgressDialog.show(NotesActivity.this, null, getString(R.string.backup_title));
		}

		@Override
		protected Void doInBackground(File... params) {
			
			//check if any folder is already created
			final String externalStoragePath = Environment.getExternalStorageDirectory().getPath();
			final File folder = new File(externalStoragePath +"/"+ getString(R.string.app_name));
			if(!folder.exists()) {
				folder.mkdirs();
			}
			
			final Map<Long, Note> notesMap = NotesDBHelper.getCurrentNotesMap();
			for (final File file : params) {
				final String fileNameWithoutExt = Utils.getOnlyFileName(file.getName());
				final Note note = notesMap.get(Long.valueOf(fileNameWithoutExt));
				String title = file.getName(); 
				if(note != null && note.title != null) {
					title = note.title +".txt";
				}
				//save based on the title note, if unable save based on the creation time
				saveNote(file.getPath(), folder.getPath()+"/"+title);
			}
			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			super.onPostExecute(result);
			pd.dismiss();
			
			final String path = Environment.getExternalStorageDirectory().getPath() +"/"+ getString(R.string.app_name);
			Toast.makeText(NotesActivity.this, getString(R.string.saved_to_sdcard) + " " + path, Toast.LENGTH_LONG).show();
		}
		
		public void saveNote(final String sourceLocation, final String targetLocation) {
			try {
				InputStream in = new FileInputStream(sourceLocation);
				OutputStream out = new FileOutputStream(targetLocation);

				// Copy the bits from instream to outstream
				byte[] buf = new byte[1024];
				int len;
				while ((len = in.read(buf)) > 0) {
					out.write(buf, 0, len);
				}
				in.close();
				out.close();
			}catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	class DisplaySavedLists extends AsyncTask<Void, Void, Void> {

		private ArrayList<Note> data;
		private String searchString;

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			data = new ArrayList<Note>();
		}

		public void setStringString(final String searchStr) {
			searchString = searchStr;
		}
		
		@Override
		protected Void doInBackground(Void... params) {
			switch (mCurrentNotesDisplayType) {
			case DISPLAY_FILTER_ALL:
				data = mDBHelper.getAllSavedNotes();
				break;
			case DISPLAY_FILTER_SEARCH:
				data = mDBHelper.getSearchNotes(searchString);
				break;
			case DISPLAY_FILTER_LABLES:
				data = mDBHelper.getNotesForLabel(mSelectedFilterLabel);
				break;
			default:
				data = mDBHelper.getAllSavedNotes();
				break;
			}
			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			super.onPostExecute(result);

			mNotesAdapter = new NotesAdapter(NotesActivity.this, data);
			mNotesListView.setAdapter(mNotesAdapter);

			mProgressBar.setVisibility(View.GONE);
			if(data == null || data.size() == 0) {
				mNotesListView.setVisibility(View.GONE);
				mTextview.setVisibility(View.VISIBLE);

				if(mCurrentNotesDisplayType == DISPLAY_FILTER_ALL) {
					SpannableStringBuilder msgToDisplay = new SpannableStringBuilder("\nTap on   icon to create new note.");
					Bitmap smiley = BitmapFactory.decodeResource( getResources(), R.drawable.ic_add);
					msgToDisplay.setSpan(new ImageSpan( smiley ), 8, 9, Spannable.SPAN_INCLUSIVE_INCLUSIVE );
					mTextview.setText(msgToDisplay);
				} else if(mCurrentNotesDisplayType == DISPLAY_FILTER_SEARCH) {
					mTextview.setText(getString(R.string.no_search_results));
				} else if(mCurrentNotesDisplayType == DISPLAY_FILTER_LABLES) {
					mTextview.setText(getString(R.string.no_labels_results));
				}
			} else {
				mNotesListView.setVisibility(View.VISIBLE);
				mTextview.setVisibility(View.GONE);
			}
		}
	}

	private void shareNotes() {
		if(mSelectedItemsList == null || mSelectedItemsList.size() == 0) {
			return;
		}
		
		final String path = INTERNAL_STORAGE_PATH + "/" + mSelectedItemsList.get(0) + ".txt";
		final File file = new File(path);
		
		Intent i = new Intent();
		i.setAction(Intent.ACTION_SEND);
		i.setType("text/plain");
		i.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file));
		i = Intent.createChooser(i, "Send to ..");

		try {
			startActivity(i);
		} catch (Exception e) {
			
		}
	}
	
	/**
	 * displays the change logs for the version
	 */
	private void displayWatsNewDialog() {
		final AlertDialog.Builder builder = new AlertDialog.Builder(NotesActivity.this);
		builder.setTitle(R.string.welcome_msg);

		final ScrollView scrollView = new ScrollView(this);

		final TextView propertiesMsg = new TextView(this); 
		propertiesMsg.setPadding(10, 0, 0, 0);
		propertiesMsg.setTextSize(15);
		propertiesMsg.setText(R.string.change_log);

		scrollView.addView(propertiesMsg);
		builder.setView(scrollView);

		builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				 PreferenceManager.getDefaultSharedPreferences(NotesActivity.this).edit().putBoolean(SettingsActivity.PREF_FIRST_LAUNCH, false).commit();
				 displayHelpDialog(0);
			}
		});

		builder.create().show();
	}
	
	private void displayHelpDialog(int id) {
		mHelpTutorialPage = 1;

		final AlertDialog.Builder builder = new AlertDialog.Builder(NotesActivity.this);
		final Resources res = getResources();

		String title = "help_page_title_"+mHelpTutorialPage;
		String message = "help_page_description_"+mHelpTutorialPage;

		String resourceTitle = getString(res.getIdentifier(title, "string", getPackageName()));
		String resourceBody = getString(res.getIdentifier(message, "string", getPackageName()));

		builder.setTitle(resourceTitle);
		builder.setMessage(resourceBody);

		final int maxPages = res.getInteger(R.integer.max_help_tut_pages);

		builder.setPositiveButton(getString(R.string.next), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {

			}
		});

		builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				mHelpTutorialPage = 1;
			}
		});

		builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
			@Override
			public void onCancel(DialogInterface dialog) {
				mHelpTutorialPage = 1;
			}
		});

		mHeplpTutorialAlertDialog = builder.create();
		mHeplpTutorialAlertDialog.show();

		View button = mHeplpTutorialAlertDialog.getButton(DialogInterface.BUTTON_POSITIVE);
		button.setOnClickListener(new View.OnClickListener() {
			@Override public void onClick(View v) {
				mHelpTutorialPage ++;

				String title = "help_page_title_"+mHelpTutorialPage;
				String message = "help_page_description_"+mHelpTutorialPage;

				String resourceTitle = getString(res.getIdentifier(title, "string", getPackageName()));
				String resourceBody = getString(res.getIdentifier(message, "string", getPackageName()));

				mHeplpTutorialAlertDialog.setTitle(resourceTitle);
				mHeplpTutorialAlertDialog.setMessage(resourceBody);

				if(mHelpTutorialPage == maxPages - 1) {
					mHeplpTutorialAlertDialog.getButton(Dialog.BUTTON_POSITIVE).setClickable(false);
					mHeplpTutorialAlertDialog.getButton(Dialog.BUTTON_POSITIVE).setEnabled(false);
				} else {
					mHeplpTutorialAlertDialog.getButton(Dialog.BUTTON_POSITIVE).setClickable(true);
					mHeplpTutorialAlertDialog.getButton(Dialog.BUTTON_POSITIVE).setEnabled(true);
				}
			}
		});
	}
	
	@Override
	protected void onDestroy() {
		mAdView.destroy();
		super.onDestroy();
		mDBHelper.close();
	}
	
	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key) {
		final Utils utils = Utils.getInstance();
		if(SettingsActivity.PREF_TEXT_FONT.equals(key)) {
			final int noteFont = Integer.parseInt(sharedPreferences.getString(SettingsActivity.PREF_TEXT_FONT, ""+SettingsActivity.TEXT_FONT_SANS));
			utils.loadNoteFont(noteFont);
		} else if(SettingsActivity.PREF_TEXT_SIZE.equals(key)) {
			final int noteFontSize = Integer.parseInt(sharedPreferences.getString(SettingsActivity.PREF_TEXT_SIZE, ""+SettingsActivity.TEXT_SIZE_MEDIUM));
			utils.loadNoteFontSize(noteFontSize);
		} else if(key.equals(SettingsActivity.PREF_FIRST_LAUNCH)) {
			final boolean val = sharedPreferences.getBoolean(SettingsActivity.PREF_FIRST_LAUNCH, false);
			if(val) {
				displayWatsNewDialog();
			}
		}
	}

	@Override
	public void afterTextChanged(Editable arg0) {
		if(arg0 != null) {
			final String str = arg0.toString();
			if(!TextUtils.isEmpty(str)) {
				DisplaySavedLists displaySavedListsTask = new DisplaySavedLists();
				displaySavedListsTask.setStringString(str);
				displaySavedListsTask.execute();
			}
		}
	}
	@Override
	public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) { }
	@Override
	public void onTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) { }

	@Override
	public void onClick(View arg0) {
		if(arg0 instanceof ImageView) {
			if(arg0.getId() == R.id.labelfilter_navigate_back) {
				backPressedFromFilterLabels();
				return;
			}
		}
		
		final TextView labelName = (TextView) arg0.findViewById(R.id.lable_name);
		
		mDrawerLayout.closeDrawer(Gravity.LEFT);
		
		mCurrentNotesDisplayType = DISPLAY_FILTER_LABLES;
		mSelectedFilterLabel = labelName.getText().toString();
		mLabelHeaderLinearLayout.setVisibility(View.VISIBLE);
		((TextView) mLabelHeaderLinearLayout.findViewById(R.id.labelfilter_name)).setText(mSelectedFilterLabel);
		
		DisplaySavedLists displaySavedListsTask = new DisplaySavedLists();
		displaySavedListsTask.setStringString(null);
		displaySavedListsTask.execute();
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode,
			Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == GoogleDrive.REQUEST_CODE_RESOLUTION && resultCode == RESULT_OK) {
			CloudUtils.connectToGoogleDrive();
		}
		
		if(requestCode ==  GoogleDrive.REQUEST_CODE_CREATOR) {
			CloudUtils.getDriveId(data);
		}
	}
}
