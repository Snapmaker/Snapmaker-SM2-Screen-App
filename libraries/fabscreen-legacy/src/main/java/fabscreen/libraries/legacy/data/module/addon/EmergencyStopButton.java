package fabscreen.libraries.legacy.data.module.addon;

import fabscreen.libraries.legacy.data.ISlaveComputer;
import fabscreen.libraries.legacy.data.module.SnapModule;
import fabscreen.libraries.legacy.lib.LogHelper;
import io.reactivex.Observable;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.subjects.BehaviorSubject;

public class EmergencyStopButton extends SnapModule {
   private CompositeDisposable mDisposables;

   private boolean mIsEmergencyStopAvailable = false;

   private BehaviorSubject<Boolean> mEmergencyStopSubject = BehaviorSubject.createDefault(false);

   public EmergencyStopButton(ISlaveComputer slaveComputer, int moduleType) {
      super(slaveComputer, moduleType);
   }

   @Override
   public void init() {
      Disposable sub = mSlaveComputer.requestEmergencyStopStatus()
              .subscribe(status -> {
                 switch (status) {
                    case (byte) 0:
                       mEmergencyStopSubject.onNext(false);
                       mIsEmergencyStopAvailable = true;
                       onEmergencyStopConnected();
                       break;
                    case (byte) 1:
                       mEmergencyStopSubject.onNext(false);
                       mIsEmergencyStopAvailable = false;
                       break;
                    case (byte) 2:
                       mIsEmergencyStopAvailable = true;
                       mEmergencyStopSubject.onNext(true);
                       break;
                    default:
                       break;
                 }
              }, LogHelper::log);
      mDisposables.add(sub);
   }

   private void onEmergencyStopConnected() {
      Disposable sub = mSlaveComputer.watchEmergencyStopStatus().subscribe(status -> {
         if (status == 2) {
            mEmergencyStopSubject.onNext(true);
         }
      });
      mDisposables.add(sub);
   }

   public Observable<Boolean> getEmergencyStopObservable() {
      return mEmergencyStopSubject.hide();
   }

   public boolean isEmergencyStopTriggered() {
      return mEmergencyStopSubject.getValue();
   }

   public boolean isEmergencyStopAvailable() {
      return mIsEmergencyStopAvailable;
   }

   public void setEmergencyStopTriggered(boolean isTriggered) {
      mEmergencyStopSubject.onNext(isTriggered);
   }

   @Override
   public String getDisplayName() {
      return null;
   }
}
