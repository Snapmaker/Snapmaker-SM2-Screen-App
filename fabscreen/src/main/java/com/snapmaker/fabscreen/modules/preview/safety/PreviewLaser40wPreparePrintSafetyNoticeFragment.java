package com.snapmaker.fabscreen.modules.preview.safety;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.orhanobut.logger.Logger;
import com.snapmaker.fabscreen.R;
import com.snapmaker.fabscreen.modules.preview.PreviewViewModel;
import com.snapmaker.fabscreen.router.Router;

import butterknife.BindView;
import butterknife.OnClick;
import fabscreen.libraries.legacy.base.BaseFragment;
import fabscreen.libraries.legacy.base.BaseViewModel;
import fabscreen.libraries.legacy.lib.LogHelper;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.subjects.BehaviorSubject;

public class PreviewLaser40wPreparePrintSafetyNoticeFragment extends BaseFragment {

    @BindView(R.id.tv_preview_laser_prepare_print_safety_notice_message)
    TextView mTvMessage;
    @BindView(R.id.tv_preview_laser_prepare_print_safety_notice_title)
    TextView mTvTitle;
    @BindView(R.id.btn_preview_laser_prepare_print_safety_notice_next)
    Button mBtnNext;

    private PreviewViewModel mViewModel;
    private BehaviorSubject<Boolean> mMovingEventSubject = BehaviorSubject.createDefault(false);

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mViewModel = getViewModel();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getArguments() != null) {
            int mode = getArguments().getInt("indicator_mode", getModel().getPreferences().getLaserIndicatorMode());
            mViewModel.setLaserIndicatorMode(mode);
        }

        mBtnNext.setText(R.string.all_next);

        mMovingEventSubject
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(movingEvent -> {
                    mBtnNext.setEnabled(!movingEvent);
                });
    }

    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_preview_laser_40w_prepare_print_safety_notice;
    }

    @Override
    protected PreviewViewModel getViewModel() {
        return getViewModelProvider().get(PreviewViewModel.class);
    }

    private Observable<Boolean> turnOffCrossLineIndicator() {
        switch (mViewModel.getLaserIndicatorMode()) {
            case 1:
                return getModel().getSlaveComputer().sendGcode("M5").map(response -> true);
            case 0:
            default:
                return getModel().getSlaveComputer().setCrossLineLaserIndicator(false);
        }
    }

    @OnClick(R.id.btn_preview_laser_prepare_print_safety_notice_next)
    void onClickNext() {
        mMovingEventSubject.onNext(true);
        turnOffCrossLineIndicator()
                .observeOn(AndroidSchedulers.mainThread())
                .flatMap(success -> getModel().getPrintController().getHeaderSecurityStatus())
                .flatMap(success -> getModel().getSlaveComputer().setPrintOffsetWithCrossLine(mViewModel.getLaserIndicatorMode() == 0))
                .as(bindToLifecycle())
                .subscribe(success -> {
                    mMovingEventSubject.onNext(false);
                    if (requireActivity().getIntent().getBooleanExtra("force_refresh", false)) {
                        Logger.d("Refreshing print page...");
                        Router.getInstance().routeToPrintPage(true).start(getContext(), Intent.FLAG_ACTIVITY_NEW_TASK);
                        requireActivity().finish();
                    } else {
                        if (success) {
                            Router.getInstance().routeToPrintPage().start(getContext(), Intent.FLAG_ACTIVITY_NEW_TASK);
                            requireActivity().finish();
                        } else {
                            Logger.e("Set LaserIndicator mode failed, but start print instead.");
                            Router.getInstance().routeToPrintPage().start(getContext(), Intent.FLAG_ACTIVITY_NEW_TASK);
                            requireActivity().finish();
                        }
                    }
                }, e -> {
                    LogHelper.log(e);
                    mMovingEventSubject.onNext(false);
                });
    }
}
