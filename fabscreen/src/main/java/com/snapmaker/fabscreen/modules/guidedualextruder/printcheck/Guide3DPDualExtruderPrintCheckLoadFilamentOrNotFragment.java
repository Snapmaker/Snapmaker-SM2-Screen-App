package com.snapmaker.fabscreen.modules.guidedualextruder.printcheck;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.snapmaker.fabscreen.R;

import butterknife.BindView;
import butterknife.OnClick;
import fabscreen.libraries.legacy.base.BaseFragment;
import fabscreen.libraries.legacy.base.BaseViewModel;

public class Guide3DPDualExtruderPrintCheckLoadFilamentOrNotFragment extends BaseFragment {
    public static Guide3DPDualExtruderPrintCheckLoadFilamentOrNotFragment newInstance() {
        return new Guide3DPDualExtruderPrintCheckLoadFilamentOrNotFragment();
    }

    @BindView(R.id.top_bar_back)
    Button mBtnBack;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_calibration_dual_extruder_print_check_load_filament_or_not;
    }

    @Override
    protected BaseViewModel getViewModel() {
        return null;
    }

    @OnClick(R.id.btn_calibration_dual_extruder_print_check_load)
    void onClickLoad() {

    }

    @OnClick(R.id.btn_calibration_dual_extruder_print_check_print)
    void onClickPrint() {

    }
}
