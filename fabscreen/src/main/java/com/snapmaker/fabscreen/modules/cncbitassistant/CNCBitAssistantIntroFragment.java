package com.snapmaker.fabscreen.modules.cncbitassistant;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.snapmaker.fabscreen.R;
import fabscreen.libraries.legacy.base.BaseFragment;
import fabscreen.libraries.legacy.base.BaseViewModel;
import com.snapmaker.fabscreen.modules.common.CoordinateSystemPresenter;

import butterknife.OnClick;

public class CNCBitAssistantIntroFragment extends BaseFragment {
    public static CNCBitAssistantIntroFragment newInstance() {
        return new CNCBitAssistantIntroFragment();
    }

    private CoordinateSystemPresenter mCoordinateSystemPresenter;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setTitle(R.string.cnc_bit_assistant);

        mCoordinateSystemPresenter = new CoordinateSystemPresenter(getContext(), getModel(), disposables);
        mCoordinateSystemPresenter.ensureCoordinate(1);
    }

    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_cnc_bit_assistant_intro;
    }

    @Override
    protected BaseViewModel getViewModel() {
        return null;
    }

    @OnClick(R.id.btn_cnc_bit_assistant_intro_next)
    void onClickNext() {
        if (getActivity() != null) {
            ((CNCBitAssistantActivity) getActivity()).gotoCNCBitAssistantSafetyGoggles();
        }
    }
}
