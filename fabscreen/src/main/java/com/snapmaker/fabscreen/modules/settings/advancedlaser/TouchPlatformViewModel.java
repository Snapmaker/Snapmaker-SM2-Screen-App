package com.snapmaker.fabscreen.modules.settings.advancedlaser;

import com.orhanobut.logger.Logger;
import com.snapmaker.fabscreen.data.XYZMoveController;

import fabscreen.libraries.legacy.base.BaseViewModel;
import fabscreen.libraries.legacy.data.Constants;
import fabscreen.libraries.legacy.data.ISlaveComputer;
import fabscreen.libraries.legacy.data.serial.fabpacket.FabPacketContent;
import fabscreen.libraries.legacy.lib.LogHelper;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.Subject;

public class TouchPlatformViewModel extends BaseViewModel {

    private Subject<Boolean> mIsMovingSubject = BehaviorSubject.create();

    public TouchPlatformViewModel() {
        super();
    }

    public void moveXYZByStep(XYZMoveController.Direction direction, float stepWidth) {
        mIsMovingSubject.onNext(true);
        XYZMoveController.getInstance()
                .moveByStep(direction, stepWidth)
                .as(bindToLifecycle())
                .subscribe(response -> mIsMovingSubject.onNext(false));
    }

    public void initToolheadPosition() {
        mIsMovingSubject.onNext(true);
        float initX = getModel().getMachineController().getSizeX() * 0.5f;
        float initY = getModel().getMachineController().getSizeY() * 0.5f;
        float initZ = getModel().getMachineController().getSizeZ() * 0.5f;

        float currentZ = (float) (getModel().getMachineController().getMachineStatus().z - getModel().getMachineController().getCoordinateOffsetZ());

        getModel().getMachineController().updateCoordinateSystem(0)
                .flatMap(success -> {
                    Logger.d("gotoAbsolutePosition: x:%s,y:%s", initX, initY);
                    return getModel().getSlaveComputer().gotoAbsolutePosition(initX, initY, currentZ);
                })
                .flatMap(success -> {
                    Logger.d("gotoAbsolutePosition: z:%s", initZ);
                    return getModel().getSlaveComputer().gotoAbsolutePosition(initX, initY, initZ);
                })
                .flatMap(success -> getModel().getMachineController().updateCoordinateSystem(1))
                .as(bindToLifecycle())
                .subscribe(coordinateSystem -> {
                    mIsMovingSubject.onNext(false);
                });
    }

    public Observable<Boolean> getIsMovingObservable() {
        return mIsMovingSubject.hide();
    }

    public void savePlatformZOffset() {
        FabPacketContent.MachineStatus status = getModel().getMachineController().getMachineStatus();
        float offsetZ = getModel().getMachineController().getCoordinateOffsetZ();
        getModel().getPreferences().setLaserPlatformZ((float) status.z - offsetZ);
        if (getModel().getMachineController().getHeadType() == Constants.HEAD_LASER_10W) {
            getModel().getMachineController().setLaserFocus((float) status.z - offsetZ + 10).as(bindToLifecycle()).subscribe();
        }
    }

    public Observable<Boolean> setOriginZ() {
        mIsMovingSubject.onNext(true);
        return getModel().getSlaveComputer().setPosition(0, 0, 0, ISlaveComputer.FLAG_Z)
                .flatMap(response -> getModel().getMachineController().updateCoordinateSystem())
                .flatMap(update -> Observable.just(true))
                .doOnNext(next -> mIsMovingSubject.onNext(false));
    }

    public Observable<Boolean> upLiftToolhead() {
        mIsMovingSubject.onNext(true);
        return XYZMoveController.getInstance()
                .moveByStep(XYZMoveController.Direction.UP, Constants.LASER_10W_CAMERA_FOCAL_LENGTH)
                .flatMap(response -> Observable.just(true))
                .doOnNext(success -> mIsMovingSubject.onNext(false));
    }
}
