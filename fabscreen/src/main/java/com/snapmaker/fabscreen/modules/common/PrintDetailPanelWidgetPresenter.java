package com.snapmaker.fabscreen.modules.common;


import android.view.View;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.snapmaker.fabscreen.R;

import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import fabscreen.libraries.legacy.lib.DateHelper;

public class PrintDetailPanelWidgetPresenter {
    // 3dp
    @BindView(R.id.tv_widget_nozzle_temp_value)
    TextView mTvNozzleTemp;

    @BindView(R.id.tv_widget_heated_bed_temp_value)
    TextView mTvHeatedBedTemp;

    @BindView(R.id.tv_widget_3dp_work_speed_value)
    TextView mTv3DPWorkSpeed;

    @BindView(R.id.tv_widget_3dp_estimated_time_title)
    TextView mTv3DPEstimatedTimeTitle;
    @BindView(R.id.tv_widget_3dp_estimated_time_value)
    TextView mTv3DPEstimatedTime;

    @Nullable
    @BindView(R.id.tv_widget_left_nozzle_temp_value)
    TextView mTv3DPDualExtruderLeftNozzleTemp;
    @Nullable
    @BindView(R.id.tv_widget_right_nozzle_temp_value)
    TextView mTv3DPDualExtruderRightNozzleTemp;

    @Nullable
    @BindView(R.id.tv_widget_3dp_left_work_speed_value)
    TextView mTv3DPDualExtruderLeftWorkSpeed;

    @Nullable
    @BindView(R.id.tv_widget_3dp_dual_extruder_heated_bed_temp_value)
    TextView mTv3DPDualExtruderHeatedBedTemp;

    @Nullable
    @BindView(R.id.tv_widget_3dp_dual_extruder_estimated_time_title)
    TextView mTv3DPDualExtruderEstimatedTimeTitle;
    @Nullable
    @BindView(R.id.tv_widget_3dp_dual_extruder_estimated_time_value)
    TextView mTv3DPDualExtruderEstimatedTime;

    // laser
    @BindView(R.id.tv_widget_laser_power_value)
    TextView mTvLaserPower;

    @BindView(R.id.tv_widget_laser_work_speed_value)
    TextView mTvLaserWorkSpeed;

    @BindView(R.id.tv_widget_laser_estimated_time_title)
    TextView mTvLaserEstimatedTimeTitle;
    @BindView(R.id.tv_widget_laser_estimated_time_value)
    TextView mTvLaserEstimatedTime;

    // cnc
    @BindView(R.id.tv_widget_spindle_speed_value)
    TextView mTvSpindleSpeed;

    @BindView(R.id.tv_widget_cnc_work_speed_value)
    TextView mTvCNCWorkSpeed;

    @BindView(R.id.tv_widget_cnc_estimated_time_title)
    TextView mTvCNCEstimatedTimeTitle;
    @BindView(R.id.tv_widget_cnc_estimated_time_value)
    TextView mTvCNCEstimatedTime;

    public void bind(View view) {
        ButterKnife.bind(this, view);
    }

    public void setFeedRate(double feedRate) {
        String text = String.format(Locale.getDefault(), "%.0f mm/min", feedRate);
        mTv3DPWorkSpeed.setText(text);
        mTvLaserWorkSpeed.setText(text);
        mTvCNCWorkSpeed.setText(text);
    }

    public void setFeedRatePerSecond(double feedRate) {
        String text = String.format(Locale.getDefault(), "%.0f mm/s", feedRate);
        mTv3DPWorkSpeed.setText(text);
        mTvLaserWorkSpeed.setText(text);
        mTvCNCWorkSpeed.setText(text);
    }

    public void setLeftFeedRatePerSecond(double leftFeedRate) {
        String text = String.format(Locale.getDefault(), "%.0f mm/s", leftFeedRate);
        mTv3DPDualExtruderLeftWorkSpeed.setText(text);
    }


    public void setWorkSpeedPercentage(double speed) {
        String text = String.format(Locale.getDefault(), "%.0f%%", speed);
        mTv3DPWorkSpeed.setText(text);
        if (mTv3DPDualExtruderLeftWorkSpeed != null) {
            mTv3DPDualExtruderLeftWorkSpeed.setText(text);
        }
        mTvLaserWorkSpeed.setText(text);
        mTvCNCWorkSpeed.setText(text);
    }

    public void setEstimatedTime(double time) {
        String text = DateHelper.formatTime2(time);
        mTv3DPEstimatedTime.setText(text);
        mTvLaserEstimatedTime.setText(text);
        mTvCNCEstimatedTime.setText(text);
        if (mTv3DPDualExtruderEstimatedTime != null) {
            mTv3DPDualExtruderEstimatedTime.setText(text);
        }
    }

    public void useElapsedTime() {
        mTv3DPEstimatedTimeTitle.setText(R.string.print_elapsed_time);
        mTv3DPDualExtruderEstimatedTimeTitle.setText(R.string.print_elapsed_time);
        mTvLaserEstimatedTimeTitle.setText(R.string.print_elapsed_time);
        mTvCNCEstimatedTimeTitle.setText(R.string.print_elapsed_time);
    }

    public void setNozzleTemp(double target) {
        mTvNozzleTemp.setText(String.format(Locale.getDefault(), "%.0f°C", target));
    }

    public void setNozzleTemp(int which, double target) {
        if (which == 0) {
            mTv3DPDualExtruderLeftNozzleTemp.setText(String.format(Locale.getDefault(), "L: %.0f°C",target));
        } else if (which == 1) {
            mTv3DPDualExtruderRightNozzleTemp.setText(String.format(Locale.getDefault(), "R: %.0f°C",target));
        }
    }

    public void setNozzleTemp(double current, double target) {
        mTvNozzleTemp.setText(String.format(Locale.getDefault(), "%.0f/%.0f°C", current, target));
    }

    public void setLeftNozzleTemp(double current, double target) {
        mTv3DPDualExtruderLeftNozzleTemp.setText(String.format(Locale.getDefault(), "L: %.0f/%.0f°C", current, target));
    }

    public void setRightNozzleTemp(double current, double target) {
        mTv3DPDualExtruderRightNozzleTemp.setText(String.format(Locale.getDefault(), "R: %.0f/%.0f°C", current, target));
    }

    public void setHeatedBedTemp(double target) {
        mTvHeatedBedTemp.setText(String.format(Locale.getDefault(), "%.0f°C", target));
        if (mTv3DPDualExtruderHeatedBedTemp != null) {
            mTv3DPDualExtruderHeatedBedTemp.setText(String.format(Locale.getDefault(), "%.0f°C", target));
        }
    }

    public void setHeatedBedTempDualExtruder(double target) {
        mTv3DPDualExtruderHeatedBedTemp.setText(String.format(Locale.getDefault(), "%.0f°C", target));
    }

    public void setHeatedBedTemp(double current, double target) {
        mTvHeatedBedTemp.setText(String.format(Locale.getDefault(), "%.0f/%.0f°C", current, target));
    }

    public void setHeatedBedTempDualExtruder(double current, double target) {
        mTv3DPDualExtruderHeatedBedTemp.setText(String.format(Locale.getDefault(), "%.0f/%.0f°C", current, target));
    }

    public void setLaserPower(double power) {
        mTvLaserPower.setText(String.format(Locale.getDefault(), "%.1f%%", power));
    }

    public void setSpindleSpeed(double speed) {
        mTvSpindleSpeed.setText(String.format(Locale.getDefault(), "%.0f RPM", speed));
    }
}
