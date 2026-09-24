package fabscreen.libraries.legacy.view;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import butterknife.BindView;
import butterknife.ButterKnife;
import fabscreen.libraries.legacy.R;
import fabscreen.libraries.legacy.R2;


public class FabFullScreenDialog {
    private AlertDialog mDialog;

    @BindView(R2.id.iv_dialog_icon)
    ImageView mIvIcon;

    @BindView(R2.id.tv_dialog_title)
    TextView mTvTitle;

    @BindView(R2.id.tv_dialog_message)
    TextView mTvMessage;

    @BindView(R2.id.btn_dialog_positive)
    Button mBtnPositive;
    @BindView(R2.id.btn_dialog_negative)
    Button mBtnNegative;

    /**
     * Display alert.
     */
    public static FabFullScreenDialog create(Context context) {
        // create dialog
        final AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.AppTheme_Dialog_Fullscreen);
        final Activity activity = (Activity) context;

        // Create new dialog and config
        AlertDialog dialog = builder.create();

        final LayoutInflater inflater = activity.getLayoutInflater();
        final View view = inflater.inflate(R.layout.dialog_fullscreen, null);
        dialog.setView(view);

        return new FabFullScreenDialog(dialog, view);
    }

    FabFullScreenDialog(AlertDialog dialog, View view) {
        mDialog = dialog;

        ButterKnife.bind(this, view);

        // default
        mBtnPositive.setText(R.string.all_ok);
        mBtnNegative.setText(R.string.all_cancel);
        mIvIcon.setImageResource(R.drawable.pic_dialog_warning_72x72);
    }

    public FabFullScreenDialog setIcon(int resId) {
        mIvIcon.setImageResource(resId);
        return this;
    }

    public FabFullScreenDialog setTitle(int resid) {
        mTvTitle.setText(resid);
        return this;
    }

    public FabFullScreenDialog setTitle(CharSequence text) {
        mTvTitle.setText(text);
        return this;
    }

    public FabFullScreenDialog setMessage(int resid) {
        mTvMessage.setText(resid);
        return this;
    }

    public FabFullScreenDialog setMessage(CharSequence text) {
        mTvMessage.setText(text);
        return this;
    }

    public FabFullScreenDialog setPositive(int resid, boolean enabled) {
        mBtnPositive.setVisibility(View.VISIBLE);
        mBtnPositive.setText(resid);
        mBtnPositive.setEnabled(enabled);
        return this;
    }

    public FabFullScreenDialog setPositive(int resid, AlertDialog.OnClickListener listener) {
        mBtnPositive.setVisibility(View.VISIBLE);
        mBtnPositive.setText(resid);
        mBtnPositive.setOnClickListener(v -> listener.onClick(mDialog, 0));
        return this;
    }

    public FabFullScreenDialog setNegative(int resid, boolean enabled) {
        mBtnNegative.setVisibility(View.VISIBLE);
        mBtnNegative.setText(resid);
        mBtnNegative.setEnabled(enabled);
        return this;
    }

    public FabFullScreenDialog setNegative(int resid, AlertDialog.OnClickListener listener) {
        mBtnNegative.setVisibility(View.VISIBLE);
        mBtnNegative.setText(resid);
        mBtnNegative.setOnClickListener(v -> listener.onClick(mDialog, 1));
        return this;
    }

    public void show() {
        mDialog.show();
    }

    public void dismiss() {
        mDialog.dismiss();
    }

    public boolean isShowing() {
        return  mDialog.isShowing();
    }
}
