package fabscreen.libraries.legacy.data.api;

public interface DownloadProgressListener {
    void progress(long read, long contentLength, boolean isDone);
}