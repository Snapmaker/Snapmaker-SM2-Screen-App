package fabscreen.libraries.legacy.data.module.toolhead;

import fabscreen.libraries.legacy.data.ISlaveComputer;
import fabscreen.libraries.legacy.data.MockConst;
import fabscreen.libraries.legacy.data.module.ToolHead;
import fabscreen.libraries.legacy.lib.LogHelper;
import io.reactivex.Observable;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.subjects.BehaviorSubject;

public class LaserToolHead extends ToolHead {
    private CompositeDisposable mDisposables;

    private boolean mIsRotaryAvailable;

    private BehaviorSubject<Float> mLaserFocusSubject = BehaviorSubject.createDefault(0f);

    public LaserToolHead(ISlaveComputer slaveComputer, int moduleType, int toolHeadId) {
        super(slaveComputer, moduleType, toolHeadId);
    }

    @Override
    public void init() {
        // Bluetooth status
        Disposable sub = mSlaveComputer.getLaserFocalLength()
                .subscribe(focalLength -> {
                    float actualFocal;
                    if (mIsRotaryAvailable) {
                        actualFocal = focalLength
                                + MockConst.LASER_MOCK_ROTARY_WASTE_BOARD_HEIGHT
                                + MockConst.LASER_MOCK_ROTARY_HEIGHT;
                    } else {
                        actualFocal = focalLength + MockConst.LASER_MOCK_PLATE_HEIGHT;
                    }
                    mLaserFocusSubject.onNext(actualFocal);
                }, LogHelper::log);
        mDisposables.add(sub);

    }

    public void updateLaserFocus() {
        Disposable sub = mSlaveComputer.getLaserFocalLength()
                .subscribe(focalLength -> {
                    float actualFocal;
                    if (mIsRotaryAvailable) {
                        actualFocal = focalLength / 1000f
                                + MockConst.LASER_MOCK_ROTARY_WASTE_BOARD_HEIGHT
                                + MockConst.LASER_MOCK_ROTARY_HEIGHT;
                    } else {
                        actualFocal = focalLength / 1000f + MockConst.LASER_MOCK_PLATE_HEIGHT;
                    }
                    mLaserFocusSubject.onNext(actualFocal);
                });
        mDisposables.add(sub);
    }

    public float getLaserFocus() {
        return mLaserFocusSubject.getValue();
    }

    public Observable<Float> getLaserFocusObservable() {
        return mLaserFocusSubject.hide();
    }

    public Observable<Boolean> setLaserFocus(float laserFocus) {
        float actualFocal;
        if (mIsRotaryAvailable) {
            actualFocal = laserFocus - MockConst.LASER_MOCK_ROTARY_WASTE_BOARD_HEIGHT
                    - MockConst.LASER_MOCK_ROTARY_HEIGHT;
        } else {
            actualFocal = laserFocus - MockConst.LASER_MOCK_PLATE_HEIGHT;
        }
        return mSlaveComputer.setLaserFocalLength(actualFocal)
                .doOnNext(success -> mLaserFocusSubject.onNext(laserFocus));
    }


    @Override
    public String getDisplayName() {
        return null;
    }

    @Override
    public void reset() {

    }
}
