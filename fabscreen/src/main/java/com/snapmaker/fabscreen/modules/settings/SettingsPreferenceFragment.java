package com.snapmaker.fabscreen.modules.settings;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.snapmaker.fabscreen.R;
import fabscreen.libraries.legacy.base.BaseFragment;
import fabscreen.libraries.legacy.base.BaseViewModel;

import butterknife.BindView;
import butterknife.OnClick;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.subjects.BehaviorSubject;

public class SettingsPreferenceFragment extends BaseFragment {
    public static SettingsPreferenceFragment getInstance() {
        return new SettingsPreferenceFragment();
    }

    @BindView(R.id.btn_settings_preferences_analysis)
    Button mBtnAnalysis;

    private BehaviorSubject<Boolean> mFirebaseAnalyticsOptionSubject = BehaviorSubject.createDefault(true);

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setTitle(R.string.settings_user_preference);

        initView();
    }

    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_settings_preference;
    }

    @Override
    protected BaseViewModel getViewModel() {
        return null;
    }

    private void initView() {
        boolean firebaseAnalyticsFlag = getModel().getPreferences().getFirebaseAnalyticsFlag();
        mFirebaseAnalyticsOptionSubject.onNext(firebaseAnalyticsFlag);

        mFirebaseAnalyticsOptionSubject
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(enabled -> {
                    mBtnAnalysis.setActivated(enabled);
                    getFirebaseAnalytics().setAnalyticsCollectionEnabled(enabled);
                    FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(enabled);
                });

    }

    @OnClick(R.id.btn_settings_preferences_analysis)
    void onClickAnalysisOption() {
        boolean firebaseAnalyticsFlag = getModel().getPreferences().getFirebaseAnalyticsFlag();
        firebaseAnalyticsFlag = !firebaseAnalyticsFlag;

        getModel().getPreferences().setFirebaseAnalyticsFlag(firebaseAnalyticsFlag);
        mFirebaseAnalyticsOptionSubject.onNext(firebaseAnalyticsFlag);
    }

}
