package fabscreen.libraries.legacy.server.http;

import com.orhanobut.logger.Logger;

import org.apache.commons.fileupload.ProgressListener;

import java.util.concurrent.TimeUnit;

import fabscreen.libraries.legacy.lib.LogHelper;
import fabscreen.libraries.legacy.lib.fabserver.FabServer;

public class HTTPServer {
    private FabServer mServer;

    private ProgressListener mProgressListener = new ProgressListener() {
        private int progress = -1;

        @Override
        public void update(long pBytesRead, long pContentLength, int pItems) {
            final int percent = Math.round((float) pBytesRead / (float) pContentLength * 100);



            if (percent == progress) return; // unchanged

            if (pContentLength == -1) {
                Logger.w("Content length = -1, unknown length content.");
            } else {
                progress = percent;
                HTTPEventBus.getInstance().onReceiveProgress(progress);
            }
        }
    };

    public HTTPServer() {
        mServer = FabServer.newBuilder()
                .port(8080)
                .timeout(10, TimeUnit.SECONDS)
                .progressListener(mProgressListener)
                .listener(new FabServer.ServerListener() {
                    @Override
                    public void onStarted() {
                        Logger.d("HTTP server started.");
                    }

                    @Override
                    public void onStopped() {
                        Logger.d("HTTP server stopped.");
                    }

                    @Override
                    public void onException(Exception e) {
                        // TODO: An exception occurred while the server was starting.
                        LogHelper.log(e);
                    }
                }).build();
    }

    public void startServer() {
        if (mServer.isRunning()) {
            Logger.w("The server is running, failed to start server.");
        } else {
            mServer.startup();
        }
    }

    public void stopServer() {
        if (mServer.isRunning()) {
            mServer.shutdown();
        } else {
            Logger.w("The server is not running, failed to stop server.");
        }
    }
}
