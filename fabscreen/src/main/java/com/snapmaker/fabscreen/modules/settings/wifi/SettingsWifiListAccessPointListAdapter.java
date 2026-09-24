package com.snapmaker.fabscreen.modules.settings.wifi;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.snapmaker.fabscreen.R;

import java.util.ArrayList;
import java.util.List;

import fabscreen.libraries.legacy.lib.network.AccessPoint;

// Almost the same as {@code WelcomeWifiListAdapter}, except it uses a different layout.
public class SettingsWifiListAccessPointListAdapter extends BaseAdapter implements AdapterView.OnItemClickListener {
    private List<AccessPoint> mAccessPointList;
    private OnItemClickListener mOnItemClickListener;

    SettingsWifiListAccessPointListAdapter() {
        this.mAccessPointList = new ArrayList<>();
    }

    void setAccessPoints(List<AccessPoint> accessPoints) {
        mAccessPointList = accessPoints;
    }

    void setOnItemClickListener(OnItemClickListener mOnItemClickListener) {
        this.mOnItemClickListener = mOnItemClickListener;
    }

    @Override
    public int getCount() {
        return this.mAccessPointList.size();
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public AccessPoint getItem(int position) {
        return mAccessPointList.get(position);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_settings_access_point, parent, false);
        }

        if (convertView.getTag() == null) {
            ViewHolder viewHolder = new ViewHolder(convertView);
            convertView.setTag(viewHolder);
        }

        ViewHolder viewHolder = (ViewHolder) convertView.getTag();

        AccessPoint accessPoint = getItem(position);

        viewHolder.mTvName.setText(accessPoint.getSSID());
        viewHolder.mIvHasAuth.setVisibility(accessPoint.isEncrypted() ? View.VISIBLE : View.GONE);

        return convertView;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        AccessPoint accessPoint = getItem(position);

        if (mOnItemClickListener != null) {
            mOnItemClickListener.onClick(accessPoint);
        }
    }

    private class ViewHolder {
        private View mView;
        private TextView mTvName;
        private ImageView mIvHasAuth;

        private ViewHolder(View itemView) {
            mView = itemView;

            mTvName = itemView.findViewById(R.id.tv_settings_ap_item_ssid);
            mIvHasAuth = itemView.findViewById(R.id.iv_settings_ap_item_has_authentication);
        }
    }

    interface OnItemClickListener {
        void onClick(AccessPoint accessPoint);
    }
}

