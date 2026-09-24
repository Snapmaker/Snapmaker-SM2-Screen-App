package com.snapmaker.fabscreen.modules.cncoriginassistant;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.snapmaker.fabscreen.R;
import fabscreen.libraries.legacy.base.BaseFragment;
import fabscreen.libraries.legacy.base.BaseViewModel;

import butterknife.BindView;
import butterknife.OnClick;

public class CNCOriginAssistantInstallMaterialFragment extends BaseFragment {
    public static CNCOriginAssistantInstallMaterialFragment newInstance() {
        return new CNCOriginAssistantInstallMaterialFragment();
    }

    @BindView(R.id.iv_prepare_install_material)
    ImageView mIvCover;
    @BindView(R.id.tv_laser_prepare_install_material_content)
    TextView mTvContent;
    @BindView(R.id.tv_laser_prepare_install_material_tailstock_warning)
    TextView mTvTailstockWarning;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setTitle(R.string.cnc_origin_assistant_install_material_title);

        mIvCover.setImageResource(R.drawable.pic_cnc_origin_assistant_install_material_360x240);
        mTvContent.setText(R.string.cnc_origin_assistant_install_material_content);
        mTvTailstockWarning.setVisibility(TextView.VISIBLE);
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
            ((CNCOriginAssistantActivity) getActivity()).gotoCNCOriginAssistantSetCarvingToolFragment();
        }
    }
}
