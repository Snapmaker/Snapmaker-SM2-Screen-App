package fabscreen.libraries.legacy.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;

import androidx.annotation.Nullable;

import fabscreen.libraries.legacy.R;

public class GuideProgressBar extends View {
    private int mStepNum = 1;
    private int mStepIndex = 0;
    private int mViewWidth;

    private int mStepHeight;
    private int mStepWidth;
    private int mSpace;
    private Paint mCheckPaint;
    private Paint mGrayPaint;

    public GuideProgressBar(Context context) {
        super(context);
    }

    public GuideProgressBar(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initAttrs();
    }

    public GuideProgressBar(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

    }


    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        mViewWidth = MeasureSpec.getSize(widthMeasureSpec);
    }

    private void initAttrs() {
        mSpace = dp2px(3);
        mStepHeight = dp2px(5);
        mCheckPaint = new Paint();
        mCheckPaint.setColor(getResources().getColor(R.color.custom_grey_400));
        mGrayPaint = new Paint();
        mGrayPaint.setColor(getResources().getColor(R.color.custom_grey_600));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        onDrawScales(canvas);
    }

    private void onDrawScales(Canvas canvas) {
        mStepWidth = (mViewWidth - (mStepNum - 1) * mSpace) / (mStepNum);
        for (int i = 0; i < mStepNum; i++) {
            canvas.drawRect(i * (mStepWidth + mSpace), 0
                    , i * (mStepWidth + mSpace) + mStepWidth, mStepHeight
                    , i != mStepIndex - 1 ? mGrayPaint : mCheckPaint);
        }
    }

    public void setmStepNum(int mStepNum) {
        this.mStepNum = mStepNum;
    }

    public void setmStepIndex(int mStepIndex) {
        this.mStepIndex = mStepIndex;
    }

    public int getmStepNum() {
        return mStepNum;
    }

    public int getmStepIndex() {
        return mStepIndex;
    }

    private int dp2px(float dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics());
    }
}
