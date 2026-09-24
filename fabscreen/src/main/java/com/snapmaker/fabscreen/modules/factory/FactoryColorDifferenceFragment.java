package com.snapmaker.fabscreen.modules.factory;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.snapmaker.fabscreen.R;
import fabscreen.libraries.legacy.base.BaseFragment;
import fabscreen.libraries.legacy.base.BaseViewModel;

public class FactoryColorDifferenceFragment extends BaseFragment {

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setTitle("Color Difference");
    }

    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_factory_color_difference;
    }

    @Override
    protected BaseViewModel getViewModel() {
        return null;
    }

}
