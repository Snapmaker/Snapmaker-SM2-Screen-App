package com.snapmaker.fabscreen.modules.home;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import com.snapmaker.fabscreen.R;

import fabscreen.libraries.legacy.base.BaseFragment;
import fabscreen.libraries.legacy.base.BaseViewModel;

import com.snapmaker.fabscreen.modules.airpurifier.AirPurifierActivity;
import com.snapmaker.fabscreen.modules.browse.BrowseActivity;
import com.snapmaker.fabscreen.modules.calibration.CalibrationActivity;
import com.snapmaker.fabscreen.modules.control.ControlActivity;
import com.snapmaker.fabscreen.modules.enclosure.EnclosureActivity;
import com.snapmaker.fabscreen.modules.experiment.ExperimentActivity;
import com.snapmaker.fabscreen.modules.factory.FactoryToolsActivity;
import com.snapmaker.fabscreen.modules.lasercalibration.LaserCalibrationActivity;
import com.snapmaker.fabscreen.modules.settings.SettingsActivity;
import com.snapmaker.fabscreen.router.Router;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import fabscreen.libraries.legacy.data.Constants;

public class LauncherFragment extends BaseFragment {
    public static LauncherFragment newInstance() {
        return new LauncherFragment();
    }

    private static final int ENTRY_FILES = 1000;
    private static final int ENTRY_CONTROL = 2000;
    private static final int ENTRY_CALIBRATION = 3000;
    private static final int ENTRY_SETTINGS = 4000;
    private static final int ENTRY_FACTORY = 5000;
    private static final int ENTRY_EXP = 6000;
    private static final int ENTRY_ENCLOSURE = 7000;
    private static final int ENTRY_CNC_TOOL_BOX = 8000;
    private static final int ENTRY_AIR_PURIFIER = 9000;

    @BindView(R.id.rl_calibration_panel)
    GridView mGridView;

