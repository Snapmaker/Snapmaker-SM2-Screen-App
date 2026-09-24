package com.snapmaker.fabscreen.modules.guiderotary.laser;

import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.snapmaker.fabscreen.R;
import fabscreen.libraries.legacy.base.BaseFragment;
import com.snapmaker.fabscreen.modules.lasercalibration.LaserCalibrationViewModel;

import butterknife.BindView;
import butterknife.OnClick;
import fabscreen.libraries.legacy.data.Constants;
import fabscreen.libraries.legacy.lib.EditTextHelper;
import io.reactivex.android.schedulers.AndroidSchedulers;

public class GuideRotaryLaserSetWorkpieceFragment extends BaseFragment {
    public static GuideRotaryLaserSetWorkpieceFragment newInstance() {
        return new GuideRotaryLaserSetWorkpieceFragment();
    }

    @BindView(R.id.et_laser_calibration_4axis_set_workpiece_diameter)
    EditText mEtWorkpieceDiameter;
    @BindView(R.id.et_laser_calibration_4axis_set_workpiece_length)
    EditText mEtWorkpieceLength;

    @BindView(R.id.tv_laser_calibration_4axis_set_workpiece_diameter_tip)
    TextView mTvDiameterTip;
    @BindView(R.id.tv_laser_calibration_4axis_set_workpiece_length_tip)
    TextView mTvLengthTip;

    @BindView(R.id.btn_laser_calibration_4axis_set_workpiece_next)
    Button mBtnNext;

    private LaserCalibrationViewModel mViewModel;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mViewModel = getViewModel();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setTitle(R.string.cnc_origin_assistant_material_settings);

        initView();

        initData();
    }

    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_laser_calibration_4axis_set_workpiece;
    }

    @Override
    protected LaserCalibrationViewModel getViewModel() {
        return getViewModelProvider().get(LaserCalibrationViewModel.class);
    }

    @Override
    protected void back() {
        hideKeyboard();
        super.back();
    }

    private void hideKeyboard() {
        if (getView() == null) return;
        if (getContext() == null) return;

        InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null && imm.isActive()) {
            imm.hideSoftInputFromWindow(getView().getApplicationWindowToken(), 0);
        }
    }

    private void initView() {
        // We need to disabled next button first before user start inputting.
        mBtnNext.setEnabled(false);

        // Check params is ready
        mViewModel.getMaterialInputReady()
                .debounce(200, Constants.TIME_UNIT)
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(isReady -> mBtnNext.setEnabled(isReady));

        initEditText();

        // Skip the first value.
        mViewModel.getWorkpieceDiameterTipObservable()
                .skip(1)
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(tip -> {
                    switch (tip) {
                        case TIP_OK:
                            mTvDiameterTip.setVisibility(TextView.GONE);
                            mTvDiameterTip.setText("");
                            break;
                        case TIP_NOT_POSITIVE_NUMBER:
                            mTvDiameterTip.setVisibility(TextView.VISIBLE);
                            mTvDiameterTip.setText(R.string.cnc_origin_assistant_input_tip_not_positive);
                            break;
                        case TIP_EMPTY:
                            mTvDiameterTip.setVisibility(TextView.VISIBLE);
                            mTvDiameterTip.setText(R.string.cnc_origin_assistant_input_tip_empty);
                            break;
                        default:
                            break;
                    }
                });

        // Skip the first value.
        mViewModel.getWorkpieceLengthTipObservable()
                .skip(1)
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(tip -> {
                    switch (tip) {
                        case TIP_OK:
                            mTvLengthTip.setVisibility(TextView.GONE);
                            mTvLengthTip.setText("");
                            break;
                        case TIP_NOT_POSITIVE_NUMBER:
                            mTvLengthTip.setVisibility(TextView.VISIBLE);
                            mTvLengthTip.setText(R.string.cnc_origin_assistant_input_tip_not_positive);
                            break;
                        case TIP_EMPTY:
                            mTvLengthTip.setVisibility(TextView.VISIBLE);
                            mTvLengthTip.setText(R.string.cnc_origin_assistant_input_tip_empty);
                            break;
                        default:
                            break;
                    }
                });

    }

    private void initEditText() {
        // Limit the length of input.
        mEtWorkpieceLength.setFilters(new InputFilter[]{new InputFilter.LengthFilter(8)});
        mEtWorkpieceDiameter.setFilters(new InputFilter[]{new InputFilter.LengthFilter(8)});

        if (getViewModel().getWorkpieceDiameter() > 0) {
            String inputD = String.valueOf(getViewModel().getWorkpieceDiameter());
            mEtWorkpieceDiameter.setText(inputD);
            mViewModel.setWorkpieceDiameterPreInput(inputD);
        }

        if (getViewModel().getWorkpieceLength() > 0) {
            String inputL = String.valueOf(getViewModel().getWorkpieceLength());
            mEtWorkpieceLength.setText(inputL);
            mViewModel.setWorkpieceLengthPreInput(inputL);
        }
        mEtWorkpieceDiameter.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                String input = EditTextHelper.fixNumberInputSinglePoint(s).toString();
                mViewModel.setWorkpieceDiameterInput(input);
            }
        });

        mEtWorkpieceLength.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                String input = EditTextHelper.fixNumberInputSinglePoint(s).toString();
                mViewModel.setWorkpieceLengthInput(input);
            }
        });
    }

    private void initData() {
        // Set default data in guide laser
        mEtWorkpieceLength.setText(String.valueOf(75.0));
        mEtWorkpieceDiameter.setText(String.valueOf(40.0));
        mViewModel.setWorkpieceLengthInput(String.valueOf(75.0f));
        mViewModel.setWorkpieceDiameterInput(String.valueOf(40.0f));
    }

    @OnClick(R.id.btn_laser_calibration_4axis_set_workpiece_next)
    void onClickNext() {
        if (getActivity() != null) {
            ((GuideRotaryLaserActivity) getActivity()).startInstallMaterialFragment();
        }
    }
}
