package com.snapmaker.fabscreen.modules.preview;

import static fabscreen.libraries.legacy.data.Constants.MACHINE_MODEL_SNAPMAKER_A150;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.orhanobut.logger.Logger;
import com.snapmaker.fabscreen.R;
import com.snapmaker.fabscreen.modules.common.CoordinateSystemPresenter;

import butterknife.BindView;
import butterknife.OnClick;
import fabscreen.libraries.legacy.base.BaseFragment;
import fabscreen.libraries.legacy.lib.LogHelper;
import io.reactivex.android.schedulers.AndroidSchedulers;

public class PreviewLaser40wSettingsIndicatorModeFragment extends BaseFragment {

    @BindView(R.id.rg_settings_indicator_mode_select)
    RadioGroup mRdIndicatorModeCheck;

    private int mIndicatorMode = 0;
    private PreviewViewModel mViewModel;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mViewModel = getViewModel();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initView();
    }

    private void initView() {
        setTitle(R.string.preview_laser_40w_settings_laser_indicator_title);

        mRdIndicatorModeCheck.setOnCheckedChangeListener((group, checkedId) -> {
            switch (checkedId) {
                case R.id.rb_settings_indicator_mode_crossline:
                    mIndicatorMode = 0;
                    break;
                case R.id.rb_settings_indicator_mode_laser_spot:
                    mIndicatorMode = 1;
                    break;
                default:
                    break;
            }
        });

        mIndicatorMode = getModel().getPreferences().getLaserIndicatorMode();
        mRdIndicatorModeCheck.check(mIndicatorMode == 0 ? R.id.rb_settings_indicator_mode_crossline
                : R.id.rb_settings_indicator_mode_laser_spot);
    }

    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_preview_40w_settings_indicator_mode;
    }

    @Override
    protected PreviewViewModel getViewModel() {
        return getViewModelProvider().get(PreviewViewModel.class);
    }

    @OnClick(R.id.btn_settings_indicator_mode_save)
    void onClickSave() {
        Logger.d("Setting indicator mode " + mIndicatorMode);
        mViewModel.setLaserIndicatorMode(mIndicatorMode);
        getModel().getPreferences().setLaserIndicatorMode(mIndicatorMode);
        back();
    }

}
