package com.snapmaker.fabscreen.modules.cncoriginassistant;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.snapmaker.fabscreen.R;
import fabscreen.libraries.legacy.base.BaseFragment;
import fabscreen.libraries.legacy.base.BaseViewModel;

import butterknife.BindView;
import butterknife.OnClick;
import fabscreen.libraries.legacy.lib.LogHelper;
import io.reactivex.android.schedulers.AndroidSchedulers;

public class CNCOriginAssistantSafetyGogglesFragment extends BaseFragment {
    public static CNCOriginAssistantSafetyGogglesFragment newInstance() {
        return new CNCOriginAssistantSafetyGogglesFragment();
    }

    @BindView(R.id.tv_preview_laser_prepare_safety_goggles_title)
    TextView mTvTitle;
    @BindView(R.id.tv_preview_laser_prepare_safety_goggles_message)
    TextView mTvContent;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initView();
    }

    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_preview_laser_prepare_safety_goggles;
    }

    @Override
    protected BaseViewModel getViewModel() {
        return null;
    }

    private void initView() {
        mTvTitle.setText(R.string.all_safety_goggles);
        mTvContent.setText(R.string.guide_cnc_safety_goggles_content);
    }

    private AlertDialog showMachineMovingDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext(), R.style.AppTheme_Dialog);
        AlertDialog dialog = builder.create();
        dialog.setCanceledOnTouchOutside(false);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(R.color.default_theme_dark_mask);
            dialog.getWindow().setLayout(280 * 2, WindowManager.LayoutParams.WRAP_CONTENT);
        }

        LayoutInflater inflater = LayoutInflater.from(getContext());
        View view = inflater.inflate(R.layout.dialog_cnc_origin_assistant_set_origin_moving, null);
        dialog.setView(view);
        dialog.show();

        return dialog;
    }

    private void gotoOriginAssistantSetOriginFragment() {
        if (getActivity() != null) {
            ((CNCOriginAssistantActivity) getActivity()).gotoCNCOriginAssistantSetOriginFragment();
        }
    }

    @OnClick(R.id.btn_preview_laser_prepare_safety_goggles_next)
    void onClickNext() {
        if (getActivity() != null) {

            AlertDialog dialog = showMachineMovingDialog();
            getModel().getMachineController().updateCoordinateSystem(0)
                    .flatMap(response -> getModel().getSlaveComputer().sendGcode("G28"))
                    .flatMap(ret -> getModel().getMachineController().updateCoordinateSystem(1))
                    .observeOn(AndroidSchedulers.mainThread())
                    .as(bindToLifecycle())
                    .subscribe(ret -> {
                        dialog.dismiss();
                        gotoOriginAssistantSetOriginFragment();
                    }, e -> {
                        dialog.dismiss();
                        LogHelper.log(e);
                    });
        }
    }
}
