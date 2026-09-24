package fabscreen.libraries.legacy.view.bottombar;

import android.content.Context;
import android.graphics.drawable.Drawable;

import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;

public class BottomBarItem {
    private int mTitleResource;

    private int mIconResource;

    private int mNormalColorRes;
    private int mSelectedColorRes;
    private int mDisabledColorRes;

    public BottomBarItem(@StringRes int titleResource) {
        mTitleResource = titleResource;
    }

    public BottomBarItem setTitle(@StringRes int resid) {
        mTitleResource = resid;
        return this;
    }

    public BottomBarItem setIcon(@DrawableRes int iconResource) {
        mIconResource = iconResource;
        return this;
    }

    public BottomBarItem setNormalColor(@ColorRes int resid) {
        mNormalColorRes = resid;
        return this;
    }

    public BottomBarItem setSelectedColor(@ColorRes int resid) {
        mSelectedColorRes = resid;
        return this;
    }

    public BottomBarItem setDisabledColor(@ColorRes int resid) {
        mDisabledColorRes = resid;
        return this;
    }

    String getTitle(Context context) {
        if (mTitleResource != 0) {
            return context.getString(mTitleResource);
        } else {
            return "";
        }
    }

    Drawable getIcon(Context context) {
        if (mIconResource != 0) {
            return context.getDrawable(mIconResource);
        } else {
            return null;
        }
    }

    int getNormalColor(Context context) {
        if (mNormalColorRes != 0) {
            return context.getColor(mNormalColorRes);
        } else {
            return 0;
        }
    }

    int getSelectedColor(Context context) {
        if (mSelectedColorRes != 0) {
            return context.getColor(mSelectedColorRes);
        } else {
            return 0;
        }
    }

    int getDisabledColor(Context context) {
        if (mDisabledColorRes != 0) {
            return context.getColor(mDisabledColorRes);
        } else {
            return 0;
        }
    }
}
