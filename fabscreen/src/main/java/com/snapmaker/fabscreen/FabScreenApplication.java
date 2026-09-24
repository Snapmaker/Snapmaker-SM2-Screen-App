package com.snapmaker.fabscreen;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;

import com.orhanobut.logger.Logger;

import fabscreen.libraries.legacy.BaseApplication;
import fabscreen.libraries.legacy.data.Model;
import fabscreen.libraries.legacy.data.MultiLanguageManager;
import fabscreen.libraries.legacy.lib.DPCHelper;
import fabscreen.libraries.legacy.lib.LogHelper;
import fabscreen.libraries.legacy.server.http.HTTPServer;
import fabscreen.libraries.legacy.server.socket.DiscoverServer;
import fabscreen.libraries.legacy.service.SerialPortService;

public class FabScreenApplication extends BaseApplication {
    private static FabScreenApplication sApplication;
    private DiscoverServer mDiscoverServer;
    private HTTPServer mHTTPServer;

    public static FabScreenApplication getInstance() {
        return sApplication;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        if (BuildConfig.DEBUG) {
            // In simulators, we need an active device owner to enter Kiosk mode
            if (!DPCHelper.isDeviceOwner(this)) {
                DPCHelper.becomeDeviceOwner(this);
            }
        }

        LogHelper.configureLogger(this);

        Logger.i("Application started!");

        // Touchscreen of Snapmaker 2.0 comes with SDK 25 (N_MR1)
        Logger.d("Build SDK version: %d", Build.VERSION.SDK_INT);

        try {
            PackageInfo packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            String version = packageInfo.versionName;
            Logger.d("FabScreen version: %s, build: %s ", version, getBuildTime());
        } catch (PackageManager.NameNotFoundException e) {
            LogHelper.log(e);
        }

        sApplication = this;

        // Reload resources if we change locales/languages.
        int language = getModel().getPreferences().getUserSelectedLanguage();
        MultiLanguageManager.applyApplicationLanguage(getBaseContext(), MultiLanguageManager.language2Locale(language));

//        mFactory = new ViewModelProviderFactory(getModel());

        startServices();

        startDiscoverServer();

        startHttpServer();
    }

    private String getBuildTime() {
        return BuildConfig.BUILD_RELEASE_DATE;
    }

    public void startServices() {
        // start serial port service
        Intent intent = new Intent(this, SerialPortService.class);
        startService(intent);
    }

    public void stopServices() {
        Intent intent = new Intent(this, SerialPortService.class);
        stopService(intent);
    }

    public void startDiscoverServer() {
        mDiscoverServer = new DiscoverServer(this);
        mDiscoverServer.start();
    }

    public void stopDiscoverServer() {
        if (!mDiscoverServer.isInterrupted()) {
            mDiscoverServer.interrupt();
        }
        mDiscoverServer = null;
    }

    public void startHttpServer() {
        mHTTPServer = new HTTPServer();
        mHTTPServer.startServer();
    }

    public void stopHttpServer() {
        if (mHTTPServer != null) {
            mHTTPServer.stopServer();
            mHTTPServer = null;
        }
    }

    public void unregister() {
        Model model = getModel();
        if (model != null) {
            model.getLaserCameraController().unregister(this);
        }
    }
}
