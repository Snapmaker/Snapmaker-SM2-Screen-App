package com.snapmaker.fabscreen.view;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.snapmaker.fabscreen.R;
import fabscreen.libraries.legacy.lib.DimensUtils;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

public class FabProgressDialog extends Dialog {

    @BindView(R.id.tv_progress_msg)
    TextView mTvMsg;
    @BindView(R.id.progressbar)
    CircularProgressIndicator mProgressbar;
    private Unbinder mUnbinder;
    private @StringRes
    int mMsgResId;
    private boolean mCancelOnTouchOutside = false;


    public FabProgressDialog(@NonNull Context context) {
        super(context);
    }

    /**
     * Note that onCreate will be called after {@link Dialog#show()}.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_progress);

        Window window = getWindow();
        if (window == null) return;
        window.setBackgroundDrawableResource(R.drawable.progress_dialog_background);
        window.setLayout(
                DimensUtils.dp2px(280, getContext()),
                WindowManager.LayoutParams.WRAP_CONTENT);

        setCanceledOnTouchOutside(mCancelOnTouchOutside);
    }

    @Override
    protected void onStart() {
        super.onStart();
        mUnbinder = ButterKnife.bind(this);
        mProgressbar.show();
        mTvMsg.setText(mMsgResId);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mProgressbar == null) return;
        mProgressbar.hide();
        if (mUnbinder == null) return;
        mUnbinder.unbind();
    }

    public void setMessage(@StringRes int resId) {
        mMsgResId = resId;
    }

    public void setCancelOnTouchOutside(boolean cancel) {
        mCancelOnTouchOutside = cancel;
    }
}
