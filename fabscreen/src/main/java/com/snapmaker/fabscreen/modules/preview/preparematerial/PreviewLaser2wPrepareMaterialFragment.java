package com.snapmaker.fabscreen.modules.preview.preparematerial;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.orhanobut.logger.Logger;
import com.snapmaker.fabscreen.R;
import com.snapmaker.fabscreen.modules.common.CoordinateSystemPresenter;
import com.snapmaker.fabscreen.modules.common.MaterialThicknessWidgetPresenter;
import com.snapmaker.fabscreen.modules.preview.PreviewActivity;

import butterknife.OnClick;
import fabscreen.libraries.legacy.base.BaseFragment;
import fabscreen.libraries.legacy.base.BaseViewModel;

public class PreviewLaser2wPrepareMaterialFragment extends BaseFragment {
    public static PreviewLaser2wPrepareMaterialFragment newInstance() {
        return new PreviewLaser2wPrepareMaterialFragment();
    }

    private CoordinateSystemPresenter mCoordinateSystemPresenter;
    private MaterialThicknessWidgetPresenter mMaterialThicknessWidgetPresenter;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initView();
    }

    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_preview_laser_prepare_material;
    }

    @Override
    protected BaseViewModel getViewModel() {
        return null;
    }

    private void initView() {
        setTitle(R.string.laser_calibration_auto_focus);

        mMaterialThicknessWidgetPresenter = new MaterialThicknessWidgetPresenter(getContext(), getModel(), disposables);
        mMaterialThicknessWidgetPresenter.bind(getView());
        mMaterialThicknessWidgetPresenter.connectPreference();

        mCoordinateSystemPresenter = new CoordinateSystemPresenter(getContext(), getModel(), disposables);
        mCoordinateSystemPresenter.ensureCoordinate(1);
    }

    @OnClick(R.id.btn_preview_laser_prepare_material_next)
    void onClickNext() {
        final float thickness = mMaterialThicknessWidgetPresenter.getTargetValue();
        getModel().getPreferences().setLaserMaterialThickness(thickness);
        Logger.d("Set material %.1f mm.", thickness);

        if (getActivity()!= null) {
            ((PreviewActivity) getActivity()).gotoLaser2wPrepareSafetyGogglesFragment(true);
        }
    }
}
