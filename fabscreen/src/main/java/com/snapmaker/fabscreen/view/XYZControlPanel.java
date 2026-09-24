package com.snapmaker.fabscreen.view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.snapmaker.fabscreen.R;
import com.snapmaker.fabscreen.data.XYZMoveController.Direction;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import co.ceryle.segmentedbutton.SegmentedButtonGroup;
import fabscreen.libraries.legacy.view.SteeringView;

@SuppressLint("NonConstantResourceId")
public class XYZControlPanel extends ConstraintLayout {

    private OnDirectionClickListener mDirectionListener;

    private float[] mWidths = {0.1f, 1f, 10f};
    private float mStepWidth = mWidths[1];

    @BindView(R.id.sbg_control_steps)
    SegmentedButtonGroup mSbgControlSteps;
    @BindView(R.id.sv_control_panel_xy)
    SteeringView mSvControlXY;
    @BindView(R.id.btn_control_panel_z_plus)
    Button mBtnZPlus;
    @BindView(R.id.btn_control_panel_z_minus)
    Button mBtnZMinus;

    public XYZControlPanel(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        View view = LayoutInflater.from(context).inflate(R.layout.widget_control_panel_xyz_axes_mini, this);
        ButterKnife.bind(this, view);
        mSbgControlSteps.setPosition(1, false);
        mSbgControlSteps.setOnPositionChanged(position -> {
            if (mWidths.length < 3) return;
            mStepWidth = mWidths[position];
        });
        mSvControlXY.setOnDirectionClickedListener(state -> {
            switch (state) {
                case SteeringView.DIRECTION_UP:
                    mDirectionListener.onDirectionClicked(Direction.FORWARD, mStepWidth);
                    break;
                case SteeringView.DIRECTION_DOWN:
                    mDirectionListener.onDirectionClicked(Direction.BACKWARD, mStepWidth);
                    break;
                case SteeringView.DIRECTION_LEFT:
                    mDirectionListener.onDirectionClicked(Direction.LEFT, mStepWidth);
                    break;
                case SteeringView.DIRECTION_RIGHT:
                    mDirectionListener.onDirectionClicked(Direction.RIGHT, mStepWidth);
                    break;
            }
        });
    }

    public XYZControlPanel setStepWidths(float width0, float width1, float width2) {
        mWidths[0] = width0;
        mWidths[1] = width1;
        mWidths[2] = width2;
        return this;
    }

    public void setXYEnabled(boolean enabled) {
        mSvControlXY.setEnabled(enabled);
    }

    public void setZEnabled(boolean enabled) {
        mBtnZPlus.setEnabled(enabled);
        mBtnZMinus.setEnabled(enabled);
    }

    @OnClick({R.id.btn_control_panel_z_plus, R.id.btn_control_panel_z_minus})
    void onPanelItemClick(View view) {
        switch (view.getId()) {
            case R.id.btn_control_panel_z_plus:
                mDirectionListener.onDirectionClicked(Direction.UP, mStepWidth);
                break;

            case R.id.btn_control_panel_z_minus:
                mDirectionListener.onDirectionClicked(Direction.DOWN, mStepWidth);
                break;
        }
    }

    public void setOnDirectionClickListener(OnDirectionClickListener listener) {
        mDirectionListener = listener;
    }

    public interface OnDirectionClickListener {
        void onDirectionClicked(Direction direction, float stepWidth);
    }
}
