package com.snapmaker.fabscreen.modules.settings.extendkit;

import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.RadioGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.orhanobut.logger.Logger;
import com.snapmaker.fabscreen.R;
import com.snapmaker.fabscreen.modules.common.CoordinateSystemPresenter;

import butterknife.BindView;
import butterknife.OnClick;
import fabscreen.libraries.legacy.base.BaseFragment;
import fabscreen.libraries.legacy.data.Constants;
import fabscreen.libraries.legacy.data.MachineController;
import fabscreen.libraries.legacy.lib.LogHelper;
import fabscreen.libraries.legacy.view.FabAlert;
import io.reactivex.android.schedulers.AndroidSchedulers;

public class SettingsBracingKitFragment extends BaseFragment {

    @BindView(R.id.rg_settings_bracing_kit_select)
    RadioGroup mRdBracingKitCheck;

    @BindView(R.id.cb_settings_bracing_kit_dont_ask_again)
    CheckBox mCbDoNotAskAgain;

    @BindView(R.id.rl_settings_bracing_kit_dont_ask_again)
    View mViewDoNotAskAgain;

    private boolean mIsBracingKitInstalled = false;

    private SettingsExtendKitViewModel mViewModel;

    private CoordinateSystemPresenter mCoordinateSystemPresenter;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mViewModel = getActivityScopeViewModel(SettingsExtendKitViewModel.class);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mRdBracingKitCheck.setOnCheckedChangeListener((group, checkedId) -> {
            switch (checkedId) {
                case R.id.rb_settings_bracing_kit_not_installed:
                    mIsBracingKitInstalled = false;
                    break;
                case R.id.rb_settings_bracing_kit_installed:
                    mIsBracingKitInstalled = true;
                    break;
                default:
                    break;
            }
        });

        mIsBracingKitInstalled = mViewModel.isBracingKitInstalled();

        mRdBracingKitCheck.check(mIsBracingKitInstalled ? R.id.rb_settings_bracing_kit_installed : R.id.rb_settings_bracing_kit_not_installed);

        getModel().getPreferences().setExtendKitCheckOnStartUp(false);
        mViewDoNotAskAgain.setVisibility(View.GONE);
        mCbDoNotAskAgain.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // Don't ask again means no need to check every time when machine starts.
            getModel().getPreferences().setExtendKitCheckOnStartUp(!isChecked);
        });
        mCbDoNotAskAgain.setChecked(!getModel().getPreferences().getExtendKitCheckOnStartUp());

        mCoordinateSystemPresenter = new CoordinateSystemPresenter(requireContext(), getModel(), disposables);
    }

    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_settings_bracing_kit_setup;
    }

    private void recheckCoordinateSystem() {
        MachineController.WorkType headType = getModel().getMachineController().getWorkType();
        if (headType != MachineController.WorkType.FDM) {
            mCoordinateSystemPresenter.ensureCoordinate(0);
        } else {
            mCoordinateSystemPresenter.ensureCoordinate(1);
        }

        mCoordinateSystemPresenter.setOnCoordinateSwitchListener(() -> {
            requireActivity().finish();
        });
    }

    @OnClick(R.id.btn_settings_bracing_kit_lets_create)
    void onClickCreate() {
        mViewModel.setBracingKitState(mIsBracingKitInstalled);
        mViewModel.confirmExtendKitConf()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(success -> {
                    if (!success) {
                        Logger.e("Set extend kit failed!");
                        //
                    } else {
                        Logger.d("");
                        recheckCoordinateSystem();
                    }
                }, e -> {
                    LogHelper.log(e);
                    requireActivity().finish();
                });
    }
}
