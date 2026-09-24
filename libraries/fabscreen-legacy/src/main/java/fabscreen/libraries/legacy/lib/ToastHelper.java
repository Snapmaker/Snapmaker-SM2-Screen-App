package fabscreen.libraries.legacy.lib;

import android.content.Context;
import android.content.Intent;

import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;

import fabscreen.libraries.legacy.view.ToastActivity;


public class ToastHelper {
    private int mDrawableId;
    private int mMessageId;
    private int mTitleId;
    private int mShowTime;
    private int mYMargin;

    private ToastHelper(int drawableId, int messageId, int titleId, int showTime, int yMargin) {
        mDrawableId = drawableId;
        mMessageId = messageId;
        mTitleId = titleId;
        mShowTime = showTime;
        mYMargin = yMargin;
    }

    public void showToast(Context context) {
        Intent intent = new Intent(context, ToastActivity.class);
        intent.putExtra(ToastActivity.TOAST_MESSAGE, mMessageId);
        intent.putExtra(ToastActivity.TOAST_DRAWABLE, mDrawableId);
        intent.putExtra(ToastActivity.TOAST_TITLE, mTitleId);
        intent.putExtra(ToastActivity.TOAST_SHOW_TIME, mShowTime);
        intent.putExtra(ToastActivity.TOAST_Y_MARGIN, mYMargin);
        context.startActivity(intent);
    }

    public static final class Builder {
        // Default is -1,will not be displayed
        private int mDrawableId = ToastActivity.VIEW_DONE;
        private int mMessageId = ToastActivity.VIEW_DONE;
        private int mTitleId = ToastActivity.VIEW_DONE;
        private int mShowTime = ToastActivity.DEFAULT_SHOW_TIME;
        private int mYMargin = ToastActivity.DEFAULT_Y_MARGIN;


        public Builder() {
        }

        public Builder setTitle(@StringRes int titleId) {
            mTitleId = titleId;
            return this;
        }

        public Builder setDrawable(@DrawableRes int drawableId) {
            mDrawableId = drawableId;
            return this;
        }

        public Builder setMessage(@StringRes int messageId) {
            mMessageId = messageId;
            return this;
        }

        public Builder setShowTime(int showTime) {
            mShowTime = showTime;
            return this;
        }

        public Builder setYMargin(int dpValue) {
            mYMargin = dpValue;
            return this;
        }

        public ToastHelper build() {
            return new ToastHelper(mDrawableId, mMessageId, mTitleId, mShowTime, mYMargin);
        }
    }
}
