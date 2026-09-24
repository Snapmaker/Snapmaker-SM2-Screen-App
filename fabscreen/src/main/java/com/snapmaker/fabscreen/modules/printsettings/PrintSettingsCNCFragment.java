package com.snapmaker.fabscreen.modules.printsettings;

import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.snapmaker.fabscreen.R;
import fabscreen.libraries.legacy.base.BaseFragment;
import fabscreen.libraries.legacy.base.BaseViewModel;
import com.snapmaker.fabscreen.modules.common.FeedRateWidgetPresenter;

import butterknife.BindView;

public class PrintSettingsCNCFragment extends BaseFragment {
    private static final String TAG = "PrintSettingsCNC";

    @BindView(R.id.view_print_settings_cnc_page_feed_rate)
    View mViewPageFeedRate;

    private FeedRateWidgetPresenter mFeedRateWidgetPresenter;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setTitle(R.string.print_settings_adjust_settings);

        Log.d(TAG, "enter fragment");

        initFeedRatePage();
    }

    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_print_settings_cnc;
    }

    @Override
    protected BaseViewModel getViewModel() {
        return null;
    }

    private void initFeedRatePage() {
        mFeedRateWidgetPresenter = new FeedRateWidgetPresenter(getContext(), getModel(), disposables);
        mFeedRateWidgetPresenter.bind(mViewPageFeedRate);
        mFeedRateWidgetPresenter.connectPrintSettings();
    }
}
