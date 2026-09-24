package com.snapmaker.fabscreen.modules.guidedualextruder.xycalibration;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.snapmaker.fabscreen.R;
import com.snapmaker.fabscreen.router.Router;

import java.util.concurrent.TimeUnit;

import butterknife.OnClick;
import fabscreen.libraries.legacy.base.BaseFragment;
import io.reactivex.android.schedulers.AndroidSchedulers;

public class Guide3DPDualExtruderXYCalibrationCompleteFragment extends BaseFragment {


    private GuideDualExtruderXYCalibrationPrintViewModel mViewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);

        mViewModel = getActivityScopeViewModel(GuideDualExtruderXYCalibrationPrintViewModel.class);

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initView();
    }

    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_guide_3dp_dual_extruder_xy_calibration_complete;
    }

    @Override
    protected void back() {
        super.back();
    }

    private void initView() {

    }

    @OnClick(R.id.btn_guide_3dp_dual_extruder_xy_calibration_next)
    void onClickNext() {
        // gotoNext
        Router.getInstance().routeToGuide3DPDualExtruderPrintCheckPage().start(requireContext());
        AndroidSchedulers.mainThread().scheduleDirect(this::selfFinish, 200, TimeUnit.MILLISECONDS);
    }

    private void selfFinish() {
        requireActivity().finish();
    }

}
