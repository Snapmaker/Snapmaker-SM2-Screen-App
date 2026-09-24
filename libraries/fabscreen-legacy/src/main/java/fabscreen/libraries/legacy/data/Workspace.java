package fabscreen.libraries.legacy.data;

import android.content.Context;
import android.os.Environment;
import android.os.StatFs;

import com.orhanobut.logger.Logger;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import fabscreen.libraries.legacy.BaseApplication;
import fabscreen.libraries.legacy.lib.LogHelper;
import fabscreen.libraries.legacy.lib.file.FabLocalFile;
import fabscreen.libraries.legacy.lib.file.IFile;
import fabscreen.libraries.legacy.lib.file.IFileManager;
import io.reactivex.Observable;
import io.reactivex.Scheduler;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.BehaviorSubject;
import okio.BufferedSink;
import okio.BufferedSource;
import okio.Okio;

public class Workspace {
    private Context mContext;
    private Preferences mPreferences;

    private String mWorkspaceDirPath;
    private IFile mSourceFile;
    private IFile mPrintFile = null;

    private Scheduler.Worker mCopyFileWorker;
    private BehaviorSubject<Boolean> mCopyResultSubject = BehaviorSubject.createDefault(false);

    public Workspace(Context context, Preferences preferences) {
        mContext = context;
        mPreferences = preferences;
        mWorkspaceDirPath = context.getFilesDir() + "/workspace";
    }

    public void initLastPrintFile() {
        String printFilePath = mPreferences.getPrintFilePath();
        if (printFilePath == null) {
            mPrintFile = null;
        } else {
            mPrintFile = new FabLocalFile(new File(printFilePath));
        }

    }

    public IFile getPrintFile() {
        return mPrintFile;
    }

    public float getEstimatedTime() {
        return mPreferences.getPrintFileEstimatedTime();
    }

    public void setEstimatedTime(float estimatedTime) {
        mPreferences.setPrintFileEstimatedTime(estimatedTime);
    }

    public int getFileTotalLineCount() {
        return mPreferences.getPrintFileTotalLines();
    }

    public void setFileTotalLineCount(int totalCount) {
        mPreferences.setPrintFileTotalLines(totalCount);
    }

    public int getPrintSource() {
        return mPreferences.getPrintSource();
    }

    public void setPrintSource(int source) {
        mPreferences.setPrintSource(source);
    }

    public String getFileName() {
        return mPrintFile.getName();
    }

    public Observable<Boolean> addFileToWorkspace(IFile sourceFile) {
        // Reset
        if (mCopyFileWorker != null) {
            mCopyFileWorker.dispose();
        }
        if (mCopyResultSubject != null) {
            mCopyResultSubject = BehaviorSubject.createDefault(false);
        }
        if (!sourceFile.getPath().contains(mWorkspaceDirPath)) {
            clearWorkspaceFile();
        }

        mSourceFile = sourceFile;

        Logger.d("Start copy file into workspace…");

        // Create worker for copy file into workspace
        mCopyFileWorker = Schedulers.io().createWorker();
        mCopyFileWorker.schedule(this::startCopyFile);

        return mCopyResultSubject.hide();
    }

    public void setRemotePrintToWorkSpace(File targetFile) {
        mSourceFile = new FabLocalFile(new File(getWorkspaceDir().getPath(), targetFile.getName()));
        mPrintFile = mSourceFile;
        File workspaceDir = getWorkspaceDir();
        File[] files = workspaceDir.listFiles();
        for (File file : files) {
            if (!file.getName().equals(targetFile.getName())) {
                boolean ret = file.delete();
            }
        }
        mPreferences.setPrintFilePath(targetFile.getPath());
    }

    public Observable<Integer> checkAvailableSpace(IFile sourceFile) {
        if (!sourceFile.exists() || sourceFile.isDirectory()) return Observable.just(-1);

        if (sourceFile.isLocal()) return Observable.just(0);

        StatFs stat = new StatFs(Environment.getDataDirectory().getPath());
        int result = 0;
        long estimatedAvailableSpace = stat.getAvailableBytes() - sourceFile.length();

        if (estimatedAvailableSpace <= 0) {
            result = 2;
        } else if (estimatedAvailableSpace < 300 * 1024 * 1024) {
            result = 1;
        } else {
            result = 0;
        }

        return Observable.just(result);
    }

    private void clearWorkspaceFile() {
        File workspaceDir = getWorkspaceDir();
        File[] files = workspaceDir.listFiles();
        if (files != null) {
            for (File file : files) {
                boolean ret = file.delete();
            }
        }
    }

    private void startCopyFile() {
        // Create new print file
        mPrintFile = new FabLocalFile(new File(getWorkspaceDir().getPath(), mSourceFile.getName()));
        mPreferences.setPrintFilePath(mPrintFile.getPath());

        BufferedSource bufferedSource = null;
        BufferedSink bufferedSink = null;
        // need to distinguish between IFile coming from Local or USB
        IFileManager iFileManager = mSourceFile.isLocal() ?
                BaseApplication.getInstance().getFabLocalFileManager() :
                BaseApplication.getInstance().getFabUsbFileManager();
        try {
            bufferedSource = Okio.buffer(Okio.source(iFileManager.getInputStream(mSourceFile)));
            bufferedSink = Okio.buffer(Okio.sink(new FileOutputStream((File) mPrintFile.getFile())));
            // copy file from source with buffer
            int len;
            byte[] buffer = new byte[1024 * 16];
            while ((len = bufferedSource.read(buffer)) != -1) {
                bufferedSink.write(buffer, 0, len);
            }
            bufferedSink.close();
            bufferedSource.close();
            Logger.d("Copy file into workspace completed.");
            mCopyResultSubject.onNext(true);
        } catch (IOException e) {
            mCopyResultSubject.onError(e);
            LogHelper.log(e);
        } finally {
            iFileManager.deviceHang();
            try {
                if (bufferedSink != null) {
                    bufferedSink.close();
                }
                if (bufferedSource != null) {
                    bufferedSource.close();
                }
            } catch (IOException e) {
                LogHelper.log(e);
            }
        }
    }

    public File getWorkspaceDir() {
        File fileDir = new File(mWorkspaceDirPath);
        if (!fileDir.exists()) {
            boolean ret = fileDir.mkdir();
            if (!ret) {
                return null;
            }
        }
        return fileDir;
    }

    public void dispose() {
        if (mCopyFileWorker != null && !mCopyFileWorker.isDisposed()) {
            mCopyFileWorker.dispose();
        }
    }
}
