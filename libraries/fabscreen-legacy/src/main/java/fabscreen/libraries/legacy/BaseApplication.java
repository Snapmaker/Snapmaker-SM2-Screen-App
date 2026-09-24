package fabscreen.libraries.legacy;

import android.app.Application;

import com.alibaba.android.arouter.launcher.ARouter;

import fabscreen.libraries.legacy.base.ViewSub;
import fabscreen.libraries.legacy.data.Model;
import fabscreen.libraries.legacy.lib.crash.CrashCollectHandler;
import fabscreen.libraries.legacy.lib.crash.FabScreenActivityManagement;
import fabscreen.libraries.legacy.lib.file.FabLocalFileManager;
import fabscreen.libraries.legacy.lib.file.FabUsbFileManager;

public class BaseApplication extends Application {
    private static BaseApplication sInstance;
    private Model mModel;
    private ViewSub mViewSub;
    private FabUsbFileManager mFabUsbFileManager;
    private FabScreenActivityManagement mFabScreenActivityManagement;

    @Override
    public void onCreate() {
        super.onCreate();
        sInstance = this;
        mFabScreenActivityManagement = new FabScreenActivityManagement();
        CrashCollectHandler.getInstance().init(this);
        registerActivityLifecycleCallbacks(mFabScreenActivityManagement);
        initARouter();
        // initialize model
        mModel = new Model(this);
        mViewSub = new ViewSub(mModel);
        mFabUsbFileManager = new FabUsbFileManager(this);
        mViewSub.setObservableUSBAttach(mFabUsbFileManager.getFileManagerStateObservable());
        mViewSub.listenerHeaderSecurityStatus(mModel.getSlaveComputer().watchHeaderSecurityStatus());

    }

    private void initARouter() {
        if (BuildConfig.DEBUG) {
            ARouter.openLog();
            ARouter.openDebug();
        }
        ARouter.init(this);
    }

    public static BaseApplication getInstance() {
        return sInstance;
    }

    /**
     * Get model instance.
     *
     * @return global model
     */
    public Model getModel() {
        return mModel;
    }

    public ViewSub getViewSub() {
        return mViewSub;
    }

    public FabUsbFileManager getFabUsbFileManager() {
        return mFabUsbFileManager;
    }

    public FabLocalFileManager getFabLocalFileManager() {
        return new FabLocalFileManager(this, this.getFilesDir().getPath());
    }

    public FabScreenActivityManagement getFabScreenActivityManagement() {
        return mFabScreenActivityManagement;
    }
}
