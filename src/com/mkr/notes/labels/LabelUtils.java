package com.mkr.notes.labels;

import java.util.Map;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Resources;

import com.mkr.notes.R;

/**
 * this is a utils class used specifically only for labels like creating a label and deleting labels 
 * 
 * @author murali
 *
 */
public class LabelUtils {

	private static LabelUtils mLabelUtils;
	private static Context mContext;
	private static SharedPreferences mSharedPref;
	
	private static Map<String, ?> mSavedLabels;
	
	//private constructor
	private LabelUtils() { }
	
	public static LabelUtils getInstance() {
		if(mLabelUtils == null) {
			mLabelUtils = new LabelUtils();
		}
		return mLabelUtils;
	}
	
	public void init(final Context context) {
		mContext = context;
		createDefaultLablePreferences();
	}
	
	/**
	 * create all the default label shared preferences and their respective colors
	 */
	private static void createDefaultLablePreferences() {
		mSharedPref = mContext.getSharedPreferences(LabelsActivity.LABELS_SHARED_PREF, Context.MODE_PRIVATE);
		final Map<String, ?> savedLabels = mSharedPref.getAll(); 
		if(savedLabels != null && savedLabels.size() == 0) {
			final Resources res = mContext.getResources();
			final Editor edit = mSharedPref.edit();
			edit.putInt(LabelsActivity.LABELS_DEFAULT_PERSONAL, res.getColor(R.color.label_color_personal));
			edit.putInt(LabelsActivity.LABELS_DEFAULT_WORK, res.getColor(R.color.label_color_work));
			edit.putInt(LabelsActivity.LABELS_DEFAULT_IDEAS, res.getColor(R.color.label_color_ideas));
			edit.commit();
		}
	}
	
	public static Map<String, ?> getAllSavedLables() {
		mSavedLabels = mSharedPref.getAll(); 
		return mSavedLabels; 
	}
	
	public static boolean checkIfLabelAlreadyExists(final String label) {
		return mSavedLabels.containsKey(label);
	}
	
	public static void addNewLabel(final String labelName, final int labelcolor) {
		mSharedPref.edit().putInt(labelName, labelcolor).commit();
	}
	
	public static void deleteLabel(final String labelName) {
		mSharedPref.edit().remove(labelName).commit();
	}
	
	public static int getLabelColor(final String labelName) {
		return mSharedPref.getInt(labelName, mContext.getResources().getColor(R.color.label_color_work));
	}
}
