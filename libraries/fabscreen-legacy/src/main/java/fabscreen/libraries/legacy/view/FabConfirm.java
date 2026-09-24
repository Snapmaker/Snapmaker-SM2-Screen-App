package fabscreen.libraries.legacy.view;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import butterknife.BindView;
import butterknife.ButterKnife;
import fabscreen.libraries.legacy.R;
import fabscreen.libraries.legacy.R2;
import fabscreen.libraries.legacy.lib.DimensUtils;

public class FabConfirm {
    private AlertDialog mDialog;

    @BindView(R2.id.tv_dialog_confirm_desc)
    TextView mTvDescription;

    @BindView(R2.id.btn_dialog_confirm_cancel)
    Button mBtnCancel;
    @BindView(R2.id.btn_dialog_confirm_confirm)
    Button mBtnConfirm;

    @BindView(R2.id.iv_dialog_confirm_icon)
    ImageView mIvIcon;

    public static FabConfirm create(Context context) {
        // create dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.AppTheme_Dialog);
        final AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(R.color.default_theme_dark_mask);
            dialog.getWindow().setLayout(
                    DimensUtils.dp2px(280f, context),
                    WindowManager.LayoutParams.WRAP_CONTENT);
        }

        // create view
        final LayoutInflater inflater = LayoutInflater.from(context);
        final View view = inflater.inflate(R.layout.dialog_confirm, null);
//        dialog.setCanceledOnTouchOutside(false);
        dialog.setView(view);

        // instance
        return new FabConfirm(dialog, view);
    }

    FabConfirm(AlertDialog dialog, View view) {
        mDialog = dialog;
        ButterKnife.bind(this, view);
    }

    public boolean isShowing() {
        return mDialog.isShowing();
    }

    public FabConfirm setDescription(int resid) {
        mTvDescription.setText(resid);
        return this;
    }

    public FabConfirm setDescription(String desc) {
        mTvDescription.setText(desc);
        return this;
    }

    public FabConfirm setIcon(int resid) {
        mIvIcon.setVisibility(ImageView.VISIBLE);
        mIvIcon.setImageResource(resid);
        return this;
    }

    public FabConfirm setCancel(int resid, AlertDialog.OnClickListener listener) {
        mBtnCancel.setVisibility(View.VISIBLE);
        mBtnCancel.setText(resid);
        mBtnCancel.setOnClickListener(v -> listener.onClick(mDialog, 0));
        return this;
    }

    public FabConfirm setConfirm(int resid, AlertDialog.OnClickListener listener) {
        mBtnConfirm.setText(resid);
        mBtnConfirm.setOnClickListener(v -> listener.onClick(mDialog, 0));
        return this;
    }

    public FabConfirm setCanceledOnTouchOutSide(boolean cancel) {
        mDialog.setCanceledOnTouchOutside(cancel);
        return this;
    }

    public void show() {
        mDialog.show();

        // Dynamically change dialog width (trick)
        if (mDialog.getWindow() != null) {
            mDialog.getWindow().setBackgroundDrawableResource(R.color.default_theme_dark_mask);
            mDialog.getWindow().setLayout(300 * 2, WindowManager.LayoutParams.WRAP_CONTENT);
        }
    }

    public void dismiss() {
        mDialog.dismiss();
    }
}
