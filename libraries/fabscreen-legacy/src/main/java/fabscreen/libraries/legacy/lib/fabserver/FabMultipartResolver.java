package fabscreen.libraries.legacy.lib.fabserver;

import com.yanzhenjie.andserver.http.multipart.MultipartResolver;

import org.apache.commons.fileupload.ProgressListener;

public interface FabMultipartResolver extends MultipartResolver {

    /**
     * Set up ProgressListener used for listen download progress inside {@link FabMultipartResolver}.
     *
     * @param listener the listener to be set.
     */
    void setProgressListener(ProgressListener listener);
}
