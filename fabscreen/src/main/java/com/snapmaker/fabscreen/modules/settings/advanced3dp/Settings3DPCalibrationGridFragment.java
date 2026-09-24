package com.snapmaker.fabscreen.modules.settings.advanced3dp;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.snapmaker.fabscreen.R;
import fabscreen.libraries.legacy.base.BaseFragment;
import fabscreen.libraries.legacy.base.BaseViewModel;

import java.util.Locale;

import butterknife.BindView;
import butterknife.OnClick;
import co.ceryle.segmentedbutton.SegmentedButtonGroup;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.subjects.BehaviorSubject;

public class Settings3DPCalibrationGridFragment extends BaseFragment {
    public static Settings3DPCalibrationGridFragment getInstance() {
        return new Settings3DPCalibrationGridFragment();
    }

    @BindView(R.id.tv_set_calibration_grid_title)
    TextView mTvTitle;
    @BindView(R.id.tv_set_calibration_grid_value)
    TextView mTvValue;

    @BindView(R.id.sbg_calibration_grid)
    SegmentedButtonGroup mSbgCalibration;

    @BindView(R.id.iv_calibration_grid)
    ImageView mIvCalibrationGrid;

    private int mGrid = 3;

    private BehaviorSubject<Integer> mGridValueSubject = BehaviorSubject.createDefault(3);

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setTitle(R.string.settings_3dp_calibration_grid);

        initView();
    }

    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_3dp_calibration_grid;
    }

    @Override
    protected BaseViewModel getViewModel() {
        return null;
    }

    private void initView() {
        int pos = 0;
        // get initial position
        mGrid = getModel().getPreferences().get3DPCalibrationGrid();
        mGridValueSubject.onNext(mGrid);

        switch (mGrid) {
            case 3:
                pos = 0;
                break;
            case 4:
                pos = 1;
                break;
            case 5:
                pos = 2;
                break;
        }
        mSbgCalibration.setPosition(pos, false);

        // init step buttons
        mSbgCalibration.setOnClickedButtonPosition(position -> {
            switch (position) {
                case 0:
                    mGrid = 3;
                    break;
                case 1:
                    mGrid = 4;
                    break;
                case 2:
                    mGrid = 5;
                    break;
            }
            mGridValueSubject.onNext(mGrid);
        });

        mGridValueSubject
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(value -> {
                    mTvValue.setText(String.format(Locale.getDefault(), "%d × %d", value, value));
                    switch (value) {
                        case 3:
                            mIvCalibrationGrid.setImageResource(R.drawable.pic_3dp_calibration_9point_280x280);
                            break;
                        case 4:
                            mIvCalibrationGrid.setImageResource(R.drawable.pic_3dp_calibration_16point_280x280);
                            break;
                        case 5:
                            mIvCalibrationGrid.setImageResource(R.drawable.pic_3dp_calibration_25point_280x280);
                            break;
                    }
                });
    }

    @OnClick(R.id.btn_3dp_calibration_grid_save)
    void onClickSave() {
        getModel().getPreferences().set3DPCalibrationGrid(mGridValueSubject.getValue());
        back();
    }
}
