package com.snapmaker.fabscreen.modules.dualextrudercalibration.sensor;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.orhanobut.logger.Logger;
import com.snapmaker.fabscreen.R;

import butterknife.BindView;
import butterknife.OnClick;
import co.ceryle.segmentedbutton.SegmentedButtonGroup;
import fabscreen.libraries.legacy.base.BaseFragment;
import fabscreen.libraries.legacy.lib.LogHelper;
import fabscreen.libraries.legacy.view.FabConfirm;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.subjects.BehaviorSubject;

public class DualExtruderSensorCalibrationSensorLeftFineTuneFragment extends BaseFragment {
    @BindView(R.id.top_bar_back)
    Button mBtnBack;
    @BindView(R.id.btn_calibration_dual_extruder_fine_tune_save)
    Button mBtnSave;
    @BindView(R.id.btn_calibration_move_up)
    Button mBtnMoveUp;
    @BindView(R.id.btn_calibration_move_down)
    Button mBtnMoveDown;
    @BindView(R.id.btn_calibration_next_point)
    Button mBtnNext;
    @BindView(R.id.sbg_calibration_steps)
    SegmentedButtonGroup mSbgSteps;

    private DualExtruderSensorCalibrationViewModel mActivityViewModel;
    private DualExtruderSensorCalibrationFineTuneViewModel mFragmentViewModel;
    private BehaviorSubject<Boolean> mIsValueModifiedSubject = BehaviorSubject.createDefault(false);

    public static Fragment newInstance() {
        return new DualExtruderSensorCalibrationSensorLeftFineTuneFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mActivityViewModel = getActivityScopeViewModel(DualExtruderSensorCalibrationViewModel.class);
        mFragmentViewModel = getFragmentScopeViewModel(DualExtruderSensorCalibrationFineTuneViewModel.class);
    }

    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_dual_extruder_sensor_fine_tune;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initView();
    }

    private void initView() {
        setTitle(R.string.calibration_dual_extruder_sensor_calibration_left_probe_title);
        mBtnNext.setVisibility(Button.GONE);
        mBtnSave.setEnabled(false);
        Observable.combineLatest(mIsValueModifiedSubject, mFragmentViewModel.getIsMovingObservable(), (isModified, isMoving) -> isModified && !isMoving)
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(ready -> {
                    mBtnSave.setEnabled(ready);
                });

        mFragmentViewModel.getIsMovingObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(isMoving -> {
                    mBtnSave.setEnabled(!isMoving);
                    mBtnMoveUp.setEnabled(!isMoving);
                    mBtnMoveDown.setEnabled(!isMoving);
                });

        mSbgSteps.setOnClickedButtonPosition(position -> {
            switch (position) {
                case 0:
                    mFragmentViewModel.setCurrentStep(0.05f);
                    break;
                case 1:
                    mFragmentViewModel.setCurrentStep(0.1f);
                    break;
                case 2:
                    mFragmentViewModel.setCurrentStep(0.5f);
                    break;
            }
        });

        mSbgSteps.setPosition(1, false);
        mFragmentViewModel.setCurrentStep(0.1f);
    }

    public void enableButtons(boolean enabled) {
        mBtnSave.setEnabled(enabled);
        mBtnMoveUp.setEnabled(enabled);
        mBtnMoveDown.setEnabled(enabled);
        mBtnBack.setEnabled(enabled);
    }

    @Override
    protected void back() {
        FabConfirm.create(requireContext())
                .setIcon(R.drawable.pic_dialog_warning_72x72)
                .setDescription(R.string.calibration_dual_extruder_sensor_calibration_dialog_exit_calibration_desc)
                .setConfirm(R.string.all_confirm, (dialog, which) -> {
                    dialog.dismiss();
                    enableButtons(false);
                    mActivityViewModel.exitSensorCalibration()
                            .observeOn(AndroidSchedulers.mainThread())
                            .as(bindToLifecycle())
                            .subscribe(success -> {
                                if (success) {
                                    Logger.d("Abort sensor calibration.");
                                }
                                requireActivity().finish();
                            });
                }).show();
    }

    @OnClick(R.id.btn_calibration_move_up)
    void onClickMoveUp() {
        mFragmentViewModel.requestExtruderMove(0, mFragmentViewModel.getCurrentStep());
    }

    @OnClick(R.id.btn_calibration_move_down)
    void onClickMoveDown() {
        mFragmentViewModel.requestExtruderMove(1, mFragmentViewModel.getCurrentStep());
    }

    @OnClick(R.id.btn_calibration_dual_extruder_fine_tune_save)
    void onClickSave() {
        enableButtons(false);
        mActivityViewModel.saveSensorFineTuneResult(0)
                .concatMap(isSuccess -> isSuccess ? mActivityViewModel.requestGoHome() : Observable.just(false))
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(success -> {
                    enableButtons(true);
                    if (success) {
                        // Route to sensor calibration complete.
                        ((DualExtruderSensorCalibrationActivity) requireActivity()).gotoSensorCalibrationComplete();
                    } else {
                        // Failed.
                    }
                }, e -> {
                    enableButtons(true);
                });
    }
}
