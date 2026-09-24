package com.snapmaker.fabscreen.modules.guide20wlaser.getstarted;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.snapmaker.fabscreen.R;
import com.snapmaker.fabscreen.modules.guide20wlaser.Guide20wLaserActivity;
import com.snapmaker.fabscreen.modules.guide40wlaser.Guide40wLaserActivity;

import butterknife.BindView;
import butterknife.OnClick;
import fabscreen.libraries.legacy.base.BaseFragment;
import fabscreen.libraries.legacy.base.BaseViewModel;

public class Guide20wLaserGetStartedFragment extends BaseFragment {
    public static Guide20wLaserGetStartedFragment newInstance() {
        return new Guide20wLaserGetStartedFragment();
    }

    @BindView(R.id.top_bar_back)
    Button mBtnBack;
    @BindView(R.id.btn_guide_20w_laser_get_started_next)
    Button mBtnStart;

    @BindView(R.id.cb_guide_20w_laser_get_started_accept)
    CheckBox mCbAccept;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (!getModel().getPreferences().getMachineSetup40WLaser()) {
            mBtnBack.setVisibility(View.GONE);
        }

        mBtnStart.setEnabled(false);

        mCbAccept.setOnCheckedChangeListener((buttonView, isChecked) -> {
            mBtnStart.setEnabled(isChecked);
        });
    }

    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_guide_20w_laser_get_started;
    }

    @Override
    protected BaseViewModel getViewModel() {
        return null;
    }

    @OnClick(R.id.btn_guide_20w_laser_get_started_next)
    void onClickNext() {

        // FIXME: Tricky part here.
        //  We need to let user confirm safety notes everytime, even if user back from last page.
        //  Checkbox will be set unchecked if user click next. Need to refactor if any requirement was request with this page in the future.
        mCbAccept.setChecked(false);
        if (getActivity() != null) {
            ((Guide20wLaserActivity) getActivity()).startTouchPlatformFragmentIntroFragment();
        }
    }
}
