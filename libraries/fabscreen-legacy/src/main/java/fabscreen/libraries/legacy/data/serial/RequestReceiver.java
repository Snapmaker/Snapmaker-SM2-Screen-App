package fabscreen.libraries.legacy.data.serial;

import com.orhanobut.logger.Logger;

import java.util.LinkedList;
import java.util.Queue;

import io.reactivex.ObservableEmitter;

public class RequestReceiver {
    private int key;
    private Queue<ObservableEmitter<Object>> mEmitterQueue = new LinkedList<>();
    private ObservableEmitter<Object> mDefaultEmitter;

    public RequestReceiver(int key) {
        this.key = key;
    }

    public int size() {
        return mEmitterQueue.size();
    }

    public void setDefaultEmitter(ObservableEmitter<Object> emitter) {
        mDefaultEmitter = emitter;
    }

    public void addEmitter(ObservableEmitter<Object> emitter) {
        mEmitterQueue.add(emitter);
    }

    public void receive(Object data) {
        if (mEmitterQueue.isEmpty()) {
            if (mDefaultEmitter != null) {
                mDefaultEmitter.onNext(data);
            }
        } else {
            while (true) {
                if (mEmitterQueue.size() == 0) {
                    // Unexpected empty queue
                    Logger.e("emitter queue #%d (%s) is empty.", key, mEmitterQueue.hashCode());
                    break;
                }

                ObservableEmitter<Object> emitter = mEmitterQueue.remove();

                if (emitter.isDisposed()) {
                    // The command is being canceled, discard this emitter and seek for
                    // next not disposed emitter.
                    Logger.d("Found emitter disposed, skip.");
                    continue;
                }

                emitter.onNext(data);
                emitter.onComplete();
                break;
            }
        }
    }

    public void error(Throwable error) {
        if (mEmitterQueue.isEmpty()) {
            if (mDefaultEmitter != null) {
                mDefaultEmitter.onError(error);
            }
        } else {
            ObservableEmitter<Object> emitter = mEmitterQueue.remove();

            emitter.onError(error);
        }
    }

    public void complete() {
        if (mEmitterQueue.isEmpty()) {
            if (mDefaultEmitter != null) {
                mDefaultEmitter.onComplete();
            }
        } else {
            while (true) {
                if (mEmitterQueue.size() == 0) {
                    // Unexpected empty queue
                    Logger.e("emitter queue #%d (%s) is empty.", key, mEmitterQueue.hashCode());
                    break;
                }

                ObservableEmitter<Object> emitter = mEmitterQueue.remove();

                if (emitter.isDisposed()) {
                    // The command is being canceled, discard this emitter and seek for
                    // next not disposed emitter.
                    Logger.d("Found emitter disposed, skip.");
                    continue;
                }

                emitter.onComplete();
                break;
            }
        }
    }
}
