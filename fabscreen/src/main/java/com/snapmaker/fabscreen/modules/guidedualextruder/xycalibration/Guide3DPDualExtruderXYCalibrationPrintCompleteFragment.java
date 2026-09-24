package com.snapmaker.fabscreen.modules.guidedualextruder.xycalibration;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.orhanobut.logger.Logger;
import com.snapmaker.fabscreen.R;
import com.snapmaker.fabscreen.modules.common.PrintDetailPanelWidgetPresenter;
import com.snapmaker.fabscreen.modules.home.HomeActivity;

import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.OnClick;
import fabscreen.libraries.legacy.base.BaseFragment;
import fabscreen.libraries.legacy.data.Constants;
import fabscreen.libraries.legacy.data.print.IPrintController;
import fabscreen.libraries.legacy.data.print.PrintListener;
import fabscreen.libraries.legacy.lib.DateHelper;
import fabscreen.libraries.legacy.lib.LogHelper;
import fabscreen.libraries.legacy.lib.file.IFile;
import fabscreen.libraries.legacy.view.CircularProgressView;
import fabscreen.libraries.legacy.view.FabConfirm;
import fabscreen.libraries.legacy.view.FabFullScreenDialog;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.subjects.BehaviorSubject;

public class Guide3DPDualExtruderXYCalibrationPrintCompleteFragment extends BaseFragment {


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
        return R.layout.fragment_guide_3dp_dual_extruder_xy_calibration_print_complete;
    }

    @Override
    protected void back() {
        requireActivity().finish();
//        super.back();
    }

    private void initView() {

    }

    @OnClick(R.id.btn_guide_3dp_dual_extruder_xy_print_next)
    void onClickNext() {
        ((GuideDualExtruderXYCalibrationActivity) requireActivity()).goXYCalibrationCheckResultIntro();
    }

    @OnClick(R.id.btn_guide_3dp_dual_extruder_xy_print_again)
    void onClickPrintAgain() {
        ((GuideDualExtruderXYCalibrationActivity) requireActivity()).goXYCalibrationStartPage();
    }
}
