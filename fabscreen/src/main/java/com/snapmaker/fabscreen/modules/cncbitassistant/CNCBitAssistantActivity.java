package com.snapmaker.fabscreen.modules.cncbitassistant;

import android.os.Bundle;

import androidx.annotation.Nullable;

import com.snapmaker.fabscreen.R;

import fabscreen.libraries.legacy.base.BaseActivity;

public class CNCBitAssistantActivity extends BaseActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_default);

        gotoCNCBitAssistantIntro();
    }

    public void gotoCNCBitAssistantIntro() {
        addFragment(R.id.fragment_container, CNCBitAssistantIntroFragment.newInstance());
    }

    public void gotoCNCBitAssistantSafetyGoggles() {
        addFragment(R.id.fragment_container, CNCBitAssistantSafetyGogglesFragment.newInstance());
    }

    public void gotoCNCBitAssistantStep1() {
        addFragment(R.id.fragment_container, CNCBitAssistantStep1Fragment.newInstance());
    }

    public void gotoCNCBitAssistantStep2Intro() {
        addFragment(R.id.fragment_container, CNCBitAssistantStep2IntroFragment.newInstance());
    }

    public void gotoCNCBitAssistantStep2() {
        addFragment(R.id.fragment_container, CNCBitAssistantStep2Fragment.newInstance());
    }

    public void gotoCNCBitAssistantComplete() {
        addFragment(R.id.fragment_container, CNCBitAssistantCompleteFragment.newInstance());
    }
}
