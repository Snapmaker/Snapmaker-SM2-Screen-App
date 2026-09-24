package fabscreen.libraries.legacy.lib;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import androidx.annotation.Nullable;

public class PackageHelper {
    private static final String UPDATING_APP_PACKAGE_NAME = "com.snapmaker.updating";

    public static String getUpdatingAppPackageName() {
        return UPDATING_APP_PACKAGE_NAME;
    }

    @Nullable
    public static PackageInfo getUpdatingAppPackageInfo(Context context) {
        PackageManager pm = context.getPackageManager();
        for (PackageInfo info : pm.getInstalledPackages(0)) {
            if (info.packageName.equals(UPDATING_APP_PACKAGE_NAME)) {
                return info;
            }
        }
        return null;
    }

    /**
     * Check if updating is installed.
     */
    public static boolean isUpdatingAppInstalled(Context context) {
        return getUpdatingAppPackageInfo(context) != null;
    }
}
