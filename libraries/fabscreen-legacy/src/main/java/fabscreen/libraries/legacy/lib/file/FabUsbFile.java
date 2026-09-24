package fabscreen.libraries.legacy.lib.file;

import com.github.mjdev.libaums.fs.UsbFile;


public class FabUsbFile implements IFile {
    private static final String TAG = "FabUsbFile";

    private UsbFile mFile;

    FabUsbFile(UsbFile file) {
        mFile = file;
    }

    @Override
    public String getName() {
        return mFile.getName();
    }

    @Override
    public boolean isLocal() {
        return false;
    }

    @Override
    public boolean isDirectory() {
        return mFile.isDirectory();
    }

    @Override
    public long length() {
        if (mFile.isDirectory()) {
            return 0; // not supported
        } else {
            return mFile.getLength();
        }
    }

    @Override
    public boolean exists() {
        return mFile != null;
    }

    @Override
    public String getPath() {
        return mFile.getAbsolutePath();
    }

    @Override
    public long lastModified() {
        return mFile.lastModified();
    }

    @Override
    public UsbFile getFile() {
        return mFile;
    }
}
