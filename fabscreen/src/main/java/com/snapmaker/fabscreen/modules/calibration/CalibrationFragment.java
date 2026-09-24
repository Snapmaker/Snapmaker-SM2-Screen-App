package com.snapmaker.fabscreen.modules.calibration;

import android.content.Intent;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.orhanobut.logger.Logger;
import com.snapmaker.fabscreen.R;
import com.snapmaker.fabscreen.modules.home.HomeActivity;
import com.snapmaker.fabscreen.router.Router;

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

public class CalibrationFragment extends BaseFragment {
    public static CalibrationFragment newInstance() {
        return new CalibrationFragment();
    }

    @BindView(R.id.top_bar_back)
    Button mBtnBack;

    @BindView(R.id.gv_calibration)
    GridView mGvCalibrationPanel;

    @BindView(R.id.rl_calibration_panel_z_offset)
    View mViewAdjustZOffset;
    @BindView(R.id.tv_calibration_z_offset_heated_bed_warning)
    TextView mTvZOffsetHeatedBedWarning;
    @BindView(R.id.iv_calibration_panel_z_offset)
    ImageView mIvZOffsetCover;

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

    private CalibrationViewModel mViewModel;
    private CalibrationPanelAdapter mAdapter;

    private double mStep = 0.1;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mViewModel = getViewModel();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initView();

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

    @Override
    protected void back() {
        // Need to pop up confirm dialog if exiting calibration.
        FabConfirm.create(getContext())
                .setCanceledOnTouchOutSide(false)
                .setDescription(mViewModel.isAutoMode() ? R.string.calibration_exit_auto_leveling_confirm_desc
                        : R.string.calibration_exit_manual_leveling_confirm_desc)
                .setConfirm(R.string.all_ok, (dialog, which) -> {
                    dialog.dismiss();

                    if (mViewModel.isHeatedLevelingOn()) {
                        mViewModel.turnOffBed();
                    }
                    quitCalibration();
                })
                .setCancel(R.string.all_cancel, (dialog, which) -> {
                    dialog.dismiss();
                }).show();
    }

    private void initView() {
        boolean needBackHome = requireActivity().getIntent().getBooleanExtra(Router.IntentKeys.NEED_BACK_HOME, false);
        mBtnSave.setText(needBackHome ? R.string.all_save : R.string.all_complete);
        boolean isAutoMode = mViewModel.isAutoMode();
        boolean isHeatedLeveling = mViewModel.isHeatedLevelingOn();
        final int gridCount = mViewModel.getGridCount();
        if (isAutoMode) {
            setTitle(R.string.calibration_auto_leveling);
            mViewModel.getOffsetObservable()
                    .observeOn(AndroidSchedulers.mainThread())
                    .as(bindToLifecycle())
                    .subscribe(isOffset -> {
                        mViewAdjustZOffset.setVisibility(isOffset ? View.VISIBLE : View.GONE);
                    });

            mTvZOffsetHeatedBedWarning.setVisibility(isHeatedLeveling ? TextView.VISIBLE : TextView.GONE);
            mIvZOffsetCover.setBackgroundResource(isHeatedLeveling ? R.drawable.pic_calibration_heated_leveling_adjust_zoffset_240x80
                    : R.drawable.pic_calibration_adjust_zoffset_240x80);
        } else {
            setTitle(R.string.calibration_manual_leveling);

            if (isHeatedLeveling) {
                showUpHeatedBedWarningDialog();
            }
        }
        mBtnNextPoint.setVisibility(isAutoMode ? View.GONE : View.VISIBLE);
        bindButtons();

        mAdapter = new CalibrationPanelAdapter(getContext());
        ArrayList<CalibrationPoint> points = mViewModel.initPoints();
        mAdapter.setPoints(points);
        mAdapter.setOnPointClickListener(point -> {
            mViewModel.gotoPointManual(point.getCoordinateOrder());
        });

        initCalibrationPanel(gridCount);

        Observable
                .combineLatest(mViewModel.getUpdatePointsObservable(),
                        mViewModel.getIsMovingObservable(),
                        (updated, isMoving) -> true)
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(updated -> mAdapter.notifyDataSetChanged());

        mViewModel.getAutoCalibrationResultObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(isSuccess -> {
                    if (!isSuccess) {
                        showAutoCalibrationFailDialog();
                    }
                });
    }

    private void showAutoCalibrationFailDialog() {
        FabConfirm.create(getContext())
                .setCanceledOnTouchOutSide(false)
                .setIcon(R.drawable.pic_dialog_warning_72x72)
                .setDescription(R.string.calibration_dialog_auto_calibration_fail_desc)
                .setConfirm(R.string.all_retry, (dialog, which) -> {
                    dialog.dismiss();
                    //retry
                    mViewModel.retryCalibration();

                })
                .setCancel(R.string.all_quit, (dialog, which) -> {
                    dialog.dismiss();
                    //quit
                    quitCalibration();
                })
                .show();
    }

    private void quitCalibration() {
        mViewModel.exitCalibration()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(success -> requireActivity().finish(), LogHelper::log);
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

    private void bindButtons() {
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

        // bind isMoving Event
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

    private void showUpHeatedBedWarningDialog() {
        FabConfirm.create(getContext())
                .setDescription(R.string.calibration_dialog_z_offset_heated_bed_warning_desc)
                .setConfirm(R.string.all_confirm, ((dialog, which) -> {
                    dialog.dismiss();
                }))
                .show();
    }

    @OnClick(R.id.btn_calibration_move_up)
    void onClickUp() {
        mViewModel.adjustCalibrationPoint(mStep);
    }

    @OnClick(R.id.btn_calibration_move_down)
    void onClickDown() {
        mViewModel.adjustCalibrationPoint(-mStep);
    }

    @OnClick(R.id.btn_calibration_next_point)
    void onClickNext() {
        mViewModel.checkNextPointManual();
    }

    @OnClick(R.id.btn_calibration_save)
    void onClickSave() {
        mViewModel.saveCalibration()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(s -> {
                    Logger.d("3DP Calibration Saved.");

                    if (mViewModel.isHeatedLevelingOn()) {
                        // Set temperature into preferences.
                        final float heatedLevelingTemp = mViewModel.getHeatedLevelingTemperature();
                        getModel().getPreferences().set3DPCalibrationHeatedUpTemperature(heatedLevelingTemp);

                        // Turn off heated bed for safe when calibration was done already.
                        mViewModel.turnOffBed();
                    }

                    Intent intent = requireActivity().getIntent();
                    if (intent != null && intent.getBooleanExtra(Router.IntentKeys.NEED_BACK_HOME, false)) {
                        backToHome(HomeActivity.class);
                    } else {
                        finishWithResultOk();
                    }
                }, LogHelper::log);
    }
}
