package fabscreen.libraries.legacy.base;

import androidx.lifecycle.ViewModel;

import com.uber.autodispose.lifecycle.CorrespondingEventsFunction;
import com.uber.autodispose.lifecycle.LifecycleEndedException;
import com.uber.autodispose.lifecycle.LifecycleScopeProvider;

import io.reactivex.Observable;
import io.reactivex.subjects.BehaviorSubject;

// Reference [here](https://github.com/uber/AutoDispose/pull/254) to see recipe of
// how to implement AutoDisposeViewModel.
public class AutoDisposeViewModel extends ViewModel implements LifecycleScopeProvider<AutoDisposeViewModel.ViewModelEvent> {
    enum ViewModelEvent {
        CREATED,
        CLEARED
    }

    private BehaviorSubject<ViewModelEvent> lifecycleEvents = BehaviorSubject.createDefault(ViewModelEvent.CREATED);

    @Override
    protected void onCleared() {
        lifecycleEvents.onNext(ViewModelEvent.CLEARED);
        super.onCleared();
    }

    @Override
    public Observable<ViewModelEvent> lifecycle() {
        return lifecycleEvents.hide();
    }

    @Override
    public CorrespondingEventsFunction<ViewModelEvent> correspondingEvents() {
        return CORRESPONDING_EVENTS;
    }

    @Override
    public ViewModelEvent peekLifecycle() {
        return lifecycleEvents.getValue();
    }

    private static CorrespondingEventsFunction<ViewModelEvent> CORRESPONDING_EVENTS = event -> {
        if (event == ViewModelEvent.CREATED) {
            return ViewModelEvent.CLEARED;
        } else {
            throw new LifecycleEndedException("Cannot bind to ViewModel lifecycle after onCleared");
        }
    };
}
