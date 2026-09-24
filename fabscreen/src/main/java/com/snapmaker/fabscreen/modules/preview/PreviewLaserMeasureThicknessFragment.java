package com.snapmaker.fabscreen.modules.preview;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.snapmaker.fabscreen.R;
import com.snapmaker.fabscreen.view.FabProgressDialog;

import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.OnClick;
import fabscreen.libraries.legacy.base.BaseFragment;
import fabscreen.libraries.legacy.view.FabConfirm;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class PreviewLaserMeasureThicknessFragment extends BaseFragment {
    private PreviewViewModel mViewModel;

    @BindView(R.id.btn_place_material_next)
    Button mBtnNext;
    private FabProgressDialog mProgressDialog;
    private Disposable mDelayDisposable;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mViewModel = getViewModel();

    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initView();

        mViewModel.getMeasureResultObservable()
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(gotResult -> {
                    mBtnNext.setEnabled(true);
                    dismissDialog(mProgressDialog);
                    processBasedOnResult(gotResult);
                });
    }

    private void processBasedOnResult(Boolean gotResult) {
        if (gotResult) {
            PreviewActivity activity = (PreviewActivity) requireActivity();
            activity.gotoLaserPrepareMeasureSucceedFragment();
        } else {
            FabConfirm.create(requireContext())
                    .setIcon(R.drawable.pic_dialog_failed_72x72)
                    .setDescription(R.string.preview_auto_measure_thickness_fail_dialog_desc)
                    .setConfirm(R.string.all_retry, (dialog, which) -> dialog.dismiss())
                    .setCancel(R.string.all_manual_mode, (dialog, which) -> {
                        dialog.dismiss();
                        PreviewActivity activity = (PreviewActivity) requireActivity();
                        activity.gotoLaserPrepareModeNoteFragment(false);
                    })
                    .show();

        }
    }

    private void initView() {
        setTitle(R.string.preview_auto_measure_material_thickness_title);
        mProgressDialog = new FabProgressDialog(requireContext());
        mProgressDialog.setMessage(R.string.preview_auto_measure_loading_msg);
    }

    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_measure_material_thickness;
    }

    @Override
    protected PreviewViewModel getViewModel() {
        return getViewModelProvider().get(PreviewViewModel.class);
    }

    @OnClick(R.id.btn_place_material_next)
    void onNextClick() {
        mBtnNext.setEnabled(false);
        showDialog(mProgressDialog);
        mViewModel.autoMeasureMaterialThickness();
    }

    @Override
    public void onResume() {
        super.onResume();
        // Enter this fragment first time
        mViewModel.switchAFAssistLight(true);
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        // Hide or show called, when another fragment is added, or back from another fragment.
        if (hidden) {
            mViewModel.switchAFAssistLight(false);
        } else {
            // Wait 200 millis, if we still in this fragment, switch on the light.
            // PopupStack method may "skip" this fragment to back to beneath fragments, but actually
            // the above fragments will be removed one by one, onHiddenChanged(true) will be called
            // "unexpectedly".
            if (isVisible()) {
                mDelayDisposable = AndroidSchedulers.mainThread().scheduleDirect(() -> mViewModel.switchAFAssistLight(true), 400, TimeUnit.MILLISECONDS);
            }
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        // leave this fragment
        mViewModel.switchAFAssistLight(false);
        if (mDelayDisposable == null) return;
        // If onStop is called, light won't need to be turned on after delay.
        mDelayDisposable.dispose();
    }

    @Override
    protected void back() {
        mViewModel.switchAFAssistLight(false);
        super.back();
    }
}
