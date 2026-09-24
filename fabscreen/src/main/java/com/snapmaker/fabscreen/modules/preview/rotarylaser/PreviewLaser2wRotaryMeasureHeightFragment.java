package com.snapmaker.fabscreen.modules.preview.rotarylaser;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.snapmaker.fabscreen.R;
import com.snapmaker.fabscreen.data.XYZMoveController;
import com.snapmaker.fabscreen.modules.common.ControlXYZPanelWidgetPresenter;
import com.snapmaker.fabscreen.modules.common.CoordinateSystemPresenter;
import com.snapmaker.fabscreen.modules.lasercalibration.LaserCalibrationViewModel;
import com.snapmaker.fabscreen.modules.preview.PreviewActivity;
import com.snapmaker.fabscreen.view.FabProgressDialog;

import java.util.Locale;

import butterknife.BindView;
import butterknife.OnClick;
import fabscreen.libraries.legacy.base.BaseFragment;
import fabscreen.libraries.legacy.data.ISlaveComputer;
import fabscreen.libraries.legacy.data.MockConst;
import fabscreen.libraries.legacy.lib.LogHelper;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.Subject;

public class PreviewLaser2wRotaryMeasureHeightFragment extends BaseFragment {
    public static PreviewLaser2wRotaryMeasureHeightFragment newInstance() {
        return new PreviewLaser2wRotaryMeasureHeightFragment();
    }

    @BindView(R.id.tv_toolhead_mask_text)
    TextView mMaskTextDesc;

    @BindView(R.id.top_bar_back)
    Button mBtnBack;
    @BindView(R.id.btn_laser_40w_touch_platform_complete)
    Button mBtnComplete;

    @BindView(R.id.widget_control_panel_xyz_axes)
    View mViewXYZControlPanel;

    @BindView(R.id.iv_laser_40w_touch_platform)
    ImageView mIvPlatformHeight;

    private FabProgressDialog mProgressDialog;
    private LaserCalibrationViewModel mLaserCalibrationViewModel;
    private ControlXYZPanelWidgetPresenter mControlPanelPresenter;
    private Subject<Boolean> mIsMovingSubject = BehaviorSubject.create();

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mLaserCalibrationViewModel = getViewModel();

    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setTitle(R.string.preview_laser_prepare_4axis_measure_height);

        initView();

    }

    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_laser_40w_rotary_touch_platform;
    }

    @Override
    protected LaserCalibrationViewModel getViewModel() {
        // Be aware we are using LaserCalibrationViewModel to get material diameter and length here.
        return getViewModelProvider().get(LaserCalibrationViewModel.class);
    }

    private void initView() {

        // init dialog.
        mProgressDialog = new FabProgressDialog(requireContext());
        mProgressDialog.setCancelOnTouchOutside(false);
        mProgressDialog.setMessage(R.string.dialog_warning_moving);

        mBtnComplete.setText(R.string.all_next);
        mMaskTextDesc.setText(R.string.preview_laser_40w_prepare_low_lever_and_touch_platform_desc);
        mIvPlatformHeight.setImageResource(R.drawable.pic_laser_2w_4axis_prepare_platform_height_360x320);

        setTitle(R.string.settings_laser_touch_platform_title);

        mControlPanelPresenter = new ControlXYZPanelWidgetPresenter(getContext(), getModel(), disposables);
        mControlPanelPresenter.bind(mViewXYZControlPanel, 0.1f, 1f, 5f);
        mControlPanelPresenter.connect();
        mControlPanelPresenter.getMovingEventObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(isMoving -> {
                    mIsMovingSubject.onNext(isMoving);
                });

        CoordinateSystemPresenter presenter = new CoordinateSystemPresenter(requireContext(), getModel(), disposables);
        presenter.ensureCoordinate(1);

        presenter.setOnCoordinateSwitchListener(this::initPosition);

        mIsMovingSubject
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(isMoving -> {
                    mBtnBack.setEnabled(!isMoving);
                    mBtnComplete.setEnabled(!isMoving);
                });
    }

    void initPosition() {
        mIsMovingSubject.onNext(true);
        // Reuse laser rotary measure height moving logic. (where implemented in LaserCalibration4AxisInstallMaterialFragment)
        int sizeX = getModel().getMachineController().getSizeX();
        int sizeY = getModel().getMachineController().getSizeY();
        int sizeZ = getModel().getMachineController().getSizeZ();
        float materialLength = mLaserCalibrationViewModel.getWorkpieceLength();

        float initialY = Math.min(sizeY - MockConst.ROTARY_MOCK_CHUCK_LENGTH - (2 * 10 + 1), sizeY - MockConst.ROTARY_MOCK_CHUCK_LENGTH - materialLength);



        // Make sure on CS#0 and then go to center for measuring height.
        getModel().getMachineController().updateCoordinateSystem(0)
                .flatMap(res -> getModel().getSlaveComputer().sendGcode(String.format(Locale.US, "G0 Z%d F1800", sizeZ)))
                .flatMap(res -> getModel().getSlaveComputer().sendGcode(String.format(Locale.US, "G0 X%.2f Y%.2f F3000", sizeX / 2f - 6, initialY)))
                .flatMap(ret -> getModel().getMachineController().updateCoordinateSystem(1))
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(success -> {
                    mIsMovingSubject.onNext(false);
                    dismissDialog(mProgressDialog);
                }, e -> {
                    mIsMovingSubject.onNext(false);
                    dismissDialog(mProgressDialog);
                    LogHelper.log(e);
                });
    }

    void moveByStep(XYZMoveController.Direction direction, float stepWidth) {
        mIsMovingSubject.onNext(true);
        XYZMoveController.getInstance()
                .moveByStep(direction, stepWidth)
                .as(bindToLifecycle())
                .subscribe(response -> mIsMovingSubject.onNext(false));
    }

    Observable<Boolean> setOriginZ() {
        mIsMovingSubject.onNext(true);
        return getModel().getSlaveComputer().setPosition(0, 0, 0, ISlaveComputer.FLAG_Z)
                .flatMap(response -> getModel().getMachineController().updateCoordinateSystem())
                .flatMap(update -> Observable.just(true))
                .doOnNext(next -> mIsMovingSubject.onNext(false));
    }

    @OnClick(R.id.btn_laser_40w_touch_platform_complete)
    void onClickNext() {
        setOriginZ()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(set -> {
                    ((PreviewActivity) requireActivity()).gotoLaser2wPullInFocusLeverFragment();
                }, LogHelper::log);
    }
}
