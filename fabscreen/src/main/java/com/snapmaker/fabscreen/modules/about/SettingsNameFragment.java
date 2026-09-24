package com.snapmaker.fabscreen.modules.about;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.snapmaker.fabscreen.R;
import com.snapmaker.fabscreen.modules.welcome.name.WelcomeNameViewModel;

import butterknife.BindView;
import butterknife.OnClick;
import fabscreen.libraries.legacy.base.BaseFragment;
import io.reactivex.android.schedulers.AndroidSchedulers;

public class SettingsNameFragment extends BaseFragment {
    public static SettingsNameFragment getInstance() {
        return new SettingsNameFragment();
    }

    @BindView(R.id.et_welcome_name_input)
    EditText mEtMachineName;
    @BindView(R.id.tv_welcome_name_tip)
    TextView mTvMachineNameTip;
    @BindView(R.id.btn_welcome_name_next)
    Button mBtnSave;

    private WelcomeNameViewModel mViewModel;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mViewModel = getViewModel();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initView();
    }

    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_welcome_name;
    }

    private void initView() {
        mEtMachineName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                mViewModel.updateName(s.toString());
            }
        });

        mViewModel.getNameObservable()
                .take(1)
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(name -> mEtMachineName.setText(name));

        mViewModel.getNameTipObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(tip -> {
                    switch (tip) {
                        case WelcomeNameViewModel.TIP_OK:
                            mTvMachineNameTip.setText("");
                            mBtnSave.setEnabled(true);
                            break;
                        case WelcomeNameViewModel.TIP_EMPTY:
                            mTvMachineNameTip.setText(R.string.welcome_name_tip_empty);
                            mBtnSave.setEnabled(false);
                            break;
                    }
                });
    }

    @Override
    protected WelcomeNameViewModel getViewModel() {
        return getViewModelProvider().get(WelcomeNameViewModel.class);
    }

    @OnClick(R.id.btn_welcome_name_next)
    void save() {
        mViewModel.saveName();
        back();
    }
}
