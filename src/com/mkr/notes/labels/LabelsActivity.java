package com.mkr.notes.labels;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.inputmethod.InputMethodManager;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.mkr.notes.NotesActivity;
import com.mkr.notes.R;
import com.mkr.notesdatabase.NotesDBHelper;

public class LabelsActivity extends Activity {

	public static final String LABELS_SHARED_PREF = "lables_pref";
	public static final String LABELS_DEFAULT_PERSONAL = "Personal";
	public static final String LABELS_DEFAULT_WORK = "Work";
	public static final String LABELS_DEFAULT_IDEAS = "Ideas";

	//adapter that displays the created labels
	private LabelsAdapter mLabelAdapter;
	
	//all the below views are related to custom theme which displays the colors 
	private CreateThemeView mViewSatVal;
	private View mViewHue;
	private ImageView mViewCursor;
	private ImageView mViewTarget;
	private ViewGroup mViewContainer;

	final float[] currentColorHsv = new float[3];

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.labels_activity);

		final ActionBar actionBar = getActionBar();
		if(actionBar != null) {
			actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_HOME | ActionBar.DISPLAY_SHOW_TITLE);
			actionBar.setTitle(getString(R.string.labels_manager));
			actionBar.setDisplayHomeAsUpEnabled(true);
		}
		
		final ListView labelsListView = (ListView) findViewById(R.id.labels_list_view);
		labelsListView.setCacheColorHint(Color.TRANSPARENT);
		labelsListView.setDividerHeight(0);
		labelsListView.setDivider(null);

		mLabelAdapter = new LabelsAdapter();
		labelsListView.setAdapter(mLabelAdapter);
	};

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.labels_menu, menu);
		return true;
	} 

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			final Intent i = new Intent(LabelsActivity.this, NotesActivity.class);
			i.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
			startActivity(i);
			break;
		case R.id.menu_add_new_label:
			createNewLabel();
			break;
		};
		return true;
	}

	/**
	 * display the create new label dialog. 
	 */
	private void createNewLabel() {
		final AlertDialog.Builder builder = new AlertDialog.Builder(LabelsActivity.this);
		builder.setTitle(getString(R.string.lables_create_new_title));

		final InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
		
		final View dialogView = getLayoutInflater().inflate(R.layout.new_lable_dialog_layout, null);
		final EditText edit = (EditText)dialogView.findViewById(R.id.create_lable_edit_text);
		
		mViewHue = dialogView.findViewById(R.id.theme_viewHue);
		mViewSatVal = (CreateThemeView) dialogView.findViewById(R.id.theme_viewSatBri);
		mViewCursor = (ImageView) dialogView.findViewById(R.id.theme_select_cursor);
		mViewTarget = (ImageView) dialogView.findViewById(R.id.theme_select_target);
		mViewContainer = (ViewGroup) dialogView.findViewById(R.id.theme_viewContainer);

		mViewSatVal.setHue(getHue());

		mViewHue.setOnTouchListener(new View.OnTouchListener() {
			@Override public boolean onTouch(View v, MotionEvent event) {
				if (event.getAction() == MotionEvent.ACTION_MOVE
						|| event.getAction() == MotionEvent.ACTION_DOWN
						|| event.getAction() == MotionEvent.ACTION_UP) {

					if(event.getAction() == MotionEvent.ACTION_DOWN) {
						imm.hideSoftInputFromWindow(edit.getWindowToken(), 0);
					}
					
					float y = event.getY();
					if (y < 0.f) y = 0.f;
					if (y > mViewHue.getMeasuredHeight()) y = mViewHue.getMeasuredHeight() - 0.001f; // to avoid looping from end to start.
					float hue = 360.f - 360.f / mViewHue.getMeasuredHeight() * y;
					if (hue == 360.f) hue = 0.f;
					setHue(hue);

					// update view
					mViewSatVal.setHue(getHue());
					moveCursor();

					return true;
				}
				return false;
			}
		});

		mViewSatVal.setOnTouchListener(new View.OnTouchListener() {
			@Override public boolean onTouch(View v, MotionEvent event) {
				if (event.getAction() == MotionEvent.ACTION_MOVE
						|| event.getAction() == MotionEvent.ACTION_DOWN
						|| event.getAction() == MotionEvent.ACTION_UP) {

					if(event.getAction() == MotionEvent.ACTION_DOWN) {
						imm.hideSoftInputFromWindow(edit.getWindowToken(), 0);
					}
					
					float x = event.getX(); // touch event are in dp units.
					float y = event.getY();

					if (x < 0.f) x = 0.f;
					if (x > mViewSatVal.getMeasuredWidth()) x = mViewSatVal.getMeasuredWidth();
					if (y < 0.f) y = 0.f;
					if (y > mViewSatVal.getMeasuredHeight()) y = mViewSatVal.getMeasuredHeight();

					setSat(1.f / mViewSatVal.getMeasuredWidth() * x);
					setVal(1.f - (1.f / mViewSatVal.getMeasuredHeight() * y));

					// update view
					moveTarget();
					return true;
				}
				return false;
			}
		});

		ViewTreeObserver vto = dialogView.getViewTreeObserver();
		vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
			@Override public void onGlobalLayout() {
				//moveCursor();
				//moveTarget();
				setCursorTargetDefaultPositions();
				dialogView.getViewTreeObserver().removeGlobalOnLayoutListener(this);
			}
		});

		builder.setView(dialogView);

		imm.toggleSoftInput(InputMethodManager.SHOW_FORCED,0);
		
		builder.setPositiveButton(getString(R.string.lables_create), new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				final String labelName = edit.getText().toString();
				final boolean isAlreadyExists = LabelUtils.checkIfLabelAlreadyExists(labelName);
				if(isAlreadyExists) {
					Toast.makeText(LabelsActivity.this, String.format(getString(R.string.lables_already_exists), labelName), Toast.LENGTH_LONG).show();
				} else {
					LabelUtils.addNewLabel(labelName, getColor());
					mLabelAdapter.updateValues();
					mLabelAdapter.notifyDataSetChanged();
				}
			}
		});

		builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) { }
		});

		final AlertDialog dialog = builder.create();

		edit.addTextChangedListener(new TextWatcher() {
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				if(s != null && s.length() >= 1) {
					dialog.getButton(Dialog.BUTTON_POSITIVE).setClickable(true);
					dialog.getButton(Dialog.BUTTON_POSITIVE).setEnabled(true);
				} else {
					dialog.getButton(Dialog.BUTTON_POSITIVE).setClickable(false);
					dialog.getButton(Dialog.BUTTON_POSITIVE).setEnabled(false);
				}
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
			@Override
			public void afterTextChanged(Editable s) { }
		});

		dialog.show();
		dialog.getButton(Dialog.BUTTON_POSITIVE).setClickable(false);
		dialog.getButton(Dialog.BUTTON_POSITIVE).setEnabled(false);
	}

	private void setCursorTargetDefaultPositions() {

		RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) mViewCursor.getLayoutParams();
		layoutParams.leftMargin = (int) (mViewHue.getLeft() - Math.floor(mViewCursor.getMeasuredWidth() / 2) - mViewContainer.getPaddingLeft());
		layoutParams.topMargin = 10;
		mViewCursor.setLayoutParams(layoutParams);

		RelativeLayout.LayoutParams layoutParams1 = (RelativeLayout.LayoutParams) mViewTarget.getLayoutParams();
		layoutParams1.leftMargin = 50;
		layoutParams1.topMargin = 50;
		mViewTarget.setLayoutParams(layoutParams1);
	}

	protected void moveCursor() {
		float y = mViewHue.getMeasuredHeight() - (getHue() * mViewHue.getMeasuredHeight() / 360.f);
		if (y == mViewHue.getMeasuredHeight()) y = 0.f;
		RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) mViewCursor.getLayoutParams();
		layoutParams.leftMargin = (int) (mViewHue.getLeft() - Math.floor(mViewCursor.getMeasuredWidth() / 2) - mViewContainer.getPaddingLeft());
		layoutParams.topMargin = (int) (mViewHue.getTop() + y - Math.floor(mViewCursor.getMeasuredHeight() / 2) - mViewContainer.getPaddingTop());
		mViewCursor.setLayoutParams(layoutParams);
	}

	protected void moveTarget() {
		float x = getSat() * mViewSatVal.getMeasuredWidth();
		float y = (1.f - getVal()) * mViewSatVal.getMeasuredHeight();
		RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) mViewTarget.getLayoutParams();
		layoutParams.leftMargin = (int) (mViewSatVal.getLeft() + x - Math.floor(mViewTarget.getMeasuredWidth() / 2) - mViewContainer.getPaddingLeft());
		layoutParams.topMargin = (int) (mViewSatVal.getTop() + y - Math.floor(mViewTarget.getMeasuredHeight() / 2) - mViewContainer.getPaddingTop());
		mViewTarget.setLayoutParams(layoutParams);
	}

	private int getColor() {
		return Color.HSVToColor(currentColorHsv);
	}

	private float getHue() {
		return currentColorHsv[0];
	}

	private float getSat() {
		return currentColorHsv[1];
	}

	private float getVal() {
		return currentColorHsv[2];
	}

	private void setHue(float hue) {
		currentColorHsv[0] = hue;
	}

	private void setSat(float sat) {
		currentColorHsv[1] = sat;
	}

	private void setVal(float val) {
		currentColorHsv[2] = val;
	}

	private void deleteLabelDialog(final String label) {
		AlertDialog.Builder builder = new AlertDialog.Builder(LabelsActivity.this);
		builder.setTitle(R.string.lables_delete_title);
		builder.setMessage(R.string.lables_delete_msg);
		builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				LabelUtils.deleteLabel(label);
				mLabelAdapter.updateValues();
				mLabelAdapter.notifyDataSetChanged();
				
				NotesDBHelper.getInstance(LabelsActivity.this).updateLables();
			}
		});
		
		builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) { }
		});
		builder.create().show();
	}
	
	/**
	 * adapter displays all the labels
	 * @author murali
	 *
	 */
	class LabelsAdapter extends BaseAdapter {

		private Map<String, ?> mLabelsMap;
		private ArrayList<String> mLabelNames;

		public LabelsAdapter() {
			getData();
		}

		public void updateValues() {
			getData();
		}
		
		private void getData() {
			mLabelsMap = LabelUtils.getAllSavedLables();
			final Iterator<String> keysIterator = mLabelsMap.keySet().iterator();
			
			mLabelNames = new ArrayList<String>();
			//make sure that the labels are always added in the same order
			mLabelNames.add(0, LabelsActivity.LABELS_DEFAULT_PERSONAL);
			mLabelNames.add(1, LabelsActivity.LABELS_DEFAULT_WORK);
			mLabelNames.add(2, LabelsActivity.LABELS_DEFAULT_IDEAS);
			
			while (keysIterator.hasNext()) {
				final String labelName = keysIterator.next();
				if(!(LabelsActivity.LABELS_DEFAULT_PERSONAL.equals(labelName) 
						|| LabelsActivity.LABELS_DEFAULT_WORK.equals(labelName) 
						|| LabelsActivity.LABELS_DEFAULT_IDEAS.equals(labelName))) {
					mLabelNames.add(labelName);
				}
			}
		}
		
		@Override
		public int getCount() {
			return mLabelsMap.size();
		}

		@Override
		public Object getItem(int position) {
			return position;
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			ViewHolder holder;
			if(convertView == null) {
				holder = new ViewHolder();

				convertView = View.inflate(LabelsActivity.this, R.layout.label_item_activity, null);

				holder.mLabelName = (TextView) convertView.findViewById(R.id.lable_name_activity);
				holder.mLabelColor = (TextView) convertView.findViewById(R.id.lable_color_activity);
				holder.mDelIcon = (ImageView) convertView.findViewById(R.id.lable_delete_activity);
				holder.mDelIcon.setVisibility(View.VISIBLE);

				convertView.setTag(holder);
			} else {
				holder = (ViewHolder) convertView.getTag();
			}

			final String labelName = mLabelNames.get(position);
			holder.mLabelName.setText(labelName);
			//this is just a safety try catch. there will always be a color to a label 
			try {
				holder.mLabelColor.setBackgroundColor((Integer)mLabelsMap.get(labelName));
			}catch(Exception e) {  
				holder.mLabelColor.setBackgroundColor(Color.GREEN);
			}
			if(LABELS_DEFAULT_PERSONAL.equals(labelName) 
					|| LABELS_DEFAULT_WORK.equals(labelName) 
					|| LABELS_DEFAULT_IDEAS.equals(labelName)) {
				holder.mDelIcon.setVisibility(View.INVISIBLE);
			} else {
				holder.mDelIcon.setVisibility(View.VISIBLE);
			}

			holder.mDelIcon.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					deleteLabelDialog(labelName);
				}
			});
			return convertView;
		}
	}

	static class ViewHolder {
		private ImageView mDelIcon;
		private TextView mLabelName;
		private TextView mLabelColor;
	}
}
