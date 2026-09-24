package com.snapmaker.fabscreen.modules.factory;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.snapmaker.fabscreen.R;
import fabscreen.libraries.legacy.base.BaseFragment;
import fabscreen.libraries.legacy.base.BaseViewModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.OnClick;
import fabscreen.libraries.legacy.data.NetworkController;
import fabscreen.libraries.legacy.lib.network.AccessPoint;
import io.reactivex.Observable;
import io.reactivex.subjects.BehaviorSubject;

public class FactoryWifiSignalFragment extends BaseFragment {
    private static final String TAG = FactoryWifiSignalFragment.class.getSimpleName();
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;

    @BindView(R.id.lv_factory_wifi_signal_list)
    ListView mLvWifiList;

    private WifiListAdapter mAdapter;

    private BehaviorSubject<Boolean> mHasPermission = BehaviorSubject.createDefault(false);

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setTitle("Wifi Signal");

        if (getContext() == null) return;

        mAdapter = new WifiListAdapter(getContext());
        mLvWifiList.setAdapter(mAdapter);

        NetworkController controller = getModel().getNetworkController();

        if (!controller.isWifiEnabled()) {
            controller.setWifiEnabled(true);
        }

        controller.watchAccessPointList()
                .as(bindToLifecycle())
                .subscribe((accessPointList) -> {
                    ArrayList<WifiItemViewModel> list = new ArrayList<>();
                    for (AccessPoint ap : accessPointList) {
                        if (ap.getSSID().isEmpty()) {
                            continue;
                        }
                        Log.d(TAG, "ap " + ap.getSSID() + " rssi " + ap.getRssi());

                        WifiItemViewModel wifiItemViewModel = new WifiItemViewModel(ap.getSSID(), String.valueOf(ap.getRssi()));
                        list.add(wifiItemViewModel);
                    }

                    mAdapter.setLists(list);
                });

        mHasPermission
                .as(bindToLifecycle())
                .subscribe(hasPermission -> onClickFresh());

        Observable.interval(2, TimeUnit.SECONDS)
                .as(bindToLifecycle())
                .subscribe(tick -> {
                    if (getContext().checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
                            != PackageManager.PERMISSION_GRANTED) {
                        requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
                    } else {
                        mHasPermission.onNext(true);
                        mHasPermission.onComplete();
                    }
                });
    }

    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_factory_wifi_signal;
    }

    @Override
    protected BaseViewModel getViewModel() {
        return null;
    }

    @OnClick(R.id.btn_factory_wifi_signal_fresh)
    void onClickFresh() {
        getModel().getNetworkController().startScan();
    }

    class WifiItemViewModel {
        String mSsid;
        String mRssi;

        WifiItemViewModel(String ssid, String rssi) {
            mSsid = ssid;
            mRssi = rssi;
        }

        public String getRssi() {
            return mRssi;
        }

        public String getSsid() {
            return mSsid;
        }
    }

    class WifiListAdapter extends BaseAdapter {
        private Context mContext;
        private List<WifiItemViewModel> mLists;

        public WifiListAdapter(Context context) {
            mContext = context;
            mLists = new ArrayList<>();
        }

        public void setLists(List<WifiItemViewModel> lists) {
            mLists = lists;
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return mLists.size();
        }

        @Override
        public WifiItemViewModel getItem(int position) {
            return mLists.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(mContext).inflate(R.layout.item_wifi_signal, parent, false);
            }

            TextView tvSSID = convertView.findViewById(R.id.tv_ssid);
            TextView tvRSSI = convertView.findViewById(R.id.tv_rssi);

            WifiItemViewModel viewModel = getItem(position);

            tvSSID.setText(viewModel.getSsid());
            tvRSSI.setText(String.format(Locale.getDefault(), "%s dBm", viewModel.getRssi()));

            return convertView;
        }
    }
}
