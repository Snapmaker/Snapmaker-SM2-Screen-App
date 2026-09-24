package com.snapmaker.fabscreen.modules.welcome;

import android.os.Bundle;

import androidx.annotation.Nullable;

import com.snapmaker.fabscreen.R;
import com.snapmaker.fabscreen.modules.welcome.hello.WelcomeHelloFragment;
import com.snapmaker.fabscreen.modules.welcome.language.WelcomeLanguageFragment;
import com.snapmaker.fabscreen.modules.welcome.name.WelcomeNameFragment;
import com.snapmaker.fabscreen.modules.welcome.terms.WelcomeTermsFragment;
import com.snapmaker.fabscreen.modules.welcome.wifi.WelcomeWifiFragment;
import com.snapmaker.fabscreen.modules.welcome.wifipassword.WelcomeWifiPasswordFragment;

import fabscreen.libraries.legacy.base.BaseActivity;

public class WelcomeActivity extends BaseActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            setContentView(R.layout.activity_default);
            // Hide language entry before copywriting has confirmed.
            if (getModel().getPreferences().getMachineSetupLanguage()) {
                startHelloFragment();
            } else {
                startLanguageFragment();
            }
        }
    }

    /**
     * Language Page 0, Language
     */
    public void startLanguageFragment() {
        addFragment(R.id.fragment_container, WelcomeLanguageFragment.newInstance());
    }

    /**
     * Welcome page 1, hello
     */
    public void startHelloFragment() {
        addFragment(R.id.fragment_container, WelcomeHelloFragment.newInstance());
    }

    /**
     * Welcome page 2, terms
     * <p>
     * User should agree terms and conditions to use this app.
     */
    public void startTermsFragment() {
        addFragment(R.id.fragment_container, WelcomeTermsFragment.newInstance());
    }

    /**
     * Welcome page 3, name
     */
    public void startNameFragment() {
        addFragment(R.id.fragment_container, WelcomeNameFragment.newInstance());
    }

    /**
     * Welcome page 4, Wi-Fi
     */
    public void startWiFiFragment() {
        addFragment(R.id.fragment_container, WelcomeWifiFragment.newInstance());
    }

    /**
     * Welcome page 4, Wi-Fi password
     */
    public void startPasswordFragment() {
        addFragment(R.id.fragment_container, WelcomeWifiPasswordFragment.newInstance());
    }
}
