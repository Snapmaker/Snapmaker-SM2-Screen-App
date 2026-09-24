package com.snapmaker.fabscreen.modules.guidelaser.measureheight;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.orhanobut.logger.Logger;
import com.snapmaker.fabscreen.R;
import fabscreen.libraries.legacy.base.BaseFragment;
import fabscreen.libraries.legacy.base.BaseViewModel;
import com.snapmaker.fabscreen.modules.guidelaser.GuideLaserActivity;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.OnClick;
import fabscreen.libraries.legacy.data.MockConst;
import fabscreen.libraries.legacy.lib.LogHelper;
import io.reactivex.android.schedulers.AndroidSchedulers;

public class GuideLaserMeasureHeightIntroFragment extends BaseFragment {
    public static GuideLaserMeasureHeightIntroFragment newInstance() {
        return new GuideLaserMeasureHeightIntroFragment();
    }

    @BindView(R.id.iv_guide_intro_cover)
    ImageView mIvCover;
    @BindView(R.id.tv_guide_intro_title)
    TextView mTvTitle;
    @BindView(R.id.tv_guide_intro_content)
    TextView mTvContent;
    @BindView(R.id.btn_guide_intro_next)
    Button mBtnNext;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initView();
    }

    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_guide_intro;
    }

    @Override
    protected BaseViewModel getViewModel() {
        return null;
    }

    private void initView() {
        mIvCover.setImageResource(R.drawable.pic_guide_laser_measure_height_360x320);

        mTvTitle.setText(R.string.guide_laser_measure_height_title);
        mTvContent.setText(R.string.guide_laser_measure_height_content);
    }

    private void ensureHomed() {
        getModel().getMachineController().updateCoordinateSystem(0)
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(coordinateSystem -> {
                    boolean homed = coordinateSystem.homed;
                    if (homed) {
                        startMovements();
                    } else {
                        getModel().getSlaveComputer().sendGcode("G28")
                                .observeOn(AndroidSchedulers.mainThread())
                                .as(bindToLifecycle())
                                .subscribe(response -> checkHome());
                    }
                }, LogHelper::log);
    }

    private void checkHome() {
        getModel().getMachineController().updateCoordinateSystem()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(coordinateSystem -> {
                    boolean homed = coordinateSystem.homed;
                    if (homed) {
                        startMovements();
                    } else {
                        AndroidSchedulers.mainThread().scheduleDirect(this::checkHome, 2000, TimeUnit.MILLISECONDS);
                    }
                }, LogHelper::log);
    }

    private void startMovements() {
        // Now offset x, y, z = 0 after homing.
        int sizeX = getModel().getMachineController().getSizeX();
        int sizeY = getModel().getMachineController().getSizeY();
        final float thickness = getModel().getPreferences().getLaserMaterialThickness();

        // Go to center of plat for measuring height.
        float z = MockConst.LASER_PLATE_SAFETY_HEIGHT + thickness + MockConst.LASER_HOOD_HEIGHT + 10;
        getModel().getSlaveComputer().sendGcode(String.format(Locale.US, "G0 X%.2f Y%.2f F3000", sizeX / 2f, sizeY / 2f))
                .flatMap(res -> getModel().getSlaveComputer().sendGcode(String.format(Locale.US, "G0 Z%.2f F1800", z)))
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(success -> {
                    // done
                    finish();
                }, e -> {
                    LogHelper.log(e);
                    Logger.e("Failed to move.");
                });
    }

    private void finish() {
        mBtnNext.setEnabled(true);
        if (getActivity() == null) return;
        ((GuideLaserActivity) getActivity()).startMeasureHeightFragment();
    }

    @OnClick(R.id.btn_guide_intro_next)
    void onClickNext() {
        mBtnNext.setEnabled(false);
        ensureHomed();
    }
}
