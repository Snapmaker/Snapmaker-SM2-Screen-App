package fabscreen.libraries.legacy.data;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import java.io.File;

import fabscreen.libraries.legacy.BuildConfig;
import fabscreen.libraries.legacy.data.print.IPrintController;
import fabscreen.libraries.legacy.data.print.PrintController;
import fabscreen.libraries.legacy.data.print.TickCounter;
import fabscreen.libraries.legacy.data.remote.RemoteController;
import fabscreen.libraries.legacy.data.serial.SerialController;
import fabscreen.libraries.legacy.data.version.VersionRequirementManager;
import fabscreen.libraries.legacy.server.http.HTTPEventBus;

public class Model {
    private Preferences preferences;

    private Context mContext;

    private ISlaveComputer slaveComputer;

    private NetworkController mNetworkController;

    private ILaserCameraController mLaserCameraController;

    private MachineController mMachineController;

    private IPrintController mPrintController;

    private RemoteController mRemoteController;

    private Workspace mWorkspace;
    private MultiLanguageManager mMultiLanguageManager;
    private VersionRequirementManager mVersionRequirementManager;

    public Model(Context context) {
        mContext = context;
        preferences = new Preferences(mContext);
        mVersionRequirementManager = new VersionRequirementManager();
        if (BuildConfig.DEBUG) {
            slaveComputer = new MockSlaveComputer();
            mLaserCameraController = new MockLaserCameraController();
        } else {
            slaveComputer = new SerialController(context);
            mLaserCameraController = new LaserCameraController(context);
        }
        mNetworkController = new NetworkController(context);
        mPrintController = new PrintController(slaveComputer, new TickCounter(preferences));
        mMachineController = new MachineController(slaveComputer, mLaserCameraController, mPrintController, preferences, mVersionRequirementManager);
        mRemoteController = new RemoteController(getDataDir(), mPrintController);
        mWorkspace = new Workspace(context, preferences);
        mMultiLanguageManager = new MultiLanguageManager(preferences);
    }

    public Preferences getPreferences() {
        return preferences;
    }

    public ISlaveComputer getSlaveComputer() {
        return slaveComputer;
    }

    public HTTPEventBus getHTTPEventBus() {
        return HTTPEventBus.getInstance();
    }

    public NetworkController getNetworkController() {
        return mNetworkController;
    }

    public ILaserCameraController getLaserCameraController() {
        return mLaserCameraController;
    }

    public MachineController getMachineController() {
        return mMachineController;
    }

    public IPrintController getPrintController() {
        return mPrintController;
    }

    public RemoteController getRemoteController() {
        return mRemoteController;
    }

    public Workspace getWorkspace() {
        return mWorkspace;
    }

    public MultiLanguageManager getMultiLanguageManager() {
        return mMultiLanguageManager;
    }

    public VersionRequirementManager getVersionRequirementManager() {
        return mVersionRequirementManager;
    }

    /**
     * Files dir (i.e. /data/user/0/com.snapmaker.fabscreen/files)
     * <p>
     * Used to storage
     *
     * @return File
     */
    public File getFilesDir() {
        return mContext.getFilesDir();
    }

    public File getCacheDir() {
        return mContext.getCacheDir();
    }

    /**
     * Get application data directory.
     *
     * dataDir = "/data/data/com.snapmaker.fabscreen/app_data"
     */
    public File getDataDir() {
        return mContext.getDir("data", Context.MODE_PRIVATE);
    }

    public String getCurrentVersion() {
        try {
            PackageInfo packageInfo = mContext.getPackageManager().getPackageInfo(mContext.getPackageName(), 0);
            return packageInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            return "Unknown";
        }
    }

    public void onEmergencyStop() {
        slaveComputer.onEmergencyStop();
        mLaserCameraController.onEmergencyStop();
        mPrintController.onEmergencyStop();
        mWorkspace.dispose();
    }

    public void setPrintController(IPrintController printController) {
        mPrintController = printController;
    }
}
