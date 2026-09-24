package com.snapmaker.fabscreen.modules.factory;

import android.graphics.Matrix;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.snapmaker.fabscreen.R;
import fabscreen.libraries.legacy.base.BaseFragment;
import fabscreen.libraries.legacy.base.BaseViewModel;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import fabscreen.libraries.legacy.lib.LogHelper;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.subjects.BehaviorSubject;

public class FactoryCpuBenchmarkFragment extends BaseFragment {
    private static final String TAG = FactoryCpuBenchmarkFragment.class.getSimpleName();

    @BindView(R.id.tv_factory_cpu_usage)
    TextView mTvCpuUsage;

    private BehaviorSubject<Boolean> mStartSubject = BehaviorSubject.createDefault(false);

    public Runnable mRunnable = () -> {
        Random r = new Random();
        while (mStartSubject.getValue()) {
            double ramdomFloat = r.nextDouble() * (r.nextDouble() * 100000000 * Math.PI);
            runMatrix(ramdomFloat * r.nextFloat() + 1);
        }
    };

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setTitle("Cpu Test");

        // init textView
        mTvCpuUsage.setText(String.format(Locale.getDefault(), "Cpu:"));

        // display cpu usage
        Observable.interval(1, TimeUnit.SECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(tick -> {
                    float cpuUsage = getCpuUsage();
                    Log.d("cpuRate", "cpu " + cpuUsage);
                    mTvCpuUsage.setText(String.format(Locale.getDefault(), "Cpu: %.1f", cpuUsage));
                }, Throwable::printStackTrace);

        startStressTest();
    }

    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_factory_cpu_benchmark;
    }

    @Override
    protected BaseViewModel getViewModel() {
        return null;
    }

    @Override
    protected void back() {
        mStartSubject.onNext(false);
        super.back();
    }

    private void startStressTest() {
        mStartSubject.onNext(true);
        // start 4 threads for multi-core processing
        Thread stress1 = new Thread(mRunnable);
        Thread stress2 = new Thread(mRunnable);
        Thread stress3 = new Thread(mRunnable);
        Thread stress4 = new Thread(mRunnable);

        stress1.start();
        stress2.start();
        stress3.start();
        stress4.start();
    }

    // get cpu info from system file "/proc/stat"
    // https://stackoverflow.com/questions/2467579/how-to-get-cpu-usage-statistics-on-android/55173196#55173196
    private float getCpuUsage() {

        Map<String, String> map1 = getCpuInfoMap();
        // totalTime = user + nice + system + idle + ioWait + irq + softIrq
        long totalTime1 = getTotalTime(map1);
        long idleTime1 = Long.parseLong(map1.get("idle"));
        // sleep 360 ms for get second info
        try {
            Thread.sleep(360);
        } catch (InterruptedException e) {
            LogHelper.log(e);
        }

        Map<String, String> map2 = getCpuInfoMap();
        long totalTime2 = getTotalTime(map2);
        long idleTime2 = Long.parseLong(map2.get("idle"));

        return (float) (100 * ((totalTime2 - totalTime1) - (idleTime2 - idleTime1)) / (totalTime2 - totalTime1));
    }

    // random matrix calculate
    private void runMatrix(double r) {
        Random random = new Random();

        Matrix matrix = new Matrix();
        matrix.setScale(100000, 100000);

        for (int i = 0; i < 10000; i++) {
            matrix.setTranslate(random.nextFloat(), random.nextFloat());
            matrix.setRotate(random.nextFloat(), (float) r, (float) r);
        }
    }


    private Map<String, String> getCpuInfoMap() {
        String[] cpuInfo = null;
        // read cpu info with system file
        BufferedReader br = null;
        try {
            br = new BufferedReader(new InputStreamReader(new FileInputStream("/proc/stat")));
            String load = br.readLine();
            br.close();
            cpuInfo = load.split(" ");
        } catch (FileNotFoundException e) {
            LogHelper.log(e);
        } catch (IOException e) {
            LogHelper.log(e);
        }
        Map<String, String> map = new HashMap<>();
        map.put("user", cpuInfo[2]);
        map.put("nice", cpuInfo[3]);
        map.put("system", cpuInfo[4]);
        map.put("idle", cpuInfo[5]);
        map.put("io_wait", cpuInfo[6]);
        map.put("irq", cpuInfo[7]);
        map.put("soft_irq", cpuInfo[8]);
        return map;
    }

    // get cpu info
    private long getTotalTime(Map<String, String> map) {
        return Long.parseLong(map.get("user")) + Long.parseLong(map.get("nice"))
                + Long.parseLong(map.get("system")) + Long.parseLong(map.get("idle"))
                + Long.parseLong(map.get("io_wait")) + Long.parseLong(map.get("irq"))
                + Long.parseLong(map.get("soft_irq"));
    }
}
