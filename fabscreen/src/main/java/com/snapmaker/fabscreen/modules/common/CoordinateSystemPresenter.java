package com.snapmaker.fabscreen.modules.common;


import android.content.Context;
import android.util.Log;

import com.snapmaker.fabscreen.R;

import fabscreen.libraries.legacy.data.Constants;
import fabscreen.libraries.legacy.data.Model;
import fabscreen.libraries.legacy.lib.LogHelper;
import fabscreen.libraries.legacy.view.FabFullScreenDialog;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;

public class CoordinateSystemPresenter extends BasePresenter {
    private final static String TAG = CoordinateSystemPresenter.class.getSimpleName();

    private FabFullScreenDialog mDialog;
    private int mCoordinateID;
    private OnCoordinateSwitchListener mCoordinateSwitchListener;

    public CoordinateSystemPresenter(Context context, Model model, CompositeDisposable compositeDisposable) {
        super(context, model, compositeDisposable);
    }

    public void ensureCoordinate(int coordinateID) {
        if (mDialog != null) return;

        mCoordinateID = coordinateID;

        Disposable sub = getModel().getMachineController().updateCoordinateSystem()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(coordinateSystem -> {
                    boolean homed = coordinateSystem.homed;

                    if (!homed) {
                        mDialog = FabFullScreenDialog.create(getContext());
                        mDialog.setIcon(R.drawable.pic_dialog_homing_72x72);
                        mDialog.setTitle(R.string.all_homing_title_homing);
                        mDialog.setMessage(R.string.all_homing_content);
                        mDialog.setPositive(R.string.all_homing_title_going_home, (dialog, which) -> {
                            mDialog.setPositive(R.string.all_homing_title_homing, false);

                            Disposable sub1 = getModel().getSlaveComputer()
                                    .sendGcode("G28")
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .subscribe(success -> checkHome());
                            addDisposable(sub1);
                        });
                        mDialog.show();
                    } else {
                        switchCoordinate();
                    }
                }, e -> {
                    Log.e(TAG, "update coordinate system failed.");
                    LogHelper.log(e);
                });
        addDisposable(sub);
    }

    private void checkHome() {
        Disposable sub = getModel().getMachineController().updateCoordinateSystem()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(coordinateSystem -> {
                    boolean homed = coordinateSystem.homed;
                    if (homed) {
                        // Switch coordinate system
                        switchCoordinate();
                    } else {
                        // Check homed state periodically until it's homed
                        AndroidSchedulers.mainThread().scheduleDirect(this::checkHome, 2000, Constants.TIME_UNIT);
                    }
                });
        addDisposable(sub);
    }

    private void switchCoordinate() {
        int coordinateID = getModel().getMachineController().getCoordinateID();
        boolean coordinateAligned = getModel().getMachineController().isCoordinateAligned();

        // Switch to coordinate system #1 if not configured
        if (coordinateID != mCoordinateID || !coordinateAligned) {
            addDisposable(getModel().getMachineController().updateCoordinateSystem(mCoordinateID).subscribe());
        }

        if (mDialog != null) {
            mDialog.dismiss();
            mDialog = null;
        }

        if (mCoordinateSwitchListener == null) return;
        mCoordinateSwitchListener.onCoordinateSwitched();
    }

    public void setOnCoordinateSwitchListener(OnCoordinateSwitchListener listener) {
        mCoordinateSwitchListener = listener;
    }

    public interface OnCoordinateSwitchListener {
        void onCoordinateSwitched();
    }
}
