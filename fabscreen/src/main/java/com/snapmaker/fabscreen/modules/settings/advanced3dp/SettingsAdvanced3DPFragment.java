package com.snapmaker.fabscreen.modules.settings.advanced3dp;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.orhanobut.logger.Logger;
import com.snapmaker.fabscreen.R;
import fabscreen.libraries.legacy.base.BaseFragment;
import fabscreen.libraries.legacy.base.BaseViewModel;
import com.snapmaker.fabscreen.modules.settings.SettingsActivity;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.OnClick;
import fabscreen.libraries.legacy.data.Constants;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.subjects.BehaviorSubject;

public class SettingsAdvanced3DPFragment extends BaseFragment {
    public static SettingsAdvanced3DPFragment getInstance() {
        return new SettingsAdvanced3DPFragment();
    }

    private static final int ITEM_CALIBRATION_GRID = 1;

    private BehaviorSubject<Integer> mCalibrationModeSubject = BehaviorSubject.createDefault(0);

    @BindView(R.id.btn_settings_advanced_auto_calibration)
    Button mBtnAutoCalibrationSwitch;
    @BindView(R.id.btn_settings_advanced_fast_calibration)
    Button mBtnFastCalibrationSwitch;
    @BindView(R.id.btn_settings_advanced_heated_leveling)
    Button mBtnHeatedLevelingSwitch;
    @BindView(R.id.view_settings_advanced_fast_calibration_background)
    Button mBtnFastCalibrationBackground;

    @BindView(R.id.tv_settings_advance_fast_calibration_desc)
    TextView mTvFastCalibrationDesc;

    @BindView(R.id.lv_advanced_3dp_item_list)
    ListView mLv3dpAdvanceList;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initView();
    }

    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_settings_advanced_3dp;
    }

    @Override
    protected BaseViewModel getViewModel() {
        return null;
    }

    private void initView() {
        setTitle(R.string.settings_print_settings_3dp);

        // show fast calibration option if auto calibration is on
        mCalibrationModeSubject
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(mode -> {
//                    mBtnFastCalibrationSwitch.setVisibility(mode == 0 ? Button.VISIBLE : Button.GONE);
//                    mTvFastCalibrationDesc.setVisibility(mode == 0 ? TextView.VISIBLE : TextView.GONE);
//                    mBtnFastCalibrationBackground.setVisibility(mode == 0 ? TextView.VISIBLE : TextView.GONE);
                    mBtnAutoCalibrationSwitch.setActivated(mode == 0);
                });

        int calibrationMode = getModel().getPreferences().get3DPCalibrationMode();
        mCalibrationModeSubject.onNext(calibrationMode);

        boolean fastCalibrationOn = getModel().getPreferences().get3DPFastCalibrationOn();
        mBtnFastCalibrationSwitch.setActivated(fastCalibrationOn);

        boolean isHeatedLeveling = getModel().getPreferences().get3DPCalibrationHeatedLevelingOn();
        mBtnHeatedLevelingSwitch.setActivated(isHeatedLeveling);

        // Advanced list
        List<Integer> items = new ArrayList<Integer>() {{
            if (getModel().getMachineController().getMachineModel() != Constants.MACHINE_MODEL_SNAPMAKER_A150) {
                add(ITEM_CALIBRATION_GRID);
            }
        }};

        ItemAdapter itemAdapter = new ItemAdapter(getContext());
        itemAdapter.setItem(items);

        mLv3dpAdvanceList.setAdapter(itemAdapter);

    }

    @OnClick(R.id.btn_settings_advanced_auto_calibration)
    void onClickCalibrationMode() {
        mCalibrationModeSubject.onNext(1 - mCalibrationModeSubject.getValue());
        Logger.i("Setting 3DP calibration mode " + mCalibrationModeSubject.getValue());

        getModel().getPreferences().set3DPCalibrationMode(mCalibrationModeSubject.getValue());
    }

    @OnClick(R.id.btn_settings_advanced_fast_calibration)
    void onClickFastCalibration() {
        boolean fastCalibrationOn = getModel().getPreferences().get3DPFastCalibrationOn();
        fastCalibrationOn = !fastCalibrationOn;

        Logger.i("Setting fast calibration " + fastCalibrationOn);

        getModel().getPreferences().set3DPFastCalibrationOn(fastCalibrationOn);
        mBtnFastCalibrationSwitch.setActivated(fastCalibrationOn);
    }

    @OnClick(R.id.btn_settings_advanced_heated_leveling)
    void onClickHeatedLeveling() {
        boolean isHeatedLevelingOn = getModel().getPreferences().get3DPCalibrationHeatedLevelingOn();
        isHeatedLevelingOn = !isHeatedLevelingOn;
        Logger.i("Setting heated leveling " + isHeatedLevelingOn);

        getModel().getPreferences().set3DPCalibrationHeatedLevelingOn(isHeatedLevelingOn);
        mBtnHeatedLevelingSwitch.setActivated(isHeatedLevelingOn);
    }

    private class ItemAdapter extends BaseAdapter {
        private Context mContext;
        private List<Integer> mItems;

        ItemAdapter(Context context) {
            this.mContext = context;
        }

        public void setItem(List<Integer> items) {
            this.mItems = items;
        }

        @Override
        public int getCount() {
            return mItems.size();
        }

        @Override
        public Integer getItem(int position) {
            return mItems.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(mContext).inflate(R.layout.item_settings_item, parent, false);
            }

            int item = getItem(position);


            Button button = convertView.findViewById(R.id.btn_settings_item_name);
            TextView tvDesc = convertView.findViewById(R.id.tv_settings_item_desc);

            // TODO
            tvDesc.setText(null);

            switch (item) {
                case ITEM_CALIBRATION_GRID: {
                    button.setText(R.string.settings_3dp_calibration_grid);
                    button.setOnClickListener(v -> {
                        if (getActivity() != null) {
                            ((SettingsActivity) getActivity()).goto3DPCalibrationGrid();
                        }
                    });
                    break;
                }
            }

            return convertView;
        }
    }
}
