package fabscreen.libraries.legacy.lib.file;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import io.reactivex.Observable;

public interface IFileManager {
    /**
     * Mount default device.
     *
     * @return an Observable to indicate the mount result.
     */
    Observable<Boolean> mount();

    /**
     * UnMount default device.
     */
    void unmount();

    IFile getRootFile();

    long getUsedSpace();

    long getTotalSpace();

    Observable<IFile> search(String path);

    boolean isRoot();

    IFile getCurrentDirectory();

    void gotoDirectory(IFile file);

    void popDirectory();

    IFile createFile(IFile file, String name) throws IOException;

    Observable<ArrayList<IFile>> listFiles();

    void close();

    Observable<Boolean> getFileManagerStateObservable();

    /**
     * Remove one file from FileSystem.
     *
     * @param file remove file
     * @throws IOException
     */
    void removeFile(IFile file) throws IOException;

    /**
     * Rename file.
     */
    void renameFile(IFile file, String name) throws IOException;

    /**
     * Get parent file.
     */
    IFile getParent();


    /**
     * Return the InputStream access to a file.
     *
     * @return common InputStream
     * @throws IOException if IFile is a directory.
     */
    InputStream getInputStream(IFile file) throws IOException;

    void deviceHang();
}
