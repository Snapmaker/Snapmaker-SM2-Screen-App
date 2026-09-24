package com.snapmaker.fabscreen.modules.guidedualextruder.printcheck;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.orhanobut.logger.Logger;
import com.snapmaker.fabscreen.R;
import com.snapmaker.fabscreen.modules.dualextrudercalibration.printcheck.CalibrationDualExtruderPrintCheckActivity;
import com.snapmaker.fabscreen.modules.guidedualextruder.xycalibration.GuideDualExtruderXYCalibrationActivity;
import com.snapmaker.fabscreen.router.Router;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.OnClick;
import fabscreen.libraries.legacy.base.BaseFragment;
import fabscreen.libraries.legacy.base.BaseViewModel;
import fabscreen.libraries.legacy.data.Constants;
import fabscreen.libraries.legacy.lib.LogHelper;
import fabscreen.libraries.legacy.lib.file.FabLocalFile;
import fabscreen.libraries.legacy.view.FabConfirm;
import io.reactivex.android.schedulers.AndroidSchedulers;

public class Guide3DPDualExtruderPrintCheckGetStartedFragment extends BaseFragment {
    public static Guide3DPDualExtruderPrintCheckGetStartedFragment newInstance() {
        return new Guide3DPDualExtruderPrintCheckGetStartedFragment();
    }

    private GuideDualExtruderPrintCheckPrintViewModel mViewModel;

    @BindView(R.id.top_bar_back)
    Button mBtnBack;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mViewModel = getActivityScopeViewModel(GuideDualExtruderPrintCheckPrintViewModel.class);
    }

    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_guide_3dp_dual_extruder_print_check_get_started;
    }

    @Override
    protected BaseViewModel getViewModel() {
        return null;
    }

    private void copyFileFromRaw(int rawId) {
        InputStream rawFileInputStream = getResources().openRawResource(rawId);
        File file = null;
        try {
            file = new File(getModel().getCacheDir().getAbsoluteFile() + "/dual_extruder_print_check.gcode");
            if (file.exists()) {
                boolean operate = file.delete(); // Ignore delete operation result.
            }
            try (FileOutputStream fos = new FileOutputStream(file)) {
                int count;
                byte[] bytes = new byte[20480];
                while ((count = rawFileInputStream.read(bytes)) != -1) {
                    fos.write(bytes, 0, count);
                }
            }

            if (file != null) {
                mViewModel.setPrintCheckFile(new FabLocalFile(file));
                getModel().getWorkspace().setPrintSource(Constants.PRINT_SOURCE_INTERNAL_NOT_RECOVERABLE_FILES);
                parseFile();
            }
        } catch (IOException e) {
            file = null;
            LogHelper.log(e);
        } finally {
            try {
                rawFileInputStream.close();
            } catch (IOException e2) {
                LogHelper.log(e2);
            }
        }
    }

    private void parseFile() {
        mViewModel.parseFile()
                .throttleLast(100, TimeUnit.MILLISECONDS)
                .distinctUntilChanged()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(progress -> {
                    if (progress == -1) {
                        Logger.e("Parse file error.");
                        return;
                    }

                    if (progress == 100) {
                        Logger.d("Parse file complete.");
                        ((Guide3DPDualExtruderPrintCheckActivity) requireActivity()).startPrintCheckPrint();
                    }
                });
    }

    @OnClick(R.id.btn_guide_3dp_dual_extruder_print_check_get_started)
    void onClickStart() {
        // prepare file
        int machineModel = getModel().getMachineController().getMachineModel();
        int rawFileResId = R.raw.a150_dual_extruder_print_check_04_04;
        switch (machineModel) {
            case Constants.MACHINE_MODEL_SNAPMAKER_A150:
                rawFileResId = R.raw.a150_dual_extruder_print_check_04_04;
                break;
            case Constants.MACHINE_MODEL_SNAPMAKER_A250:
                rawFileResId = R.raw.a250_dual_extruder_print_check_04_04;
                break;
            case Constants.MACHINE_MODEL_SNAPMAKER_A350:
                rawFileResId = R.raw.a350_dual_extruder_print_check_04_04;
                break;
            default:
                // not match
                break;
        }
        copyFileFromRaw(rawFileResId);
    }

    @OnClick(R.id.btn_guide_3dp_dual_extruder_print_check_skip)
    void onClickSkip() {
        FabConfirm.create(requireContext())
                .setCanceledOnTouchOutSide(false)
                .setIcon(R.drawable.pic_dialog_warning_72x72)
                .setDescription(R.string.calibration_dual_extruder_print_check_get_started_skip_confirm_content)
                .setConfirm(R.string.all_skip, (dialog, which) -> {
                    dialog.dismiss();
                    Router.getInstance().routeToGuide3DPDualExtruderCompletePage().start(requireContext());
                    AndroidSchedulers.mainThread().scheduleDirect(this::selfFinish, 200, TimeUnit.MILLISECONDS);
                })
                .setCancel(R.string.all_cancel, (dialog, which) -> {
                    dialog.dismiss();
                })
                .show();
    }

    private void selfFinish() {
        requireActivity().finish();
    }
}
