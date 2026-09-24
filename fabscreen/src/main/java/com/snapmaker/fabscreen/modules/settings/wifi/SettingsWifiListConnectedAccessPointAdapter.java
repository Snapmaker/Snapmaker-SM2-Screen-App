package com.snapmaker.fabscreen.modules.settings.wifi;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.snapmaker.fabscreen.R;

import fabscreen.libraries.legacy.lib.network.AccessPoint;

public class SettingsWifiListConnectedAccessPointAdapter extends BaseAdapter {

    private Context mContext;
    private AccessPoint mAccessPoint = null;
    private boolean mIsConnected = false;

    private onClickInfoListener mOnClickInfoListener;

    SettingsWifiListConnectedAccessPointAdapter(Context context) {
        mContext = context;
    }

    @Override
    public int getCount() {
        return mAccessPoint == null ? 0 : 1;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public AccessPoint getItem(int position) {
        return mAccessPoint;
    }

    void setAccessPoint(AccessPoint accessPoint) {
        mAccessPoint = accessPoint;
    }

    private void setConnected(boolean isConnected) {
        mIsConnected = isConnected;
    }

    public void setOnClickInfoListener(onClickInfoListener listener) {
        mOnClickInfoListener = listener;
    }

    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_settings_access_point, parent, false);
        }

        TextView tvSSID = convertView.findViewById(R.id.tv_settings_ap_item_ssid);
        ImageView ivAuth = convertView.findViewById(R.id.iv_settings_ap_item_has_authentication);
        ImageView ivSignal = convertView.findViewById(R.id.iv_settings_ap_item_signal);
        ImageView ivState = convertView.findViewById(R.id.iv_settings_ap_item_connected);
        ProgressBar pbState = convertView.findViewById(R.id.pb_settings_ap_item_connecting);

        ImageView ivInfo = convertView.findViewById(R.id.iv_settings_ap_item_info);

        // set Text & image visible
        AccessPoint accessPoint = getItem(position);
        ivAuth.setVisibility(ImageView.INVISIBLE);
        ivSignal.setVisibility(ImageView.INVISIBLE);
        ivState.setVisibility(ImageView.INVISIBLE);

        pbState.setVisibility(mIsConnected ? ProgressBar.INVISIBLE : ProgressBar.VISIBLE);
        ivState.setVisibility(mIsConnected ? ImageView.VISIBLE : ImageView.INVISIBLE);

        ivInfo.setVisibility(accessPoint.getConnectState() == AccessPoint.ConnectState.CONNECTED ? ImageView.VISIBLE : ImageView.GONE);

        ivInfo.setOnClickListener(v -> {
            if (mOnClickInfoListener != null) {
                mOnClickInfoListener.onClickInfo(accessPoint);
            }
        });

        // FIXME: if connected
        if (!accessPoint.getSSID().isEmpty()) {
            tvSSID.setText(accessPoint.getSSID());
        }
        return convertView;
    }

    public void notifyNetworkStateChange(AccessPoint accessPoint) {
        if (accessPoint == AccessPoint.NULL_ACCESS_POINT) {
            setAccessPoint(null);
            setConnected(false);
        } else {
            setAccessPoint(accessPoint);
            setConnected(accessPoint.getConnectState() == AccessPoint.ConnectState.CONNECTED);
        }
        notifyDataSetChanged();
    }

    public interface onClickInfoListener {
        void onClickInfo(AccessPoint accessPoint);
    }
}
