package fabscreen.libraries.legacy.view.bottombar;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

import fabscreen.libraries.legacy.R;

public class BottomBarTab extends RelativeLayout {
    private ImageView mIvIcon;
    private TextView mTvTitle;
    private View mViewSeparator;

    private String mTitle;
    private Drawable mIcon;

    private int mNormalColor;
    private int mSelectedColor;
    private int mDisabledColor;

    private int mPosition;

    private boolean mSeparatorVisible = false;
    private boolean mEnabled = true;
    // private boolean mSelected = false;

    public BottomBarTab(Context context) {
        this(context, null);
    }

    public BottomBarTab(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BottomBarTab(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public BottomBarTab(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);

        initAttrs(attrs, defStyleAttr, defStyleRes);
        initialize();
    }

    private void initAttrs(@Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        // undefined
    }

    private void initialize() {
        setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT));

        LayoutInflater inflater = LayoutInflater.from(getContext());
        View view = inflater.inflate(R.layout.view_bottom_bar_tab, this, true);

        mIvIcon = view.findViewById(R.id.iv_bottom_bar_tab_icon);
        mTvTitle = view.findViewById(R.id.tv_bottom_bar_tab_title);
        mViewSeparator = view.findViewById(R.id.view_bottom_bar_tab_separator);
    }

    // public methods

    void setTitle(String title) {
        mTitle = title;
    }

    void setIcon(Drawable icon) {
        mIcon = icon;
    }

    void setNormalColor(int color) {
        mNormalColor = color;
    }

    void setSelectedColor(int color) {
        mSelectedColor = color;
    }

    void setDisabledColor(int color) {
        mDisabledColor = color;
    }

    int getPosition() {
        return mPosition;
    }

    void setPosition(int position) {
        mPosition = position;
    }

    void setWidth(int width) {
        ViewGroup.LayoutParams params = getLayoutParams();
        params.width = width;
        setLayoutParams(params);
    }

    void setSeparatorVisible(boolean visible) {
        mSeparatorVisible = visible;
    }

    void initialize(Context context) {
        mEnabled = true;

        mTvTitle.setText(mTitle);
        mTvTitle.setTextAlignment(TEXT_ALIGNMENT_CENTER);

        mIvIcon.setEnabled(true);
        mIvIcon.setSelected(false);

        ColorStateList stateList = new ColorStateList(
                new int[][]{
                        new int[]{-android.R.attr.state_enabled},
                        new int[]{android.R.attr.state_selected},
                        new int[]{}
                },
                new int[]{
                        mDisabledColor,
                        mSelectedColor,
                        mNormalColor
                }
        );
        mIvIcon.setImageTintList(stateList);
        mIvIcon.setImageDrawable(mIcon);

        mViewSeparator.setVisibility(mSeparatorVisible ? View.VISIBLE : View.GONE);
    }

    @Override
    public boolean isEnabled() {
        return mEnabled;
    }

    void enable() {
        if (mEnabled) {
            return;
        }

        mEnabled = true;

        mIvIcon.setEnabled(true);
        deselect();
    }


    void disable() {
        if (!mEnabled) {
            return;
        }

        mEnabled = false;

        mIvIcon.setEnabled(false);
        mTvTitle.setTextColor(mDisabledColor);
    }

    void select() {
        if (!mEnabled) {
            return;
        }

        // mSelected = true;

        mIvIcon.setSelected(true);
        mTvTitle.setTextColor(mSelectedColor);
    }

    void deselect() {
        if (!mEnabled) {
            return;
        }

        // mSelected = false;

        mIvIcon.setSelected(false);
        mTvTitle.setTextColor(mNormalColor);
    }
}
