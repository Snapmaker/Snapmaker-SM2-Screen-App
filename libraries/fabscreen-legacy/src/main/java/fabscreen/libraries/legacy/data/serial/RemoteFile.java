package fabscreen.libraries.legacy.data.serial;

public class RemoteFile {
    private String mFilename;
    private boolean mIsDir;

    RemoteFile(String filename, boolean isDir) {
        mFilename = filename;
        mIsDir = isDir;
    }

    public boolean isDir() {
        return mIsDir;
    }

    public String getName() {
        return mFilename;
    }
}
