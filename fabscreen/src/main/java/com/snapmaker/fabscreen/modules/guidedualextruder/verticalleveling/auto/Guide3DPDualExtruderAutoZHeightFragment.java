package com.snapmaker.fabscreen.modules.guidedualextruder.verticalleveling.auto;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.snapmaker.fabscreen.R;
import com.snapmaker.fabscreen.router.Router;

import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.OnClick;
import fabscreen.libraries.legacy.base.BaseFragment;
import fabscreen.libraries.legacy.lib.LogHelper;
import io.reactivex.android.schedulers.AndroidSchedulers;

public class Guide3DPDualExtruderAutoZHeightFragment extends BaseFragment {

    private Guide3DPDualExtruderAutoZHeightViewModel mViewModel;

    public static Fragment newInstance() {
        return new Guide3DPDualExtruderAutoZHeightFragment();
    }

    @BindView(R.id.tv_cur_work_status)
    TextView mTvCurrentWorkStatus;
    @BindView(R.id.tv_calibrate_progress)
    TextView mTvCalibrateProgress;
    @BindView(R.id.btn_complete)
    Button mBtnComplete;

    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_auto_z_height_calibration;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mViewModel = getFragmentScopeViewModel(Guide3DPDualExtruderAutoZHeightViewModel.class);
        initView();
    }

    private void initView() {
        if (mBtnTopBarBack == null) return;
        mBtnTopBarBack.setEnabled(false);
        mViewModel.getProcessObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(this::refreshUI, LogHelper::log);
    }

    private void refreshUI(Guide3DPDualExtruderAutoZHeightViewModel.Process process) {
        if (mBtnTopBarBack == null) return;
        switch (process) {
            case CALIBRATING:
                mTvCurrentWorkStatus.setText(R.string.calibration_auto_z_height_title);
                mTvCalibrateProgress.setVisibility(View.VISIBLE);
                mBtnComplete.setText(R.string.all_calibrating);
                mBtnComplete.setEnabled(false);
                mBtnTopBarBack.setVisibility(View.VISIBLE);
                break;
            case FAIL:
                mTvCurrentWorkStatus.setText(R.string.calibration_z_height_failed);
                mTvCalibrateProgress.setVisibility(View.INVISIBLE);
                mBtnComplete.setText(R.string.all_ok);
                mBtnComplete.setEnabled(true);
                mBtnTopBarBack.setVisibility(View.VISIBLE);
                break;
            case COMPLETE:
                mTvCurrentWorkStatus.setText(R.string.calibration_z_height_complete);
                mTvCalibrateProgress.setVisibility(View.INVISIBLE);
                mBtnComplete.setText(R.string.all_complete);
                mBtnComplete.setEnabled(true);
                mBtnTopBarBack.setVisibility(View.INVISIBLE);
                break;
        }
    }

    @OnClick(R.id.btn_complete)
    void onCompleteClicked() {
        Router.getInstance().routeToGuide3DPDualExtruderXYCalibrationPage().start(requireContext());
        AndroidSchedulers.mainThread().scheduleDirect(this::selfFinish, 200, TimeUnit.MILLISECONDS);
    }

    private void selfFinish() {
        requireActivity().finish();
    }
}
