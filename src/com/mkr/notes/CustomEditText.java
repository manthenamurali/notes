package com.mkr.notes;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.EditText;

public class CustomEditText extends EditText {

	private Rect mRect;
    private Paint mPaint;

    private int mTextColor;
    private int mLineColor;
    private int mBackGroundColor;
    
    // This constructor is used by LayoutInflater
    public CustomEditText(Context context, AttributeSet attrs) {
        super(context, attrs);

        // Creates a Rect and a Paint object, and sets the style and color of the Paint object.
        mRect = new Rect();
        
        mPaint = new Paint();
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setColor(0x800000FF);
    }

	@Override
    protected void onDraw(Canvas canvas) {
		
		int height = canvas.getHeight() * 3;
        int curHeight = 0;
        final Rect r = mRect;
        final Paint paint = mPaint;
        int baseline = getLineBounds(0, r);

        paint.setColor(mLineColor);
        for (curHeight = baseline + 1; curHeight < height; 
                                                 curHeight += getLineHeight()) {
            canvas.drawLine(r.left, curHeight, r.right, curHeight, paint);
        }
        
        paint.setColor(mTextColor);
        super.onDraw(canvas);
    }
	
	public void applytheme() {
		
		final String themeValue = Utils.getInstance().loadTheme();
		final String[] values = themeValue.split(Utils.DELIMITER);
		
		mTextColor = Integer.parseInt(values[0]);
		mLineColor = Integer.parseInt(values[1]);
		mBackGroundColor = Integer.parseInt(values[2]);

		this.setBackgroundColor(mBackGroundColor);
	}
}
