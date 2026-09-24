package com.snapmaker.fabscreen.modules.guide3dp.calibration;

import android.app.Activity;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.widget.Button;
import android.widget.GridView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.orhanobut.logger.Logger;
import com.snapmaker.fabscreen.R;
import com.snapmaker.fabscreen.modules.calibration.CalibrationPanelAdapter;
import com.snapmaker.fabscreen.modules.calibration.CalibrationViewModel;
import com.snapmaker.fabscreen.modules.guide3dp.Guide3DPActivity;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.OnClick;
import co.ceryle.segmentedbutton.SegmentedButtonGroup;
import fabscreen.libraries.legacy.base.BaseFragment;
import fabscreen.libraries.legacy.data.calibration.CalibrationPoint;
import fabscreen.libraries.legacy.lib.LogHelper;
import fabscreen.libraries.legacy.view.FabConfirm;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;

public class Guide3DPCalibrationFragment extends BaseFragment {
    public static Guide3DPCalibrationFragment newInstance() {
        return new Guide3DPCalibrationFragment();
    }

    @BindView(R.id.top_bar_back)
    Button mBtnBack;

    @BindView(R.id.gv_calibration)
    GridView mGvCalibrationPanel;

    @BindView(R.id.rl_calibration_panel_z_offset)
    View mViewAdjustZOffset;

    @BindView(R.id.sbg_calibration_steps)
    SegmentedButtonGroup mSbgSteps;

    @BindView(R.id.btn_calibration_move_up)
    Button mBtnMoveUp;
    @BindView(R.id.btn_calibration_move_down)
    Button mBtnMoveDown;
    @BindView(R.id.btn_calibration_next_point)
    Button mBtnNextPoint;

    @BindView(R.id.btn_calibration_save)
    Button mBtnSave;

    private double mStep = 0.1;

    private CalibrationViewModel mViewModel;
    private CalibrationPanelAdapter mAdapter;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mViewModel = getViewModel();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initView();

