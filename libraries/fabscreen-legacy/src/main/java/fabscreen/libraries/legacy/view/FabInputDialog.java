package fabscreen.libraries.legacy.view;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import fabscreen.libraries.legacy.R;

public class FabInputDialog {
    private static FabInputDialog sInstance;

    private AlertDialog mDialog;
    private TextView mTvTitle;
    private EditText mEtInput;
    private Button mBtnConnect;

    /**
     * Display alert.
     */
    public static FabInputDialog create(Context context) {
        // Dismiss existing dialog
        if (sInstance != null) {
            sInstance.mDialog.cancel();
            sInstance.mDialog.dismiss();
        }

        final AlertDialog.Builder builder = new AlertDialog.Builder(context);
        final Activity activity = (Activity) context;

        // Create new dialog and config
        AlertDialog dialog = builder.create();

        final LayoutInflater inflater = activity.getLayoutInflater();
        final View view = inflater.inflate(R.layout.dialog_input, null);
        dialog.setView(view);

        sInstance = new FabInputDialog();
        sInstance.mDialog = dialog;
        sInstance.mTvTitle = view.findViewById(R.id.tv_dialog_input_title);
        sInstance.mEtInput = view.findViewById(R.id.et_dialog_input_content);
        sInstance.mEtInput.requestFocus();

        // Disable ActionMode in EditText
        sInstance.mEtInput.setLongClickable(false);
        sInstance.mEtInput.setCustomSelectionActionModeCallback(new ActionMode.Callback() {
            @Override
            public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                return false;
            }

            @Override
            public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                return false;
            }

            @Override
            public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                return false;
            }

            @Override
            public void onDestroyActionMode(ActionMode mode) {

            }
        });

        sInstance.mBtnConnect = view.findViewById(R.id.btn_dialog_connect);
        sInstance.mBtnConnect.setText(R.string.all_connect);

        InputMethodManager im = (InputMethodManager) view.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (im.isActive()) {
            im.toggleSoftInputFromWindow(view.getApplicationWindowToken(), 0, 0);
        }

        return sInstance;
    }

    public FabInputDialog setTitle(CharSequence text) {
        mTvTitle.setText(text);
        return this;
    }

    public FabInputDialog setTitle(int resid) {
        mTvTitle.setText(resid);
        return this;
    }

    public FabInputDialog setButton(CharSequence text, AlertDialog.OnClickListener listener) {
        mBtnConnect.setText(text);
        mBtnConnect.setOnClickListener(v -> {
            listener.onClick(mDialog, 0);
            mDialog.dismiss();
        });
        return this;
    }

    public static FabInputDialog getsInstance() {
        return sInstance;
    }

    public String getEditTextContent() {
        return mEtInput.getText().toString();
    }

    public FabInputDialog setEditText(String text) {
        if (text == null) {
            text = "";
        }
        mEtInput.setText(text);
        mEtInput.setSelection(text.length());
        return this;
    }

    public void show() {
        mDialog.show();
    }

    public void dismiss() {
        mDialog.dismiss();
    }

    public boolean isShowing() {
        return mDialog.isShowing();
    }
}
