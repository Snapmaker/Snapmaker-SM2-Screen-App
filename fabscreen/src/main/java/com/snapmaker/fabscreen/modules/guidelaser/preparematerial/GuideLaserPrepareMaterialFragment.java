package com.snapmaker.fabscreen.modules.guidelaser.preparematerial;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.orhanobut.logger.Logger;
import com.snapmaker.fabscreen.R;
import fabscreen.libraries.legacy.base.BaseFragment;
import fabscreen.libraries.legacy.base.BaseViewModel;
import com.snapmaker.fabscreen.modules.common.MaterialThicknessWidgetPresenter;
import com.snapmaker.fabscreen.modules.guidelaser.GuideLaserActivity;

import butterknife.OnClick;

public class GuideLaserPrepareMaterialFragment extends BaseFragment {
    public static GuideLaserPrepareMaterialFragment newInstance() {
        return new GuideLaserPrepareMaterialFragment();
    }

    private MaterialThicknessWidgetPresenter mMaterialThicknessWidgetPresenter;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setTitle(R.string.laser_calibration_material_thickness);

        initView();
    }

    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_guide_laser_prepare_material;
    }

    @Override
    protected BaseViewModel getViewModel() {
        return null;
    }

    private void initView() {
        mMaterialThicknessWidgetPresenter = new MaterialThicknessWidgetPresenter(getContext(), getModel(), disposables);
        mMaterialThicknessWidgetPresenter.bind(getView());
        mMaterialThicknessWidgetPresenter.connectPreference();
    }

    @OnClick(R.id.btn_guide_laser_prepare_material_next)
    void onClickNext() {
        final float thickness = mMaterialThicknessWidgetPresenter.getTargetValue();
        getModel().getPreferences().setLaserMaterialThickness(thickness);

        Logger.d("Set material %.1f mm.", thickness);

        if (getActivity() == null) return;
        ((GuideLaserActivity) getActivity()).startMeasureHeightIntroFragment();
    }
}
