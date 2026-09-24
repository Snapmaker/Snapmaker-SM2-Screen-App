package com.snapmaker.fabscreen.modules.guide40wlaser;

import android.os.Bundle;

import androidx.annotation.Nullable;

import com.snapmaker.fabscreen.R;
import com.snapmaker.fabscreen.modules.guide40wlaser.complete.Guide40wLaserCompleteFragment;
import com.snapmaker.fabscreen.modules.guide40wlaser.getstarted.Guide40wLaserGetStartedFragment;
import com.snapmaker.fabscreen.modules.guide40wlaser.touchplatform.Guide40wLaserPullInFocusLeverFragment;
import com.snapmaker.fabscreen.modules.guide40wlaser.touchplatform.Guide40wLaserPullOutFocusLeverFragment;
import com.snapmaker.fabscreen.modules.guide40wlaser.touchplatform.Guide40wLaserTouchPlatformFragment;
import com.snapmaker.fabscreen.modules.guide40wlaser.touchplatform.Guide40wLaserTouchPlatformFragmentIntroFragment;
import com.snapmaker.fabscreen.modules.settings.advancedlaser.TouchPlatformViewModel;

import fabscreen.libraries.legacy.base.BaseActivity;

public class Guide40wLaserActivity extends BaseActivity {

    private TouchPlatformViewModel mPlatformHeightViewModel;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            setContentView(R.layout.activity_default);

            mPlatformHeightViewModel = getViewModel(TouchPlatformViewModel.class);

            startGetStartedFragment();
        }
    }

    public void startGetStartedFragment() {
        addFragment(R.id.fragment_container, Guide40wLaserGetStartedFragment.newInstance());
    }

    public void startPullOutFocusLeverFragment() {
        addFragment(R.id.fragment_container, Guide40wLaserPullOutFocusLeverFragment.newInstance());
    }

    public void startPullInFocusLeverFragment() {
        addFragment(R.id.fragment_container, Guide40wLaserPullInFocusLeverFragment.newInstance());
    }

    /**
     * Touch Platform
     */
    public void startTouchPlatformFragmentIntroFragment() {
        addFragment(
                Guide40wLaserTouchPlatformFragmentIntroFragment.class.getSimpleName(),
                R.id.fragment_container,
                Guide40wLaserTouchPlatformFragmentIntroFragment.newInstance());
    }

    public void startTouchPlatformFragment() {
        addFragment(R.id.fragment_container, Guide40wLaserTouchPlatformFragment.newInstance());
    }


    /**
     * Complete
     */
    public void startCompleteFragment() {
        addFragment(R.id.fragment_container, Guide40wLaserCompleteFragment.newInstance());
    }

}
