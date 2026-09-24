package com.snapmaker.fabscreen.modules.common;

import android.content.Context;
import android.view.View;

import com.snapmaker.fabscreen.R;

import fabscreen.libraries.legacy.data.Model;
import io.reactivex.disposables.CompositeDisposable;

public class MaterialThicknessWidgetPresenter extends SetValueRulerWidgetPresenter {
    public MaterialThicknessWidgetPresenter(Context context, Model model, CompositeDisposable compositeDisposable) {
        super(context, model, compositeDisposable);
    }

    @Override
    public void bind(View view) {
        super.bind(view);

        setPrecision(1);

        mTvTitle.setText(R.string.laser_set_material_thickness);

        mTvValueCurrent.setVisibility(View.GONE);
        mTvValueSlash.setVisibility(View.GONE);
        mTvValueUnit.setVisibility(View.VISIBLE);

        mTvValueUnit.setText(R.string.all_unit_mm);
        mRvRuler.setMaxValue(145);
        mRvRuler.setUnit(0.1f);
    }

    public void connectPreference() {
        final float thickness0 = getModel().getPreferences().getLaserMaterialThickness();
        setTargetValue(thickness0);
    }
}
