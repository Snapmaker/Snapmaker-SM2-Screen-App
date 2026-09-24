package com.snapmaker.fabscreen.modules.preview.rotarylaser;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.snapmaker.fabscreen.R;
import com.snapmaker.fabscreen.modules.lasercalibration.LaserCalibrationViewModel;
import com.snapmaker.fabscreen.modules.preview.PreviewActivity;
import com.snapmaker.fabscreen.view.FabProgressDialog;

import java.util.Locale;

import butterknife.BindView;
import butterknife.OnClick;
import fabscreen.libraries.legacy.base.BaseFragment;
import fabscreen.libraries.legacy.data.Constants;
import fabscreen.libraries.legacy.data.MockConst;
import fabscreen.libraries.legacy.lib.LogHelper;
import io.reactivex.android.schedulers.AndroidSchedulers;

public class PreviewLaserRotaryMeasureHeightIntroFragment extends BaseFragment {
    public static PreviewLaserRotaryMeasureHeightIntroFragment newInstance() {
        return new PreviewLaserRotaryMeasureHeightIntroFragment();
    }

    @BindView(R.id.btn_preview_laser_4axis_measure_height_intro_next)
    Button mBtnNext;
    @BindView(R.id.iv_prepare_install_material)
    ImageView mIvCover;

    private FabProgressDialog mProgressDialog;
    private LaserCalibrationViewModel mLaserCalibrationViewModel;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mLaserCalibrationViewModel = getViewModel();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setTitle(R.string.preview_laser_prepare_4axis_measure_height);

        // init dialog.
        mProgressDialog = new FabProgressDialog(requireContext());
        mProgressDialog.setMessage(R.string.dialog_warning_moving);

        if (getModel().getMachineController().getHeadType() == Constants.HEAD_LASER_10W) {
            mIvCover.setImageResource(R.drawable.pic_10w_laser_measure_height_360x240);
        }
        // 20w / 40w laser pic

    }

    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_preview_laser_prepare_4axis_measure_height_intro;
    }

    @Override
    protected LaserCalibrationViewModel getViewModel() {
        // Be aware we are using LaserCalibrationViewModel to get material diameter and length here.
        return getViewModelProvider().get(LaserCalibrationViewModel.class);
    }

    @OnClick(R.id.btn_preview_laser_4axis_measure_height_intro_next)
    void onClickNext() {
        showDialog(mProgressDialog);
        mBtnNext.setEnabled(false);

        int headType = getModel().getMachineController().getHeadType();

        // Reuse laser rotary measure height moving logic. (where implemented in LaserCalibration4AxisInstallMaterialFragment)
        int sizeX = getModel().getMachineController().getSizeX();
        int sizeY = getModel().getMachineController().getSizeY();
        float materialLength = mLaserCalibrationViewModel.getWorkpieceLength();

        float initialY = Math.min(sizeY - MockConst.ROTARY_MOCK_CHUCK_LENGTH - (2 * 10 + 1), sizeY - MockConst.ROTARY_MOCK_CHUCK_LENGTH - materialLength);

        int mockToolHeadHeight = MockConst.LASER_HOOD_HEIGHT;
        switch (headType) {
            case Constants.HEAD_LASER:
            case Constants.HEAD_LASER_10W:
                mockToolHeadHeight = MockConst.LASER_HOOD_HEIGHT;
                break;
            case Constants.HEAD_LASER_20W:
            case Constants.HEAD_LASER_40W:
                // TODO: To be confirmed.
                mockToolHeadHeight = MockConst.LASER_HOOD_HEIGHT + 10;
                break;
        }

        float z = MockConst.LASER_MOCK_ROTARY_WASTE_BOARD_HEIGHT
                + MockConst.LASER_MOCK_ROTARY_HEIGHT
                + (mLaserCalibrationViewModel.getWorkpieceDiameter() / 2f)
                + mockToolHeadHeight
                + 15;

        // Make sure on CS#0 and then go to center for measuring height.
        getModel().getMachineController().updateCoordinateSystem(0)
                .flatMap(res -> getModel().getSlaveComputer().sendGcode(String.format(Locale.US, "G0 X%.2f Y%.2f F3000", sizeX / 2f - 6, initialY)))
                .flatMap(res -> getModel().getSlaveComputer().sendGcode(String.format(Locale.US, "G0 Z%.2f F1800", z)))
                .flatMap(ret -> getModel().getMachineController().updateCoordinateSystem(1))
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(success -> {
                    dismissDialog(mProgressDialog);
                    mBtnNext.setEnabled(true);
                    if (getActivity() != null) {
                        ((PreviewActivity) getActivity()).gotoLaserRotaryMeasureHeight();
                    }
                }, e -> {
                    dismissDialog(mProgressDialog);
                    mBtnNext.setEnabled(true);
                    LogHelper.log(e);
                });
    }
}
