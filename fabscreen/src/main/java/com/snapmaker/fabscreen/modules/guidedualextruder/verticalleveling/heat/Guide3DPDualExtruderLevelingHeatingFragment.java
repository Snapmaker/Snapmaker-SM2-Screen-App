package com.snapmaker.fabscreen.modules.guidedualextruder.verticalleveling.heat;

import android.graphics.Color;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.snapmaker.fabscreen.R;
import com.snapmaker.fabscreen.modules.guidedualextruder.verticalleveling.GuideDualExtruderVerticalLevellingActivity;

import butterknife.BindView;
import butterknife.OnClick;
import fabscreen.libraries.legacy.base.BaseFragment;
import fabscreen.libraries.legacy.lib.LogHelper;
import fabscreen.libraries.legacy.view.FabConfirm;
import fabscreen.libraries.legacy.view.RulerView;
import io.reactivex.android.schedulers.AndroidSchedulers;

public class Guide3DPDualExtruderLevelingHeatingFragment extends BaseFragment {
    public static Fragment newInstance() {
        return new Guide3DPDualExtruderLevelingHeatingFragment();
    }

    private Guide3DPDualExtruderLevelingHeatingViewModel mFragmentViewModel;
    @BindView(R.id.tv_bed_temp_value)
    TextView mBedTempValue;
    @BindView(R.id.rv_temp)
    RulerView mRvTemp;
    @BindView(R.id.btn_calibrate)
    Button mBtnCalibrate;

    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_leveling_heating;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mFragmentViewModel = getFragmentScopeViewModel(Guide3DPDualExtruderLevelingHeatingViewModel.class);
        initView();
    }

    private void initView() {
        setTitle(R.string.guide_3dp_dual_extruder_Bed_leveling_heating_title);
        alertAndStartHeat();
        mRvTemp.setOnValueChangedListener(value -> mFragmentViewModel.onUserSetTemperature(value));

        mFragmentViewModel.getTempObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(this::refreshTempUI, LogHelper::log);
    }

    private void refreshTempUI(Guide3DPDualExtruderLevelingHeatingViewModel.BedTemperature temperature) {
        String rawString = temperature.current + "/" + temperature.target;
        SpannableString tempString = new SpannableString(rawString);
        tempString.setSpan(new ForegroundColorSpan(Color.WHITE), rawString.indexOf('/'), rawString.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        mBedTempValue.setText(tempString);
        mRvTemp.setCurrentValue(temperature.target);
        // FIXME: 9/21/22 Is this tolerance ok?
        if (temperature.target - temperature.current <= 2) {
            mBtnCalibrate.setText(R.string.all_calibrate);
            mBtnCalibrate.setEnabled(true);
        } else {
            mBtnCalibrate.setText(R.string.all_heating);
            mBtnCalibrate.setEnabled(false);
        }
    }

    private void alertAndStartHeat() {
        FabConfirm.create(requireContext())
                .setDescription(getString(R.string.alert_bed_start_heating))
                .setConfirm(R.string.guide_got_it, (dialog, which) -> {
                    dialog.dismiss();
                    mFragmentViewModel.startHeatingBed();
                })
                .setCanceledOnTouchOutSide(false)
                .show();
    }

    @OnClick(R.id.btn_calibrate)
    void onCalibrateClicked() {
        FabConfirm.create(requireContext()).setIcon(R.drawable.pic_dialog_warning_72x72)
                .setDescription(R.string.guide_3dp_dual_extruder_heated_leveling_clean_nozzle_content)
                .setConfirm(R.string.all_confirm, ((dialog, which) -> {
                    dialog.dismiss();
                    ((GuideDualExtruderVerticalLevellingActivity) requireActivity()).goBedLeveling(true);
                })).show();
    }
}
