package com.snapmaker.fabscreen.modules.factory;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.snapmaker.fabscreen.BuildConfig;
import com.snapmaker.fabscreen.R;
import fabscreen.libraries.legacy.base.BaseFragment;
import fabscreen.libraries.legacy.base.BaseViewModel;
import com.snapmaker.fabscreen.router.Router;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import butterknife.BindView;
import fabscreen.libraries.legacy.data.Constants;

public class FactoryToolsFragment extends BaseFragment {
    @BindView(R.id.lv_factory_tools)
    ListView mListView;

    @BindView(R.id.tv_factory_version)
    TextView mTvVersion;
    @BindView(R.id.tv_factory_tool_head)
    TextView mTvToolHead;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setTitle(R.string.all_factory);

        initViews();

    }

    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_factory;
    }

    @Override
    protected BaseViewModel getViewModel() {
        return null;
    }

    private void initViews() {
        List<Integer> items = new ArrayList<>();
        items.add(ListViewAdapter.CODE_COLOR_FRAUD);
        items.add(ListViewAdapter.CODE_CPU_BENCHMARK);
        items.add(ListViewAdapter.CODE_WIFI_SIGNAL);
        items.add(ListViewAdapter.CODE_BRIGHTNESS_TEST);
        items.add(ListViewAdapter.CODE_TOUCH_PANEL_TEST);
        items.add(ListViewAdapter.CODE_BLUETOOTH_TEST);
        items.add(ListViewAdapter.CODE_U_DISK_TEST);
        items.add(ListViewAdapter.CODE_COLOR_DIFFERENCE_TEST);
        items.add(ListViewAdapter.CODE_LASER_INDICATOR_TEST);

        ListViewAdapter adapter = new ListViewAdapter(getContext());
        adapter.setItems(items);

        mListView.setAdapter(adapter);
        mListView.setOnItemClickListener(adapter);

        final int headType = getModel().getMachineController().getHeadType();
        String toolHead;
        switch (headType) {
            case Constants.HEAD_FACTORY_3DP:
                toolHead = "Factory 3DP";
                break;
            case Constants.HEAD_FACTORY_CNC:
                toolHead = "Factory CNC";
                break;
            case Constants.HEAD_FACTORY_LASER:
                toolHead = "Factory Laser";
                break;
            case Constants.HEAD_LASER:
                toolHead = "1.6W Laser Module";
                break;
            case Constants.HEAD_LASER_2W_IR:
                toolHead = "2W Laser IR Module";
                break;
            case Constants.HEAD_LASER_10W:
                toolHead = "10W Laser Module";
                break;
            case Constants.HEAD_LASER_20W:
                toolHead = "20W Laser Module";
                break;
            case Constants.HEAD_LASER_40W:
                toolHead = "40W Laser Module";
                break;
            case Constants.HEAD_3DP:
                toolHead = "3DP Module";
                break;
            case Constants.HEAD_3DP_DUAL_EXTRUDER:
                toolHead = "Dual Extruder Module";
                break;
            case Constants.HEAD_CNC:
                toolHead = "CNC Module";
                break;
            case Constants.HEAD_UNPLUGGED:
                toolHead = "No toolhead plugged";
                break;
            default:
                toolHead = "Unknown";
                break;
        }

        mTvVersion.setText(String.format(Locale.getDefault(), "Version: %s", BuildConfig.VERSION_NAME));
        mTvToolHead.setText(String.format(Locale.getDefault(), "Factory mode: %s", toolHead));
    }

    public class ListViewAdapter extends BaseAdapter implements AdapterView.OnItemClickListener {
        private static final int CODE_COLOR_FRAUD = 0;
        private static final int CODE_CPU_BENCHMARK = 1;
        private static final int CODE_WIFI_SIGNAL = 2;
        private static final int CODE_BRIGHTNESS_TEST = 3;
        private static final int CODE_TOUCH_PANEL_TEST = 4;
        private static final int CODE_BLUETOOTH_TEST = 5;
        private static final int CODE_U_DISK_TEST = 6;
        private static final int CODE_COLOR_DIFFERENCE_TEST = 7;
        private static final int CODE_LASER_INDICATOR_TEST = 8;

        private Context mContext;
        private List<Integer> mItems = new ArrayList<>();

        ListViewAdapter(Context context) {
            mContext = context;
        }

        public void setItems(List<Integer> items) {
            mItems = items;
        }

        @Override
        public int getCount() {
            return mItems.size();
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public Object getItem(int position) {
            return mItems.get(position);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(mContext).inflate(R.layout.item_factory_tools, parent, false);
            }

            int code = (int) getItem(position);

            TextView textView = convertView.findViewById(R.id.tv_factory_tools_name);

            switch (code) {
                case CODE_COLOR_FRAUD: {
                    textView.setText("坏点检测");
                    break;
                }
                case CODE_CPU_BENCHMARK: {
                    textView.setText("CPU 性能测试");
                    break;
                }
                case CODE_WIFI_SIGNAL: {
                    textView.setText("Wi-Fi 信号测试");
                    break;
                }
                case CODE_BRIGHTNESS_TEST: {
                    textView.setText("亮度测试");
                    break;
                }
                case CODE_TOUCH_PANEL_TEST: {
                    textView.setText("TP 测试");
                    break;
                }
                case CODE_BLUETOOTH_TEST: {
                    textView.setText("模块蓝牙测试");
                    break;
                }
                case CODE_U_DISK_TEST: {
                    textView.setText("U 盘读取测试");
                    break;
                }
                case CODE_COLOR_DIFFERENCE_TEST: {
                    textView.setText("图标色差测试");
                    break;
                }
                case CODE_LASER_INDICATOR_TEST: {
                    textView.setText("弱光标定测试");
                }

                default:
                    break;
            }

            return convertView;
        }


        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            int code = (int) getItem(position);

            switch (code) {
                case CODE_COLOR_FRAUD: {
                    if (getActivity() == null) return;
                    ((FactoryToolsActivity) getActivity()).gotoColorFraudFragment();
                    break;
                }
                case CODE_CPU_BENCHMARK: {
                    if (getActivity() == null) return;
                    ((FactoryToolsActivity) getActivity()).gotoCpuBenchmarkFragment();
                    break;
                }
                case CODE_WIFI_SIGNAL: {
                    if (getActivity() == null) return;
                    ((FactoryToolsActivity) getActivity()).gotoWifiSignalFragment();
                    break;
                }
                case CODE_BRIGHTNESS_TEST: {
                    if (getActivity() == null) return;
                    ((FactoryToolsActivity) getActivity()).gotoBrightnessTestFragment();
                    break;
                }
                case CODE_TOUCH_PANEL_TEST: {
                    if (getActivity() == null) return;
                    ((FactoryToolsActivity) getActivity()).gotoTouchPanelTestFragment();
                    break;
                }
                case CODE_BLUETOOTH_TEST: {
                    if (getActivity() == null) return;
                    ((FactoryToolsActivity) getActivity()).gotoBluetoothFragment();
                    break;
                }
                case CODE_U_DISK_TEST: {
                    // Reuse file page for testing u-disk.
                    Router.getInstance().routeToFilesPage().start(getContext());
                    break;
                }
                case CODE_COLOR_DIFFERENCE_TEST: {
                    if (getActivity() == null) return;
                    ((FactoryToolsActivity) getActivity()).gotoColorDifferenceFragment();
                    break;
                }
                case CODE_LASER_INDICATOR_TEST: {
                    if (getActivity() == null) return;
                    ((FactoryToolsActivity) getActivity()).gotoLaserIndicatorTestFragment();
                    break;
                }
            }
        }
    }
}
