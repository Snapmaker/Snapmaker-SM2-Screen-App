package com.snapmaker.fabscreen.modules.preview.rotarylaser;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.snapmaker.fabscreen.R;
import fabscreen.libraries.legacy.base.BaseFragment;
import fabscreen.libraries.legacy.base.BaseViewModel;
import com.snapmaker.fabscreen.modules.preview.PreviewActivity;

import butterknife.OnClick;
import fabscreen.libraries.legacy.data.Constants;

public class PreviewLaserRotaryInstallMaterialFragment extends BaseFragment {
    public static PreviewLaserRotaryInstallMaterialFragment newInstance() {
        return new PreviewLaserRotaryInstallMaterialFragment();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setTitle(R.string.cnc_origin_assistant_install_material_title);
    }

    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_preview_laser_prepare_install_material;
    }

    @Override
    protected BaseViewModel getViewModel() {
        return null;
    }

    @OnClick(R.id.btn_preview_laser_install_material_next)
    void onClickNext() {
        if (getActivity() != null) {
            switch (getModel().getMachineController().getHeadType()) {
                case Constants.HEAD_LASER:
                    ((PreviewActivity) getActivity()).gotoLaserPrepareSafetyGogglesFragment(true);
                    break;
                case Constants.HEAD_LASER_10W:
                    ((PreviewActivity) getActivity()).gotoLaserRotaryMeasureHeightIntro();
                    break;
                case Constants.HEAD_LASER_20W:
                case Constants.HEAD_LASER_40W:
                    ((PreviewActivity) getActivity()).gotoLaser40wRotaryMeasureHeight();
                    break;
                case Constants.HEAD_LASER_2W_IR:
                    ((PreviewActivity) getActivity()).gotoLaser2wRotaryMeasureHeight();
                    break;
                default:
                    break;

            }
        }
    }
}
