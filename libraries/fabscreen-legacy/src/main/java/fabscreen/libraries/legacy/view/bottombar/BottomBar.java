package fabscreen.libraries.legacy.view.bottombar;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Point;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import androidx.annotation.ColorRes;
import androidx.annotation.Nullable;

import java.util.ArrayList;

import fabscreen.libraries.legacy.R;

public class BottomBar extends FrameLayout {
    private FrameLayout mContainer;
    private LinearLayout mTabContainer;

    private int mBackgroundColor;

    private ArrayList<BottomBarItem> mItems = new ArrayList<>();
    private ArrayList<BottomBarTab> mTabs = new ArrayList<>();
    private int mSelectedPosition = -1;

    private OnTabSelectedListener mOnTabSelectedListener;

    public BottomBar(Context context) {
        this(context, null);
    }

    public BottomBar(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BottomBar(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public BottomBar(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);

        initAttrs(attrs, defStyleAttr, defStyleRes);
        initialize(context);
    }

    private void initAttrs(@Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        // undefined
        mBackgroundColor = Color.WHITE;
    }

    private void initialize(Context context) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View parentView = inflater.inflate(R.layout.view_bottom_bar_container, this, true);

        mContainer = parentView.findViewById(R.id.bottom_bar_container);
        mTabContainer = parentView.findViewById(R.id.bottom_bar_tab_container);
    }

    private static int getScreenWidth(Context context) {
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Point size = new Point();
        wm.getDefaultDisplay().getSize(size);
        return size.x;
    }

    private void setUpTab(BottomBarTab tab, BottomBarItem item, int width, int position, boolean firstTab) {
        tab.setWidth(width);

        Context context = getContext();

        // bind
        tab.setPosition(position);

        tab.setTitle(item.getTitle(context));
        tab.setIcon(item.getIcon(context));

        tab.setNormalColor(item.getNormalColor(context));
        tab.setSelectedColor(item.getSelectedColor(context));
        tab.setDisabledColor(item.getDisabledColor(context));

        tab.setSeparatorVisible(!firstTab);

        tab.setOnClickListener(v -> {
            BottomBarTab view = (BottomBarTab) v;
            if (view.isEnabled()) {
                selectTabInternal(view.getPosition(), true);
            }
        });

        tab.initialize(context);

        mTabContainer.addView(tab);
    }

    private void selectTabInternal(int position, boolean callListener) {
        if (mSelectedPosition == position) {
            return;
        }

        if (mSelectedPosition != -1) {
            mTabs.get(mSelectedPosition).deselect();
        }
        mTabs.get(position).select();
        mSelectedPosition = position;

        if (callListener && mOnTabSelectedListener != null) {
            mOnTabSelectedListener.onTabSelected(position);
        }
    }

    // public methods

    public void setBarBackgroundColor(@ColorRes int backgroundColorResource) {
        this.mBackgroundColor = getContext().getColor(backgroundColorResource);
    }

    public void addItem(BottomBarItem item) {
        mItems.add(item);
    }

    public void initialize() {
        mSelectedPosition = -1;
        mTabs.clear();

        mContainer.setBackgroundColor(mBackgroundColor);

        if (!mItems.isEmpty()) {
            mTabContainer.removeAllViews();

            int screenWidth = getScreenWidth(getContext());
            // When there are more than 5 tabs, use constant width of (ScreenWidth / 4.5) which
            // shows 4.5 tabs visible to users. Otherwise, full up bottom bar with equal width
            // of tabs.
            int tabWidth = (int) (screenWidth / (Math.min(mItems.size(), 4.5)));

            for (int i = 0; i < mItems.size(); i++) {
                BottomBarItem item = mItems.get(i);
                BottomBarTab tab = new BottomBarTab(getContext());

                setUpTab(tab, item, tabWidth, i, i == 0);

                mTabs.add(tab);
            }

            selectTabInternal(0, false);
        }
    }

    public void selectTab(int position) {
        selectTabInternal(position, true);
    }

    public void enableTab(int position) {
        BottomBarTab tab = mTabs.get(position);
        tab.enable();
    }

    public void disableTab(int position) {
        BottomBarTab tab = mTabs.get(position);
        tab.disable();
    }

    public void setOnTabSelectedListener(OnTabSelectedListener listener) {
        mOnTabSelectedListener = listener;
    }

    public interface OnTabSelectedListener {
        void onTabSelected(int position);
    }
}
