package com.snapmaker.fabscreen.modules.dualextrudercalibration.bedleveling.manual;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.snapmaker.fabscreen.R;

import butterknife.OnClick;
import fabscreen.libraries.legacy.base.BaseFragment;

public class ManualLevelingCompleteFragment extends BaseFragment {
    public static Fragment newInstance() {
        return new ManualLevelingCompleteFragment();
    }

    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_manaul_leveling_complete;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (mBtnTopBarBack != null) {
            mBtnTopBarBack.setVisibility(View.INVISIBLE);
        }
    }

    @OnClick(R.id.btn_complete)
    void onCompleteClicked() {
        requireActivity().finish();
    }
}
