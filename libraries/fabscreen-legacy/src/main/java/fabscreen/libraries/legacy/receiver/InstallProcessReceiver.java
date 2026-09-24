package fabscreen.libraries.legacy.receiver;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageInfo;
import android.content.pm.PackageInstaller;
import android.util.Log;

import androidx.annotation.NonNull;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

import fabscreen.libraries.legacy.BuildConfig;
import fabscreen.libraries.legacy.R;
import fabscreen.libraries.legacy.lib.PackageHelper;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class InstallProcessReceiver extends BroadcastReceiver {
    private static final String TAG = "InstallProcessReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        String url = intent.getStringExtra("URL");
        String operation = intent.getStringExtra("OPERATION");

        if (operation == null) return;

        switch (operation) {
            case "update": {
                if (url != null) {
                    downloadAndInstall(context, url);
                } else {
                    installUpdatingPackage(context);
                }
                break;
            }
            case "factory_reset": {
                factoryReset(BuildConfig.APPLICATION_ID);
                break;
            }
            case "local_file": {
                startUpdatingApp(context);

                if (BuildConfig.DEBUG) {
                    install(context, BuildConfig.APPLICATION_ID, url);
                } else {
                    install(url);
                }
                break;
            }
        }
    }

    private void startUpdatingApp(Context context) {
        if (context != null) {
            Intent updateIntent = context.getPackageManager().getLaunchIntentForPackage("com.snapmaker.updating");
            context.startActivity(updateIntent);
        }
    }

    private void installUpdatingPackage(Context context) {
        final String packageName = PackageHelper.getUpdatingAppPackageName();

        // If update app is installed, then skip installation
        PackageInfo packageInfo = PackageHelper.getUpdatingAppPackageInfo(context);
        if (packageInfo != null && packageInfo.versionName.compareTo("1.3") >= 0) {
            Log.d(TAG, packageName + " " + packageInfo.versionName + " is already installed");
            return;
        }

        // Install updating app
        Log.d(TAG, "Installing " + packageName + "...");
        InputStream is = context.getResources().openRawResource(R.raw.fabscreen_updating_1_3);
        install(context, packageName, is);
    }

    private void downloadAndInstall(Context context, String url) {
        Request request = new Request.Builder().url(url).build();
        OkHttpClient client = new OkHttpClient();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, IOException e) {
                // Download failed
                e.printStackTrace();
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                InputStream is = null;
                FileOutputStream fos = null;

                try {
                    is = response.body().byteStream();

                    File file = new File(context.getFilesDir(), "download.apk");
                    fos = new FileOutputStream(file);

                    byte[] buf = new byte[1024];
                    int n;
                    while ((n = is.read(buf)) != -1) {
                        fos.write(buf, 0, n);
                    }

                    fos.flush();
                    fos.close();

                    // Launch updating app when updating
                    if (PackageHelper.isUpdatingAppInstalled(context)) {
                        startUpdatingApp(context);
                    }

                    // Install
                    install(file.getAbsolutePath());
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    if (is != null) {
                        is.close();
                    }
                    if (fos != null) {
                        fos.close();
                    }
                }
            }
        });
    }

    private void factoryReset(String packageName) {
        // usage: pm uninstall -k [packageName]

        String[] args = {"pm", "uninstall", "-k", packageName};

        ProcessBuilder processBuilder = new ProcessBuilder(args);
        Process process = null;
        BufferedReader successResult = null;
        BufferedReader errorResult = null;
        StringBuilder successMsg = new StringBuilder();
        StringBuilder errorMsg = new StringBuilder();

        try {
            process = processBuilder.start();
            successResult = new BufferedReader(new InputStreamReader(process.getInputStream()));
            errorResult = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            String result;
            while ((result = successResult.readLine()) != null) {
                successMsg.append(result);
            }
            while ((result = errorResult.readLine()) != null) {
                errorMsg.append(result);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (successResult != null) {
                    successResult.close();
                }
                if (errorResult != null) {
                    errorResult.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (process != null) {
                process.destroy();
            }
        }
    }

    private void install(String apkPath) {
        File file = new File(apkPath);
        if (apkPath.length() == 0 || file.length() <= 0 || !file.exists() || !file.isFile()) {
            Log.e(TAG, "file read fail");
            return;
        }
        String[] args = {"pm", "install", "-r", "-i", "com.snapmaker.fabscreen", "--user", "0", apkPath};
        ProcessBuilder processBuilder = new ProcessBuilder(args);
        Process process = null;
        BufferedReader successResult = null;
        BufferedReader errorResult = null;
        StringBuilder successMsg = new StringBuilder();
        StringBuilder errorMsg = new StringBuilder();
        try {
            process = processBuilder.start();
            successResult = new BufferedReader(new InputStreamReader(process.getInputStream()));
            errorResult = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            String result;
            while ((result = successResult.readLine()) != null) {
                successMsg.append(result);
            }
            while ((result = errorResult.readLine()) != null) {
                errorMsg.append(result);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (successResult != null) {
                    successResult.close();
                }
                if (errorResult != null) {
                    errorResult.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (process != null) {
                process.destroy();
            }
        }
    }

    private void install(Context context, String packageName, String apkPath) {
        File file = new File(apkPath);
        try {
            FileInputStream fis = new FileInputStream(file);
            install(context, packageName, fis);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void install(Context context, String packageName, InputStream is) {
        // Prepare params for installing one APK file with MODE_FULL_INSTALL
        // We could use MODE_INHERIT_EXISTING to install multiple split APKs
        PackageInstaller packageInstaller = context.getPackageManager().getPackageInstaller();
        PackageInstaller.SessionParams params = new PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL);

        // Get a PackageInstaller.Session for performing the actual update
        try {
            int sessionId = packageInstaller.createSession(params);
            PackageInstaller.Session session = packageInstaller.openSession(sessionId);

            // Copy APK file bytes into OutputStream provided by install Session
            OutputStream outputStream = session.openWrite(packageName, 0, -1);

            BufferedInputStream bis = new BufferedInputStream(is);

            final int readSize = 1 << 16;
            final byte b[] = new byte[readSize];
            int n;
            while ((n = bis.read(b, 0, readSize)) != -1) {
                outputStream.write(b, 0, n);
            }
            bis.close();
            is.close();

            outputStream.flush();
            session.fsync(outputStream);

            outputStream.close();

            // The app gets killed after installation session commit
            PendingIntent mPendingIntent = PendingIntent.getBroadcast(context, sessionId, new Intent("android.intent.action.MAIN"), 0);
            IntentSender mIntentSender = mPendingIntent.getIntentSender();
            session.commit(mIntentSender);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}