    private ModuleEntryAdapter mAdapter;
    private AlertDialog mToolboxDialog;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initView();
    }

    @Override
    public void onResume() {
        super.onResume();
        final int headType = getModel().getMachineController().getHeadType();
        final boolean isRotaryAvailable = getModel().getMachineController().isRotaryModuleAvailable();
        final List<ModuleEntry> entries = new ArrayList<>();

        entries.add(new ModuleEntry(ENTRY_FILES, R.string.browse_files, BrowseActivity.class, R.drawable.ic_module_files_80x80));
        entries.add(new ModuleEntry(ENTRY_CONTROL, R.string.all_control, ControlActivity.class, R.drawable.ic_module_control_80x80));

        switch (headType) {
            case Constants.HEAD_3DP:
            case Constants.HEAD_3DP_DUAL_EXTRUDER:
                entries.add(new ModuleEntry(ENTRY_CALIBRATION, R.string.all_calibration, CalibrationActivity.class, R.drawable.ic_module_calibration_80x80));
                break;
            case Constants.HEAD_CNC:
            case Constants.HEAD_CNC_200W:
                if (isRotaryAvailable) {
                    entries.add(new ModuleEntry(ENTRY_CNC_TOOL_BOX, R.string.cnc_tool_box, null, R.drawable.ic_module_toolbox_80x80));
                } else {
                    entries.add(new ModuleEntry(ENTRY_CALIBRATION, R.string.all_calibration, LaserCalibrationActivity.class, R.drawable.ic_module_calibration_80x80));
                }
                break;
            case Constants.HEAD_LASER:
                entries.add(new ModuleEntry(ENTRY_CALIBRATION, R.string.all_calibration, LaserCalibrationActivity.class, R.drawable.ic_module_calibration_80x80));
                break;
            case Constants.HEAD_LASER_10W:
                break;
            case Constants.HEAD_LASER_2W_IR:
            case Constants.HEAD_LASER_20W:
            case Constants.HEAD_LASER_40W:
                if (!isRotaryAvailable) {
                    entries.add(new ModuleEntry(ENTRY_CALIBRATION, R.string.all_calibration, LaserCalibrationActivity.class, R.drawable.ic_module_calibration_80x80));
                }
                break;
        }
        entries.add(new ModuleEntry(ENTRY_SETTINGS, R.string.all_settings, SettingsActivity.class, R.drawable.ic_module_settings_80x80));

        boolean debugFlag = getModel().getPreferences().getDebugFlag();
        if (debugFlag) {
            entries.add(new ModuleEntry(ENTRY_FACTORY, R.string.all_factory, FactoryToolsActivity.class, R.drawable.ic_module_settings_80x80));
            entries.add(new ModuleEntry(ENTRY_EXP, R.string.all_experiment, ExperimentActivity.class, R.drawable.ic_module_settings_80x80));
        }

        // check enclosure status
        boolean isEnclosureReady = getModel().getMachineController().isEnclosureReady();
        if (isEnclosureReady) {
            entries.add(new ModuleEntry(ENTRY_ENCLOSURE, R.string.all_enclosure, EnclosureActivity.class, R.drawable.ic_module_enclosure_80x80));
        }

        if (getModel().getMachineController().isAirPurifierPlugged()) {
            entries.add(new ModuleEntry(ENTRY_AIR_PURIFIER, R.string.all_air_purifier, AirPurifierActivity.class, R.drawable.ic_module_air_purifier_80x80));
        }

        mAdapter.setEntries(entries);
        mAdapter.notifyDataSetChanged();
    }

    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_launcher;
    }

    @Override
    protected BaseViewModel getViewModel() {
        return null;
    }

    private void initView() {
        mAdapter = new ModuleEntryAdapter(getContext());
        mGridView.setAdapter(mAdapter);
    }

    public void showToolboxDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext(), R.style.AppTheme_Dialog);
        if (mToolboxDialog != null) {
            if (mToolboxDialog.isShowing()) return;
        }
        mToolboxDialog = builder.create();
        if (mToolboxDialog.getWindow() != null) {
            mToolboxDialog.getWindow().setBackgroundDrawableResource(R.color.default_theme_dark_mask);
            mToolboxDialog.getWindow().setLayout(280 * 2, WindowManager.LayoutParams.WRAP_CONTENT);
        }

        LayoutInflater inflater = LayoutInflater.from(getContext());
        View view = inflater.inflate(R.layout.dialog_cnc_toolbox, null);
        // init button icon
        Button btnOriginAssist = view.findViewById(R.id.btn_cnc_origin_assistant_entry);
        Button btnBitAssistant = view.findViewById(R.id.btn_cnc_bit_assistant_entry);

        btnOriginAssist.setOnClickListener(v -> {
            Router.getInstance().routeToCNCOriginAssistantPage().start(getContext());
            if (mToolboxDialog != null) {
                mToolboxDialog.dismiss();
            }
        });

        btnBitAssistant.setOnClickListener(v -> {
            Router.getInstance().routeToCNCBitAssistantPage().start(getContext());
            if (mToolboxDialog != null) {
                mToolboxDialog.dismiss();
            }
        });
        mToolboxDialog.setView(view);
        mToolboxDialog.setCanceledOnTouchOutside(true);
        mToolboxDialog.show();
    }

    /**
     * Module Entry represents a entry information of a module: its name, class and icon.
     */
    class ModuleEntry {
        private int entryCode;
        private int nameRes;
        private Class entryClass;
        private int resourceId;

        ModuleEntry(int entryCode, @StringRes int nameRes, Class entryClass, int resourceId) {
            this.entryCode = entryCode;
            this.nameRes = nameRes;
            this.entryClass = entryClass;
            this.resourceId = resourceId;
        }

        int getEntryCode() {
            return entryCode;
        }

        int getNameRes() {
            return nameRes;
        }

        int getResourceId() {
            return resourceId;
        }

        Class getEntryClass() {
            return entryClass;
        }
    }

    class ModuleEntryAdapter extends BaseAdapter {
        private Context mContext;
        private List<ModuleEntry> mEntries = new ArrayList<>();

        ModuleEntryAdapter(Context context) {
            mContext = context;
        }

        void setEntries(List<ModuleEntry> entries) {
            this.mEntries = entries;
        }

        @Override
        public int getCount() {
            return mEntries.size();
        }

        @Override
        public ModuleEntry getItem(int i) {
            return mEntries.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(mContext).inflate(R.layout.item_module_entry, parent, false);
            }

            final ModuleEntry entry = getItem(position);

            // icon
            final Button icon = convertView.findViewById(R.id.icon);
            icon.setBackgroundResource(entry.getResourceId());
            icon.setOnClickListener(v -> {
                switch (entry.getEntryCode()) {
                    case ENTRY_CALIBRATION: {
                        int headType = getModel().getMachineController().getHeadType();
                        switch (headType) {
                            case Constants.HEAD_3DP:
                                Router.getInstance()
                                        .routeToCalibrationPage(true)
                                        .start(mContext);
                                break;
                            case Constants.HEAD_3DP_DUAL_EXTRUDER:
                                Router.getInstance()
                                        .routeToDualExtruderCalibration()
                                        .start(requireContext());
                                break;
                            case Constants.HEAD_LASER:
                            case Constants.HEAD_LASER_2W_IR:
                            case Constants.HEAD_LASER_10W:
                            case Constants.HEAD_LASER_20W:
                            case Constants.HEAD_LASER_40W:
                                Router.getInstance()
                                        .routeToLaserCalibrationPage()
                                        .start(mContext);
                                break;
                            case Constants.HEAD_CNC:
                            case Constants.HEAD_CNC_200W:
                                Router.getInstance()
                                        .routeToLaserCalibrationPage()
                                        .start(mContext);
                            default:
                                break;
                        }
                        break;
                    }
                    case ENTRY_CNC_TOOL_BOX: {
                        showToolboxDialog();
                        break;
                    }
                    default:
                        Router.getInstance()
                                .routeWithClass(entry.getEntryClass())
                                .start(mContext);
                        break;
                }
            });

            // module name
            final TextView title = convertView.findViewById(R.id.title);
            if (entry.getNameRes() != 0) {
                title.setText(entry.getNameRes());
            }

            return convertView;
        }
    }
}
