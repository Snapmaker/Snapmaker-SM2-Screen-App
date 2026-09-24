package com.snapmaker.fabscreen.modules.experiment;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import android.view.View;

import com.orhanobut.logger.Logger;
import com.snapmaker.fabscreen.R;

import butterknife.BindView;
import fabscreen.libraries.legacy.base.BaseFragment;
import fabscreen.libraries.legacy.base.BaseViewModel;
import fabscreen.libraries.legacy.view.SlidingRulerView;

public class ExperimentFragment extends BaseFragment {

    @BindView(R.id.srv_experiment)
    SlidingRulerView mSrvTest;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setTitle(R.string.all_experiment);
        mSrvTest.setShowStr("X");
        mSrvTest.setOnProgressChangeListener(index -> {
            Logger.d("index " + index);
        });
    }

    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_experiment;
    }

    @Override
    protected BaseViewModel getViewModel() {
        return null;
    }
}
