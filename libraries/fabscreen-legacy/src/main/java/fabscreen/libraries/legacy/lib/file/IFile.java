package fabscreen.libraries.legacy.lib.file;

public interface IFile {
    /**
     * Return the actual file or directory name or '/' for root directory.
     *
     * @return The name of the file or directory.
     */
    String getName();

    /**
     * @return True if representing a directory.
     */
    boolean isDirectory();

    /**
     * @return True if file is not in external storage.
     */
    boolean isLocal();

    /**
     * Return the file length.
     *
     * @return File length in bytes.
     */
    long length();

    /**
     * @return Return the file path according to where the file is.
     */
    String getPath();

    /**
     * Return the time this directory or file was last modified.
     *
     * @return Time in milliseconds since January 1 00:00:00, 1970 UTC
     */
    long lastModified();

    /**
     * @return Object the inner file object.
     */
    Object getFile();

    /**
     * @return True if file exists.
     */
    boolean exists();

}
