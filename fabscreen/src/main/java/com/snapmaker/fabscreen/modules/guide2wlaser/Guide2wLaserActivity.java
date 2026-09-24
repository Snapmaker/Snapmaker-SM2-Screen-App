package com.snapmaker.fabscreen.modules.guide2wlaser;

import android.os.Bundle;

import androidx.annotation.Nullable;

import com.snapmaker.fabscreen.R;
import com.snapmaker.fabscreen.modules.guide20wlaser.complete.Guide20wLaserCompleteFragment;
import com.snapmaker.fabscreen.modules.guide20wlaser.getstarted.Guide20wLaserGetStartedFragment;
import com.snapmaker.fabscreen.modules.guide20wlaser.touchplatform.Guide20wLaserPullInFocusLeverFragment;
import com.snapmaker.fabscreen.modules.guide20wlaser.touchplatform.Guide20wLaserPullOutFocusLeverFragment;
import com.snapmaker.fabscreen.modules.guide20wlaser.touchplatform.Guide20wLaserTouchPlatformFragment;
import com.snapmaker.fabscreen.modules.guide20wlaser.touchplatform.Guide20wLaserTouchPlatformFragmentIntroFragment;
import com.snapmaker.fabscreen.modules.guide2wlaser.complete.Guide2wLaserCompleteFragment;
import com.snapmaker.fabscreen.modules.guide2wlaser.getstarted.Guide2wLaserGetStartedFragment;
import com.snapmaker.fabscreen.modules.guide2wlaser.touchplatform.Guide2wLaserPullInFocusLeverFragment;
import com.snapmaker.fabscreen.modules.guide2wlaser.touchplatform.Guide2wLaserPullOutFocusLeverFragment;
import com.snapmaker.fabscreen.modules.guide2wlaser.touchplatform.Guide2wLaserTouchPlatformFragment;
import com.snapmaker.fabscreen.modules.guide2wlaser.touchplatform.Guide2wLaserTouchPlatformFragmentIntroFragment;

import fabscreen.libraries.legacy.base.BaseActivity;

public class Guide2wLaserActivity extends BaseActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            setContentView(R.layout.activity_default);

            startGetStartedFragment();
        }
    }

    public void startGetStartedFragment() {
        addFragment(R.id.fragment_container, Guide2wLaserGetStartedFragment.newInstance());
    }

    public void startPullOutFocusLeverFragment() {
        addFragment(R.id.fragment_container, Guide2wLaserPullOutFocusLeverFragment.newInstance());
    }

    public void startPullInFocusLeverFragment() {
        addFragment(R.id.fragment_container, Guide2wLaserPullInFocusLeverFragment.newInstance());
    }

    /**
     * Touch Platform
     */
    public void startTouchPlatformFragmentIntroFragment() {
        addFragment(
                Guide2wLaserTouchPlatformFragmentIntroFragment.class.getSimpleName(),
                R.id.fragment_container,
                Guide2wLaserTouchPlatformFragmentIntroFragment.newInstance());
    }

    public void startTouchPlatformFragment() {
        addFragment(R.id.fragment_container, Guide2wLaserTouchPlatformFragment.newInstance());
    }

    /**
     * Complete
     */
    public void startCompleteFragment() {
        addFragment(R.id.fragment_container, Guide2wLaserCompleteFragment.newInstance());
    }
}
