package fabscreen.libraries.legacy.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.util.AttributeSet;

import androidx.appcompat.widget.AppCompatButton;

import fabscreen.libraries.legacy.R;

public class ProgressButton extends AppCompatButton {
    private float mCornerRadius = 0;
    private float mProgressMargin = 0;

    private boolean mFinish;

    private int mProgress;
    private int mMaxProgress = 100;
    private int mMinProgress = 0;

    private GradientDrawable mDrawableButton;
    private Drawable mDrawableDefaultBackground;
    private Drawable mDrawableProgressBackground;
    private GradientDrawable mDrawableProgress;

    public ProgressButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        initAttrs(context, attrs);
    }

    public ProgressButton(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initAttrs(context, attrs);
    }

    private void initAttrs(Context context, AttributeSet attrs) {
        mDrawableProgress = new GradientDrawable();
        mDrawableButton = new GradientDrawable();

        int defaultProgressStartColor = getResources().getColor(R.color.gradient_blue_start, null);
        int defaultProgressEndColor = getResources().getColor(R.color.gradient_blue_end, null);

        TypedArray attr = context.obtainStyledAttributes(attrs, R.styleable.ProgressButton);

        try {
            mProgressMargin = attr.getDimension(R.styleable.ProgressButton_pb_progressMargin, mProgressMargin);
            mCornerRadius = attr.getDimension(R.styleable.ProgressButton_pb_radius, mCornerRadius);

            // Default background
            mDrawableDefaultBackground = attr.getDrawable(R.styleable.ProgressButton_pb_background);
            if (mDrawableDefaultBackground == null) {
                mDrawableDefaultBackground = context.getDrawable(R.drawable.all_button_round_default_normal);
            }

            // Drawable Background button when progress is done
            mDrawableButton.setOrientation(GradientDrawable.Orientation.LEFT_RIGHT);
            mDrawableButton.setColors(new int[]{defaultProgressStartColor, defaultProgressEndColor});

            // Progress background
            mDrawableProgressBackground = attr.getDrawable(R.styleable.ProgressButton_pb_progressBackground);

            //Set progress drawable color
            int progressStartColor = attr.getColor(R.styleable.ProgressButton_pb_progressStartColor, defaultProgressStartColor);
            int progressEndColor = attr.getColor(R.styleable.ProgressButton_pb_progressEndColor, defaultProgressEndColor);
            mDrawableProgress.setOrientation(GradientDrawable.Orientation.LEFT_RIGHT);
            mDrawableProgress.setColors(new int[]{progressStartColor, progressEndColor});

            mProgress = attr.getInteger(R.styleable.ProgressButton_pb_progress, mProgress);
            mMinProgress = attr.getInteger(R.styleable.ProgressButton_pb_minProgress, mMinProgress);
            mMaxProgress = attr.getInteger(R.styleable.ProgressButton_pb_maxProgress, mMaxProgress);
        } finally {
            attr.recycle();
        }

        //Set corner radius
        mDrawableButton.setCornerRadius(mCornerRadius);
        mDrawableProgress.setCornerRadius(mCornerRadius - mProgressMargin);
        // default background
        setBackgroundDrawable(mDrawableDefaultBackground);

        mFinish = false;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (mProgress > mMinProgress && mProgress <= mMaxProgress && !mFinish) {
            // Calculate the width of progress
            float progressWidth =
                    (float) getMeasuredWidth() * ((float) (mProgress - mMinProgress) / mMaxProgress - mMinProgress);

            // If progress width less than 2x corner radius, the radius of progress will be wrong
            if (progressWidth < mCornerRadius * 2) {
                progressWidth = mCornerRadius * 2;
            }

            // Set rect of progress
            mDrawableProgress.setBounds((int) mProgressMargin, (int) mProgressMargin,
                    (int) (progressWidth - mProgressMargin), getMeasuredHeight() - (int) mProgressMargin);

            mDrawableProgress.draw(canvas);

            if (mProgress == mMaxProgress) {
                setBackgroundDrawable(mDrawableButton);
                mFinish = true;
            }
        }
        super.onDraw(canvas);
    }

    /**
     * Set current progress
     */
    public void setProgress(int progress) {
        if (!mFinish && progress <= mMaxProgress && progress >= mMinProgress) {
            mProgress = progress;
            setBackgroundDrawable(mDrawableProgressBackground);
            invalidate();
        }
    }

    public void setMaxProgress(int maxProgress) {
        mMaxProgress = maxProgress;
    }

    public void setMinProgress(int minProgress) {
        mMinProgress = minProgress;
    }

    public void reset() {
        mFinish = false;
        mProgress = mMinProgress;
        setBackgroundDrawable(mDrawableDefaultBackground);
        invalidate();
    }
}
