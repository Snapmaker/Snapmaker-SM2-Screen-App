package com.snapmaker.fabscreen.modules.settings.extendkit;

import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.RadioGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.snapmaker.fabscreen.R;
import com.snapmaker.fabscreen.modules.common.CoordinateSystemPresenter;

import butterknife.BindView;
import butterknife.OnClick;
import fabscreen.libraries.legacy.base.BaseFragment;
import fabscreen.libraries.legacy.data.MachineController;

public class SettingsQuickSwapFragment extends BaseFragment {

    @BindView(R.id.rg_settings_quick_swap_select)
    RadioGroup mRdQuickSwapCheck;

    @BindView(R.id.cb_settings_quick_swap_dont_ask_again)
    CheckBox mCbDoNotAskAgain;

    @BindView(R.id.rl_settings_quick_swap_dont_ask_again)
    View mViewDoNotAskAgain;

    private boolean mIsQuickSwapInstalled = false;

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

        mViewModel.requestExtendKitStatus();

        mRdQuickSwapCheck.setOnCheckedChangeListener((group, checkedId) -> {
            switch (checkedId) {
                case R.id.rb_settings_quick_swap_not_installed:
                    mIsQuickSwapInstalled = false;
                    break;
                case R.id.rb_settings_quick_swap_installed:
                    mIsQuickSwapInstalled = true;
                    break;
                default:
                    break;
            }
        });

        mIsQuickSwapInstalled = mViewModel.isQuickSwapInstalled();

        mRdQuickSwapCheck.check(mIsQuickSwapInstalled ? R.id.rb_settings_quick_swap_installed : R.id.rb_settings_quick_swap_not_installed);

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
        return R.layout.fragment_settings_quick_swap_setup;
    }

    private void recheckCoordinateSystem() {
        MachineController.WorkType headType = getModel().getMachineController().getWorkType();
        if (headType != MachineController.WorkType.FDM) {
            mCoordinateSystemPresenter.ensureCoordinate(0);
        } else {
            mCoordinateSystemPresenter.ensureCoordinate(1);
        }

        mCoordinateSystemPresenter.setOnCoordinateSwitchListener(() -> {

        });
    }

    @OnClick(R.id.btn_settings_quick_swap_next_step)
    void onClickCreate() {
        // TODO: add ViewModel
        mViewModel.setQuickSwapInstalled(mIsQuickSwapInstalled);
        ((SettingsExtendKitActivity) requireActivity()).startBracingKitSetUp();
    }
}
