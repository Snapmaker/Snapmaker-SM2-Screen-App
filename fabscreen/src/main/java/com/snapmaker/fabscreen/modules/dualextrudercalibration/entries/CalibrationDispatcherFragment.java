package com.snapmaker.fabscreen.modules.dualextrudercalibration.entries;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.snapmaker.fabscreen.R;
import com.snapmaker.fabscreen.router.Router;

import java.util.Objects;

import butterknife.BindView;
import fabscreen.libraries.legacy.base.BaseFragment;

public class CalibrationDispatcherFragment extends BaseFragment implements EntriesAdapter.ItemClickListener {

    private CalibrationDispatcherViewModel mViewModel;
    private EntriesAdapter mEntriesAdapter;

    public static Fragment newInstance() {
        return new CalibrationDispatcherFragment();
    }

    @BindView(R.id.rv_entries)
    RecyclerView mRvEntries;

    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_calibration_dispatcher;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mViewModel = getFragmentScopeViewModel(CalibrationDispatcherViewModel.class);
        initView();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mEntriesAdapter != null) {
            mViewModel.refreshBadge();
            mEntriesAdapter.notifyItemRangeChanged(1, 2);
        }
    }

    private void initView() {
        setTitle(R.string.all_calibration);
        mRvEntries.setLayoutManager(new LinearLayoutManager(requireContext()));
        mEntriesAdapter = new EntriesAdapter(mViewModel.getEntryList());
        mRvEntries.setAdapter(mEntriesAdapter);
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL);
        dividerItemDecoration.setDrawable(Objects.requireNonNull(ResourcesCompat.getDrawable(getResources(), R.drawable.list_divider, requireActivity().getTheme())));
        mRvEntries.addItemDecoration(dividerItemDecoration);
        mEntriesAdapter.setOnItemClickListener(this);
    }

    @Override
    public void onItemClick(EntryData entry) {
        switch (entry.entry) {
            case CALIBRATION_HEATED_BED:
                Router.getInstance().routeToDualExtruderBedLeveling().start(requireContext());
                break;
            case CALIBRATION_Z_HEIGHT:
                Router.getInstance().routeToDualExtruderZHeightCalibration().start(requireContext());
                break;
            case CALIBRATION_XY_OFFSET:
                Router.getInstance().routeToDualExtruderXYCalibration().start(requireContext());
                break;
            case CALIBRATION_SENSOR:
                Router.getInstance().routeToDualExtruderSensorCalibration().start(requireContext());
                break;
            case CALIBRATION_CHECK:
                Router.getInstance().routeToDualExtruderPrintCheck().start(requireContext());
                break;
            default:
                break;
        }
    }
}
