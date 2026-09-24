package com.snapmaker.fabscreen.modules.guide20wlaser;

import android.os.Bundle;

import androidx.annotation.Nullable;

import com.snapmaker.fabscreen.R;
import com.snapmaker.fabscreen.modules.guide20wlaser.complete.Guide20wLaserCompleteFragment;
import com.snapmaker.fabscreen.modules.guide20wlaser.getstarted.Guide20wLaserGetStartedFragment;
import com.snapmaker.fabscreen.modules.guide20wlaser.touchplatform.Guide20wLaserPullInFocusLeverFragment;
import com.snapmaker.fabscreen.modules.guide20wlaser.touchplatform.Guide20wLaserPullOutFocusLeverFragment;
import com.snapmaker.fabscreen.modules.guide20wlaser.touchplatform.Guide20wLaserTouchPlatformFragment;
import com.snapmaker.fabscreen.modules.guide20wlaser.touchplatform.Guide20wLaserTouchPlatformFragmentIntroFragment;

import fabscreen.libraries.legacy.base.BaseActivity;

public class Guide20wLaserActivity extends BaseActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            setContentView(R.layout.activity_default);

            startGetStartedFragment();
        }
    }

    public void startGetStartedFragment() {
        addFragment(R.id.fragment_container, Guide20wLaserGetStartedFragment.newInstance());
    }

    public void startPullOutFocusLeverFragment() {
        addFragment(R.id.fragment_container, Guide20wLaserPullOutFocusLeverFragment.newInstance());
    }

    public void startPullInFocusLeverFragment() {
        addFragment(R.id.fragment_container, Guide20wLaserPullInFocusLeverFragment.newInstance());
    }

    /**
     * Touch Platform
     */
    public void startTouchPlatformFragmentIntroFragment() {
        addFragment(
                Guide20wLaserTouchPlatformFragmentIntroFragment.class.getSimpleName(),
                R.id.fragment_container,
                Guide20wLaserTouchPlatformFragmentIntroFragment.newInstance());
    }

    public void startTouchPlatformFragment() {
        addFragment(R.id.fragment_container, Guide20wLaserTouchPlatformFragment.newInstance());
    }

    /**
     * Complete
     */
    public void startCompleteFragment() {
        addFragment(R.id.fragment_container, Guide20wLaserCompleteFragment.newInstance());
    }
}
