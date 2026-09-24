package com.snapmaker.fabscreen.modules.guidedualextruder.printcheck;

import static fabscreen.libraries.legacy.data.MultiLanguageManager.LANGUAGE_DEFAULT;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.snapmaker.fabscreen.R;
import com.snapmaker.fabscreen.modules.dualextrudercalibration.printcheck.DualExtruderPrintCheckViewModel;
import com.snapmaker.fabscreen.router.Router;

import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.OnClick;
import fabscreen.libraries.legacy.base.BaseFragment;
import fabscreen.libraries.legacy.base.BaseViewModel;
import io.reactivex.android.schedulers.AndroidSchedulers;

public class Guide3DPDualExtruderPrintCheckConfirmCompleteFragment extends BaseFragment {
    public static Guide3DPDualExtruderPrintCheckConfirmCompleteFragment newInstance() {
        return new Guide3DPDualExtruderPrintCheckConfirmCompleteFragment();
    }

    @BindView(R.id.iv_calibration_dual_extruder_print_check_confirm)
    ImageView mIvCheckConfirm;

    @BindView(R.id.tv_calibration_dual_extruder_print_check_confirm_complete_title)
    TextView mTvTitle;
    @BindView(R.id.tv_calibration_dual_extruder_print_check_confirm_complete_desc)
    TextView mTvDesc;

    @BindView(R.id.btn_calibration_dual_extruder_print_check_confirm_complete)
    Button mBtnComplete;

    private GuideDualExtruderPrintCheckPrintViewModel mViewModel;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mViewModel = getActivityScopeViewModel(GuideDualExtruderPrintCheckPrintViewModel.class);
        initView();
    }

    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_calibration_dual_extruder_print_check_confirm_complete;
    }

    private void initView() {
        int result = mViewModel.getPrintCheckResult();
        boolean isUsingEnglish = getModel().getMultiLanguageManager().getCurrentLanguage() == LANGUAGE_DEFAULT;

        switch (result) {
            case DualExtruderPrintCheckViewModel.PRINT_RESULT_ALL_FAILED:
                mIvCheckConfirm.setImageResource(isUsingEnglish ?
                        R.drawable.pic_dual_extruder_print_check_all_failed_en_260x142
                        : R.drawable.pic_dual_extruder_print_check_all_failed_zh_cn_260x142);
                mTvTitle.setText(R.string.calibration_dual_extruder_print_check_confirm_complete_title_all_failed);
                mTvDesc.setText(R.string.calibration_dual_extruder_print_check_confirm_complete_desc_failed);
                break;
            case DualExtruderPrintCheckViewModel.PRINT_RESULT_ALL_OK:
                mIvCheckConfirm.setImageResource(R.drawable.pic_dual_extruder_print_check_all_success_260x142);
                mTvTitle.setText(R.string.calibration_dual_extruder_print_check_confirm_complete_title_all_successful);
                mTvDesc.setText("");
                break;
            case DualExtruderPrintCheckViewModel.PRINT_RESULT_XY_OK:
                mIvCheckConfirm.setImageResource(isUsingEnglish ?
                        R.drawable.pic_dual_extruder_print_check_xy_success_en_260x142
                        : R.drawable.pic_dual_extruder_print_check_xy_success_zh_cn_260x142);
                mTvTitle.setText(R.string.calibration_dual_extruder_print_check_confirm_complete_title_z_failed);
                mTvDesc.setText(R.string.calibration_dual_extruder_print_check_confirm_complete_desc_failed);
                break;
            case DualExtruderPrintCheckViewModel.PRINT_RESULT_Z_OK:
                mIvCheckConfirm.setImageResource(isUsingEnglish ?
                        R.drawable.pic_dual_extruder_print_check_z_success_en_260x142
                        : R.drawable.pic_dual_extruder_print_check_z_success_zh_cn_260x142);
                mTvTitle.setText(R.string.calibration_dual_extruder_print_check_confirm_complete_title_xy_failed);
                mTvDesc.setText(R.string.calibration_dual_extruder_print_check_confirm_complete_desc_failed);
                break;
            default:
                break;
        }
    }

    @OnClick(R.id.btn_calibration_dual_extruder_print_check_confirm_complete)
    void onClickComplete() {
        getModel().getPreferences().setNeedDoXYOffsetCalibration(!mViewModel.getPrintResultXY());
        getModel().getPreferences().setNeedDoZHeightCalibration(!mViewModel.getPrintResultZ());
        Router.getInstance().routeToGuide3DPDualExtruderCompletePage().start(requireContext());
        AndroidSchedulers.mainThread().scheduleDirect(this::selfFinish, 200, TimeUnit.MILLISECONDS);
    }

    private void selfFinish() {
        requireActivity().finish();
    }
}
