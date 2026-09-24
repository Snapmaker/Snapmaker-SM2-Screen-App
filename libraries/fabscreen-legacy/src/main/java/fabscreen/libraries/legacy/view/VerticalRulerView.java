package fabscreen.libraries.legacy.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.Scroller;

import androidx.annotation.Nullable;

import fabscreen.libraries.legacy.R;

public class VerticalRulerView extends View {
    // layout attributes
    private int mBackgroundColor;

    private float mLongScaleHeight = dp2px(2);
    private float mLongScaleWidth = dp2px(60);
    private int mLongScaleColor = Color.parseColor("#616161");

    private float mShortScaleHeight = dp2px(2);
    private float mShortScaleWidth = dp2px(40);
    private int mShortScaleColor = Color.parseColor("#616161");

    private float mMarkerTextMargin = dp2px(8);
    private float mMarkerTextSize = sp2px(14);
    private int mMarkerTextColor = Color.parseColor("#616161");
    private float mUnitSpacing = dp2px(12);

    private int mIndicatorColor = Color.parseColor("#ff3333");

    // how many units contained in a division
    private int mUnitCountPerDivision = 5;

    // value attributes
    private float mMinValue = 0;
    private float mMaxValue = 200;
    private float mUnit = 1;
    private float mCurrentValue = 28;

    // derived attributes
    private int mMaxIndex;

    // measured width and height
    private int mWidth;
    private int mHeight;

    private float mMaxTextWidth;

    private Paint mScalePaint;
    private Paint mTextPaint;

    private VelocityTracker mVelocityTracker;
    private int mMinFlingVelocity;
    private int mMaxFlingVelocity;
    private Scroller mScroller;
    private float mTouchY;
    private float mTouchValue;

    private OnValueChangedListener mListener;

    public interface OnValueChangedListener {
        void onValueChanged(float value);
    }

    public VerticalRulerView(Context context) {
        this(context, null);
    }

