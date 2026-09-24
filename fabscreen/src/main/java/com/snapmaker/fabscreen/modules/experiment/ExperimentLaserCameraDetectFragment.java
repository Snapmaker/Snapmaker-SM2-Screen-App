package com.snapmaker.fabscreen.modules.experiment;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.snapmaker.fabscreen.R;
import fabscreen.libraries.legacy.base.BaseFragment;
import fabscreen.libraries.legacy.base.BaseViewModel;

import butterknife.BindView;
import fabscreen.libraries.legacy.data.imgprocess.LaserCalibrationProcess;

public class ExperimentLaserCameraDetectFragment extends BaseFragment {
    @BindView(R.id.iv_experiment_laser_image)
    ImageView mIvImage;
    @BindView(R.id.iv_experiment_laser_image_output)
    ImageView mIvImageOut;
    @BindView(R.id.tv_experiment_laser_detection)
    TextView mTvDetection;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Bitmap bitmap = BitmapFactory.decodeFile(getModel().getCacheDir() + "/capture.jpg");

        mIvImage.setImageBitmap(bitmap);

        LaserCalibrationProcess.setDebugImageView(mIvImageOut);
        int index = LaserCalibrationProcess.process(getContext(), bitmap);
        Log.d("LaserCameraDetect", "index = " + index);
        mTvDetection.setText("Detected: " + index);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        LaserCalibrationProcess.setDebugImageView(null);
    }

    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_experiment_laser;
    }

    @Override
    protected BaseViewModel getViewModel() {
        return null;
    }
}
