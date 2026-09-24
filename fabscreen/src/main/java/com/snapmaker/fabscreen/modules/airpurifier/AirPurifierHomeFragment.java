package com.snapmaker.fabscreen.modules.airpurifier;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.snapmaker.fabscreen.R;
import fabscreen.libraries.legacy.base.BaseFragment;
import com.snapmaker.fabscreen.modules.common.AirPurifierControlWidgetPresenter;
import com.snapmaker.fabscreen.modules.enclosure.EnclosureViewModel;

import butterknife.BindView;

public class AirPurifierHomeFragment extends BaseFragment {
    public static AirPurifierHomeFragment newInstance() {
        return new AirPurifierHomeFragment();
    }

    @BindView(R.id.widget_add_on_air_purifier_panel)
    View mViewAirPurifier;

    private AirPurifierControlWidgetPresenter mAirPurifierControlWidgetPresenter;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setTitle(R.string.all_air_purifier);

        initView();
    }

    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_air_purifier_home;
    }

    @Override
    protected EnclosureViewModel getViewModel() {
        return getViewModelProvider().get(EnclosureViewModel.class);
    }

    private void initView() {
        mAirPurifierControlWidgetPresenter = new AirPurifierControlWidgetPresenter(getContext(), getModel(), disposables);
        mAirPurifierControlWidgetPresenter.bind(mViewAirPurifier);
        mAirPurifierControlWidgetPresenter.connectStatus();
    }

}
