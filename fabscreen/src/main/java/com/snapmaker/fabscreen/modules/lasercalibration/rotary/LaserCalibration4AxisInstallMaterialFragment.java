package com.snapmaker.fabscreen.modules.lasercalibration.rotary;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.orhanobut.logger.Logger;
import com.snapmaker.fabscreen.R;
import fabscreen.libraries.legacy.base.BaseFragment;
import com.snapmaker.fabscreen.modules.lasercalibration.LaserCalibrationActivity;
import com.snapmaker.fabscreen.modules.lasercalibration.LaserCalibrationViewModel;

import java.util.Locale;

import butterknife.BindView;
import butterknife.OnClick;
import fabscreen.libraries.legacy.data.MockConst;
import fabscreen.libraries.legacy.lib.LogHelper;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.subjects.BehaviorSubject;

public class LaserCalibration4AxisInstallMaterialFragment extends BaseFragment {
    public static LaserCalibration4AxisInstallMaterialFragment newInstance() {
        return new LaserCalibration4AxisInstallMaterialFragment();
    }

    @BindView(R.id.btn_preview_laser_install_material_next)
    Button mBtnNext;

    private LaserCalibrationViewModel mViewModel;
    private BehaviorSubject<Boolean> mIsMovingSubject = BehaviorSubject.createDefault(false);

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mViewModel = getViewModel();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setTitle(R.string.cnc_origin_assistant_install_material_title);

        initView();
    }

    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_preview_laser_prepare_install_material;
    }

    @Override
    protected LaserCalibrationViewModel getViewModel() {
        return getViewModelProvider().get(LaserCalibrationViewModel.class);
    }

    private void initView() {
        // On moving event
        mIsMovingSubject.observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(isMoving -> mBtnNext.setEnabled(!isMoving));
    }

    private void finish() {
        mIsMovingSubject.onNext(false);

        if (getActivity() != null) {
            ((LaserCalibrationActivity) getActivity()).gotoLaser4AxisMeasureHeight();
        }
    }

    @OnClick(R.id.btn_preview_laser_install_material_next)
    void onClickNext() {
        int sizeX = getModel().getMachineController().getSizeX();
        int sizeY = getModel().getMachineController().getSizeY();
        float materialLength = mViewModel.getWorkpieceLength();

        float initialY = Math.min(sizeY - MockConst.ROTARY_MOCK_CHUCK_LENGTH - (2 * 10 + 1), sizeY - MockConst.ROTARY_MOCK_CHUCK_LENGTH - materialLength);

        float z = MockConst.LASER_MOCK_ROTARY_WASTE_BOARD_HEIGHT
                + MockConst.LASER_MOCK_ROTARY_HEIGHT
                + (mViewModel.getWorkpieceDiameter() / 2f)
                + MockConst.LASER_HOOD_HEIGHT
                + 15;

        mIsMovingSubject.onNext(true);
        // Make sure on CS#0 and then go to center for measuring height.
        getModel().getMachineController().updateCoordinateSystem(0)
                .flatMap(res -> getModel().getSlaveComputer().sendGcode(String.format(Locale.US, "G0 X%.2f Y%.2f F3000", sizeX / 2f - 6, initialY)))
                .flatMap(res -> getModel().getSlaveComputer().sendGcode(String.format(Locale.US, "G0 Z%.2f F1800", z)))
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(success -> {
                    mIsMovingSubject.onNext(false);
                    finish();
                }, e -> {
                    mIsMovingSubject.onNext(false);
                    LogHelper.log(e);
                    Logger.e("Failed to move.");
                });
    }
}
