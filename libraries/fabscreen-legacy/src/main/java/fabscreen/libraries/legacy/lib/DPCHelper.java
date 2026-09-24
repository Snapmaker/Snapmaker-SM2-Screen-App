package fabscreen.libraries.legacy.lib;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.os.BatteryManager;
import android.provider.Settings;


import java.io.DataOutputStream;
import java.io.IOException;

import fabscreen.libraries.legacy.receiver.DeviceOwnerReceiver;

public class DPCHelper {
    private static final String Battery_PLUGGED_ANY = Integer.toString(BatteryManager.BATTERY_PLUGGED_AC
            | BatteryManager.BATTERY_PLUGGED_USB
            | BatteryManager.BATTERY_PLUGGED_WIRELESS);

    private static void runCommand(String cmd) {
        if (!cmd.endsWith("\n")) {
            cmd = cmd + "\n";
        }

        try {
            // First use must request superuser
            Process process = Runtime.getRuntime().exec("/system/xbin/su");
            DataOutputStream os = new DataOutputStream(process.getOutputStream());
            os.writeBytes(cmd);
            os.writeBytes("exit\n");
            os.flush();
            os.close();
            process.waitFor();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static boolean isDeviceOwner(Context context) {
        DevicePolicyManager dpm = (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
        return dpm != null && dpm.isDeviceOwnerApp(context.getPackageName());
    }

    /**
     * Set package as device owner.
     */
    public static void becomeDeviceOwner(Context context) {
        if (isDeviceOwner(context)) {
            return;
        }

        String DEVICE_ADMIN_RECEIVER = "com.snapmaker.fabscreen/.receiver.DeviceOwnerReceiver";
        runCommand("dpm set-device-owner " + DEVICE_ADMIN_RECEIVER + "\n");
        runCommand("dpm set-active-admin " + DEVICE_ADMIN_RECEIVER + "\n");
    }

    private static void disableToasts(Context context) {
        if (!isDeviceOwner(context)) {
            return;
        }
        // Optimize: how to avoid duplicate calls
        runCommand("appops set android TOAST_WINDOW deny\n");
    }

    /**
     * Enable LockTask mode.
     *
     * https://developer.android.com/work/cosu
     */
    public static void enableKioskMode(Context context) {
        ComponentName deviceAdmin = new ComponentName(context, DeviceOwnerReceiver.class);

        DevicePolicyManager dpm = (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
        if (dpm == null) return;
        if (!dpm.isAdminActive(deviceAdmin)) return;
        if (!dpm.isDeviceOwnerApp(context.getPackageName())) return;

        // Check if LockTask mode is already set
        if (dpm.isLockTaskPermitted(context.getPackageName())) return;

        disableToasts(context);

        // add package to LockTask packages
        dpm.setLockTaskPackages(deviceAdmin, new String[]{context.getPackageName()});

        // Disable status bar
        dpm.setStatusBarDisabled(deviceAdmin, true);

        // enable STAY_ON_WHILE_PLUGGED_IN
        dpm.setGlobalSetting(
                deviceAdmin,
                Settings.Global.STAY_ON_WHILE_PLUGGED_IN,
                Battery_PLUGGED_ANY);

        /*
        // set dedicated device activity as home intent receiver so that it is started
        // on reboot
        IntentFilter intentFilter = new IntentFilter(Intent.ACTION_MAIN);
        intentFilter.addCategory(Intent.CATEGORY_DEFAULT);
        intentFilter.addCategory(Intent.CATEGORY_HOME);
        ComponentName activity = new ComponentName(context, MainActivity.class);
        dpm.addPersistentPreferredActivity(deviceAdmin, intentFilter, activity);

        // FIXME: for debug, maybe delete this later.
        dpm.clearPackagePersistentPreferredActivities(deviceAdmin, context.getPackageName());
        */
    }
}
