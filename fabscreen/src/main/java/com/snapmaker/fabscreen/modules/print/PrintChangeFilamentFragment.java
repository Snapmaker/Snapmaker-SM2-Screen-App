package com.snapmaker.fabscreen.modules.print;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.snapmaker.fabscreen.R;
import fabscreen.libraries.legacy.base.BaseFragment;
import fabscreen.libraries.legacy.base.BaseViewModel;
import com.snapmaker.fabscreen.modules.common.LoadFilamentWidgetPresenter;
import com.snapmaker.fabscreen.modules.common.NozzleWidgetPresenter;
import fabscreen.libraries.legacy.view.RulerView;

import butterknife.BindView;
import butterknife.OnClick;
import io.reactivex.android.schedulers.AndroidSchedulers;

public class PrintChangeFilamentFragment extends BaseFragment {
    private static final String TAG = PrintChangeFilamentFragment.class.getSimpleName();

    @BindView(R.id.top_bar_back)
    Button mIbBack;

    @BindView(R.id.rv_widget_set_value_ruler_ruler)
    RulerView mRvRuler;
    @BindView(R.id.btn_print_change_filament_complete)
    Button mBtnComplete;

    private NozzleWidgetPresenter mNozzleWidgetPresenter;
    private LoadFilamentWidgetPresenter mLoadFilamentWidgetPresenter;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setTitle(R.string.print_change_filament);

        initView();
        initData();
    }

    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_print_change_filament;
    }

    @Override
    protected BaseViewModel getViewModel() {
        return null;
    }

    private void initView() {
        mIbBack.setVisibility(View.GONE);

        mNozzleWidgetPresenter = new NozzleWidgetPresenter(getContext(), getModel(), disposables);
        mNozzleWidgetPresenter.bind(getView());
        mNozzleWidgetPresenter.connectMachineStatus();

        mLoadFilamentWidgetPresenter = new LoadFilamentWidgetPresenter(getContext(), getModel(), disposables);
        mLoadFilamentWidgetPresenter.bind(getView());
        mLoadFilamentWidgetPresenter.connect();

        mLoadFilamentWidgetPresenter.getIsLoadingObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(isMoving -> {
                    mRvRuler.setEnabled(!isMoving);
                    mBtnComplete.setEnabled(!isMoving);
                });
    }

    private void initData() {
        mNozzleWidgetPresenter.setTargetValue(200);
    }

    @OnClick(R.id.btn_print_change_filament_complete)
    void onClickComplete() {
        getModel().getPrintController().setResume();
        back();
    }
}
