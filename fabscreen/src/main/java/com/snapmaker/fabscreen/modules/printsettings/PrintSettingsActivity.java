package com.snapmaker.fabscreen.modules.printsettings;

import android.app.Activity;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.orhanobut.logger.Logger;
import com.snapmaker.fabscreen.FabScreenApplication;
import com.snapmaker.fabscreen.R;
import com.snapmaker.fabscreen.router.Router;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import fabscreen.libraries.legacy.base.BaseActivity;
import fabscreen.libraries.legacy.data.Constants;

public class PrintSettingsActivity extends BaseActivity {
    private static final String TAG = PrintSettingsActivity.class.getSimpleName();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_default);

        int headType = getModel().getMachineController().getHeadType();

        switch (headType) {
            case Constants.HEAD_3DP:
            case Constants.HEAD_3DP_DUAL_EXTRUDER:
                goto3DPPrintSettings();
                break;
            case Constants.HEAD_LASER:
            case Constants.HEAD_LASER_2W_IR:
            case Constants.HEAD_LASER_10W:
            case Constants.HEAD_LASER_20W:
            case Constants.HEAD_LASER_40W:
                gotoLaserPrintSettings();
                break;
            case Constants.HEAD_CNC:
            case Constants.HEAD_CNC_200W:
                gotoCNCPrintSettings();
                break;
        }
    }

    @Override
    public void onBackPressed() {
        List<WeakReference<Activity>> activityReferences = FabScreenApplication.getInstance().getFabScreenActivityManagement().getRunningActivities();

        if (activityReferences.isEmpty()) {
            super.onBackPressed();
        } else {
            Activity targetActivity = activityReferences.get(activityReferences.size() - 2).get();
            if (targetActivity != null) {
                Router.getInstance().routeWithClass(targetActivity.getClass()).start(this);
                finish();
            }
        }
    }

    /**
     * 3DP Settings
     */
    public void goto3DPPrintSettings() {
        Fragment fragment = new PrintSettings3DPFragment();
        addFragment(R.id.fragment_container, fragment);
    }

    public void gotoLaserPrintSettings() {
        Fragment fragment = new PrintSettingsLaserFragment();
        addFragment(R.id.fragment_container, fragment);
    }

    public void gotoCNCPrintSettings() {
        Fragment fragment = new PrintSettingsCNCFragment();
        addFragment(R.id.fragment_container, fragment);
    }
}
