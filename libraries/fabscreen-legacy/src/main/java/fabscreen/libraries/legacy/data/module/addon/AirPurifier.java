package fabscreen.libraries.legacy.data.module.addon;

import static fabscreen.libraries.legacy.data.Constants.FIVE_MINUTES_DELAY_DURATION;

import com.orhanobut.logger.Logger;

import fabscreen.libraries.legacy.BaseApplication;
import fabscreen.libraries.legacy.data.Constants;
import fabscreen.libraries.legacy.data.ISlaveComputer;
import fabscreen.libraries.legacy.data.module.SnapModule;
import fabscreen.libraries.legacy.data.print.MachinePrintJobState;
import fabscreen.libraries.legacy.data.serial.fabpacket.FabPacketContent;
import fabscreen.libraries.legacy.lib.LogHelper;
import io.reactivex.Observable;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.subjects.BehaviorSubject;

public class AirPurifier extends SnapModule {
   private CompositeDisposable mDisposables;

   private boolean mAirPurifierFanEnabled = false;
   private int mAirPurifierFanSpeed = 0;
   private int mAirPurifierFilterLifeTime = 0;

   private BehaviorSubject<FabPacketContent.AirPurifierStatus> mAirPurifierStatusSubject = BehaviorSubject.createDefault(FabPacketContent.AirPurifierStatus.MOCK_AIR_PURIFIER_STATUS_NOT_PLUGGED);
   private BehaviorSubject<FabPacketContent.AirPurifierFan> mAirPurifierFanSubject = BehaviorSubject.createDefault(new FabPacketContent.AirPurifierFan());
   private BehaviorSubject<Integer> mAirPurifierLifeTimeSubject = BehaviorSubject.createDefault(0);


   public AirPurifier(ISlaveComputer slaveComputer, int moduleType) {
      super(slaveComputer, moduleType);
   }

   @Override
   public void init() {
      Disposable sub = mSlaveComputer.requestAirPurifierAddOnStatus()
              .subscribe(airPurifierStatus -> {
                 mAirPurifierStatusSubject.onNext(airPurifierStatus);
                 if (airPurifierStatus.status != (byte) 0x01) {
                    onAirPurifierConnected();
                 }

                 switch (airPurifierStatus.status) {
                    case 0x00:
                       // OK
                       break;
                    case 0x01:
                       // not plugged
                       break;
                    case 0x02:
                       // no power
                       break;
                    case 0x03:
                       // error
                       break;
                    default:
                       break;
                 }
              }, LogHelper::log);
      mDisposables.add(sub);
   }

   // Air Purifier
   private void onAirPurifierConnected() {
      // Initialize air purifier status
      Disposable sub = mSlaveComputer.watchAirPurifierAddOnStatus()
              .subscribe(airPurifierStatus -> {
                 mAirPurifierStatusSubject.onNext(airPurifierStatus);
              }, LogHelper::log);
      mDisposables.add(sub);

      sub = mSlaveComputer.getAirPurifierFilterLifeTime()
              .doOnNext(l -> mAirPurifierLifeTimeSubject.onNext(l))
              .flatMap(life -> mSlaveComputer.watchAirPurifierFilterLifeTime())
              .subscribe(life -> {
                 mAirPurifierLifeTimeSubject.onNext(life);
              });
      mDisposables.add(sub);

      sub = mSlaveComputer.requestAirPurifierFan().subscribe(airPurifierFan -> {
         mAirPurifierFanEnabled = airPurifierFan.isOn;
         mAirPurifierFanSpeed = airPurifierFan.level;
         mAirPurifierFanSubject.onNext(airPurifierFan);
      }, LogHelper::log);
      mDisposables.add(sub);

      // Observe Print state with PrintController for auto turn off task.
//      printStateSubscription = mPrintController.getPrintJobStateObservable()
//              .distinctUntilChanged()
//              .subscribe(state -> {
//                 if (isAirPurifierAutoTurnOffNeeded()) {
//                    setAirPurifierAutoTurnOffEnabled(MachinePrintJobState.PRINT_JOB_STATE_FINISHED.valueEqual(state.getValue()));
//                 } else {
//                    // TODO: Temporary fix.
//                    //  If we stop or complete print job, update air purifier once for synchronizing status.
//                    //  We need to do that because controller won't push air purifier status when print job finished or stopped.
//                    if (!MachinePrintJobState.isStatePrinting(state.getValue())) {
//                       disposables.add(updateAirPurifierStatus().subscribe());
//                    }
//                 }
//              }, LogHelper::log);
//      mDisposables.add(printStateSubscription);
   }

//   private boolean isAirPurifierAutoTurnOffEnabled() {
//      return mAirPurifierAutoTurnOffSubscription != null;
//   }

