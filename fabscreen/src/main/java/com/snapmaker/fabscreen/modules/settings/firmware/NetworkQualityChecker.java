package com.snapmaker.fabscreen.modules.settings.firmware;

import android.net.TrafficStats;

import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.BehaviorSubject;

public class NetworkQualityChecker {
    public final static int NETWORK_QUALITY_UNKNOWN = 0;
    public final static int NETWORK_QUALITY_NORMAL = 1;
    public final static int NETWORK_QUALITY_BAD = 2;

    private Disposable disposable;
    private long mLastRxBytesCount;
    private BehaviorSubject<Integer> mNetWorkQualitySubject = BehaviorSubject.createDefault(NETWORK_QUALITY_UNKNOWN);
    private float mAverageSpeed = 0; // KB per second

    public NetworkQualityChecker() {

    }

    public Observable<Integer> getNetWorkQualityObservable() {
        return mNetWorkQualitySubject.hide();
    }

    public float getAverageSpeed() {
        return mAverageSpeed;
    }

    private boolean startInterval() {
        if (disposable != null) {
            disposable.dispose();
        }

        // Initial last rx bytes count
        mLastRxBytesCount = TrafficStats.getTotalRxBytes();
        if (mLastRxBytesCount == TrafficStats.UNSUPPORTED) {
            return false;
        }

        // check overall receive speed every 5 seconds
        disposable = Observable.interval(5, TimeUnit.SECONDS)
                .observeOn(Schedulers.computation())
                .subscribe(tick -> {
                    long currentRxBytesCount = TrafficStats.getTotalRxBytes();
                    mAverageSpeed = (currentRxBytesCount - mLastRxBytesCount) / (1024.0f * 5);
                    if (mAverageSpeed < 30) {
                        mNetWorkQualitySubject.onNext(NETWORK_QUALITY_BAD);
                    } else {
                        mNetWorkQualitySubject.onNext(NETWORK_QUALITY_NORMAL);
                    }
                    mLastRxBytesCount = currentRxBytesCount;
                });

        return true;
    }

    public boolean start() {
        return startInterval();
    }

    public void stop() {
        if (disposable != null) {
            disposable.dispose();
        }
        mNetWorkQualitySubject.onNext(NETWORK_QUALITY_UNKNOWN);
        mLastRxBytesCount = 0;
        mAverageSpeed = 0;
    }

}
