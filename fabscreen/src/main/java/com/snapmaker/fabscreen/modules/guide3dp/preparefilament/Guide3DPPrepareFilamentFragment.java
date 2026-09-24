package com.snapmaker.fabscreen.modules.guide3dp.preparefilament;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.snapmaker.fabscreen.R;
import fabscreen.libraries.legacy.base.BaseFragment;
import fabscreen.libraries.legacy.base.BaseViewModel;
import com.snapmaker.fabscreen.modules.common.LoadFilamentWidgetPresenter;
import com.snapmaker.fabscreen.modules.common.NozzleWidgetPresenter;
import com.snapmaker.fabscreen.modules.guide3dp.Guide3DPActivity;
import fabscreen.libraries.legacy.view.FabConfirm;

import butterknife.BindView;
import butterknife.OnClick;
import fabscreen.libraries.legacy.data.serial.fabpacket.FabPacketContent;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;

public class Guide3DPPrepareFilamentFragment extends BaseFragment {
    public static Guide3DPPrepareFilamentFragment newInstance() {
        return new Guide3DPPrepareFilamentFragment();
    }

    @BindView(R.id.btn_guide_3dp_prepare_filament_next)
    Button mBtnNext;

    private NozzleWidgetPresenter mNozzleWidgetPresenter;
    private LoadFilamentWidgetPresenter mLoadFilamentWidgetPresenter;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setTitle(R.string.control_load_filament);

        initView();
    }

    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_guide_3dp_prepare_filament;
    }

    @Override
    protected BaseViewModel getViewModel() {
        return null;
    }

    private void initView() {
        mNozzleWidgetPresenter = new NozzleWidgetPresenter(getContext(), getModel(), disposables);
        mNozzleWidgetPresenter.bind(getView());
        mNozzleWidgetPresenter.connectMachineStatus();

        mLoadFilamentWidgetPresenter = new LoadFilamentWidgetPresenter(getContext(), getModel(), disposables);
        mLoadFilamentWidgetPresenter.bind(getView());
        mLoadFilamentWidgetPresenter.connect();

        mLoadFilamentWidgetPresenter.getReadyToLoadObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(readyToLoad -> mBtnNext.setEnabled(readyToLoad));

        FabConfirm.create(getContext())
                .setDescription(R.string.control_heat_warning)
                .setConfirm(R.string.all_confirm, (dialog, which) -> {
                    dialog.dismiss();
                    mNozzleWidgetPresenter.setTargetValue(200);
                })
                .setCancel(R.string.all_cancel, (dialog, which) -> dialog.dismiss())
                .show();
    }

    private Observable<FabPacketContent.GcodeResponse> turnHeadOff() {
        // Set the temperature off
        return getModel().getSlaveComputer().sendGcode("M104 S0");
    }

    @OnClick(R.id.btn_guide_3dp_prepare_filament_next)
    void onClickNext() {
        turnHeadOff()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(response -> {
                    if (getActivity() == null) return;
                    ((Guide3DPActivity) getActivity()).startCompleteFragment();
                });
    }

    @Override
    protected void back() {
        // Set the temperature off
        turnHeadOff()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(response -> {
                    super.back();
                });
    }
}
