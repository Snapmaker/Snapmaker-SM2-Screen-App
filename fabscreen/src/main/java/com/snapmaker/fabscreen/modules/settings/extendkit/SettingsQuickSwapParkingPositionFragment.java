package com.snapmaker.fabscreen.modules.settings.extendkit;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.snapmaker.fabscreen.R;
import com.snapmaker.fabscreen.modules.common.CoordinateSystemPresenter;
import com.snapmaker.fabscreen.view.FabProgressDialog;

import java.util.Locale;

import butterknife.BindView;
import butterknife.OnClick;
import fabscreen.libraries.legacy.base.BaseFragment;
import fabscreen.libraries.legacy.data.Constants;
import fabscreen.libraries.legacy.lib.LogHelper;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.subjects.BehaviorSubject;

public class SettingsQuickSwapParkingPositionFragment extends BaseFragment {
    private static final int PARKING_PROCESS_IDLE = 0;
    private static final int PARKING_PROCESS_MOVING = 1;
    private static final int PARKING_PROCESS_MOVED = 2;

    @BindView(R.id.tv_quick_swap_park_position_title)
    TextView mTvParkingTitle;
    @BindView(R.id.tv_quick_swap_park_position_desc)
    TextView mTvParkingDesc;

    @BindView(R.id.btn_quick_swap_park_position_start)
    Button mBtnStart;

    FabProgressDialog mProgressDialog;

    private int mHeadType;
    private CoordinateSystemPresenter mCoordinateSystemPresenter;

    private BehaviorSubject<Integer> mParkingProcessSubject = BehaviorSubject.createDefault(PARKING_PROCESS_IDLE);

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mCoordinateSystemPresenter = new CoordinateSystemPresenter(requireContext(), getModel(), disposables);
        mHeadType = getModel().getMachineController().getHeadType();
        if (mHeadType == Constants.HEAD_3DP_DUAL_EXTRUDER || mHeadType == Constants.HEAD_3DP) {
            mCoordinateSystemPresenter.ensureCoordinate(0);
        } else {
            mCoordinateSystemPresenter.ensureCoordinate(1);
        }

        mParkingProcessSubject.observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(process -> {
                    mBtnStart.setEnabled(process == PARKING_PROCESS_IDLE);
                    mBtnStart.setVisibility(process != PARKING_PROCESS_MOVED ? Button.VISIBLE : Button.GONE);
                    switch (process) {
                        case PARKING_PROCESS_IDLE:
                            break;
                        case PARKING_PROCESS_MOVING: {
                            showMovingDialog();
                            break;
                        }
                        case PARKING_PROCESS_MOVED:
                            mTvParkingTitle.setText(R.string.settings_quick_swap_park_completed_title);
                            mTvParkingDesc.setText(R.string.settings_quick_swap_park_completed_desc);
                            break;
                    }
                });
    }

    private Observable<Boolean> moveToProperPosition() {
        boolean isRotaryAvailable = getModel().getMachineController().isRotaryModuleAvailable();
        float initX = getModel().getMachineController().getSizeX() * 0.5f;
        float initY = getModel().getMachineController().getSizeY();
        float initZ = isRotaryAvailable ? getModel().getMachineController().getSizeZ() * 0.8f : getModel().getMachineController().getSizeZ() * 0.5f;

        boolean isUsingG53 = mHeadType == Constants.HEAD_3DP || mHeadType == Constants.HEAD_3DP_DUAL_EXTRUDER;

        return getModel().getMachineController().updateCoordinateSystem(0)
                .flatMap(system -> getModel().getSlaveComputer().sendGcode(String.format(Locale.ENGLISH, "G0 X%.2f Y%.2f F1800", initX, initY)))
                .flatMap(response -> getModel().getSlaveComputer().sendGcode(String.format(Locale.ENGLISH, "G0 Z%.2f F1800", initZ)))
                .flatMap(response -> {
                    if (isUsingG53) {
                        return Observable.just(true);
                    } else {
                        return getModel().getMachineController().updateCoordinateSystem(1)
                                .flatMap(response2 -> Observable.just(true));
                    }
                });
    }

    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_settings_quick_swap_parking_position;
    }

    private void showMovingDialog() {
        if (mProgressDialog != null && !mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
        }

        mProgressDialog = new FabProgressDialog(requireContext());
        mProgressDialog.setMessage(R.string.dialog_warning_moving);
        mProgressDialog.setCancelOnTouchOutside(false);

        mProgressDialog.show();
    }

    @OnClick(R.id.btn_quick_swap_park_position_start)
    void onClickStart() {
        mParkingProcessSubject.onNext(PARKING_PROCESS_MOVING);

        moveToProperPosition().observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(success -> {

                    if (mProgressDialog != null) {
                        mProgressDialog.dismiss();
                        mProgressDialog = null;
                    }

                    if (success) {
                        mParkingProcessSubject.onNext(PARKING_PROCESS_MOVED);
                    }

                }, e -> {
                    if (mProgressDialog != null) {
                        mProgressDialog.dismiss();
                        mProgressDialog = null;
                    }
                    LogHelper.log(e);
                });
    }

}
