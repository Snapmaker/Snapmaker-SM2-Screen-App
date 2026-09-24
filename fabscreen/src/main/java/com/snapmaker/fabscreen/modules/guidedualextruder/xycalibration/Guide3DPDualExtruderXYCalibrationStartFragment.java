package com.snapmaker.fabscreen.modules.guidedualextruder.xycalibration;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.orhanobut.logger.Logger;
import com.snapmaker.fabscreen.R;
import com.snapmaker.fabscreen.router.Router;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

import butterknife.OnClick;
import fabscreen.libraries.legacy.base.BaseFragment;
import fabscreen.libraries.legacy.data.Constants;
import fabscreen.libraries.legacy.lib.LogHelper;
import fabscreen.libraries.legacy.lib.file.FabLocalFile;
import io.reactivex.android.schedulers.AndroidSchedulers;

public class Guide3DPDualExtruderXYCalibrationStartFragment extends BaseFragment {

    private GuideDualExtruderXYCalibrationPrintViewModel mViewModel;

    public static Fragment newInstance() {
        return new Guide3DPDualExtruderXYCalibrationStartFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mViewModel = getActivityScopeViewModel(GuideDualExtruderXYCalibrationPrintViewModel.class);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    private void copyFileFromRaw(int rawId) {
        InputStream rawFileInputStream = getResources().openRawResource(rawId);
        File file = null;
        try {
            file = new File(getModel().getCacheDir().getAbsoluteFile() + "/xy_calibration_print.gcode");
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
                mViewModel.setCalibrationPrintFile(new FabLocalFile(file));
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
                        ((GuideDualExtruderXYCalibrationActivity) requireActivity()).goXYCalibrationPrintPage();
                    }
                });
    }

    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_guide_3dp_dual_extruder_xy_calibration_start;
    }

    @OnClick(R.id.btn_load_filament)
    void onClickLoadFilament() {
        Router.getInstance().routeToLoadFilamentActivity().start(requireContext());
    }

    @OnClick(R.id.btn_start)
    void onStartClick() {
        // prepare file
        int machineModel = getModel().getMachineController().getMachineModel();
        int rawFileResId = R.raw.a150_xy_calibration_print_04_04;
        switch (machineModel) {
            case Constants.MACHINE_MODEL_SNAPMAKER_A150:
                rawFileResId = R.raw.a150_xy_calibration_print_04_04;
                break;
            case Constants.MACHINE_MODEL_SNAPMAKER_A250:
                rawFileResId = R.raw.a250_xy_calibration_print_04_04;
                break;
            case Constants.MACHINE_MODEL_SNAPMAKER_A350:
                rawFileResId = R.raw.a350_xy_calibration_print_04_04;
                break;
            default:
                // not match
                break;
        }
        copyFileFromRaw(rawFileResId);
    }
}
