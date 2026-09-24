package com.snapmaker.fabscreen.modules.preview.rotarycnc;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.snapmaker.fabscreen.R;
import fabscreen.libraries.legacy.base.BaseFragment;
import fabscreen.libraries.legacy.base.BaseViewModel;
import com.snapmaker.fabscreen.modules.preview.PreviewActivity;

import butterknife.OnClick;

public class PreviewCNCPrepareRotaryOriginRemindFragment extends BaseFragment {

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_preview_cnc_origin_remind;
    }

    @Override
    protected BaseViewModel getViewModel() {
        return null;
    }

    @OnClick(R.id.btn_preview_cnc_rotary_origin_remind_next)
    void onClickNext() {
        PreviewActivity activity = (PreviewActivity) getActivity();

        if (activity != null) {
            activity.gotoCNCPrepareSafetyGogglesFragment();
        }
    }
}
