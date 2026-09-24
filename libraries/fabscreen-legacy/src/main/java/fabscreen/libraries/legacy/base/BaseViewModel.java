package fabscreen.libraries.legacy.base;

import com.uber.autodispose.AutoDispose;
import com.uber.autodispose.AutoDisposeConverter;

import fabscreen.libraries.legacy.BaseApplication;
import fabscreen.libraries.legacy.data.Model;

public class BaseViewModel extends AutoDisposeViewModel {
    private final Model mModel;

    protected BaseViewModel() {
        mModel = BaseApplication.getInstance().getModel();
    }

    protected Model getModel() {
        return mModel;
    }

    protected <T> AutoDisposeConverter<T> bindToLifecycle() {
        return AutoDispose.autoDisposable(this);
    }
}
