package fabscreen.libraries.legacy.view;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;

import fabscreen.libraries.legacy.R;

public class SteeringView extends View {
    public static final int DIRECTION_IDLE = 0;
    public static final int DIRECTION_UP = 1;
    public static final int DIRECTION_DOWN = 2;
    public static final int DIRECTION_LEFT = 3;
    public static final int DIRECTION_RIGHT = 4;

    public interface OnDirectionClickedListener {
        void onClick(int state);
    }

    private int mWidth;
    private int mHeight;
    private int direction = 0;

    private OnDirectionClickedListener mListener;

    public SteeringView(Context context) {
        this(context, null);
    }

    public SteeringView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        mWidth = getMeasuredWidth();
        mHeight = getMeasuredHeight();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (!isEnabled()) {
            setBackgroundResource(R.drawable.btn_steering_view_disabled_240x240);
        } else {
            switch (direction) {
                case DIRECTION_IDLE:
                    setBackgroundResource(R.drawable.btn_steering_view_240x240);
                    break;
                case DIRECTION_UP:
                    setBackgroundResource(R.drawable.btn_steering_view_up_240x240);
                    break;
                case DIRECTION_DOWN:
                    setBackgroundResource(R.drawable.btn_steering_view_down_240x240);
                    break;
                case DIRECTION_LEFT:
                    setBackgroundResource(R.drawable.btn_steering_view_left_240x240);
                    break;
                case DIRECTION_RIGHT:
                    setBackgroundResource(R.drawable.btn_steering_view_right_240x240);
                    break;
            }
        }
    }

    private void setDirection(int direction) {
        if (direction != this.direction) {
            this.direction = direction;
            invalidate();
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!isEnabled()) {
            return false;
        }

        final float x = event.getX();
        final float y = event.getY();
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_MOVE:
                final int newDirection = calculateNewDirection(x, y);
                setDirection(newDirection);
                break;
            case MotionEvent.ACTION_UP:
                if (mListener != null) {
                    mListener.onClick(direction);
                }
                direction = DIRECTION_IDLE;
                invalidate();
                break;
        }

        return true;
    }

    private int calculateNewDirection(float x, float y) {
        final float centerX = mWidth * .5f;
        final float centerY = mHeight * .5f;

        final double dist = Math.sqrt((x - centerX) * (x - centerX) + (y - centerY) * (y - centerY));
        if (dist < mWidth * 0.2 || dist > mWidth * 0.4) {
            return direction; // unchanged
        }

        if (x < centerX && Math.abs(x - centerX) >= Math.abs(y - centerY)) {
            return DIRECTION_LEFT;
        } else if (x > centerX && Math.abs(x - centerX) >= Math.abs(y - centerY)) {
            return DIRECTION_RIGHT;
        } else if (y < centerY && Math.abs(x - centerX) <= Math.abs(y - centerY)) {
            return DIRECTION_UP;
        } else if (y > centerY && Math.abs(x - centerX) <= Math.abs(y - centerY)) {
            return DIRECTION_DOWN;
        }

        // never reach here
        return DIRECTION_IDLE;
    }

    public void setOnDirectionClickedListener(OnDirectionClickedListener listener) {
        this.mListener = listener;
    }
}
