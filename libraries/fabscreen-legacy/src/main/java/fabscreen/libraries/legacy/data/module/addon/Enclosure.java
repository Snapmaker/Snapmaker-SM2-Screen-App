package fabscreen.libraries.legacy.data.module.addon;

import com.orhanobut.logger.Logger;

import fabscreen.libraries.legacy.BaseApplication;
import fabscreen.libraries.legacy.data.ISlaveComputer;
import fabscreen.libraries.legacy.data.module.SnapModule;
import fabscreen.libraries.legacy.lib.LogHelper;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;

public class Enclosure extends SnapModule {

   private CompositeDisposable mDisposables;

   private boolean mIsEnclosureReady = false;
   private boolean mIsEnclosureDoorDetectionEnabled = false;

   private int mEnclosureLedValue = 0;
   private int mEnclosureFanValue = 0;

   public Enclosure(ISlaveComputer slaveComputer, int moduleType) {
      super(slaveComputer, moduleType);
   }

   @Override
   public void init() {
      Disposable sub = mSlaveComputer.getEnclosureStatus()
              .subscribe(enclosureStatus -> {
                 mIsEnclosureReady = enclosureStatus.isReady();
                 mIsEnclosureDoorDetectionEnabled = enclosureStatus.isEnclosureEnabled();
                 mEnclosureLedValue = enclosureStatus.ledLevel;
                 mEnclosureFanValue = enclosureStatus.fanLevel;
              }, LogHelper::log);
      mDisposables.add(sub);


      // Auto lights on if preference set
       final boolean isEnclosureAutoLightingOn = BaseApplication.getInstance().getModel().getPreferences().getEnclosureAutoLightingOn();
       if (isEnclosureAutoLightingOn) {
           // Set Enclosure Lighting, range [0 - 100]
           // Full power(100) as "on" while setting enclosure lighting.
           final int value = 100;
           sub = mSlaveComputer.setEnclosureLed(100).subscribe(success -> {
               Logger.d("Set Enclosure lighting " + success);
           }, LogHelper::log);
           mDisposables.add(sub);
       }

   }

   @Override
   public String getDisplayName() {
      return null;
   }

   public boolean isEnclosureReady() {
       return mIsEnclosureReady;
   }

   public boolean isEnclosureLedOn() {
       return mEnclosureLedValue != 0;
   }

   public boolean isEnclosureFanOn() {
       return mEnclosureFanValue != 0;
   }

   public boolean ismIsEnclosureDoorDetectionEnabled() {
       return mIsEnclosureDoorDetectionEnabled;
   }

   public int getEnclosureFanValue() {
       return mEnclosureFanValue;
   }

   public int getEnclosureLedValue() {
       return mEnclosureLedValue;
   }
}
