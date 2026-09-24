package fabscreen.libraries.legacy.data.module.virtual;

import com.orhanobut.logger.Logger;

import fabscreen.libraries.legacy.data.ISlaveComputer;
import fabscreen.libraries.legacy.data.module.SnapModule;
import fabscreen.libraries.legacy.data.serial.fabpacket.FabPacketContent;
import fabscreen.libraries.legacy.lib.LogHelper;
import io.reactivex.Observable;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.subjects.BehaviorSubject;

public class QuickSwapKit extends SnapModule {

   private CompositeDisposable mDisposables;

   private BehaviorSubject<FabPacketContent.ExtendKitInfo> mQuickSwapInfoSubject = BehaviorSubject.createDefault(new FabPacketContent.ExtendKitInfo((byte) 0, (byte) 0));

   public QuickSwapKit(ISlaveComputer slaveComputer, int moduleType) {
      super(slaveComputer, moduleType);
   }

   @Override
   public void init() {
      Disposable sub = mSlaveComputer.requestExtendKitInfo()
              .subscribe(info -> {
                 Logger.d("Request quick swap state " + info.toString());
                 mQuickSwapInfoSubject.onNext(info);
              }, LogHelper::log);
      mDisposables.add(sub);
   }

   // Quick SWAP Kit
   public Observable<FabPacketContent.ExtendKitInfo> getQuickSwapKitInfoObservable(){
      return mQuickSwapInfoSubject.hide();
   }

   public FabPacketContent.ExtendKitInfo getQuickSwapInfo() {
      return mQuickSwapInfoSubject.getValue();
   }

   public Observable<Boolean> setQuickSwapInstall(boolean installed, byte extendInfo) {
      return mSlaveComputer.setExtendKitInfo(installed, extendInfo)
              .flatMap(result -> {
                 if (result != 0) {
                    Logger.w("Set quick swap state failed, replace from last state");
                    return Observable.just(mQuickSwapInfoSubject.getValue());
                 } else {
                    return mSlaveComputer.requestExtendKitInfo();
                 }
              })
              .doOnNext(info -> {
                 Logger.d("Set quick swap state success, info:\n " + info.toString());
                 mQuickSwapInfoSubject.onNext(info);
              })
              .flatMap(info -> Observable.just(true));
   }

   @Override
   public String getDisplayName() {
      return null;
   }
}