    public VerticalRulerView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public VerticalRulerView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public VerticalRulerView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);

        initAttrs(attrs, defStyleAttr, defStyleRes);
        initialize(context);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        setOnTouchListener((v, event) -> {
            v.getParent().requestDisallowInterceptTouchEvent(true);
            if (event.getAction() == MotionEvent.ACTION_UP) {
                v.performClick();
            }
            return false;
        });
    }

    private void initAttrs(@Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        TypedArray a = getContext().getTheme().obtainStyledAttributes(attrs, R.styleable.RulerView, defStyleAttr, defStyleRes);

        mBackgroundColor = a.getColor(R.styleable.RulerView_rv_backgroundColor, Color.DKGRAY);

        mLongScaleHeight = a.getDimension(R.styleable.RulerView_rv_longScaleHeight, mLongScaleHeight);
        mLongScaleWidth = a.getDimension(R.styleable.RulerView_rv_longScaleWidth, mLongScaleWidth);
        mLongScaleColor = a.getColor(R.styleable.RulerView_rv_longScaleColor, mLongScaleColor);

        mShortScaleHeight = a.getDimension(R.styleable.RulerView_rv_shortScaleHeight, mShortScaleHeight);
        mShortScaleWidth = a.getDimension(R.styleable.RulerView_rv_shortScaleWidth, mShortScaleWidth);
        mShortScaleColor = a.getColor(R.styleable.RulerView_rv_shortScaleColor, mShortScaleColor);

        mMarkerTextMargin = a.getDimension(R.styleable.RulerView_rv_markerTextMargin, mMarkerTextMargin);
        mMarkerTextSize = a.getDimension(R.styleable.RulerView_rv_markerTextSize, mMarkerTextSize);
        mMarkerTextColor = a.getColor(R.styleable.RulerView_rv_markerTextColor, mMarkerTextColor);

        mIndicatorColor = a.getColor(R.styleable.RulerView_rv_indicatorColor, mIndicatorColor);

        mMinValue = a.getFloat(R.styleable.RulerView_rv_minValue, mMinValue);
        mMaxValue = a.getFloat(R.styleable.RulerView_rv_maxValue, mMaxValue);
        mCurrentValue = a.getFloat(R.styleable.RulerView_rv_currentValue, mCurrentValue);

        mUnit = a.getFloat(R.styleable.RulerView_rv_unit, mUnit);
        mUnitSpacing = a.getDimension(R.styleable.RulerView_rv_unitSpacing, mUnitSpacing);
        mUnitCountPerDivision = a.getInt(R.styleable.RulerView_rv_unitCountPerDivision, mUnitCountPerDivision);

        a.recycle();
    }

    private int dp2px(float dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics());
    }

    private int sp2px(float sp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, getResources().getDisplayMetrics());
    }

    private void initialize(Context context) {
        mMaxIndex = (int) ((mMaxValue - mMinValue) / mUnit);

        mScalePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        mTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mTextPaint.setColor(mMarkerTextColor);
        mTextPaint.setTextSize(mMarkerTextSize);

        // Fixme: Calculate the longest length of the text.
        mMaxTextWidth = mTextPaint.measureText(String.valueOf(mMaxValue * (1 + mUnit)));

        ViewConfiguration viewConfiguration = ViewConfiguration.get(context);
        mMinFlingVelocity = viewConfiguration.getScaledMinimumFlingVelocity();
        mMaxFlingVelocity = viewConfiguration.getScaledMaximumFlingVelocity();

        mScroller = new Scroller(context);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        mWidth = MeasureSpec.getSize(widthMeasureSpec);

        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        mHeight = MeasureSpec.getSize(heightMeasureSpec);

        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        switch (widthMode) {
            case MeasureSpec.AT_MOST:
                mWidth = (int) (Math.max(mLongScaleWidth, mShortScaleWidth) + (mMarkerTextMargin * 2) + mMaxTextWidth);
                break;
            case MeasureSpec.EXACTLY:
            case MeasureSpec.UNSPECIFIED:
                break;
        }

        setMeasuredDimension(mWidth, mHeight);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        // background color
        setBackgroundColor(mBackgroundColor);

        // draw scales
        onDrawScales(canvas);
    }

    private void drawScale(Canvas canvas, int scaleIndex, int y, int indicatorIndex) {
        if (scaleIndex % mUnitCountPerDivision == 0) {
            if (scaleIndex == indicatorIndex) {
                mScalePaint.setColor(mIndicatorColor);
            } else {
                mScalePaint.setColor(mLongScaleColor);
            }

            // long scale
            mScalePaint.setStrokeWidth(mLongScaleHeight);
            canvas.drawLine(0, y, mLongScaleWidth, y, mScalePaint);

            // text
            float number = mMinValue + scaleIndex * mUnit;
            String text = Float.toString(number);
            if (text.endsWith(".0")) {
                text = text.substring(0, text.length() - 2);
            }

            // Get text height.
            float textHeight = mTextPaint.getFontMetrics().bottom - mTextPaint.getFontMetrics().top;
            canvas.drawText(text, mLongScaleWidth + mMarkerTextMargin, y + textHeight * 0.25f , mTextPaint);
        } else {
            if (scaleIndex == indicatorIndex) {
                mScalePaint.setColor(mIndicatorColor);
            } else {
                mScalePaint.setColor(mShortScaleColor);
            }

            // short scale
            mScalePaint.setStrokeWidth(mShortScaleHeight);
            canvas.drawLine(mLongScaleWidth / 2 - mShortScaleWidth / 2, y, mLongScaleWidth / 2 + mShortScaleWidth / 2, y, mScalePaint);
        }
    }

    private void onDrawScales(Canvas canvas) {
        // some calculations
        int bottomSideScaleIndex = (int) ((mCurrentValue - mMinValue) / mUnit);
        int topSideScaleIndex = bottomSideScaleIndex + 1;
        int closestScaleIndex = bottomSideScaleIndex;

        // check if the top side scale is closer than the bottom side one
        if (topSideScaleIndex < mMaxIndex && mMinValue * 2 + (bottomSideScaleIndex + topSideScaleIndex) * mUnit < mCurrentValue * 2) {
            closestScaleIndex = topSideScaleIndex;
        }

        int scaleIndex = bottomSideScaleIndex;
        int y = (mHeight / 2 - (int) ((mMinValue + scaleIndex * mUnit - mCurrentValue) / mUnit * mUnitSpacing));
        while (scaleIndex >= 0 && y <= mHeight) {
            drawScale(canvas, scaleIndex, y, closestScaleIndex);
            scaleIndex--;
            y += mUnitSpacing;
        }
        scaleIndex = topSideScaleIndex;
        y = mHeight / 2 + (int) ((mCurrentValue - mMinValue - scaleIndex * mUnit) / mUnit * mUnitSpacing);
        while (scaleIndex <= mMaxIndex && y >= 0) {
            drawScale(canvas, scaleIndex, y, closestScaleIndex);
            scaleIndex++;
            y -= mUnitSpacing;
        }
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!isEnabled()) return true;

        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }
        mVelocityTracker.addMovement(event);

        final float ey = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (!mScroller.isFinished()) {
                    mScroller.forceFinished(true);
                }
                mTouchY = ey;
                mTouchValue = mCurrentValue;
                break;
            case MotionEvent.ACTION_MOVE:
                float deltaY = ey - mTouchY;
                float newValue = mTouchValue + deltaY * mUnit / mUnitSpacing;
                setCurrentValue(newValue);
                break;
            case MotionEvent.ACTION_UP:
                mVelocityTracker.computeCurrentVelocity(1000, mMaxFlingVelocity);
                int yVelocity = (int) mVelocityTracker.getYVelocity();

                if (Math.abs(yVelocity) > mMinFlingVelocity) {
                    int currentX = (int) ((mCurrentValue - mMinValue) / mUnit * mUnitSpacing);
                    mScroller.fling(0, currentX, 0, yVelocity, 0, 0, 0, (int) ((mMaxValue - mMinValue) / mUnit * mUnitSpacing));
                    invalidate();
                } else {
                    stickToClosestScale();
                }

                break;
        }

        return true;
    }

    @Override
    public void computeScroll() {
        if (mScroller.computeScrollOffset()) {
            int currentY = mScroller.getCurrY();
            float value = mMinValue + currentY / mUnitSpacing * mUnit;
            setCurrentValue(value);
        } else {
            stickToClosestScale();
        }
    }

    private void stickToClosestScale() {
        int leftSideScaleIndex = (int) ((mCurrentValue - mMinValue) / mUnit);
        int rightSideScaleIndex = leftSideScaleIndex + 1;
        int closestScaleIndex = leftSideScaleIndex;

        // check if the right side scale is closer than the left side one
        if (rightSideScaleIndex < mMaxIndex && mMinValue * 2 + (leftSideScaleIndex + rightSideScaleIndex) * mUnit < mCurrentValue * 2) {
            closestScaleIndex = rightSideScaleIndex;
        }

        setCurrentValue(mMinValue + closestScaleIndex * mUnit);
    }

    public void setCurrentValue(float value) {
        value = Math.min(value, mMaxValue);
        value = Math.max(value, mMinValue);

        if (mCurrentValue != value) {
            mCurrentValue = value;

            if (mListener != null) {
                mListener.onValueChanged(value);
            }

            invalidate();
        }
    }

    public void setMinValue(float value) {
        mMinValue = value;
        mMaxIndex = (int) ((mMaxValue - mMinValue) / mUnit);

        invalidate();
    }

    public void setMaxValue(float value) {
        mMaxValue = value;
        mMaxIndex = (int) ((mMaxValue - mMinValue) / mUnit);

        invalidate();
    }

    public void setUnit(float unit) {
        mUnit = unit;
        mMaxIndex = (int) ((mMaxValue - mMinValue) / mUnit);

        invalidate();
    }

    public void setOnValueChangedListener(OnValueChangedListener listener) {
        mListener = listener;
    }
}
