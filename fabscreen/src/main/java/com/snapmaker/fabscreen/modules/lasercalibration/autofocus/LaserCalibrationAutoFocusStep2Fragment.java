package com.snapmaker.fabscreen.modules.lasercalibration.autofocus;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.orhanobut.logger.Logger;
import com.snapmaker.fabscreen.R;
import com.snapmaker.fabscreen.modules.lasercalibration.LaserCalibrationActivity;

import java.io.FileOutputStream;

import butterknife.BindView;
import butterknife.OnClick;
import fabscreen.libraries.legacy.base.BaseFragment;
import fabscreen.libraries.legacy.base.BaseViewModel;
import fabscreen.libraries.legacy.data.Constants;
import fabscreen.libraries.legacy.data.ISlaveComputer;
import fabscreen.libraries.legacy.data.imgprocess.LaserCalibrationProcess;
import fabscreen.libraries.legacy.lib.LogHelper;
import fabscreen.libraries.legacy.view.FabAlert;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.BehaviorSubject;

public class LaserCalibrationAutoFocusStep2Fragment extends BaseFragment {
    private final static int STATUS_IDLE = 0;
    private final static int STATUS_LASER_TEST = 1;
    private final static int STATUS_PROCESSING = 2;
    private final static int STATUS_COMPLETE = 3;
    private final static int STATUS_ERROR = 4;

    public static LaserCalibrationAutoFocusStep2Fragment newInstance() {
        return new LaserCalibrationAutoFocusStep2Fragment();
    }

    @BindView(R.id.top_bar_back)
    Button mBtnBack;
    @BindView(R.id.tv_guide_content)
    TextView mTvContent;
    @BindView(R.id.btn_guide_next)
    Button mBtnComplete;

