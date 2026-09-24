package com.snapmaker.fabscreen.modules.factory;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.snapmaker.fabscreen.R;
import fabscreen.libraries.legacy.base.BaseFragment;
import fabscreen.libraries.legacy.base.BaseViewModel;

import butterknife.BindView;
import butterknife.OnClick;

public class FactoryTouchPanelTestFragment extends BaseFragment {
    private static final String TAG = FactoryTouchPanelTestFragment.class.getSimpleName();

    @BindView(R.id.dsv_factory_view)
    DragSurfaceView mDragSurfaceView;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mDragSurfaceView = new DragSurfaceView(getContext(), null);
    }

    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_factory_touch_panel_test;
    }

    @Override
    protected BaseViewModel getViewModel() {
        return null;
    }

    @OnClick(R.id.btn_factory_touch_panel_back)
    void onClickBack() {
        if (mDragSurfaceView != null) {
            mDragSurfaceView.surfaceDestroyed(mDragSurfaceView.getHolder());
        }
        if (getActivity() != null) {
            getActivity().onBackPressed();
        }
    }
}
