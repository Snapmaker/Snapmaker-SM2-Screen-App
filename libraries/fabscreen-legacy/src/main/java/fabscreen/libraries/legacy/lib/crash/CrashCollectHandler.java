package fabscreen.libraries.legacy.lib.crash;

import android.content.Context;
import android.content.Intent;

import com.google.firebase.crashlytics.FirebaseCrashlytics;

import java.util.ArrayList;

import fabscreen.libraries.legacy.BaseApplication;
import fabscreen.libraries.legacy.lib.LogHelper;

public class CrashCollectHandler implements Thread.UncaughtExceptionHandler {
    private Context mContext;
    private Thread.UncaughtExceptionHandler mDefaultHandler;
    public static final String FABSCREEN_CRASH = "FABSCREEN_CRASH";
    private static CrashCollectHandler mInstance;
    private boolean isCrashed;

    public static CrashCollectHandler getInstance() {
        if (mInstance == null) {
            mInstance = new CrashCollectHandler();
        }
        return mInstance;
    }


    public void init(Context context) {
        mContext = context;
        isCrashed = false;
        mDefaultHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(this);
    }

    @Override
    public void uncaughtException(Thread t, Throwable e) {
        if (!isCrashed) {
            synchronized (this) {
                if (!isCrashed) {
                    isCrashed = true;
                    ArrayList<Intent> intents = BaseApplication.getInstance().getFabScreenActivityManagement().getIntents();
                    Intent intent = mContext.getPackageManager().getLaunchIntentForPackage("com.snapmaker.updating");
                    // If the intent is not empty, send the intent to restart
                    if (intent != null) {
                        intent.putParcelableArrayListExtra(FABSCREEN_CRASH, intents);
                        mContext.startActivity(intent);
                    }
                    // Shut down all activit, report an error message, shut down the current process, and terminate the JVM
                    BaseApplication.getInstance().getFabScreenActivityManagement().removeAllActivity();
                    FirebaseCrashlytics.getInstance().recordException(e);
                    LogHelper.log(e);
                    if (intent != null) {
                        intent.putParcelableArrayListExtra(FABSCREEN_CRASH, intents);
                        mContext.startActivity(intent);
                    }
                    android.os.Process.killProcess(android.os.Process.myPid());
                    System.exit(0);
                }
            }

        }
    }
}