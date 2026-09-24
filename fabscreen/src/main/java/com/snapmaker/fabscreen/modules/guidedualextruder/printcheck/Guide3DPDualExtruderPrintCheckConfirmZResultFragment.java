package com.snapmaker.fabscreen.modules.guidedualextruder.printcheck;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.RadioGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.snapmaker.fabscreen.R;
import com.snapmaker.fabscreen.modules.dualextrudercalibration.printcheck.CalibrationDualExtruderPrintCheckActivity;
import com.snapmaker.fabscreen.modules.dualextrudercalibration.printcheck.DualExtruderPrintCheckViewModel;

import butterknife.BindView;
import butterknife.OnClick;
import fabscreen.libraries.legacy.base.BaseFragment;
import fabscreen.libraries.legacy.base.BaseViewModel;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.subjects.BehaviorSubject;

public class Guide3DPDualExtruderPrintCheckConfirmZResultFragment extends BaseFragment {
    public static Guide3DPDualExtruderPrintCheckConfirmZResultFragment newInstance() {
        return new Guide3DPDualExtruderPrintCheckConfirmZResultFragment();
    }

    @BindView(R.id.top_bar_back)
    Button mBtnBack;
    @BindView(R.id.btn_calibration_dual_extruder_print_check_confirm_z_next)
    Button mBtnZNext;

    @BindView(R.id.rg_calibration_dual_extruder_print_check_confirm_z)
    RadioGroup mRgCheckZ;

    private BehaviorSubject<Integer> mCalibrationPrintCheckResult = BehaviorSubject.createDefault(-1);
    private GuideDualExtruderPrintCheckPrintViewModel mViewModel;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mViewModel = getActivityScopeViewModel(GuideDualExtruderPrintCheckPrintViewModel.class);

        setTitle(R.string.calibration_dual_extruder_print_check_print_confirm_z_title);
        mRgCheckZ.setOnCheckedChangeListener((group, checkedId) -> {
            switch (checkedId) {
                case R.id.rb_calibration_dual_extruder_print_check_confirm_z_success:
                    mCalibrationPrintCheckResult.onNext(1);
                    break;
                case R.id.rb_calibration_dual_extruder_print_check_confirm_z_failed:
                    mCalibrationPrintCheckResult.onNext(0);
                    break;
                default:
                    break;
            }
        });

        mCalibrationPrintCheckResult
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(result -> {
                    mBtnZNext.setEnabled(result != -1);
                    mViewModel.setPrintResultZ(result > 0);
                });
    }

    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_calibration_dual_extruder_print_check_confirm_z_result;
    }

    @Override
    protected BaseViewModel getViewModel() {
        return null;
    }

    @OnClick(R.id.btn_calibration_dual_extruder_print_check_confirm_z_next)
    void onClickZNext() {
        // ViewModel set Z result
        ((Guide3DPDualExtruderPrintCheckActivity) requireActivity()).startPrintCheckConfirmXY();
    }
}
