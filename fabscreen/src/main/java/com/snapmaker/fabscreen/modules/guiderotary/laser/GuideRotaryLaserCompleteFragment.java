package com.snapmaker.fabscreen.modules.guiderotary.laser;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.orhanobut.logger.Logger;
import com.snapmaker.fabscreen.R;
import fabscreen.libraries.legacy.base.BaseFragment;
import fabscreen.libraries.legacy.base.BaseViewModel;

import butterknife.BindView;
import butterknife.OnClick;
import fabscreen.libraries.legacy.data.Constants;

public class GuideRotaryLaserCompleteFragment extends BaseFragment {
    public static GuideRotaryLaserCompleteFragment newInstance() {
        return new GuideRotaryLaserCompleteFragment();
    }

    @BindView(R.id.tv_guide_complete_content)
    TextView mTvContent;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initView();
    }

    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_guide_complete;
    }

    @Override
    protected BaseViewModel getViewModel() {
        return null;
    }

    private void initView() {
        mTvContent.setText(R.string.guide_laser_rotary_complete_content);
    }

    @OnClick(R.id.btn_guide_complete_next)
    void onClickNext() {
        Logger.i("Guide Rotary Laser finished.");
        switch (getModel().getMachineController().getHeadType()) {
            case Constants.HEAD_LASER:
                getModel().getPreferences().setMachineSetupRotaryLaser(true);
                break;
            case Constants.HEAD_LASER_2W_IR:
                getModel().getPreferences().setMachineSetupRotary2WLaser(true);
                break;
            case Constants.HEAD_LASER_10W:
                getModel().getPreferences().setMachineSetupRotary10WLaser(true);
                break;
            case Constants.HEAD_LASER_20W:
                getModel().getPreferences().setMachineSetupRotary20WLaser(true);
                break;
            case Constants.HEAD_LASER_40W:
                getModel().getPreferences().setMachineSetupRotary40WLaser(true);
                break;
            default:
                break;
        }

        if (getActivity() != null) {
            getActivity().finish();
        }
    }
}
