package fabscreen.libraries.legacy.view;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import fabscreen.libraries.legacy.R;

public class FabLoading {
    private static FabLoading sInstance;

    private AlertDialog mDialog;
    private TextView mTvTitle;
    private TextView mTvDesc;
    private TextView mTvProgress;
    private CircularProgressView mCpvProgress;

    public static FabLoading create(Context context) {
        // Dismiss existing dialog
        if (sInstance != null) {
            sInstance.mDialog.cancel();
            sInstance.mDialog.dismiss();
        }

        final AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.AppTheme_Dialog_Fullscreen);
        final Activity activity = (Activity) context;

        // Create new dialog and config
        AlertDialog dialog = builder.create();

        final LayoutInflater inflater = activity.getLayoutInflater();
        final View view = inflater.inflate(R.layout.dialog_fullscreen_loading, null);
        dialog.setView(view);

        sInstance = new FabLoading();
        sInstance.mDialog = dialog;
        sInstance.mTvTitle = view.findViewById(R.id.tv_download_title);
        sInstance.mTvDesc = view.findViewById(R.id.tv_download_desc);
        sInstance.mTvProgress = view.findViewById(R.id.tv_download_progress);
        sInstance.mCpvProgress = view.findViewById(R.id.cpv_download_progress);

        return sInstance;
    }

    public FabLoading setTitle(CharSequence text) {
        mTvTitle.setText(text);
        return this;
    }

    public FabLoading setTitle(int resid) {
        mTvTitle.setText(resid);
        return this;
    }

    public FabLoading setDescription(CharSequence text) {
        mTvDesc.setText(text);
        return this;
    }

    public FabLoading setDescription(int resid) {
        mTvDesc.setText(resid);
        return this;
    }

    public FabLoading setProgress(CharSequence text) {
        mTvProgress.setText(text);
        mCpvProgress.setPercentage(Integer.valueOf(text.toString()), true);
        return this;
    }

    public void show() {
        mDialog.show();
    }

    public void dismiss() {
        if (sInstance != null) {
            sInstance.mDialog.cancel();
            sInstance.mDialog.dismiss();
        }
    }
}
