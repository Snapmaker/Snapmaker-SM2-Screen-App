package fabscreen.libraries.legacy.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.orhanobut.logger.Logger;

import fabscreen.libraries.legacy.R;

public class SlidingRulerView extends View {
    //    private float cy;
    int mBitmapWidth;
    private float mLongScaleHeight = dp2px(160);
    private float mShortScaleHeight = dp2px(117);
    private Bitmap mBitmapTravelingScale = null;
    private float mStrokeWidth = dp2px(5);
    private float mScaleWidth = dp2px(8);
    private int mScaleColor = Color.parseColor("#C9C9C9");
    private int mTrackColor = Color.parseColor("#10111C");
    private float mUnitSpacing = dp2px(40);
    // how many units contained in a division
    private int mUnitCountPerDivision = 5;
    private Paint mScalePaint;
    private Paint mTextPaint;
    private Paint mTrackPaint;
    // value attributes
    private int mMinValue = -10;
    private int mMaxValue = 10;
    private int mMidWidth;
    private RectF[] rectFS;
    private float cx;
    private int paddingStart;
    private int paddingTop;
    //    private int paddingBottom;
    private float mMaxWidth;
    private float mMinWidth;
    private OnProgressChangeListener onProgressChangeListener;
    private boolean isDrawFixedHorizontalLine = true;
    private String mShowStr = "";
    private int mIndex;
    private float mTextSize;
    private int mTextColor;

    public SlidingRulerView(Context context) {
        this(context, null);
    }

