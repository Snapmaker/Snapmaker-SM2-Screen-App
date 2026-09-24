package com.snapmaker.fabscreen.modules.settings.extendkit;


import com.orhanobut.logger.Logger;

import fabscreen.libraries.legacy.base.BaseViewModel;
import fabscreen.libraries.legacy.data.serial.fabpacket.FabPacketContent;
import fabscreen.libraries.legacy.lib.LogHelper;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.subjects.BehaviorSubject;

public class SettingsExtendKitViewModel extends BaseViewModel {

    private boolean mIsQuickSwapInstalled;
    private byte mExtendKitConf;
    private byte mUserSelectConf;
    private BehaviorSubject<Byte> mExtendKitConfSubject = BehaviorSubject.createDefault((byte) 0x00);

    public SettingsExtendKitViewModel() {
        FabPacketContent.ExtendKitInfo kitInfo = getModel().getMachineController().getExtendKitInfo();
        if (kitInfo != null) {
            mExtendKitConfSubject.onNext(kitInfo.getExtendKitConf());
        }
        getModel().getMachineController().getExtendKitInfoObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(extendKitInfo -> {
                    mExtendKitConfSubject.onNext(extendKitInfo.getExtendKitConf());
                    mIsQuickSwapInstalled = extendKitInfo.isQuickSwapInstalled();
                }, LogHelper::log);
    }



    public Observable<Byte> getExtendKitObservable() {
        return mExtendKitConfSubject.hide();
    }

    public byte getExtendKitConfValue() {
        return mExtendKitConfSubject.getValue();
    }

    public void setBracingKitState(boolean installed) {
        if (installed) {
            mUserSelectConf |= FabPacketContent.ExtendKitInfo.BIT_EXTEND_KIT_BRACING_KIT;
        } else {
            mUserSelectConf &= ~FabPacketContent.ExtendKitInfo.BIT_EXTEND_KIT_BRACING_KIT;
        }
    }

    public void setQuickSwapInstalled(boolean installed) {
        if (installed) {
            mUserSelectConf |= FabPacketContent.ExtendKitInfo.BIT_EXTEND_KIT_QUICK_SWAP;
        } else {
            mUserSelectConf &= ~FabPacketContent.ExtendKitInfo.BIT_EXTEND_KIT_QUICK_SWAP;
        }
    }

    public boolean isQuickSwapInstalled() {
        return (mExtendKitConfSubject.getValue() & FabPacketContent.ExtendKitInfo.BIT_EXTEND_KIT_QUICK_SWAP) > 0 || mIsQuickSwapInstalled;
    }

    public boolean isBracingKitInstalled() {
        return (mExtendKitConfSubject.getValue() & FabPacketContent.ExtendKitInfo.BIT_EXTEND_KIT_BRACING_KIT) > 0;
    }


    public void requestExtendKitStatus() {
        getModel().getMachineController().requestExtendKitInfo();
    }

    public Observable<Boolean> confirmExtendKitConf() {
        return getModel().getMachineController().setExtendKitConf(mUserSelectConf);
    }
}
