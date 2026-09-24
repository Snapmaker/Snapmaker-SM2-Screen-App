package fabscreen.libraries.legacy.data.module.motion;

import fabscreen.libraries.legacy.data.Constants;
import fabscreen.libraries.legacy.data.ISlaveComputer;
import fabscreen.libraries.legacy.data.module.SnapModule;
import fabscreen.libraries.legacy.lib.LogHelper;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.subjects.BehaviorSubject;

public class RotaryModule extends SnapModule {
    private CompositeDisposable mDisposables;

    private boolean mIsRotaryAvailable = false;

    private BehaviorSubject<Byte> mRotaryModuleStatusSubject = BehaviorSubject.createDefault((byte) 1);

    public RotaryModule(ISlaveComputer slaveComputer, int moduleType) {
        super(slaveComputer, moduleType);
    }

    @Override
    public void init() {
        Disposable sub = mSlaveComputer.requestRotaryModuleStatus()
                .subscribe(status -> {
                    mRotaryModuleStatusSubject.onNext(status);
                    switch (status) {
                        case (byte) 0:
                            // rotary connected and ready to go
                            mIsRotaryAvailable = true;
                            break;
                        case (byte) 1:
                            // rotary not connected
                        case (byte) 2:
                            // rotary detected but not available
                            mIsRotaryAvailable = false;
                            break;
                        default:
                            break;
                    }
                }, LogHelper::log);
        mDisposables.add(sub);
    }

    @Override
    public String getDisplayName() {
        return null;
    }
}
