package com.snapmaker.fabscreen.modules.guiderotary.cnc;

import android.os.Bundle;

import androidx.annotation.Nullable;

import com.snapmaker.fabscreen.R;

import fabscreen.libraries.legacy.base.BaseActivity;

public class GuideRotaryCNCActivity extends BaseActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            setContentView(R.layout.activity_default);

            startGetStartedFragment();
        }
    }

    public void startGetStartedFragment() {
        addFragment(R.id.fragment_container, GuideRotaryCNCGetStartedFragment.newInstance());
    }
    public void startOriginAssistantIntroFragment() {
        addFragment(R.id.fragment_container, GuideRotaryCNCOriginAssistantIntroFragment.newInstance());
    }

    public void startBitAssistantIntroFragment() {
        addFragment(R.id.fragment_container, GuideRotaryCNCBitAssistantIntroFragment.newInstance());
    }

    public void startCompleteFragment() {
        addFragment(R.id.fragment_container, GuideRotaryCNCCompleteFragment.newInstance());
    }
}
