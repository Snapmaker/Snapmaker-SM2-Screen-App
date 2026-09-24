package fabscreen.libraries.legacy.lib.file;

import android.util.Log;

import java.io.File;

import fabscreen.libraries.legacy.lib.FileHelper;

public class FabLocalFile implements IFile {
    private static final String TAG = "FabLocalFile";

    private File mFile;

    public FabLocalFile(File file) {
        mFile = file;
    }

    @Override
    public String getName() {
        return mFile.getName();
    }

    @Override
    public boolean isLocal() {
        return true;
    }

    @Override
    public boolean isDirectory() {
        return mFile.isDirectory();
    }

    @Override
    public long length() {
        if (mFile.isDirectory()) {
            // not supported
            return 0;
        } else {
            return mFile.length();
        }
    }

    @Override
    public boolean exists() {
        return mFile.exists();
    }

    @Override
    public String getPath() {
        return mFile.getPath();
    }

    @Override
    public long lastModified() {
        return mFile.lastModified();
    }

    @Override
    public File getFile() {
        return mFile;
    }
}
