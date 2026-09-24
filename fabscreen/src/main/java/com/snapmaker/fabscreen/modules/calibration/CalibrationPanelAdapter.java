package com.snapmaker.fabscreen.modules.calibration;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.snapmaker.fabscreen.R;
import fabscreen.libraries.legacy.view.ActionButton;

import java.util.ArrayList;
import java.util.List;

import fabscreen.libraries.legacy.data.calibration.CalibrationPoint;

public class CalibrationPanelAdapter extends BaseAdapter {
    private Context mContext;
    private List<CalibrationPoint> mPoints;
    private boolean mIsButtonsEnabled;
    private OnPointClickListener mOnPointClickListener;

    public CalibrationPanelAdapter(Context context) {
        mContext = context;
    }

    @Override
    public int getCount() {
        return mPoints.size();
    }

    @Override
    public CalibrationPoint getItem(int position) {
        return mPoints.get(position);
    }

    public void setPoints(ArrayList<CalibrationPoint> points) {
        this.mPoints = points;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    public void setButtonsEnabled(boolean enabled) {
        mIsButtonsEnabled = enabled;
    }

    public void setOnPointClickListener(OnPointClickListener listener) {
        mOnPointClickListener = listener;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(mContext).inflate(R.layout.item_calibration_point, parent, false);
        }

        CalibrationPoint point = getItem(position);

        // TODO: refactor button view instead of ActionButton
        final ActionButton button = convertView.findViewById(R.id.btn_calibration_point);
        final TextView mTvIndex = convertView.findViewById(R.id.tv_calibration_index);

        mTvIndex.setText(String.valueOf(point.getViewOrder()));

        button.setEnabled(mIsButtonsEnabled);

        button.setActivated(point.isActivated());
        button.setSelected(point.isSelected());

        button.setOnClickListener(v -> mOnPointClickListener.onPointClick(point));

        return convertView;
    }

    public interface OnPointClickListener {
        void onPointClick(CalibrationPoint point);
    }
}
