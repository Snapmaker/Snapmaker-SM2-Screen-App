package fabscreen.libraries.legacy.view;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.SweepGradient;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;

import androidx.annotation.Nullable;

import fabscreen.libraries.legacy.R;

public class CircularProgressView extends View {
    private RectF mRectF = new RectF();

    private int mStrokeWidth = dp2px(10);
    private int mOuterColor = Color.parseColor("#22232D");
    private int mInnerColor = Color.parseColor("#3C3D46");
    private int mBackgroundColor = Color.parseColor("#333333");
    private int mProgressColor = Color.parseColor("#ee3333");
    private int mProgressCircleColor = Color.parseColor("#ACCDFF");
    private float mPercentage = 42;

    // center point
    private Point mCenterPoint;

    // paint
    private Paint mOuterCirclePaint;
    private Paint mInnerCirclePaint;

    private Paint mBackgroundPaint;
    private Paint mTintPaint;
    private Paint mCurrentPointPaint;

    public CircularProgressView(Context context) {
        this(context, null);
    }

    public CircularProgressView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CircularProgressView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public CircularProgressView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);

        initAttrs(attrs, defStyleAttr, defStyleRes);
        initialize();
    }

    private void initAttrs(@Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        TypedArray a = getContext().getTheme().obtainStyledAttributes(attrs, R.styleable.CircularProgressView, defStyleAttr, defStyleRes);

        mStrokeWidth = (int) a.getDimension(R.styleable.CircularProgressView_cpv_strokeWidth, mStrokeWidth);
        mOuterColor = a.getColor(R.styleable.CircularProgressView_cpv_frameColor, mOuterColor);
        mInnerColor = a.getColor(R.styleable.CircularProgressView_cpv_fillInColor, mInnerColor);
        mBackgroundColor = a.getColor(R.styleable.CircularProgressView_cpv_backgroundColor, mBackgroundColor);
        mProgressColor = a.getColor(R.styleable.CircularProgressView_cpv_progressColor, mProgressColor);
        mProgressCircleColor = a.getColor(R.styleable.CircularProgressView_cpv_progressCircleColor, mProgressCircleColor);

        mPercentage = a.getFloat(R.styleable.CircularProgressView_cpv_percentage, mPercentage);

        a.recycle();
    }

    private int dp2px(float dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics());
    }

    private void initialize() {
        mBackgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mBackgroundPaint.setStyle(Paint.Style.STROKE);
        mBackgroundPaint.setStrokeWidth(mStrokeWidth);
        mBackgroundPaint.setColor(mBackgroundColor);

        mTintPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mTintPaint.setStyle(Paint.Style.STROKE);
        mTintPaint.setStrokeWidth(mStrokeWidth);
        mTintPaint.setStrokeCap(Paint.Cap.ROUND);
        mTintPaint.setColor(mProgressColor);

        mOuterCirclePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mOuterCirclePaint.setStyle(Paint.Style.STROKE);
        mOuterCirclePaint.setStrokeWidth(mStrokeWidth);
        mOuterCirclePaint.setColor(mOuterColor);

        mInnerCirclePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mInnerCirclePaint.setColor(mInnerColor);

        mCurrentPointPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mCurrentPointPaint.setColor(mProgressCircleColor);

        mCenterPoint = new Point();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        mCenterPoint.x = w / 2;
        mCenterPoint.y = h / 2;

        float radius = (Math.min(w, h)) * 0.5f - mStrokeWidth * 2;

        mRectF.left = mCenterPoint.x - radius;
        mRectF.right = mCenterPoint.x + radius;
        mRectF.top = mCenterPoint.y - radius;
        mRectF.bottom = mCenterPoint.y + radius;

        int[] colors = {Color.parseColor("#3cc1ea"), Color.parseColor("#2f83ff"), Color.parseColor("#3cc1ea")};
        float[] positions = {0, 0.9f, 1f};
        Shader shader = new SweepGradient(w / 2, h / 2, colors, positions);
        mTintPaint.setShader(shader);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        // Rotate canvas by -90° cause the 0° corresponding to 3 o'clock
        canvas.save();
        canvas.rotate(-90, mCenterPoint.x, mCenterPoint.y);

        // outer
        float outerRadius = Math.min(mRectF.width(), mRectF.height()) * 0.5f + mStrokeWidth;
        canvas.drawCircle(mCenterPoint.x, mCenterPoint.y, outerRadius, mOuterCirclePaint);

        // inner
        float innerRadius = (Math.min(mRectF.width(), mRectF.height()) - mStrokeWidth) * 0.5f;
        canvas.drawCircle(mCenterPoint.x, mCenterPoint.y, innerRadius, mInnerCirclePaint);

        // progress
        float progressAngle = mPercentage / 100 * 360;
        canvas.drawArc(mRectF, progressAngle, 360 - progressAngle, false, mBackgroundPaint);
        canvas.drawArc(mRectF, 0, progressAngle, false, mTintPaint);

        // current
        float progressCircleX = (float) Math.cos(Math.toRadians(progressAngle)) * Math.min(mRectF.width(), mRectF.height()) * 0.5f + mCenterPoint.x;
        float progressCircleY = (float) Math.sin(Math.toRadians(progressAngle)) * Math.min(mRectF.width(), mRectF.height()) * 0.5f + mCenterPoint.y;
        canvas.drawCircle(progressCircleX, progressCircleY, mStrokeWidth * 0.35f, mCurrentPointPaint);

        canvas.restore();
    }

    public void setPercentage(float percentage) {
        setPercentage(percentage, false);
    }

    public void setPercentage(float percentage, boolean forceUpdate) {
        if (forceUpdate) {
            mPercentage = percentage;
            invalidate();
        } else {
            ValueAnimator animator = ValueAnimator.ofFloat(mPercentage, percentage);
            animator.setDuration(1000);
            animator.addUpdateListener(animation -> {
                mPercentage = (float) animation.getAnimatedValue();
                invalidate();
            });
            animator.start();
        }
    }
}
