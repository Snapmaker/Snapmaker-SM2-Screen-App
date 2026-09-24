package com.snapmaker.fabscreen.modules.guidedualextruder.getstarted;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.snapmaker.fabscreen.R;
import com.snapmaker.fabscreen.modules.guidedualextruder.Guide3DPDualExtruderActivity;

import butterknife.BindView;
import butterknife.OnClick;
import fabscreen.libraries.legacy.base.BaseFragment;
import fabscreen.libraries.legacy.base.BaseViewModel;

public class Guide3DPDualExtruderSafetyNotesFragment extends BaseFragment {
    public static Guide3DPDualExtruderSafetyNotesFragment newInstance() {
        return new Guide3DPDualExtruderSafetyNotesFragment();
    }

    @BindView(R.id.top_bar_back)
    Button mBtnBack;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

//        if (!getModel().getPreferences().getMachineSetupCNC()) {
//            mBtnBack.setVisibility(View.GONE);
//        }
    }

    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_guide_3dp_dual_extruder_safety_notes;
    }

    @Override
    protected BaseViewModel getViewModel() {
        return null;
    }

    @OnClick(R.id.btn_guide_3dp_dual_extruder_safety_notes_next)
    void onClickNext() {
        ((Guide3DPDualExtruderActivity) requireActivity()).startGuideDualExtruderGetStartedFragment();
    }

}
