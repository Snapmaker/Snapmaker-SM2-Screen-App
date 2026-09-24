package com.snapmaker.fabscreen.modules.preview;

import android.graphics.Bitmap;

import com.orhanobut.logger.Logger;
import com.snapmaker.fabscreen.data.XYZMoveController;

import java.io.FileOutputStream;
import java.util.Locale;

import fabscreen.libraries.legacy.base.BaseViewModel;
import fabscreen.libraries.legacy.data.Constants;
import fabscreen.libraries.legacy.data.MockConst;
import fabscreen.libraries.legacy.data.imgprocess.LaserDistanceMeasureProcess;
import fabscreen.libraries.legacy.data.serial.fabpacket.FabPacketContent;
import fabscreen.libraries.legacy.lib.LogHelper;
import io.reactivex.Observable;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;

import static fabscreen.libraries.legacy.data.MockConst.CAMERA_HEIGHT_OFFSET;
import static fabscreen.libraries.legacy.data.MockConst.H1_Z_POSITION;
import static fabscreen.libraries.legacy.data.MockConst.H2_Z_POSITION;

public class PreviewViewModel extends BaseViewModel {

    private final float mH1;
    private final float mH2;
    private float mS1plus;
    private float mS2plus;
    private float mSxplus;

    private Subject<Boolean> mMeasureResultSubject = PublishSubject.create();
    private Subject<Boolean> mIsMovingSubject = BehaviorSubject.create();
    private Subject<Boolean> mCameraMoveSubject = PublishSubject.create();
    private Subject<Boolean> mResultBackSubject = PublishSubject.create();

    private BehaviorSubject<Integer> mLaserIndicatorModeSubject = BehaviorSubject.createDefault(0);

    private float mMeasuredThickness = 0f;

    public PreviewViewModel() {
        super();
        mH1 = H1_Z_POSITION + CAMERA_HEIGHT_OFFSET;
        mH2 = H2_Z_POSITION + CAMERA_HEIGHT_OFFSET;
        mS1plus = getModel().getPreferences().getLaserThicknessS1Plus();
        mS2plus = getModel().getPreferences().getLaserThicknessS2Plus();
    }

