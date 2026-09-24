package com.snapmaker.fabscreen.modules.settings.advancedlaser;

import android.content.Context;
import android.view.View;

import com.snapmaker.fabscreen.R;
import com.snapmaker.fabscreen.modules.common.SetValueRulerWidgetPresenter;

import fabscreen.libraries.legacy.data.Constants;
import fabscreen.libraries.legacy.data.Model;
import io.reactivex.disposables.CompositeDisposable;

public class LaserShotPowerPresenter extends SetValueRulerWidgetPresenter {
    public LaserShotPowerPresenter(Context context, Model model, CompositeDisposable compositeDisposable) {
        super(context, model, compositeDisposable);
    }

    @Override
    public void bind(View view) {
        super.bind(view);

        setPrecision(1);

        mTvTitle.setText(R.string.settings_laser_low_intensity_laser_power);

        mTvValueCurrent.setVisibility(View.GONE);
        mTvValueSlash.setVisibility(View.GONE);
        mTvValueUnit.setVisibility(View.VISIBLE);

        mTvValueUnit.setText(R.string.all_unit_percentage);

        int headType = getModel().getMachineController().getHeadType();
        switch (headType) {
            case Constants.HEAD_LASER:
                mRvRuler.setUnit(0.5f);
                mRvRuler.setMinValue(0.5f);
                mRvRuler.setMaxValue(3f);
                break;
            case Constants.HEAD_LASER_10W:
                mRvRuler.setUnit(0.5f);
                mRvRuler.setMinValue(1.0f);
                mRvRuler.setMaxValue(3f);
                break;
            case Constants.HEAD_LASER_20W:
            case Constants.HEAD_LASER_40W:
                mRvRuler.setUnit(0.1f);
                mRvRuler.setMinValue(0.2f);
                mRvRuler.setMaxValue(1f);
                break;
            default:
                mRvRuler.setUnit(0.5f);
                mRvRuler.setMinValue(0.5f);
                mRvRuler.setMaxValue(3f);
                break;
        }
    }

    public void connect() {
        getModel().getMachineController().requestLaserShotOutputPower();

        float outputPower = getModel().getMachineController().getLaserOutputPower();
        setTargetValue(outputPower);
    }
}