   private boolean isAirPurifierAutoTurnOffNeeded() {
      return BaseApplication.getInstance().getModel().getPreferences().getAirPurifierAutoTurnOffFlag() && mAirPurifierFanEnabled;
   }

   private void setAirPurifierAutoTurnOffEnabled(boolean enabled) {
//      if (enabled) {
//         // Prevent multiply subscribe when this calls.
//         if (mAirPurifierAutoTurnOffSubscription != null) {
//            mAirPurifierAutoTurnOffSubscription.dispose();
//         }
//
//         Logger.d("Start countdown for Air Purifier auto turn off...");
//
//         mAirPurifierAutoTurnOffSubscription = Observable.interval(FIVE_MINUTES_DELAY_DURATION, Constants.TIME_UNIT)
//                 .take(1)
//                 .subscribe(t -> {
//                    // Turn off the air purifier when timeout
//                    Logger.d("Air Purifier auto turn off.");
//                    disposables.add(
//                            mSlaveComputer.setAirPurifierEnabled(false)
//                                    .subscribe(success -> {/**/}, LogHelper::log)
//                    );
//                 });
//      } else {
//         Logger.d("Air Purifier auto turn off canceled.");
//         // Disable Auto Turn Off Task
//         if (mAirPurifierAutoTurnOffSubscription != null) {
//            mAirPurifierAutoTurnOffSubscription.dispose();
//            mAirPurifierAutoTurnOffSubscription = null;
//         }
//      }
   }

   public Observable<FabPacketContent.AirPurifierStatus> getAirPurifierStatusObservable() {
      return mAirPurifierStatusSubject.hide();
   }

   public boolean isAirPurifierPlugged() {
      return mAirPurifierStatusSubject.getValue().status != 0x01;
   }

   public boolean isAirPurifierReady() {
      return mAirPurifierStatusSubject.getValue().status == 0x00;
   }

   public Observable<FabPacketContent.AirPurifierStatus> updateAirPurifierStatus() {
      return mSlaveComputer.requestAirPurifierAddOnStatus().doOnNext(airPurifierStatus -> {
         mAirPurifierStatusSubject.onNext(airPurifierStatus);
      });
   }

   public FabPacketContent.AirPurifierStatus getAirPurifierStatus() {
      return mAirPurifierStatusSubject.getValue();
   }

   public Observable<FabPacketContent.AirPurifierFan> updateAirPurifierFan() {
      return mSlaveComputer.requestAirPurifierFan().doOnNext(airPurifierFan -> {
         mAirPurifierFanEnabled = airPurifierFan.isOn;
         mAirPurifierFanSpeed = airPurifierFan.level;
         mAirPurifierFanSubject.onNext(airPurifierFan);
      });
   }

   public int getAirPurifierFanSpeed() {
      return mAirPurifierFanSpeed;
   }

   public boolean isAirPurifierFanOn() {
      return mAirPurifierFanEnabled;
   }

   public int getAirPurifierFilterLifeTime() {
      return mAirPurifierLifeTimeSubject.getValue();
   }

   public Observable<Boolean> setAirPurifierEnabled(boolean enabled) {
      return mSlaveComputer.setAirPurifierEnabled(enabled)
              .doOnNext(ret -> {
                 mDisposables.add(updateAirPurifierFan().subscribe(success -> {/**/}, LogHelper::log));
//                 if (isAirPurifierAutoTurnOffEnabled()) {
//                    setAirPurifierAutoTurnOffEnabled(false);
//                 }
              });
   }

   public Observable<Integer> getAirPurifierFilterLifeTimeObservable() {
      return mAirPurifierLifeTimeSubject.hide();
   }

   public Observable<FabPacketContent.AirPurifierFan> getAirPurifierFanObservable() {
      return mAirPurifierFanSubject.hide();
   }

   @Override
   public String getDisplayName() {
      return null;
   }
}
