package com.snapmaker.fabscreen.modules.emergencystop;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;

import androidx.annotation.Nullable;

import com.snapmaker.fabscreen.R;

import butterknife.ButterKnife;
import fabscreen.libraries.legacy.base.BaseActivity;

public class EmergencyStopActivity extends BaseActivity {

    private boolean mIsTriggerOnPowerUp;
    private AlertDialog mDialog;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_default);
        ButterKnife.bind(this);

        Intent intent = getIntent();
        Bundle bundle = intent.getExtras();
        if (bundle != null) {
            mIsTriggerOnPowerUp = bundle.getBoolean("is_triggered_on_power_up");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        showEmergencyWarningDialog(mIsTriggerOnPowerUp);
    }

    private void showEmergencyWarningDialog(boolean isTriggeredOnPowerUp) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AppTheme_Dialog);
        if (mDialog != null) {
            if (mDialog.isShowing()) return;
        }
        mDialog = builder.create();
        if (mDialog.getWindow() != null) {
            mDialog.getWindow().setBackgroundDrawableResource(R.color.default_theme_dark_mask);
            mDialog.getWindow().setLayout(280 * 2, WindowManager.LayoutParams.WRAP_CONTENT);
        }

        LayoutInflater inflater = LayoutInflater.from(this);
        View view = inflater.inflate(isTriggeredOnPowerUp
                        ? R.layout.dialog_emergency_stop_warning_2
                        : R.layout.dialog_emergency_stop_warning,
                null);
        mDialog.setView(view);
        mDialog.setCanceledOnTouchOutside(false);
        mDialog.show();
    }
}
