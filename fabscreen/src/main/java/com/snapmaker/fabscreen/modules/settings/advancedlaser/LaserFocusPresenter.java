package com.snapmaker.fabscreen.modules.settings.advancedlaser;

import android.content.Context;
import android.view.View;

import com.snapmaker.fabscreen.R;
import com.snapmaker.fabscreen.modules.common.SetValueRulerWidgetPresenter;

import fabscreen.libraries.legacy.data.Model;
import io.reactivex.disposables.CompositeDisposable;

public class LaserFocusPresenter extends SetValueRulerWidgetPresenter {
    public LaserFocusPresenter(Context context, Model model, CompositeDisposable compositeDisposable) {
        super(context, model, compositeDisposable);
    }

    @Override
    public void bind(View view) {
        super.bind(view);

        setPrecision(1);

        mTvTitle.setText(R.string.all_laser_height);

        mTvValueCurrent.setVisibility(View.GONE);
        mTvValueSlash.setVisibility(View.GONE);
        mTvValueUnit.setVisibility(View.VISIBLE);

        mTvValueUnit.setText(R.string.all_unit_mm);
        mRvRuler.setUnit(0.1f);
        mRvRuler.setMinValue(0);
        mRvRuler.setMaxValue(40);
    }

    public void connect() {
        final float laserFocus = getModel().getMachineController().getLaserFocus();
        setTargetValue(laserFocus);
    }
}
