package com.snapmaker.fabscreen.modules.settings;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.snapmaker.fabscreen.R;
import com.snapmaker.fabscreen.modules.settings.advanced3dp.Settings3DPCalibrationGridFragment;
import com.snapmaker.fabscreen.modules.settings.advanced3dp.SettingsAdvanced3DPFragment;
import com.snapmaker.fabscreen.modules.settings.advancedlaser.Laser40wCrosslineIndicatorOffsetFragment;
import com.snapmaker.fabscreen.modules.settings.advancedlaser.Laser40wFireSensorSensitivityFragment;
import com.snapmaker.fabscreen.modules.settings.advancedlaser.LaserFocusModificationFragment;
import com.snapmaker.fabscreen.modules.settings.advancedlaser.LaserShotOutputPowerFragment;
import com.snapmaker.fabscreen.modules.settings.advancedlaser.SettingsAdvancedLaser10WFragment;
import com.snapmaker.fabscreen.modules.settings.advancedlaser.SettingsAdvancedLaser20WFragment;
import com.snapmaker.fabscreen.modules.settings.advancedlaser.SettingsAdvancedLaser2WFragment;
import com.snapmaker.fabscreen.modules.settings.advancedlaser.SettingsAdvancedLaser40WFragment;
import com.snapmaker.fabscreen.modules.settings.advancedlaser.SettingsAdvancedLaserFragment;
import com.snapmaker.fabscreen.modules.settings.advancedlaser.SettingsLaser10wToolheadFocusCalibrationFragment;
import com.snapmaker.fabscreen.modules.settings.advancedlaser.SettingsLaser10wTouchPlatformFragment;
import com.snapmaker.fabscreen.modules.settings.language.SettingsLanguageFragment;
import com.snapmaker.fabscreen.modules.settings.wifi.SettingsWifiFragment;
import com.snapmaker.fabscreen.modules.welcome.wifipassword.WelcomeWifiPasswordFragment;

import fabscreen.libraries.legacy.base.BaseActivity;

public class SettingsActivity extends BaseActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_default);

        gotoSettingsHome();
    }

    private void gotoSettingsHome() {
        SettingsHomeFragment fragment = new SettingsHomeFragment();
        addFragment(R.id.fragment_container, fragment);
    }

    public void gotoWiFiList() {
        addFragment(R.id.fragment_container, SettingsWifiFragment.newInstance());
    }

    /**
     * Start PasswordFragment to obtain password.
     */
    public void startWifiPasswordFragment() {
        addFragment(R.id.fragment_container, WelcomeWifiPasswordFragment.newInstance());
    }

    public void gotoSettingsSecurity() {
        Fragment fragment = SettingsSecurityFragment.getInstance();
        addFragment(R.id.fragment_container, fragment);
    }

    public void gotoSettingsPreference() {
        Fragment fragment = SettingsPreferenceFragment.getInstance();
        addFragment(R.id.fragment_container, fragment);
    }

    public void gotoSettingsLanguage() {
        addFragment(R.id.fragment_container, SettingsLanguageFragment.getInstance());
    }

    public void gotoAdvanced3DP() {
        addFragment(R.id.fragment_container, SettingsAdvanced3DPFragment.getInstance());
    }

    public void goto3DPCalibrationGrid() {
        addFragment(R.id.fragment_container, Settings3DPCalibrationGridFragment.getInstance());
    }

    public void gotoAdvancedLaser() {
        addFragment(R.id.fragment_container, SettingsAdvancedLaserFragment.getInstance());
    }

    public void gotoAdvancedLaser10w() {
        addFragment(
                SettingsAdvancedLaser10WFragment.class.getSimpleName(),
                R.id.fragment_container,
                SettingsAdvancedLaser10WFragment.getInstance());
    }
    public void gotoAdvancedLaser20w() {
        addFragment(
                SettingsAdvancedLaser20WFragment.class.getSimpleName(),
                R.id.fragment_container,
                SettingsAdvancedLaser20WFragment.getInstance());
    }

    public void gotoAdvancedLaser40w() {
        addFragment(
                SettingsAdvancedLaser40WFragment.class.getSimpleName(),
                R.id.fragment_container,
                SettingsAdvancedLaser40WFragment.getInstance());
    }

    public void gotoAdvancedLaser2w() {
        addFragment(SettingsAdvancedLaser2WFragment.class.getSimpleName(),
                R.id.fragment_container,
                SettingsAdvancedLaser2WFragment.getInstance());
    }

    public void gotoLaser10wToolheadFocusCalibration() {
        addFragment(
                SettingsLaser10wToolheadFocusCalibrationFragment.class.getSimpleName(),
                R.id.fragment_container,
                SettingsLaser10wToolheadFocusCalibrationFragment.getInstance());
    }

    public void gotoLaser10wTouchPlatformFragment() {
        addFragment(R.id.fragment_container, SettingsLaser10wTouchPlatformFragment.getInstance());
    }

    public void gotoCameraCalibrationStep1() {
        addFragment(R.id.fragment_container, SettingsCameraCalibrationStep1Fragment.newInstance());
    }

    public void gotoCameraCalibrationStep2() {
        Fragment fragment = SettingsCameraCalibrationStep2Fragment.newInstance();
        addFragment(R.id.fragment_container, fragment);
    }

    public void startLaserFocusModificationFragment() {
        addFragment(R.id.fragment_container, LaserFocusModificationFragment.newInstance());
    }

    public void startFireSensorSensitivityFragment() {
        addFragment(R.id.fragment_container, Laser40wFireSensorSensitivityFragment.newInstance());
    }
    
    public void startCrossLineIndicatorOffsetFragment() {
        addFragment(R.id.fragment_container, Laser40wCrosslineIndicatorOffsetFragment.newInstance());
    }

    public void startAdvanceAirPurifierPage() {
        addFragment(R.id.fragment_container, SettingsAdvanceAirPurifierFragment.getInstance());
    }

    public void goto10wCameraCalibrationIntroFragment() {
        addFragment(R.id.fragment_container, Setting10wCameraCalibrationIntroFragment.newInstance());
    }

    public void goto10wCameraCalibrationStep1() {
        addFragment(R.id.fragment_container, Settings10wCameraCalibrationStep1Fragment.newInstance());
    }

    public void goto10wCameraCalibrationStep2() {
        Fragment fragment = Settings10wCameraCalibrationStep2Fragment.newInstance();
        addFragment(R.id.fragment_container, fragment);
    }

    public void gotoLaserShotOutputPower() {
        Fragment fragment = LaserShotOutputPowerFragment.newInstance();
        addFragment(R.id.fragment_container, fragment);
    }

    public void popSettingsPage() {
        FragmentManager fragmentManager = getSupportFragmentManager();
        final int fragmentCount = fragmentManager.getFragments().size();

        // Pop all fragments except the root one
        for (int i = 1; i < fragmentCount; i++) {
            fragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        }
    }
}
