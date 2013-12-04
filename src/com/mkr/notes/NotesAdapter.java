package com.mkr.notes;

import java.util.ArrayList;
import java.util.List;

import com.mkr.notes.labels.LabelUtils;

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class NotesAdapter extends android.widget.BaseAdapter {

	private final Context mContext;
	private final ArrayList<Note> mListToDisplay;
	private List<Long> mSelectedList;
	
	public NotesAdapter(final Context context, final ArrayList<Note> data) {
		mContext = context;
		mListToDisplay = data;
	}
	
	@Override
	public int getCount() {
		return mListToDisplay.size();
	}

	@Override
	public Object getItem(int position) {
		return position;
	}

	@Override
	public long getItemId(int position) {
		return position;
	}
	
	public void setSelectedLists(List<Long> selectedList) {
		mSelectedList = selectedList;
		notifyDataSetChanged();
	}

	@SuppressWarnings("deprecation")
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		Holder holder;
		if(convertView == null) {
			
			holder = new Holder();
			convertView = View.inflate(mContext, R.layout.notes_list_item, null);
			holder.mTitle = (TextView) convertView.findViewById(R.id.title);
			holder.mMonth = (TextView) convertView.findViewById(R.id.month);
			holder.mDate = (TextView) convertView.findViewById(R.id.date);
			holder.mDay = (TextView) convertView.findViewById(R.id.day);
			holder.mListParent = (RelativeLayout) convertView.findViewById(R.id.list_item_relative);
			holder.mLabelColor = (TextView) convertView.findViewById(R.id.label_color);
			
			holder.mTitle.setTypeface(Utils.getInstance().getRobotoSlabFontTypeface());
			holder.mMonth.setTypeface(Utils.getInstance().getRobotoSlabFontTypeface());
			holder.mDate.setTypeface(Utils.getInstance().getRobotoSlabFontTypeface());
			
			convertView.setTag(holder);
		} else {
			holder = (Holder) convertView.getTag();
		}
		
		final Note data = mListToDisplay.get(position);
		holder.mCreationDate = data.createDate;
		holder.mNotePath = data.NotePath;
		holder.mNoteLabel = data.NoteLabel; 
				
		holder.mTitle.setText(data.title);
		holder.mLabelColor.setBackgroundColor(LabelUtils.getLabelColor(data.NoteLabel));
		
		String date;
		if(Utils.getDateTypeToDisplay() == SettingsActivity.DATE_TYPE_MODIFIED) {
			date = Utils.getReadableTime(data.modifiedDate);
		} else {
			date = Utils.getReadableTime(data.modifiedDate);
		}
		
		holder.mMonth.setText(date.substring(4, 7));
		holder.mDate.setText(date.substring(8, 10));
		holder.mDay.setText(date.substring(0, 3));
				
		if(mSelectedList != null && mSelectedList.contains(data.createDate)) {
			holder.mListParent.setBackgroundDrawable(mContext.getResources().getDrawable(R.drawable.background_selected));
		} else {
			holder.mListParent.setBackgroundDrawable(mContext.getResources().getDrawable(R.drawable.background));
		}
		
		return convertView;
	}
	
	static class Holder {
		private RelativeLayout mListParent;
		public TextView mTitle;
		public TextView mMonth;
		public TextView mLabelColor;
		public TextView mDate;
		public TextView mDay;
		public long mCreationDate;
		public String mNotePath;
		public String mNoteLabel;
	}
}
