package com.snapmaker.fabscreen.modules.enclosure;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.orhanobut.logger.Logger;
import com.snapmaker.fabscreen.R;
import fabscreen.libraries.legacy.base.BaseFragment;

import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.OnClick;
import fabscreen.libraries.legacy.data.Constants;
import io.reactivex.android.schedulers.AndroidSchedulers;

public class EnclosureSettingsFragment extends BaseFragment {
    public static EnclosureSettingsFragment getInstance() {
        return new EnclosureSettingsFragment();
    }

    @BindView(R.id.view_enclosure_settings_door_detection)
    View mViewDoorDetection;

    @BindView(R.id.btn_enclosure_settings_door_detection)
    Button mBtnDoorDetection;
    @BindView(R.id.btn_enclosure_settings_auto_lighting)
    Button mBtnAutoLighting;

    EnclosureViewModel mViewModel;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mViewModel = getViewModel();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setTitle(R.string.all_enclosure);

        initView();
    }

    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_enclosure_settings;
    }

    @Override
    protected EnclosureViewModel getViewModel() {
        return getViewModelProvider().get(EnclosureViewModel.class);
    }

    private void initView() {
        // show up door detection settings only in laser or cnc module
        final int headType = getModel().getMachineController().getHeadType();
        mViewDoorDetection.setVisibility(headType == Constants.FILE_TYPE_3DP ? Button.GONE : Button.VISIBLE);

        mViewModel.getEnclosureStatusObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(status -> {
                    mBtnDoorDetection.setActivated(status.isEnclosureEnabled());
                });

        boolean isAutoLighting = mViewModel.isEnclosureAutoLighting();
        mBtnAutoLighting.setActivated(isAutoLighting);
    }

    private void showAutoLightingSettingsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext(), R.style.AppTheme_Dialog);
        AlertDialog dialog = builder.create();
        dialog.setCanceledOnTouchOutside(false);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(R.color.default_theme_dark_mask);
            dialog.getWindow().setLayout(300 * 2, WindowManager.LayoutParams.WRAP_CONTENT);
        }

        LayoutInflater inflater = LayoutInflater.from(getContext());
        View view = inflater.inflate(R.layout.dialog_auto_lighting, null);
        dialog.setView(view);
        dialog.show();

        // Auto Dismiss after 3 seconds
        AndroidSchedulers.mainThread().scheduleDirect(dialog::dismiss, 3000, TimeUnit.MILLISECONDS);
    }

    @OnClick(R.id.btn_enclosure_settings_door_detection)
    void onClickDoorDetection() {
        boolean isEnabled = mViewModel.isDoorDetectionEnabled();

        mViewModel.setDoorDetection(!isEnabled);
    }

    @OnClick(R.id.btn_enclosure_settings_auto_lighting)
    void onClickAutoLighting() {
        boolean autoLighting = mViewModel.isEnclosureAutoLighting();

        autoLighting = !autoLighting;
        Logger.d("Set Enclosure Auto Lighting " + autoLighting);

        mViewModel.setEnclosureAutoLighting(autoLighting);
        mBtnAutoLighting.setActivated(autoLighting);

        showAutoLightingSettingsDialog();
    }
}
