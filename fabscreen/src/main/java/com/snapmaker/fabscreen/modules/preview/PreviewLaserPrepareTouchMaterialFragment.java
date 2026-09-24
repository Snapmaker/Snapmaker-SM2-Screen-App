package com.snapmaker.fabscreen.modules.preview;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.snapmaker.fabscreen.R;
import com.snapmaker.fabscreen.view.XYZControlPanel;

import butterknife.BindView;
import butterknife.OnClick;
import fabscreen.libraries.legacy.base.BaseFragment;
import io.reactivex.android.schedulers.AndroidSchedulers;

public class PreviewLaserPrepareTouchMaterialFragment extends BaseFragment {

    private PreviewViewModel mViewModel;

    @BindView(R.id.xyz_panel_touch_material)
    XYZControlPanel mControlPanel;
    @BindView(R.id.btn_touch_material_next)
    Button mBtnNext;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mViewModel = getViewModel();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setTitle(R.string.all_touch_material_title);

        mControlPanel.setStepWidths(0.1f, 1f, 10f)
                .setOnDirectionClickListener((direction, stepWidth) -> {
                    mViewModel.moveXYZByStep(direction, stepWidth);
                });

        mViewModel.getIsMovingObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(isMoving -> {
                    mControlPanel.setXYEnabled(!isMoving);
                    mControlPanel.setZEnabled(!isMoving);
                });
    }

    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_preview_laser_prepare_touch_material;
    }

    @Override
    protected PreviewViewModel getViewModel() {
        return getViewModelProvider().get(PreviewViewModel.class);
    }

    @OnClick(R.id.btn_touch_material_next)
    void onNextClicked() {
        mViewModel.save10wMeasuredThickness(mViewModel.getMeasuredThicknessByTouch());
        mViewModel.liftToolhead(false)
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(success -> {
                    PreviewActivity activity = (PreviewActivity) requireActivity();
                    activity.gotoLaserPrepareSafetyGogglesFragment(false);
                });
    }
}
