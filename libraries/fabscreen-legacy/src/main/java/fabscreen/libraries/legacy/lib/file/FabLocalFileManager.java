package fabscreen.libraries.legacy.lib.file;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import fabscreen.libraries.legacy.lib.FileHelper;
import io.reactivex.Observable;

public class FabLocalFileManager implements IFileManager {
    private final static String TAG = "FabUsbFileManager";

    private Context context;
    private String rootPath;

    private Stack<IFile> mStack = new Stack<>();
    private static final String ILLEGAL_CHAR_REGEX = "[/|\\\\:*\"<>?]";

    public FabLocalFileManager(Context context, String rootPath) {
        this.context = context;
        this.rootPath = rootPath;
    }

    @Override
    public Observable<Boolean> mount() {
        mStack.clear();
        IFile rootFile = getRootFile();
        mStack.add(rootFile);
        return Observable.just(true);
    }

    @Override
    public void unmount() {
        // no doing
    }

    @Override
    public IFile getRootFile() {
        File file = new File(rootPath);
        return new FabLocalFile(file);
    }

    /**
     * partition size, in bytes.
     */
    @Override
    public long getUsedSpace() {
        // TODO: dynamically change according to file changes (delete)?
        File rootDir = new File(rootPath);
        return rootDir.getTotalSpace() - rootDir.getFreeSpace();
    }

    @Override
    public long getTotalSpace() {
        File rootDir = new File(rootPath);
        return rootDir.getTotalSpace();
    }

    @Override
    public Observable<IFile> search(String path) {
        File file = new File(path);
        FabLocalFile localFile = new FabLocalFile(file);
        return Observable.just(localFile);
    }

    @Override
    public boolean isRoot() {
        return mStack.size() <= 1;
    }

    @Override
    public IFile getCurrentDirectory() {
        return mStack.peek();
    }

    @Override
    public void gotoDirectory(IFile file) {
        mStack.push(file);
    }

    @Override
    public void popDirectory() {
        mStack.pop();
    }

    @Override
    public IFile createFile(IFile file, String name) {
        return null;
    }

    @Override
    public Observable<ArrayList<IFile>> listFiles() {
        ArrayList<IFile> fileList = new ArrayList<>();
        File mFile = (File) mStack.peek().getFile();
        File[] files = mFile.listFiles();
        for (File file : files) {
            fileList.add(new FabLocalFile(file));
        }
        return Observable.just(fileList);
    }

    @Override
    public void close() {
    }

    @Override
    public Observable<Boolean> getFileManagerStateObservable() {
        return Observable.just(false);
    }

    @Override
    public void removeFile(IFile file) {
        FileHelper.removeFile(((File) file.getFile()));
    }

    @Override
    public void renameFile(IFile file, String name) throws IOException {
        if (name.isEmpty() || name.length() > 255) {
            throw new IOException("Filename too long.");
        }

        // Check if there is any illegal characters exists
        final Pattern illegalCharacters = Pattern.compile(ILLEGAL_CHAR_REGEX);
        Matcher matcher = illegalCharacters.matcher(name);
        if (matcher.find()) {
            Log.d(TAG, "String name contains illegal character!");
            throw new IOException("");
        }
        File mFile = (File) file.getFile();
        String newPath = mFile.getParent().concat("/" + name);
        File dest = new File(newPath);

        // duplicate
        if (dest.exists()) {
            // File already exists.
            throw new IOException("File with the given name already exists!");
        }

        // rename file
        if (!mFile.renameTo(dest)) {
            throw new IOException("Failed to rename file.");
        }
    }

    @Override
    public IFile getParent() {
        return null;
    }

    @Override
    public InputStream getInputStream(IFile file) throws IOException {
        if (file.isDirectory()) {
            throw new IOException("Could not get InputStream, " + this.getClass().getName() + " isn't a file.");
        }

        return new FileInputStream((File) file.getFile());
    }

    @Override
    public void deviceHang() {
        // Local cannot be separated, do not Hang
    }
}
