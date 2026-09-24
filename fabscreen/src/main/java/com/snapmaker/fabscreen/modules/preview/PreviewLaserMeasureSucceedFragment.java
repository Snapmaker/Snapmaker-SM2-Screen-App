package com.snapmaker.fabscreen.modules.preview;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.snapmaker.fabscreen.R;

import butterknife.BindView;
import butterknife.OnClick;
import fabscreen.libraries.legacy.base.BaseFragment;
import io.reactivex.android.schedulers.AndroidSchedulers;

public class PreviewLaserMeasureSucceedFragment extends BaseFragment {

    private PreviewViewModel mViewModel;

    @BindView(R.id.top_bar_back)
    Button mBtnBack;
    @BindView(R.id.btn_measure_succeed_next)
    Button mBtnNext;
    @BindView(R.id.tv_measure_succeed_desc)
    TextView mTvDesc;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mViewModel = getViewModel();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mTvDesc.setText(getString(R.string.preview_auto_measure_thickness_succeed_desc, mViewModel.getMeasuredThicknessString()));

        mViewModel.getResultBackObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(settled -> {
                    if (!settled) return;
                    requireFragmentManager().popBackStack(PreviewLaserMeasureThicknessFragment.class.getSimpleName(), 0);
                });
        mViewModel.getIsMovingObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(isMoving -> {
                    mBtnBack.setEnabled(!isMoving);
                    mBtnNext.setEnabled(!isMoving);
                });
    }

    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_laser_measure_succeed;
    }

    @Override
    protected PreviewViewModel getViewModel() {
        return getViewModelProvider().get(PreviewViewModel.class);
    }

    @OnClick(R.id.btn_measure_succeed_next)
    void onNextClicked(View view) {
        mBtnBack.setEnabled(false);
        view.setEnabled(false);
        mViewModel.liftToolhead(true)
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(success -> {
                    mBtnBack.setEnabled(true);
                    view.setEnabled(true);
                    if (!success) return;
                    PreviewActivity activity = (PreviewActivity) requireActivity();
                    activity.gotoLaserPrepareSafetyGogglesFragment(true);
                });
    }

    @Override
    protected void back() {
        mViewModel.initCameraPosition(true);
    }
}
