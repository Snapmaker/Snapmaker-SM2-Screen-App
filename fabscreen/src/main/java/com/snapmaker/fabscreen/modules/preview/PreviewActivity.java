package com.snapmaker.fabscreen.modules.preview;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.alibaba.android.arouter.facade.annotation.Route;
import com.orhanobut.logger.Logger;
import com.snapmaker.fabscreen.BuildConfig;
import com.snapmaker.fabscreen.R;
import com.snapmaker.fabscreen.modules.preview.platformheight.PreviewLaser2wPullInFocusLeverFragment;
import com.snapmaker.fabscreen.modules.preview.platformheight.PreviewLaser2wTouchPlatformFragment;
import com.snapmaker.fabscreen.modules.preview.platformheight.PreviewLaser40wPullInFocusLeverFragment;
import com.snapmaker.fabscreen.modules.preview.platformheight.PreviewLaser40wTouchPlatformFragment;
import com.snapmaker.fabscreen.modules.preview.preparematerial.PreviewLaser2wPrepareMaterialFragment;
import com.snapmaker.fabscreen.modules.preview.preparematerial.PreviewLaser40wPrepareMaterialFragment;
import com.snapmaker.fabscreen.modules.preview.preparematerial.PreviewLaserPrepareMaterialFragment;
import com.snapmaker.fabscreen.modules.preview.rotarycnc.PreviewCNCPrepareRotaryInstallTailstockFragment;
import com.snapmaker.fabscreen.modules.preview.rotarycnc.PreviewCNCPrepareRotaryOriginRemindFragment;
import com.snapmaker.fabscreen.modules.preview.rotarycnc.PreviewCNCPrepareRotarySetOriginFragment;
import com.snapmaker.fabscreen.modules.preview.rotarylaser.PreviewLaser2wRotaryMeasureHeightFragment;
import com.snapmaker.fabscreen.modules.preview.rotarylaser.PreviewLaser40wRotaryMeasureHeightFragment;
import com.snapmaker.fabscreen.modules.preview.rotarylaser.PreviewLaserRotaryInstallMaterialFragment;
import com.snapmaker.fabscreen.modules.preview.rotarylaser.PreviewLaserRotaryMeasureHeightFragment;
import com.snapmaker.fabscreen.modules.preview.rotarylaser.PreviewLaserRotaryMeasureHeightIntroFragment;
import com.snapmaker.fabscreen.modules.preview.rotarylaser.PreviewLaserRotaryPrepareModeFragment;
import com.snapmaker.fabscreen.modules.preview.rotarylaser.PreviewLaserRotarySetMaterialFragment;
import com.snapmaker.fabscreen.modules.preview.rotarylaser.PreviewLaserRotarySetOriginFragment;
import com.snapmaker.fabscreen.modules.preview.safety.PreviewCNCPrepareSafetyGogglesFragment;
import com.snapmaker.fabscreen.modules.preview.safety.PreviewLaser2wPrepareSafetyGogglesFragment;
import com.snapmaker.fabscreen.modules.preview.safety.PreviewLaser40wPrepareFocusLeverFragment;
import com.snapmaker.fabscreen.modules.preview.safety.PreviewLaser40wPreparePrintSafetyNoticeFragment;
import com.snapmaker.fabscreen.modules.preview.safety.PreviewLaser40wPrepareSafetyGogglesFragment;
import com.snapmaker.fabscreen.modules.preview.safety.PreviewLaserPrepareSafetyGogglesFragment;
import com.snapmaker.fabscreen.modules.preview.setorigin.PreviewLaser2wPrepareSetOriginFragment;
import com.snapmaker.fabscreen.modules.preview.setorigin.PreviewLaser40wPrepareSetOriginFragment;
import com.snapmaker.fabscreen.modules.preview.setorigin.PreviewLaserPrepareSetOriginFragment;

import fabscreen.libraries.legacy.base.BaseActivity;
import fabscreen.libraries.legacy.route.RoutePath;

@Route(path = RoutePath.PREVIEW_ACTIVITY)
public class PreviewActivity extends BaseActivity {

