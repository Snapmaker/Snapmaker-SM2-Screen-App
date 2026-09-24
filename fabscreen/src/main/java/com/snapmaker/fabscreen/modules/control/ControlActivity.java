package com.snapmaker.fabscreen.modules.control;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.snapmaker.fabscreen.R;

import fabscreen.libraries.legacy.base.BaseActivity;
import fabscreen.libraries.legacy.data.Constants;

public class ControlActivity extends BaseActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_default);

        int headType = getModel().getMachineController().getHeadType();

        initRootFragment(headType);
    }

    private void initRootFragment(int headType) {
        Fragment fragment = null;

        switch (headType) {
            case Constants.HEAD_UNPLUGGED:
            case Constants.HEAD_3DP:
                // Do we have fragment when head type is not detected?
                fragment = new Control3DPFragment();
                break;
            case Constants.HEAD_3DP_DUAL_EXTRUDER:
                fragment = new ControlDualExtruder3DPFragment();
                break;
            case Constants.HEAD_LASER:
            case Constants.HEAD_LASER_2W_IR:
            case Constants.HEAD_LASER_10W:
            case Constants.HEAD_LASER_20W:
            case Constants.HEAD_LASER_40W:
                fragment = new ControlLaserFragment();
                break;
            case Constants.HEAD_CNC:
            case Constants.HEAD_CNC_200W:
                fragment = new ControlCNCFragment();
                break;
        }

        if (fragment != null) {
            replaceFragment(R.id.fragment_container, fragment);
        }
    }
}


