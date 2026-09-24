package com.snapmaker.fabscreen.modules.welcome.terms;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.snapmaker.fabscreen.R;
import fabscreen.libraries.legacy.base.BaseFragment;
import fabscreen.libraries.legacy.base.BaseViewModel;
import com.snapmaker.fabscreen.modules.welcome.WelcomeActivity;

import butterknife.BindView;
import butterknife.OnClick;

public class WelcomeTermsFragment extends BaseFragment {
    public static WelcomeTermsFragment newInstance() {
        return new WelcomeTermsFragment();
    }

    @BindView(R.id.cb_welcome_terms_accept)
    CheckBox mCbConfirm;

    @BindView(R.id.btn_welcome_terms_agree)
    Button mBtnNext;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mBtnNext.setEnabled(false);

        mCbConfirm.setOnCheckedChangeListener((buttonView, isChecked) -> {
            mBtnNext.setEnabled(isChecked);
        });
    }

    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_welcome_terms;
    }

    @Override
    protected BaseViewModel getViewModel() {
        return null;
    }

    @OnClick(R.id.btn_welcome_terms_agree)
    void onClickNext() {
        if (getActivity() != null) {
            ((WelcomeActivity) getActivity()).startNameFragment();
        }
    }
}