    private Bundle mBundle;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_default);

        gotoPreviewFragment();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (intent.getBooleanExtra("force_refresh", false)) {
            Logger.d("refresh preview.");
            FragmentManager fragmentManager = getSupportFragmentManager();
            final int fragmentCount = fragmentManager.getFragments().size();

            // Pop all fragments except the root one
            for (int i = 1; i < fragmentCount; i++) {
                fragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
            }

            mBundle = intent.getExtras();

            PreviewFragment fragment = new PreviewFragment();
            fragment.setArguments(mBundle);


            replaceFragment(R.id.fragment_container, fragment);
        } else {
            Logger.d("onNewIntent preview.");
        }
    }

    // preview
    public void gotoPreviewFragment() {
        Intent intent = getIntent();
        mBundle = intent.getExtras();

        PreviewFragment fragment = new PreviewFragment();
        fragment.setArguments(mBundle);

        addFragment(R.id.fragment_container, fragment);
    }

    // prepare

    /**
     * Laser prepare, step 0, select mode
     */
    public void gotoLaserPrepareModeFragment() {
        PreviewLaserPrepareModeFragment fragment = new PreviewLaserPrepareModeFragment();
        addFragment(
                PreviewLaserPrepareModeFragment.class.getSimpleName(),
                R.id.fragment_container,
                fragment);
    }

    /**
     * Laser prepare, set material height
     * Auto mode, step 1
     */
    public void gotoLaserPrepareMaterialFragment() {
        addFragment(R.id.fragment_container, PreviewLaserPrepareMaterialFragment.newInstance());
    }

    /**
     * Laser prepare, safety goggles
     * <p>
     * Auto mode, step 2
     * Manual mode, step 1
     */
    public void gotoLaserPrepareSafetyGogglesFragment(boolean autoMode) {
        PreviewLaserPrepareSafetyGogglesFragment fragment = new PreviewLaserPrepareSafetyGogglesFragment();
        Bundle bundle = new Bundle();
        bundle.putBoolean("auto_mode", autoMode);
        fragment.setArguments(bundle);
        addFragment(R.id.fragment_container, fragment);
    }

    /**
     * Laser Prepare, Set Origin
     * <p>
     * Auto mode, step 3
     * Manual mode, step 2
     * <p>
     * also used in CNC prepare
     *
     * @param autoMode boolean
     */
    public void gotoLaserPrepareSetOriginFragment(boolean autoMode) {
        PreviewLaserPrepareSetOriginFragment fragment = new PreviewLaserPrepareSetOriginFragment();
        Bundle bundle;
        if (BuildConfig.DEBUG) {
            bundle = new Bundle();
        } else {
            bundle = (Bundle) mBundle.clone();// so tricky!
        }
        bundle.putBoolean("auto_mode", autoMode);
        fragment.setArguments(bundle);
        addFragment(R.id.fragment_container, fragment);
    }

    public void gotoLaserPrepareModeNoteFragment(boolean autoMode) {
        PreviewLaserPrepareModeNoteFragment fragment = new PreviewLaserPrepareModeNoteFragment();
        Bundle bundle;

        if (BuildConfig.DEBUG) {
            bundle = new Bundle();
        } else {
            bundle = (Bundle) mBundle.clone();
        }

        bundle.putBoolean("auto_mode", autoMode);
        fragment.setArguments(bundle);
        addFragment(
                PreviewLaserPrepareModeNoteFragment.class.getSimpleName(),
                R.id.fragment_container,
                fragment);
    }

    public void gotoLaserPrepareMeasureThicknessFragment() {
        addFragment(
                PreviewLaserMeasureThicknessFragment.class.getSimpleName(),
                R.id.fragment_container,
                new PreviewLaserMeasureThicknessFragment());
    }

    public void gotoLaserPrepareMeasureSucceedFragment() {
        addFragment(R.id.fragment_container, new PreviewLaserMeasureSucceedFragment());
    }

    public void gotoLaserPrepareTouchMaterialFragment() {
        addFragment(R.id.fragment_container, new PreviewLaserPrepareTouchMaterialFragment());
    }

    // Laser with Rotary
    public void gotoLaserRotarySetupModeFragment() {
        PreviewLaserRotaryPrepareModeFragment fragment = new PreviewLaserRotaryPrepareModeFragment();
        addFragment(R.id.fragment_container, fragment);
    }

    public void gotoLaserRotarySetOriginFragment(boolean autoMode) {
        PreviewLaserRotarySetOriginFragment fragment = new PreviewLaserRotarySetOriginFragment();
        Bundle bundle;
        if (mBundle != null) {
            bundle = (Bundle) mBundle.clone();
        } else {
            Intent intent = getIntent();
            bundle = intent.getExtras();
        }
        bundle.putBoolean("auto_mode", autoMode);
        fragment.setArguments(bundle);
        addFragment(R.id.fragment_container, fragment);
    }

    public void gotoLaserRotarySetMaterial() {
        PreviewLaserRotarySetMaterialFragment fragment = new PreviewLaserRotarySetMaterialFragment();
        addFragment(R.id.fragment_container, fragment);
    }

    public void gotoLaserRotaryInstallMaterial() {
        PreviewLaserRotaryInstallMaterialFragment fragment = new PreviewLaserRotaryInstallMaterialFragment();
        addFragment(R.id.fragment_container, fragment);
    }

    public void gotoLaserRotaryMeasureHeightIntro() {
        PreviewLaserRotaryMeasureHeightIntroFragment fragment = new PreviewLaserRotaryMeasureHeightIntroFragment();
        addFragment(R.id.fragment_container, fragment);
    }

    public void gotoLaserRotaryMeasureHeight() {
        PreviewLaserRotaryMeasureHeightFragment fragment = new PreviewLaserRotaryMeasureHeightFragment();
        addFragment(R.id.fragment_container, fragment);
    }

    public void gotoLaser40wRotaryMeasureHeight() {
        PreviewLaser40wRotaryMeasureHeightFragment fragment = new PreviewLaser40wRotaryMeasureHeightFragment();
        addFragment(R.id.fragment_container, fragment);
    }

    public void gotoLaser40wPrepareModeFragment() {
        PreviewLaser40wPrepareModeFragment fragment = new PreviewLaser40wPrepareModeFragment();
        addFragment(
                PreviewLaser40wPrepareModeFragment.class.getSimpleName(),
                R.id.fragment_container,
                fragment);
    }

    public void gotoLaser40wPrepareMaterialFragment() {
        PreviewLaser40wPrepareMaterialFragment fragment = new PreviewLaser40wPrepareMaterialFragment();
        addFragment(
                PreviewLaser40wPrepareMaterialFragment.class.getSimpleName(),
                R.id.fragment_container,
                fragment);
    }

    public void gotoLaser40wPrepareSafetyGogglesFragment(boolean autoMode) {
        PreviewLaser40wPrepareSafetyGogglesFragment fragment = new PreviewLaser40wPrepareSafetyGogglesFragment();
        Bundle bundle = new Bundle();
        bundle.putBoolean("auto_mode", autoMode);
        fragment.setArguments(bundle);
        addFragment(R.id.fragment_container, fragment);
    }

    public void gotoLaser40wPrepareSetOriginFragment(boolean autoMode) {
        PreviewLaser40wPrepareSetOriginFragment fragment = new PreviewLaser40wPrepareSetOriginFragment();
        Bundle bundle;
        if (BuildConfig.DEBUG) {
            bundle = new Bundle();
        } else {
            bundle = (Bundle) mBundle.clone();// so tricky!
        }
        bundle.putBoolean("auto_mode", autoMode);
        fragment.setArguments(bundle);
        addFragment(R.id.fragment_container, fragment);
    }

    public void gotoLaser40wPreparePrintSafetyNoticeFragment(int mode) {
        PreviewLaser40wPreparePrintSafetyNoticeFragment fragment = new PreviewLaser40wPreparePrintSafetyNoticeFragment();
        Bundle bundle = new Bundle();
        bundle.putInt("indicator_mode", mode);
        fragment.setArguments(bundle);
        addFragment(R.id.fragment_container, fragment);
    }

    public void gotoLaser40wFocusLeverFragment() {
        PreviewLaser40wPrepareFocusLeverFragment fragment = new PreviewLaser40wPrepareFocusLeverFragment();
        Bundle bundle;
        if (BuildConfig.DEBUG) {
            bundle = new Bundle();
        } else {
            bundle = (Bundle) mBundle.clone();// so tricky!
        }
        bundle.putBoolean("auto_mode", false);
        fragment.setArguments(bundle);
        addFragment(R.id.fragment_container, fragment);
    }

    public void gotoLaser40wTouchPlatformFragment() {
        PreviewLaser40wTouchPlatformFragment fragment = new PreviewLaser40wTouchPlatformFragment();
        addFragment(R.id.fragment_container, fragment);
    }

    public void gotoLaser40wPullInFocusLeverFragment() {
        PreviewLaser40wPullInFocusLeverFragment fragment = new PreviewLaser40wPullInFocusLeverFragment();
        Bundle bundle;
        if (BuildConfig.DEBUG) {
            bundle = new Bundle();
        } else {
            bundle = (Bundle) mBundle.clone();// so tricky!
        }
        bundle.putBoolean("auto_mode", false);
        fragment.setArguments(bundle);
        addFragment(R.id.fragment_container, fragment);
    }

    public void gotoLaser40wSettingsIndicatorModeFragment() {
        PreviewLaser40wSettingsIndicatorModeFragment fragment = new PreviewLaser40wSettingsIndicatorModeFragment();
        addFragment(R.id.fragment_container, fragment);
    }

    // 2W IR Laser
    public void gotoLaser2wPrepareModeFragment() {
        PreviewLaser2wPrepareModeFragment fragment = new PreviewLaser2wPrepareModeFragment();
        addFragment(R.id.fragment_container, fragment);
    }

    public void gotoLaser2wPrepareMaterialFragment() {
        PreviewLaser2wPrepareMaterialFragment fragment = PreviewLaser2wPrepareMaterialFragment.newInstance();
        addFragment(R.id.fragment_container, fragment);
    }

    public void gotoLaser2wPrepareSafetyGogglesFragment(boolean autoMode) {
        PreviewLaser2wPrepareSafetyGogglesFragment fragment = new PreviewLaser2wPrepareSafetyGogglesFragment();
        Bundle bundle = new Bundle();
        bundle.putBoolean("auto_mode", autoMode);
        fragment.setArguments(bundle);
        addFragment(R.id.fragment_container, fragment);
    }

    public void gotoLaser2wPrepareSetOriginFragment(boolean autoMode) {
        PreviewLaser2wPrepareSetOriginFragment fragment = new PreviewLaser2wPrepareSetOriginFragment();
        Bundle bundle;
        if (BuildConfig.DEBUG) {
            bundle = new Bundle();
        } else {
            bundle = (Bundle) mBundle.clone();// so tricky!
        }
        bundle.putBoolean("auto_mode", autoMode);
        fragment.setArguments(bundle);
        addFragment(R.id.fragment_container, fragment);
    }

    public void gotoLaser2wTouchPlatformFragment() {
        PreviewLaser2wTouchPlatformFragment fragment = new PreviewLaser2wTouchPlatformFragment();
        addFragment(R.id.fragment_container, fragment);
    }

    public void gotoLaser2wPullInFocusLeverFragment() {
        PreviewLaser2wPullInFocusLeverFragment fragment = PreviewLaser2wPullInFocusLeverFragment.newInstance();
        Bundle bundle;
        if (BuildConfig.DEBUG) {
            bundle = new Bundle();
        } else {
            bundle = (Bundle) mBundle.clone();// so tricky!
        }
        bundle.putBoolean("auto_mode", false);
        fragment.setArguments(bundle);
        addFragment(R.id.fragment_container, fragment);
    }

    public void gotoLaser2wRotaryMeasureHeight() {
        PreviewLaser2wRotaryMeasureHeightFragment fragment = new PreviewLaser2wRotaryMeasureHeightFragment();
        addFragment(R.id.fragment_container, fragment);
    }

    /**
     * CNC Safety Goggles
     * step1
     */
    public void gotoCNCPrepareSafetyGogglesFragment() {
        addFragment(R.id.fragment_container, PreviewCNCPrepareSafetyGogglesFragment.newInstance());
    }

    // CNC with Rotary

    /**
     * CNC Rotary Origin Remind
     * step1
     */
    public void gotoCNCPrepareRotaryOriginRemindFragment() {
        PreviewCNCPrepareRotaryOriginRemindFragment fragment = new PreviewCNCPrepareRotaryOriginRemindFragment();
        addFragment(R.id.fragment_container, fragment);
    }

    /**
     * CNC Rotary Set Origin
     * step3
     */
    public void gotoCNCPrepareRotarySetOriginFragment() {
        PreviewCNCPrepareRotarySetOriginFragment fragment = new PreviewCNCPrepareRotarySetOriginFragment();
        addFragment(R.id.fragment_container, fragment);
    }

    /**
     * CNC Rotary Install Tailstock
     * step4
     */
    public void gotoCNCRotaryInstallTailstockFragment() {
        PreviewCNCPrepareRotaryInstallTailstockFragment fragment = new PreviewCNCPrepareRotaryInstallTailstockFragment();
        Bundle bundle = (Bundle) mBundle.clone();
        fragment.setArguments(bundle);
        addFragment(R.id.fragment_container, fragment);
    }
}
