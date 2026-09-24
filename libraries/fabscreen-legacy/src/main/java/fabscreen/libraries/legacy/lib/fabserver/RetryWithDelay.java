package fabscreen.libraries.legacy.lib.fabserver;

import com.orhanobut.logger.Logger;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import io.reactivex.Observable;
import io.reactivex.functions.Function;

public class RetryWithDelay implements Function<Observable<? extends Throwable>, Observable<?>> {

    private int mMaxRetries;
    private int mRetryDelayMillis;
    private int mRetryCount;

    public RetryWithDelay(int maxRetries, int retryDelayMillis) {
        this.mMaxRetries = maxRetries;
        mRetryDelayMillis = retryDelayMillis;
    }

    public RetryWithDelay(int maxRetries) {
        this.mMaxRetries = maxRetries;
    }

    public RetryWithDelay() {
    }

    @Override
    public Observable<?> apply(Observable<? extends Throwable> attempts) {
        return attempts.flatMap(e -> {
            if (e instanceof TimeoutException) {
                ++mRetryCount;
                Logger.d("Retry with delay number:" + mRetryCount);
                if (mMaxRetries == 0) {
                    return Observable.timer(mRetryCount, TimeUnit.SECONDS);
                } else {
                    if (mRetryCount <= mMaxRetries) {
                        return Observable.timer(mRetryDelayMillis == 0 ? mRetryCount : mRetryDelayMillis, TimeUnit.SECONDS);
                    }
                    return Observable.error(e);
                }
            }
            return Observable.error(e);
        });
    }
}
