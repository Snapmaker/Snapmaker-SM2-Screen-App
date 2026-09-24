package fabscreen.libraries.legacy.view.RectEnergyBar;

import android.content.Context;
import android.graphics.Point;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;

import java.util.ArrayList;

import fabscreen.libraries.legacy.R;

public class RectEnergyBar extends FrameLayout {

    private int mWidth;
    private int mHeight;

    private int mLevel = 0;

    private LinearLayout mEnergyBarContainer;
    private ArrayList<RectEnergyBarButton> mButtons = new ArrayList<>();

    public RectEnergyBar(Context context) {
        this(context, null);
    }

    public RectEnergyBar(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RectEnergyBar(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public RectEnergyBar(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);

        initAttrs(attrs, defStyleAttr, defStyleRes);
        initialize(context);
    }

    private void initAttrs(@Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        // undefined
    }

    private void initialize(Context context) {
        LayoutInflater inflater  = LayoutInflater.from(context);
        View parentView = inflater.inflate(R.layout.view_rect_energy_bar_container, this, true);
        mEnergyBarContainer = parentView.findViewById(R.id.ll_rect_energy_bar_tab_container);
    }

    public void setMaxLevel(int level) {
        mLevel = level;
    }

    public void initialize() {
        mEnergyBarContainer.removeAllViews();
        // temporary
        mWidth = 640;
        int buttonWidth = mWidth / mLevel;

        for (int i = 0; i < mLevel; i++) {
            RectEnergyBarButton button = new RectEnergyBarButton(getContext());
            setUpButton(button, i, buttonWidth);
        }
    }

    private void setUpButton(RectEnergyBarButton button, int position, int width) {
        button.setPosition(position);
        button.setWidth(width);
        button.initialize(getContext());

        mEnergyBarContainer.addView(button);
        mButtons.add(button);
    }

    private static int getScreenWidth(Context context) {
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Point size = new Point();
        wm.getDefaultDisplay().getSize(size);
        return size.x;
    }


    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        mWidth = MeasureSpec.getSize(widthMeasureSpec);
        mHeight = MeasureSpec.getSize(heightMeasureSpec);
    }

    public void setPosition(int position) {
        setPositionInternal(position, true);
    }

    private void setPositionInternal(int position, boolean callListener) {
        for (RectEnergyBarButton button : mButtons) {
            if (button.getPosition() > position) {
                button.hide();
            } else {
                button.show();
            }
        }

        if (callListener) {
            // Reserve for callback event.
        }
    }
}
