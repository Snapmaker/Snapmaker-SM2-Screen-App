package fabscreen.libraries.legacy.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import fabscreen.libraries.legacy.R;

public class SquareActionButton extends RelativeLayout {
    private static final int SIZE_NORMAL = 0;

    private View mContainerView;
    private Button mBtnBackground;
    private Button mBtnForeground;
    private TextView mTvTitle;

    private int mSize;
    private Drawable mForegroundDrawable;
    private int mTitleRes;
    private int mTextRes;

    public SquareActionButton(Context context) {
        this(context, null);
    }

    public SquareActionButton(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SquareActionButton(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public SquareActionButton(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);

        initAttrs(attrs, defStyleAttr, defStyleRes);
        initialize(context);
    }

    private void initAttrs(@Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        TypedArray a = getContext().getTheme().obtainStyledAttributes(attrs, R.styleable.SquareActionButton, defStyleAttr, defStyleRes);

        mForegroundDrawable = a.getDrawable(R.styleable.SquareActionButton_sab_foreground);
        mTitleRes = a.getResourceId(R.styleable.SquareActionButton_sab_title, -1);
        mTextRes = a.getResourceId(R.styleable.SquareActionButton_sab_text, -1);
        mSize = a.getInt(R.styleable.SquareActionButton_sab_size, 0);

        a.recycle();
    }

    private void initialize(Context context) {
        LayoutInflater inflater = LayoutInflater.from(context);

        View view;
        switch (mSize) {
            case SIZE_NORMAL:
            default: {
                view = inflater.inflate(R.layout.view_square_action_button, this, true);
                break;
            }
        }

        mContainerView = view.findViewById(R.id.view_square_action_button);
        mTvTitle = view.findViewById(R.id.tv_view_square_action_button_title);
        mBtnBackground = view.findViewById(R.id.btn_view_square_action_button_background);
        mBtnForeground = view.findViewById(R.id.btn_view_square_action_button_foreground);

        if (mTitleRes != -1) {
            mTvTitle.setVisibility(VISIBLE);
            mTvTitle.setText(mTitleRes);
        }

        mBtnForeground.setBackground(mForegroundDrawable);
        if (mTextRes != -1) {
            mBtnForeground.setText(mTextRes);
        }
        mBtnForeground.setClickable(false);
        mBtnBackground.setClickable(false);
    }

    public void setTitle(@StringRes int resid) {
        mTvTitle.setVisibility(VISIBLE);
        mTvTitle.setText(resid);
    }

    public void setTitle(String title) {
        mTvTitle.setVisibility(VISIBLE);
        mTvTitle.setText(title);
    }

    public void setText(@StringRes int resid) {
        mBtnForeground.setText(resid);
    }

    public void setText(CharSequence text) {
        mBtnBackground.setText(text);
    }

    public void setBackground(Drawable drawable) {
        mForegroundDrawable = drawable;
        mBtnForeground.setBackground(mForegroundDrawable);
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);

        mBtnBackground.setEnabled(enabled);
        mBtnForeground.setEnabled(enabled);
    }

    @Override
    public void setPressed(boolean pressed) {
        // super.setPressed(pressed);
    }

    @Override
    public void setActivated(boolean activated) {
        super.setActivated(activated);

        mBtnBackground.setActivated(activated);
        mBtnForeground.setActivated(activated);
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN: {
                int top = mContainerView.getTop() + mBtnBackground.getTop();
                int bottom = mContainerView.getTop() + mBtnBackground.getBottom();
                int left = mContainerView.getLeft() + mBtnBackground.getLeft();
                int right = mContainerView.getLeft() + mBtnBackground.getRight();
                float x = event.getX();
                float y = event.getY();

                if (x < left || x >= right || y < top || y >= bottom) {
                    return true;
                }

                mBtnBackground.setPressed(true);
                mBtnForeground.setPressed(true);
                return true;
            }
            case MotionEvent.ACTION_UP: {
                mBtnBackground.setPressed(false);
                mBtnForeground.setPressed(false);

                int size = getRight() - getLeft();
                float x = event.getX();
                float y = event.getY();

                if (x < 0 || x >= size || y < 0 || y >= size) {
                    break;
                }

                if (isEnabled()) {
                    performClick();
                }
                return true;
            }
            case MotionEvent.ACTION_CANCEL:
                mBtnBackground.setPressed(false);
                mBtnForeground.setPressed(false);
                return true;
            default:
                break;
        }

        return false;
    }
}
