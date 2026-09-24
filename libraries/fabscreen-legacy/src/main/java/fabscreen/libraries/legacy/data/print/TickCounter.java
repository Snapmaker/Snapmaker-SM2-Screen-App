package fabscreen.libraries.legacy.data.print;

import java.util.concurrent.TimeUnit;

import fabscreen.libraries.legacy.data.Preferences;
import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.BehaviorSubject;

public class TickCounter {
    private Preferences preferences;
    private Disposable disposable;
    private BehaviorSubject<Integer> countSubject = BehaviorSubject.createDefault(0);
    private int totalTicks = 0;
    private int tick = 0;

    public TickCounter(Preferences preferences) {
        this.preferences = preferences;
        countSubject.onNext(0);
    }

    public int getCount() {
        return countSubject.getValue();
    }

    public Observable<Integer> getCountObservable() {
        return countSubject.hide();
    }

    private void startInterval() {
        if (disposable != null) {
            disposable.dispose();
        }

        disposable = Observable.interval(1, TimeUnit.SECONDS)
                .observeOn(Schedulers.computation())
                .subscribe(sequence -> {
                    tick += 1;

                    countSubject.onNext(totalTicks + tick);

                    if (tick % 60 == 0) {
                        save();
                    }
                });
    }


    public void load() {
        totalTicks = preferences.getPrintElapsedTime();
        tick = 0;
        countSubject.onNext(totalTicks);
    }

    /**
     * Save elapsed time, so we can recover it when power-loss.
     */
    public void save() {
        preferences.setPrintElapsedTime(totalTicks + tick);
    }

    public void reset() {
        totalTicks = 0;
        tick = 0;
        countSubject.onNext(0);
    }

    public void start() {
        save();
        startInterval();
    }

    public void stop() {
        totalTicks += tick;
        tick = 0;

        if (disposable != null) {
            disposable.dispose();
        }
    }
}