    /**
     * Capture photo and calculate the result based on the photo.
     * 1. Set camera expose time to 1;
     * 2. Request capture and receive photo;
     * 3. Process photo, save params, calculate thickness;
     * 4. Restore expose time to default(0).
     */
    public void autoMeasureMaterialThickness() {
        getModel().getLaserCameraController().setExposeTime(2)
                .flatMap(success -> getModel().getLaserCameraController().requestCapturePhoto())
                .flatMap(success -> getModel().getLaserCameraController().watchPhotoReceive())
                .doOnNext(bitmap -> {
                    FileOutputStream out = new FileOutputStream(getModel().getCacheDir() + "/distance.png");
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);

                    float spotX = LaserDistanceMeasureProcess.process(bitmap);
                    if (spotX < -200) {
                        mMeasureResultSubject.onNext(false);
                        return;
                    }
                    mSxplus = spotX;
                    Logger.d("Detected spot x position is %s", spotX);
                    float thickness = getCalculatedMaterialThickness();
                    Logger.i("Detected thickness is %s mm", thickness);
                    save10wMeasuredThickness(thickness);
                    mMeasureResultSubject.onNext(thickness >= 0);
                })
                .doOnError(e -> mMeasureResultSubject.onNext(false))
                .flatMap(bitmap -> getModel().getLaserCameraController().setExposeTime(0))
                .subscribeOn(Schedulers.computation())
                .as(bindToLifecycle())
                .subscribe(success -> {
                }, LogHelper::log);
    }

    public int getHeadType() {
        return getModel().getMachineController().getHeadType();
    }

    public Observable<Boolean> getMeasureResultObservable() {
        return mMeasureResultSubject.hide();
    }

    public void moveXYZByStep(XYZMoveController.Direction direction, float stepWidth) {
        mIsMovingSubject.onNext(true);
        XYZMoveController.getInstance()
                .moveByStep(direction, stepWidth)
                .as(bindToLifecycle())
                .subscribe(response -> mIsMovingSubject.onNext(false));
    }

    public String getMeasuredThicknessString() {
        return String.format(Locale.ENGLISH, "%.2f", mMeasuredThickness);
    }

    public float getMeasuredThicknessByTouch() {
        float laserPlatformZ = getModel().getPreferences().getLaserPlatformZ();
        FabPacketContent.MachineStatus status = getModel().getMachineController().getMachineStatus();
        float materialSurfaceZ = (float) status.z;
        return Math.abs(materialSurfaceZ - laserPlatformZ);
    }

    public void save10wMeasuredThickness(float thickness) {
        mMeasuredThickness = thickness;
        getModel().getPreferences().setLaserMaterialThickness(thickness);
    }

    public Observable<Boolean> getIsMovingObservable() {
        return mIsMovingSubject.hide();
    }

    public void initCameraPosition(boolean isBack) {
        mIsMovingSubject.onNext(true);
        float initX = getModel().getMachineController().getSizeX() * 0.5f - Constants.LASER_CAMERA_OFFSET_X + Constants.LASER_MEASURE_OFFSET_X;
        float initY = getModel().getMachineController().getSizeY() * 0.5f - Constants.LASER_CAMERA_OFFSET_Y;
        //Measure height.
        boolean isA150Model = getModel().getMachineController().getMachineModel() == Constants.MACHINE_MODEL_SNAPMAKER_A150;
        float initZ = isA150Model ? 150f : 170f;
        getModel().getMachineController().updateCoordinateSystem(0)
                .flatMap(success -> getModel().getSlaveComputer().gotoAbsolutePosition(initX, initY, initZ))
                .flatMap(success -> getModel().getMachineController().updateCoordinateSystem(1))
                .as(bindToLifecycle())
                .subscribe(coordinateSystem -> {
                    if (isBack) {
                        mIsMovingSubject.onNext(false);
                        mResultBackSubject.onNext(true);
                    } else {
                        mIsMovingSubject.onNext(false);
                        mCameraMoveSubject.onNext(true);
                    }
                });
    }

    public Observable<Boolean> getCameraMoveObservable() {
        return mCameraMoveSubject.hide();
    }

    public Observable<Boolean> getResultBackObservable() {
        return mResultBackSubject.hide();
    }

    final private static float MATERIAL_THICKNESS_TOLERANCE = -0.2f;

    // TODO: Move this function and calculateResult() to a separated logic class.
    private float getCalculatedMaterialThickness() {
        float thickness = calculateResult();

        // If calculated value is negative but very close to 0, we regard it as 0.
        if (thickness < 0 && thickness > MATERIAL_THICKNESS_TOLERANCE) {
            thickness = 0.f;
        }

        return thickness;
    }

    private float calculateResult() {
        float h3 = mH1 - mH2;
        return mH1 - (mH1 * ((h3 * mS1plus) + ((mS2plus * mH2) - (mS1plus * mH1))) / (h3 * mSxplus + ((mS2plus * mH2) - (mS1plus * mH1)))) + MockConst.LASER_MATERIAL_MEASURE_CALIBRATION_OBJECT_HEIGHT;
    }

    /**
     * Up/Down lift toolhead to match the focal length and the material thickness.
     */
    public Observable<Boolean> liftToolhead(boolean isAutoMode) {

        float laserPlatformZ = getModel().getPreferences().getLaserPlatformZ();
        float offsetZ = getModel().getMachineController().getCoordinateOffsetZ();

        FabPacketContent.MachineStatus status = getModel().getMachineController().getMachineStatus();
        float targetX;
        float targetY;
        float targetZ;
        if (isAutoMode) {
            // Auto mode moved to center position
            targetX = getModel().getMachineController().getSizeX() * 0.5f;
            targetY = getModel().getMachineController().getSizeY() * 0.5f;
            targetZ = mMeasuredThickness + Constants.LASER_10W_CAMERA_FOCAL_LENGTH + laserPlatformZ;
        } else {
            // Using current X Y Position
            targetX = (float) status.x - getModel().getMachineController().getCoordinateOffsetX();
            targetY = (float) status.y - getModel().getMachineController().getCoordinateOffsetY();
            targetZ = (float) getModel().getMachineController().getMachineStatus().z - offsetZ + Constants.LASER_10W_CAMERA_FOCAL_LENGTH;
        }

        Logger.d("lift to target z %s", targetZ);

        return getModel().getMachineController()
                .updateCoordinateSystem(0)
                .flatMap(coordinateSystem -> getModel().getSlaveComputer().gotoAbsolutePosition(targetX, targetY, targetZ))
                .flatMap(success -> getModel().getMachineController().updateCoordinateSystem(1))
                .flatMap(coordinateSystem -> Observable.just(true));
    }

    public void switchAFAssistLight(boolean on) {
        getModel().getSlaveComputer()
                .setAFAssistLightState(on ? 1 : 0)
                .as(bindToLifecycle())
                .subscribe(success -> {
                    // do nothing
                }, LogHelper::log);
    }

    public void savePlatformZOffset() {
        FabPacketContent.MachineStatus status = getModel().getMachineController().getMachineStatus();
        float offsetZ = getModel().getMachineController().getCoordinateOffsetZ();
        getModel().getPreferences().setLaserPlatformZ((float) status.z - offsetZ);
    }

    public Observable<Boolean> upLiftToolhead() {
        mIsMovingSubject.onNext(true);
        return XYZMoveController.getInstance()
                .moveByStep(XYZMoveController.Direction.UP, Constants.LASER_10W_CAMERA_FOCAL_LENGTH)
                .flatMap(response -> Observable.just(true))
                .doOnNext(success -> mIsMovingSubject.onNext(false));
    }

    public void initPosition() {
        mIsMovingSubject.onNext(true);
        float initX = getModel().getMachineController().getSizeX() * 0.5f;
        float initY = getModel().getMachineController().getSizeY() * 0.5f;
        float initZ = getModel().getMachineController().getSizeZ();
        getModel().getMachineController().updateCoordinateSystem(0)
                .flatMap(success -> {
                    Logger.d("gotoAbsolutePosition: x:%s,y:%s,z:%s", initX, initY, initZ);
                    return getModel().getSlaveComputer().gotoAbsolutePosition(initX, initY, initZ);
                })
                .flatMap(success -> getModel().getMachineController().updateCoordinateSystem(1))
                .as(bindToLifecycle())
                .subscribe(coordinateSystem -> {
                    mIsMovingSubject.onNext(false);
                });
    }

    public void setLaserIndicatorMode(int mode) {
        mLaserIndicatorModeSubject.onNext(mode);
    }

    public Observable<Integer> getLaserIndicatorModeObservable() {
        return mLaserIndicatorModeSubject.hide();
    }

    public int getLaserIndicatorMode() {
        return mLaserIndicatorModeSubject.getValue();
    }
}
