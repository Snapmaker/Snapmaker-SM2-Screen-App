package com.snapmaker.updating;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;


public class PackageReplacedReceiver extends BroadcastReceiver {
    private static final String PACKAGE_NAME = "com.snapmaker.fabscreen";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        String packageName = intent.getDataString();
        if (action == null || !action.equals("android.intent.action.PACKAGE_REPLACED")) return;
        if (packageName == null || !packageName.equals("package:" + PACKAGE_NAME)) return;

        ComponentName componentName = new ComponentName(PACKAGE_NAME, PACKAGE_NAME + ".modules.home.MainActivity");

        Intent launchIntent = context.getPackageManager().getLaunchIntentForPackage(PACKAGE_NAME);

        // Launch newly installed app
        if (launchIntent != null) {
            launchIntent.setComponent(componentName);
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            context.startActivity(launchIntent);
            System.exit(0);
        }
    }
}
