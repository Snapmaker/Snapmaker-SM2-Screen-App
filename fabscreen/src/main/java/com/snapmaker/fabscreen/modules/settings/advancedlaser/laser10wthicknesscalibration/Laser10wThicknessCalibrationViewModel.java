package com.snapmaker.fabscreen.modules.settings.advancedlaser.laser10wthicknesscalibration;

import android.graphics.Bitmap;

import androidx.annotation.IntDef;

import com.orhanobut.logger.Logger;

import java.io.FileOutputStream;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import fabscreen.libraries.legacy.base.BaseViewModel;
import fabscreen.libraries.legacy.data.Constants;
import fabscreen.libraries.legacy.data.MockConst;
import fabscreen.libraries.legacy.data.imgprocess.LaserDistanceMeasureProcess;
import fabscreen.libraries.legacy.lib.LogHelper;
import io.reactivex.Observable;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;

public class Laser10wThicknessCalibrationViewModel extends BaseViewModel {

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({FIRST_CAPTURE, SECOND_CAPTURE, MEASURE_CAPTURE})
    public @interface CaptureCount {
    }

    public static final int FIRST_CAPTURE = 1;
    public static final int SECOND_CAPTURE = 2;
    public static final int MEASURE_CAPTURE = 3;

    final private static float CAMERA_HEIGHT_OFFSET = 3.9f;
    private float mH1ZPosition = 170;
    private float mH2ZPosition = 150;
    private final float mH1;
    private final float mH2;
    private float mS1plus;
    private float mS2plus;
    private float mSxplus;
    private final float mPermissibleError = 0.2f;
    boolean mIsA150Model;

    private final String[] mPhotoPaths = {
            getModel().getCacheDir() + "/h1.png",
            getModel().getCacheDir() + "/h2.png",
            getModel().getCacheDir() + "/distance.png"
    };

    private final Subject<CalibrationCaptureResult> mCaptureResultSubject = PublishSubject.create();

    public Laser10wThicknessCalibrationViewModel() {
        super();

        mIsA150Model = getModel().getMachineController().getMachineModel() == Constants.MACHINE_MODEL_SNAPMAKER_A150;

        mH1ZPosition = mIsA150Model ? 150 : mH1ZPosition;
        mH2ZPosition = mIsA150Model ? 130 : mH2ZPosition;

        mH1 = mH1ZPosition + CAMERA_HEIGHT_OFFSET;
        mH2 = mH2ZPosition + CAMERA_HEIGHT_OFFSET;
    }

    public Observable<Boolean> initCameraPosition(@CaptureCount int which) {
        float initX = getModel().getMachineController().getSizeX() * 0.5f - Constants.LASER_CAMERA_OFFSET_X + Constants.LASER_MEASURE_OFFSET_X;
        float initY = getModel().getMachineController().getSizeY() * 0.5f - Constants.LASER_CAMERA_OFFSET_Y;
        float initZ = 0f;
        switch (which) {
            case FIRST_CAPTURE:
                initZ = mH1ZPosition;
                break;
            case SECOND_CAPTURE:
                initZ = mH2ZPosition;
                break;
            case MEASURE_CAPTURE:
                initZ = mIsA150Model ? 150f : 170f; //Measure height.
                break;
        }
        float finalInitZ = initZ;
        return getModel().getMachineController().updateCoordinateSystem(0)
                .flatMap(result -> {
                    Logger.d("gotoAbsolutePosition: x:%s,y:%s,z:%s", initX, initY, finalInitZ);
                    return result.coordinateID == 0 ? getModel().getSlaveComputer().gotoAbsolutePosition(initX, initY, finalInitZ) : Observable.just((false));
                })
                .flatMap(success -> success ?
                        getModel().getMachineController().updateCoordinateSystem(1).flatMap(result -> Observable.just(result.coordinateID == 1))
                        : Observable.just(false));
    }

    /**
     * Capture photo and calculate the result based on the photo.
     * <p>
     * 1. Set camera expose time to 1;
     * 2. Request capture and receive photo;
     * 3. Process photo, save params, calculate thickness;
     * 4. Restore expose time to default(0).
     *
     * @param which which time we capture(1st time, 2nd time, etc.), count from 1.
     */
    public void captureAndCalculate(@CaptureCount int which) {
        getModel().getLaserCameraController().setExposeTime(2)
                .flatMap(success -> getModel().getLaserCameraController().requestCapturePhoto())
                .flatMap(success -> getModel().getLaserCameraController().watchPhotoReceive())
                .doOnNext(bitmap -> {
                    FileOutputStream out = new FileOutputStream(mPhotoPaths[which - 1]);
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
                    float spotX = LaserDistanceMeasureProcess.process(bitmap);
                    Logger.i("Detected spot x position is %s", spotX);

                    CalibrationCaptureResult result = new CalibrationCaptureResult();
                    result.which = which;
                    if (spotX < -200) {
                        result.isSuccess = false;
                        mCaptureResultSubject.onNext(result);
                        return;
                    }
                    if (which == FIRST_CAPTURE) {
                        mS1plus = spotX;
                        result.isSuccess = true;
                    } else if (which == SECOND_CAPTURE) {
                        mS2plus = spotX;
                        result.isSuccess = true;
                    } else if (which == MEASURE_CAPTURE) {
                        mSxplus = spotX;
                        final float calculateResult = calculateResult();
                        Logger.i(">>> calculateResult = %s", calculateResult);
                        result.isSuccess = Math.abs(calculateResult - MockConst.LASER_MATERIAL_MEASURE_CALIBRATION_OBJECT_HEIGHT) < mPermissibleError;
                        // Save calibrated params(s1', s2').
                        saveCalibratedParams();
                    }

                    mCaptureResultSubject.onNext(result);
                })
                .doOnError(e -> {
                    // Log error for requesting and processing photo
                    LogHelper.log(e);
                    CalibrationCaptureResult result = new CalibrationCaptureResult();
                    result.which = which;
                    result.isSuccess = false;
                    mCaptureResultSubject.onNext(result);
                })
                .flatMap(bitmap -> getModel().getLaserCameraController().setExposeTime(0))
                .subscribeOn(Schedulers.computation())
                .as(bindToLifecycle())
                .subscribe(success -> {
                }, LogHelper::log);
    }

    public Observable<CalibrationCaptureResult> getCaptureResultObservable() {
        return mCaptureResultSubject.hide();
    }

    public void saveCalibratedParams() {
        getModel().getPreferences().setLaserParamS1Plus(mS1plus);
        getModel().getPreferences().setLaserParamS2Plus(mS2plus);
    }

    public void switchAFAssistLight(boolean on) {
        getModel().getSlaveComputer().setAFAssistLightState(on ? 1 : 0).as(bindToLifecycle()).subscribe();
    }

    private float calculateResult() {
        float h3 = mH1 - mH2;
        return mH1 - (mH1 * ((h3 * mS1plus) + ((mS2plus * mH2) - (mS1plus * mH1))) / (h3 * mSxplus + ((mS2plus * mH2) - (mS1plus * mH1)))) + MockConst.LASER_MATERIAL_MEASURE_CALIBRATION_OBJECT_HEIGHT;
    }
}