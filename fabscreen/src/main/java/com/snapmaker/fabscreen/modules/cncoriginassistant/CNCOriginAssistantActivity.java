package com.snapmaker.fabscreen.modules.cncoriginassistant;

import android.os.Bundle;

import androidx.annotation.Nullable;

import com.snapmaker.fabscreen.R;

import fabscreen.libraries.legacy.base.BaseActivity;

public class CNCOriginAssistantActivity extends BaseActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_default);

        gotoCNCOriginAssistantGetStartedFragment();
    }

    /*
     *  CNC Origin Assistant
     *
     *  1. Origin Assistant Get Started
     *  2. Set Material(workpiece)
     *  3. Install Material
     *  4. Set Carving Tool (carving bit, including custom bit)
     *  5. Origin Assistant Set Origin Intro
     *  6. Safety Goggle
     *  7. Start Origin Assistant
     *  8. Complete
     */

    public void gotoCNCOriginAssistantGetStartedFragment() {
        addFragment(R.id.fragment_container, CNCOriginAssistantGetStartedFragment.newInstance());
    }

    /**
     * Set Material(workpiece)
     * <p>
     * Origin Assistant Step1
     */
    public void gotoCNCOriginAssistantSetMaterialFragment() {
        addFragment(R.id.fragment_container, CNCOriginAssistantSetMaterialFragment.newInstance());
    }

    /**
     * Install Material(workpiece)
     * <p>
     * Origin Assistant Step2
     */
    public void gotoCNCOriginAssistantInstallMaterialFragment() {
        addFragment(R.id.fragment_container, CNCOriginAssistantInstallMaterialFragment.newInstance());
    }

    /**
     * Set Carving Tool
     * <p>
     * Origin Assistant Step3
     */
    public void gotoCNCOriginAssistantSetCarvingToolFragment() {
        addFragment(R.id.fragment_container, CNCOriginAssistantSetCarvingToolFragment.newInstance());
    }

    /**
     * Custom Carving Tool(Bit)
     * <p>
     * Origin Assistant Step3
     */
    public void gotoCNCOriginAssistantCustomBitFragment() {
        addFragment(R.id.fragment_container, CNCOriginAssistantCustomBitFragment.newInstance());
    }

    /**
     * Set Origin Intro
     * <p>
     * Origin Assistant Step4
     */
    public void gotoCNCOriginAssistantSetOriginIntroFragment() {
        addFragment(R.id.fragment_container, CNCOriginAssistantSetOriginIntroFragment.newInstance());
    }

    /**
     * Safety Goggle
     * <p>
     * Origin Assistant Step5
     */
    public void gotoCNCOriginAssistantSafetyGogglesFragment() {
        addFragment(R.id.fragment_container, CNCOriginAssistantSafetyGogglesFragment.newInstance());
    }

    /**
     * Set Origin
     * <p>
     * Origin Assistant Step6
     */
    public void gotoCNCOriginAssistantSetOriginFragment() {
        addFragment(R.id.fragment_container, CNCOriginAssistantSetOriginFragment.newInstance());
    }

    /**
     * Origin Assistant Complete
     * <p>
     * Origin Assistant Step7
     */
    public void gotoCNCOriginAssistantCompleteFragment() {
        addFragment(R.id.fragment_container, CNCOriginAssistantCompleteFragment.newInstance());
    }
}
