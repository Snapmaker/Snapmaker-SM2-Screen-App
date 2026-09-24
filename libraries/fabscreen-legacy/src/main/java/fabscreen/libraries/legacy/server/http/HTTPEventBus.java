package fabscreen.libraries.legacy.server.http;

import java.io.File;

import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;

public class HTTPEventBus {
    private static HTTPEventBus mInstance;

    private Subject<File> mReceiveFileEvent = PublishSubject.create();
    private Subject<Integer> mReceiveProgressEvent = PublishSubject.create();
    private Subject<String> mForwardGcodeEvent = PublishSubject.create();

    public static HTTPEventBus getInstance() {
        if (mInstance == null) {
            mInstance = new HTTPEventBus();
        }
        return mInstance;
    }

    public Observable<File> watchReceiveFileEvent() {
        return mReceiveFileEvent;
    }

    public Observable<Integer> watchReceiveProgressEvent() {
        return mReceiveProgressEvent;
    }

    public void onReceiveFile(File filename) {
        mReceiveFileEvent.onNext(filename);
    }

    public void onReceiveProgress(Integer progress) {
        mReceiveProgressEvent.onNext(progress);
    }
}