        // start calibration
        mViewModel.startCalibration();
    }

    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_calibration;
    }

    @Override
    protected CalibrationViewModel getViewModel() {
        return getViewModelProvider().get(CalibrationViewModel.class);
    }

    private void initView() {
        setTitle(R.string.calibration_intro_calibrate_the_bed);
        mBtnSave.setText(R.string.all_next);
        mBtnNextPoint.setVisibility(View.GONE);

        // Use auto calibration mode
        mViewModel.setAutoMode(true);
        final int gridCount = mViewModel.getGridCount();

        bindButtons();

        mAdapter = new CalibrationPanelAdapter(getContext());
        ArrayList<CalibrationPoint> points = mViewModel.initPoints();
        mAdapter.setPoints(points);
        mAdapter.setOnPointClickListener(point -> mViewModel.gotoPointManual(point.getViewOrder()));

        initCalibrationPanel(gridCount);

        mViewModel.getOffsetObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(isOffset -> {
                    mViewAdjustZOffset.setVisibility(isOffset ? View.VISIBLE : View.GONE);
                });

        mViewModel.getAutoCalibrationResultObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(isSuccess -> {
                    if (!isSuccess) {
                        showAutoCalibrationFailDialog();
                    }
                });

        Observable
                .combineLatest(mViewModel.getUpdatePointsObservable(),
                        mViewModel.getIsMovingObservable(),
                        (updated, isMoving) -> true)
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(updated -> mAdapter.notifyDataSetChanged());
    }

    private void showAutoCalibrationFailDialog() {
        FabConfirm.create(getContext())
                .setCanceledOnTouchOutSide(false)
                .setIcon(R.drawable.pic_dialog_warning_72x72)
                .setDescription(R.string.calibration_dialog_guide_auto_calibration_fail_desc)
                .setConfirm(R.string.all_retry, (dialog, which) -> {
                    dialog.dismiss();
                    //retry
                    mViewModel.retryCalibration();

                })
                .setCancel(R.string.all_skip, (dialog, which) -> {
                    dialog.dismiss();
                    //skip
                    skipCalibration();
                })
                .show();
    }

    /**
     * Skip calibration and go to PrepareFilamentIntroFragment
     * exit calibration -> jump
     */
    private void skipCalibration() {
        mViewModel.exitCalibration()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(success -> {
                    Activity activity = getActivity();
                    if (activity instanceof Guide3DPActivity) {
                        ((Guide3DPActivity) activity).startPrepareFilamentIntroFragment();
                    }
                });
    }

    private void bindButtons() {
        // disable buttons in autoMode

        // Step buttons
        mSbgSteps.setOnClickedButtonPosition(position -> {
            switch (position) {
                case 0:
                    mStep = 0.05;
                    break;
                case 1:
                    mStep = 0.1;
                    break;
                case 2:
                    mStep = 0.5;
                    break;
            }
        });

        mViewModel.getIsMovingObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(isMoving -> {
                    mBtnBack.setEnabled(!isMoving);
                    mSbgSteps.setEnabled(!isMoving);
                    mBtnMoveUp.setEnabled(!isMoving);
                    mBtnMoveDown.setEnabled(!isMoving);
                    mBtnNextPoint.setEnabled(!isMoving && !mViewModel.isAllPointSelected());
                    mBtnSave.setEnabled(!isMoving && mViewModel.isAllPointSelected() && mViewModel.isAdjustZOffset());
                    mAdapter.setButtonsEnabled(!isMoving && !mViewModel.isAutoMode());
                });
    }

    private void initCalibrationPanel(int gridCount) {
        mGvCalibrationPanel.setAdapter(mAdapter);
        mGvCalibrationPanel.setNumColumns(gridCount);

        // Use gridCount to calculate spacing.
        final int spacing = (280 - (40 * gridCount)) / (gridCount - 1);
        mGvCalibrationPanel.setVerticalSpacing(dp2px(spacing));
        mGvCalibrationPanel.setHorizontalSpacing(dp2px(spacing));
        switch (gridCount) {
            case 4:
                mGvCalibrationPanel.setBackgroundResource(R.drawable.pic_3dp_calibration_16point_280x280);
                break;
            case 5:
                mGvCalibrationPanel.setBackgroundResource(R.drawable.pic_3dp_calibration_25point_280x280);
                break;
            case 3:
            default:
                mGvCalibrationPanel.setBackgroundResource(R.drawable.pic_3dp_calibration_9point_280x280);
                break;
        }
    }

    private int dp2px(float dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics());
    }

    @OnClick(R.id.btn_calibration_move_up)
    void onClickUp() {
        mViewModel.adjustCalibrationPoint(mStep);
    }

    @OnClick(R.id.btn_calibration_move_down)
    void onClickDown() {
        mViewModel.adjustCalibrationPoint(-mStep);
    }

    @OnClick(R.id.btn_calibration_save)
    void onClickSave() {
        mViewModel.saveCalibration()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(s -> {
                    Logger.d("3DP Calibration data saved.");
                    if (getActivity() != null) {
                        ((Guide3DPActivity) getActivity()).startPrepareFilamentIntroFragment();
                    }
                }, LogHelper::log);
    }

    @Override
    protected void back() {
        mViewModel.exitCalibration()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(success -> {
                    Logger.i("Guide 3DP calibration canceled.");
                    super.back();
                }, LogHelper::log);
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden) {
            // Save(Next) button should be enabled,
            // Up/Down button should be disabled,
            // Adjust z offset view should be gone
            // when back from Guide3DPPrepareFilamentIntroFragment.
            mBtnSave.setEnabled(true);
            mBtnMoveUp.setEnabled(false);
            mBtnMoveDown.setEnabled(false);
            mViewAdjustZOffset.setVisibility(View.GONE);
        }
    }
}