    private BehaviorSubject<Integer> mCalibrationStatusSubject = BehaviorSubject.createDefault(STATUS_IDLE);
    private BehaviorSubject<Integer> mCalibrationIndexSubject = BehaviorSubject.createDefault(-1);
    private float mInitialZ;
    private float mFocalLength = 0;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initView();
    }

    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_guide_laser_auto_focus_step2;
    }

    @Override
    protected BaseViewModel getViewModel() {
        return null;
    }

    private void initView() {
        mCalibrationStatusSubject
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(status -> {
                    switch (status) {
                        case STATUS_IDLE:
                        case STATUS_LASER_TEST:
                        case STATUS_PROCESSING:
                            mBtnBack.setVisibility(View.GONE);
                            mBtnComplete.setVisibility(View.VISIBLE);
                            mBtnComplete.setEnabled(false);
                            mBtnComplete.setText(R.string.laser_calibration_processing);
                            break;
                        case STATUS_COMPLETE:
                            mBtnBack.setVisibility(View.VISIBLE);
                            mBtnComplete.setVisibility(Button.VISIBLE);
                            mBtnComplete.setEnabled(true);
                            mBtnComplete.setText(R.string.all_complete);
                            break;
                        case STATUS_ERROR:
                            mBtnBack.setVisibility(View.VISIBLE);
                            mBtnComplete.setVisibility(Button.VISIBLE);
                            mBtnComplete.setEnabled(true);
                            mBtnComplete.setText(R.string.laser_calibration_failed);
                            mTvContent.setText(R.string.laser_calibration_auto_focus_failed);
                            break;
                    }
                });

        mCalibrationIndexSubject
                .filter(index -> index != -1)
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(index -> {
                    if (index < 5) {
                        mTvContent.setText(R.string.laser_calibration_auto_focus_complete_1);
                    } else if (index <= 15) {
                        mTvContent.setText(R.string.laser_calibration_auto_focus_complete);
                    } else {
                        mTvContent.setText(R.string.laser_calibration_auto_focus_complete_2);
                    }
                });

        start();
    }

    private void start() {
        // Assume we are at CS#1
        // Save initial z coordinate
        getModel().getSlaveComputer().requestMachineStatus()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(machineStatus -> {
                    final float offsetZ = getModel().getMachineController().getCoordinateOffsetZ();
                    Logger.d("z = " + (float) machineStatus.z);
                    mInitialZ = (float) machineStatus.z - offsetZ; // calculate absolute Z
                    Logger.d("initial z = " + mInitialZ);
                    startCalibration();
                }, e -> {
                    Logger.e("Unable to get machine status.");
                    mCalibrationStatusSubject.onNext(STATUS_ERROR);
                });
    }

    /**
     * Start auto calibration:
     * 1. Call slave computer to engrave test pattern
     * 2. Move tool head to take photo, process the photo to determine which line engraved thinnest.
     */
    private void startCalibration() {
        mCalibrationStatusSubject.onNext(STATUS_LASER_TEST);
        mCalibrationIndexSubject.onNext(-1);
        getModel().getSlaveComputer().startLaserFineTune(Constants.LASER_TEST_PATTERN_Z_DIFF)
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(success -> {
                    if (success) {
                        mCalibrationStatusSubject.onNext(STATUS_PROCESSING);
                        cameraAidFocalDetection();
                    } else {
                        mCalibrationStatusSubject.onNext(STATUS_ERROR);
                    }
                }, e -> {
                    LogHelper.log(e);
                    mCalibrationStatusSubject.onNext(STATUS_ERROR);
                });
    }

    private void turnOffLight() {
        boolean cameraLightOn = getModel().getPreferences().getLaserCameraLightOn();
        if (cameraLightOn) {
            getModel().getLaserCameraController().setCameraLighting(false)
                    .observeOn(AndroidSchedulers.mainThread())
                    .as(bindToLifecycle())
                    .subscribe(success -> {
                        // do nothing
                    }, LogHelper::log);
        }
    }

    /**
     * Camera Aid Focal Detection.
     */
    private void cameraAidFocalDetection() {
        final float bottomZ = getModel().getPreferences().getLaserBottomZ();
        float cameraZ = bottomZ + 80.f;

        getModel().getSlaveComputer().gotoRelativePosition(-Constants.LASER_CAMERA_OFFSET_X, -Constants.LASER_CAMERA_OFFSET_Y, cameraZ - mInitialZ, 0)
                .flatMap(success -> {
                    Log.e("DEBUG", "move result:" + success);
                    return getModel().getLaserCameraController().requestCapturePhoto();
                })
                .flatMap(success -> {
                    Log.e("DEBUG", "capture result:" + success);
                    return getModel().getLaserCameraController().watchPhotoReceive();
                })
                .subscribeOn(Schedulers.computation())
                .map(bitmap -> {
                    Logger.d("Capture image succeed.");
                    String path = getModel().getCacheDir() + "/capture.jpg";

                    FileOutputStream out = new FileOutputStream(path);
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);

                    turnOffLight();
                    return LaserCalibrationProcess.process(getContext(), bitmap);
                })
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(index -> {
                    if (index == -1) {
                        mCalibrationStatusSubject.onNext(STATUS_ERROR);
                        Logger.w("Detection failed");
                    } else {
                        float thickness = getModel().getPreferences().getLaserMaterialThickness();
                        float offset = (-10 * Constants.LASER_TEST_PATTERN_Z_DIFF + index * Constants.LASER_TEST_PATTERN_Z_DIFF);
                        mFocalLength = mInitialZ + offset - thickness;
                        Logger.d("mFocalLength is " + mFocalLength);

                        mCalibrationIndexSubject.onNext(index);

                        // Move to new height (focal length + material height)
                        getModel().getSlaveComputer().sendGcode("G0 X0 Y0 F3000")
                                .flatMap(res -> getModel().getSlaveComputer().sendGcode("G0 Z" + offset + " F1800"))
                                .flatMap(res -> getModel().getSlaveComputer().setPosition(0, 0, 0, ISlaveComputer.FLAG_Z))
                                .observeOn(AndroidSchedulers.mainThread())
                                .as(bindToLifecycle())
                                .subscribe(response -> mCalibrationStatusSubject.onNext(STATUS_COMPLETE), LogHelper::log);
                    }
                }, e -> {
                    LogHelper.log(e);
                    Logger.w("Capture image failed.");
                    mCalibrationStatusSubject.onNext(STATUS_ERROR);
                });
    }

    private void finish() {
        Logger.d("Laser Calibration finished.");
        if (getActivity() != null) {
            getActivity().finish();
        }
    }

    @OnClick(R.id.btn_guide_next)
    void onClickNext() {
        if (mCalibrationStatusSubject.getValue() == STATUS_COMPLETE) {
            getModel().getMachineController()
                    .setLaserFocus(mFocalLength)
                    .observeOn(AndroidSchedulers.mainThread())
                    .as(bindToLifecycle())
                    .subscribe(success -> {
                        if (success) {
                            finish();
                        } else {
                            FabAlert.alert(getContext(), R.string.laser_alert_failed_to_get_focal_length);
                            finish();
                        }
                    }, e -> {
                        LogHelper.log(e);
                        FabAlert.alert(getContext(), R.string.laser_alert_failed_to_get_focal_length);
                        finish();
                    });
        } else {
            // Goto manual pick when failed
            if (getActivity() != null) {
                ((LaserCalibrationActivity) getActivity()).gotoLaserCalibrationManualFineTunePick();
            }
        }
    }
}
