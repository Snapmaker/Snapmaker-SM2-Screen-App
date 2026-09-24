package com.snapmaker.fabscreen.modules.experiment;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.snapmaker.fabscreen.R;

import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.OnClick;
import butterknife.OnTextChanged;
import fabscreen.libraries.legacy.base.BaseFragment;
import fabscreen.libraries.legacy.base.BaseViewModel;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;

public class ExperimentCrashlyticsTestFragment extends BaseFragment {
    @BindView(R.id.btn_experiment_crashlytic_test)
    Button mBtnCrashTest;
    @BindView(R.id.tv_experiment_crashlytics_show)
    TextView mTvCrashTestShow;

    int mCrashDelayTime;
    Disposable mCrashDelayDisposable;


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setTitle(R.string.all_experiment);

        mBtnCrashTest.setText("Crash!");
    }

    @OnTextChanged(R.id.ed_experiment_crashlytics)
    void onProtectTemperatureChange(CharSequence crashDelayTime) {
        if (crashDelayTime.length()!=0){
            mCrashDelayTime = Integer.parseInt(crashDelayTime.toString());
        }
    }


    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_experiment_crashlytics_test;
    }

    @Override
    protected BaseViewModel getViewModel() {
        return null;
    }

    @OnClick(R.id.btn_experiment_crashlytic_test)
    void onClickCrash() {
        throw new RuntimeException("Crashlytics Test");
    }

    @OnClick(R.id.btn_experiment_crashlytic_test_delay)
    void onClickDelayCrash() {
        mTvCrashTestShow.setText("设置延时Crash时间：" + mCrashDelayTime + " s");
        if (mCrashDelayDisposable != null && !mCrashDelayDisposable.isDisposed()) {
            mCrashDelayDisposable.dispose();
            mCrashDelayDisposable = null;
        }
        // When a crash occurs, the process terminates and all threads are destroyed automatically, without control reclaim.
        mCrashDelayDisposable = Observable.timer(mCrashDelayTime, TimeUnit.SECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(scuess -> {
                    throw new RuntimeException("Crashlytics Test");
                });

    }

}
