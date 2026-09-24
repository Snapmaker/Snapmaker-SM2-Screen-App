package com.snapmaker.fabscreen.modules.welcome.hello;

import com.snapmaker.fabscreen.R;
import fabscreen.libraries.legacy.base.BaseFragment;
import fabscreen.libraries.legacy.base.BaseViewModel;
import com.snapmaker.fabscreen.modules.welcome.WelcomeActivity;

import butterknife.OnClick;

public class WelcomeHelloFragment extends BaseFragment {
    public static WelcomeHelloFragment newInstance() {
        return new WelcomeHelloFragment();
    }

    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_welcome_hello;
    }

    @Override
    protected BaseViewModel getViewModel() {
        return null;
    }

    @OnClick(R.id.btn_welcome_hello_start)
    void onClickStart() {
        if (getActivity() != null) {
            ((WelcomeActivity) getActivity()).startTermsFragment();
        }
    }
}