    public SlidingRulerView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SlidingRulerView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public SlidingRulerView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);

        initAttrs(attrs, defStyleAttr, defStyleRes);
        initialize(context);
    }

    private void initAttrs(AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        TypedArray typedArray = getContext().getTheme().obtainStyledAttributes(attrs, R.styleable.SlidingRulerView, defStyleAttr, defStyleRes);
        mLongScaleHeight = typedArray.getDimension(R.styleable.SlidingRulerView_srv_longScaleHeight, dp2px(60));
        mShortScaleHeight = typedArray.getDimension(R.styleable.SlidingRulerView_srv_shortScaleHeight, dp2px(40));
        mStrokeWidth = typedArray.getDimension(R.styleable.SlidingRulerView_srv_strokeWidth, dp2px(3));
        mScaleWidth = typedArray.getDimension(R.styleable.SlidingRulerView_srv_scaleWidth, dp2px(4));
        mUnitSpacing = typedArray.getDimension(R.styleable.SlidingRulerView_srv_unitSpacing, dp2px(13.8f));
        mScaleColor = typedArray.getColor(R.styleable.SlidingRulerView_srv_scaleColor, Color.parseColor("#C9C9C9"));
        mUnitCountPerDivision = typedArray.getInt(R.styleable.SlidingRulerView_srv_unitCountPerDivision, 5);
        mMinValue = typedArray.getInt(R.styleable.SlidingRulerView_srv_minValue, -10);
        mMaxValue = typedArray.getInt(R.styleable.SlidingRulerView_srv_maxValue, 10);
        mTextSize = typedArray.getDimension(R.styleable.SlidingRulerView_sRulerv_textSize, TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 16, getResources().getDisplayMetrics()));
        mTextColor = typedArray.getColor(R.styleable.SlidingRulerView_sRulerv_textColor, Color.parseColor("#090909"));

        typedArray.recycle();
    }

    private void initialize(Context context) {
        mScalePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mScalePaint.setStyle(Paint.Style.FILL);
        mScalePaint.setStrokeWidth(mStrokeWidth);
        mScalePaint.setStrokeCap(Paint.Cap.ROUND);
        mScalePaint.setColor(mScaleColor);
        mBitmapTravelingScale = getBitmapFromVectorDrawable(context, R.drawable.ic_s20_xy_calibration_traveling_scale);
        mTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mTextPaint.setTextSize(mTextSize);
        mTextPaint.setColor(mTextColor);
        mTextPaint.setTypeface(Typeface.DEFAULT_BOLD);
        mTextPaint.setTextAlign(Paint.Align.CENTER);
        mTrackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mTrackPaint.setStyle(Paint.Style.FILL);
        mTrackPaint.setStrokeWidth(dp2px(8));
        mTrackPaint.setStrokeCap(Paint.Cap.ROUND);
        mTrackPaint.setColor(mTrackColor);
    }

    public Bitmap getBitmapFromVectorDrawable(Context context, int drawableId) {
        Drawable drawable = ContextCompat.getDrawable(context, drawableId);
        mBitmapWidth = drawable.getIntrinsicWidth();
        Bitmap bitmap = Bitmap.createBitmap(mBitmapWidth, drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bitmap;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        drawFixedLine(canvas);
        drawCenterMark(canvas);
        drawActivityBar(canvas);
    }

    private void drawCenterMark(Canvas canvas) {
        canvas.drawRoundRect(new RectF(paddingStart + mMidWidth - dp2px(20) / 2f,
                paddingTop + mLongScaleHeight + mScaleWidth,
                paddingStart + mMidWidth + dp2px(20) / 2f,
                paddingTop + mLongScaleHeight + mScaleWidth + dp2px(20)), 3, 3, mScalePaint);
        canvas.drawRect(new RectF(paddingStart + mMidWidth - dp2px(20) / 2f,
                paddingTop + mLongScaleHeight + mScaleWidth,
                paddingStart + mMidWidth + dp2px(20) / 2f,
                paddingTop + mLongScaleHeight + mScaleWidth + dp2px(20) / 2f), mScalePaint);
        if (!mShowStr.isEmpty()) {
            canvas.drawText(mShowStr, paddingStart + mMidWidth, paddingTop + mLongScaleHeight + mScaleWidth + dp2px(16 / 2f + 8), mTextPaint);
        }
    }

    private void drawFixedLine(Canvas canvas) {
        for (RectF r : rectFS) {
            canvas.drawRoundRect(r, 15, 15, mScalePaint);
        }
        if (isDrawFixedHorizontalLine) {
            canvas.drawRect(new RectF(
                    mMinWidth,
                    paddingTop + mLongScaleHeight,
                    mMaxWidth,
                    paddingTop + mLongScaleHeight + mScaleWidth), mScalePaint);
        }

        canvas.drawRoundRect(new RectF(
                mMinWidth - mScaleWidth / 2f,
                paddingTop + mLongScaleHeight + mScaleWidth + dp2px(20) + 52,
                mMaxWidth + mScaleWidth / 2f,
                paddingTop + mLongScaleHeight + mScaleWidth + dp2px(20) + 68),
                8, 8, mTrackPaint);
    }

    private void drawActivityBar(Canvas canvas) {
        if (cx == 0) {
            cx = rectFS[(mMaxValue - mMinValue) / 2].left + mScaleWidth / 2;
        }
        float v = cx - ((float) mBitmapWidth / 2);
        canvas.drawBitmap(mBitmapTravelingScale, v, paddingTop, null);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        paddingStart = getPaddingStart();
        paddingTop = getPaddingTop();
        mMidWidth = w / 2 - paddingStart;
        mMaxWidth = paddingStart + mUnitSpacing * (mMaxValue - mMinValue) + mScaleWidth / 2;
        mMinWidth = paddingStart;

        rectFS = new RectF[mMaxValue - mMinValue + 1];
        float v = mScaleWidth / 2;
        int mid = (mMaxValue - mMinValue) / 2;
        rectFS[mid] = new RectF(paddingStart + mMidWidth - v,
                paddingTop,
                paddingStart + mMidWidth + v,
                paddingTop + mLongScaleHeight + mScaleWidth);
        for (int i = 1; i <= mid; i++) {
            float v1 = paddingTop + mLongScaleHeight - mShortScaleHeight;
            float top = i % mUnitCountPerDivision == 0 ? paddingTop : v1;
            rectFS[mid - i] = new RectF(paddingStart + mMidWidth - v - mUnitSpacing * i, top, paddingStart + mMidWidth + v - mUnitSpacing * i, paddingTop + mLongScaleHeight + mScaleWidth);
            rectFS[mid + i] = new RectF(paddingStart + mMidWidth - v + mUnitSpacing * i, top, paddingStart + mMidWidth + v + mUnitSpacing * i, paddingTop + mLongScaleHeight + mScaleWidth);
        }
    }

    private int dp2px(float dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics());
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        cx = event.getX();
        cx = Math.max(cx, mMinWidth);
        cx = Math.min(cx, mMaxWidth);

        float v = (cx - paddingStart) / mUnitSpacing;
        int index = (v * 10 % 10 >= 5) ? (int) v + 1 : (int) v;
        if (mIndex != index) {
            mIndex = index;
        }
        float tempCx = rectFS[index].left + mScaleWidth / 2;
        if (event.getAction() == MotionEvent.ACTION_UP) {
            onProgressChangeListener.onProgressChange(index + mMinValue);
        }
        if (cx == tempCx) {
            return true;
        } else {
            cx = tempCx;
        }

        invalidate();
        return true;
    }

    public void setOnProgressChangeListener(OnProgressChangeListener o) {
        onProgressChangeListener = o;
    }

    public void setProgress(int index) {
        cx = rectFS[index - mMinValue].left;
        invalidate();
    }

    public interface OnProgressChangeListener {
        void onProgressChange(int index);
    }

    public void setShowStr(String mShowStr) {
        this.mShowStr = mShowStr;
    }
}
