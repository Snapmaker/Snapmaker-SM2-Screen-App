package fabscreen.libraries.legacy.data.print;

import android.os.SystemClock;
import android.util.Log;

import com.orhanobut.logger.Logger;

import java.io.IOException;

import fabscreen.libraries.legacy.BaseApplication;
import fabscreen.libraries.legacy.lib.file.IFile;
import fabscreen.libraries.legacy.lib.file.IFileManager;
import okio.BufferedSource;
import okio.Okio;

/**
 * G-code Player.
 * <p>
 * State Machine:
 * IDLE -> PLAYING <-> PAUSED
 * ^                     |
 * |_____________________|
 */
public class GcodePlayer {
    private static final String TAG = "GcodePlayer";

    private IFile mFile;
    private BufferedSource source;

    private String line;

    private int sentCount;
    private int receivedCount;
    private int totalCount;

    private IFileManager mIFileManager;

    GcodePlayer() {
        reset();
    }

    public void reset() {
        totalCount = sentCount = receivedCount = 0;
        line = null;

        if (source != null) {
            try {
                mIFileManager.deviceHang();
                source.close();
            } catch (IOException e) { /* ignore */ }
            source = null;
        }
    }

    void setGcodeFile(IFile file) {
        mFile = file;
    }

    private void openFile() {
        if (source != null) {
            try {
                mIFileManager.deviceHang();
                source.close();
            } catch (IOException e) { /* ignore */ }
            source = null;
        }

        try {
            mIFileManager = mFile.isLocal() ?
                    BaseApplication.getInstance().getFabLocalFileManager() :
                    BaseApplication.getInstance().getFabUsbFileManager();
            source = Okio.buffer(Okio.source(mIFileManager.getInputStream(mFile)));
        } catch (Exception e) {
            Log.e(TAG, "File does not exists.");
            e.printStackTrace();
        }
    }

    void setLineno(int startLineno) {
        // TODO: remove this test code
        final long startTime = SystemClock.elapsedRealtime();

        String line;
        int pos;

        // rewind
        // setGcodeFile(mFile);
        openFile();

        sentCount = receivedCount = startLineno;
        pos = 0;

        // run to start point
        while (pos < startLineno) {
            try {
                line = source.readUtf8Line();
            } catch (IOException e) {
                e.printStackTrace();
                // reset();
                return;
            }

            if (line == null) {
                break;
            }

            pos++;
        }
        Logger.i(String.format("set line number takes %s ms", SystemClock.elapsedRealtime() - startTime));
    }

    void setTotalCount(int totalCount) {
        this.totalCount = totalCount;
    }

    int getTotalCount() {
        return totalCount;
    }

    /**
     * Get next line.
     */
    String nextLine() {
        try {
            line = source.readUtf8Line();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        sentCount++;
        if (sentCount % 1000 == 0) {
            Logger.i(String.format("line %s: %s", sentCount, line));
        }
        return line;
    }

    /**
     * Get current line.
     */
    String getLine() {
        return line;
    }

    /**
     * Get current line number (start from 0).
     *
     * @return int the line number
     */
    int getLineNo() {
        return sentCount - 1;
    }

    void onAck() {
        receivedCount++;
    }

    void skipLine() {
        receivedCount++;
    }

    public float getProgress() {
        if (totalCount == 0) return 0;
        int nowCount = getProgressCount();
        return 1.f * nowCount / totalCount;
    }

    int getSentCount() {
        return sentCount;
    }

    int getReceivedCount() {
        return receivedCount;
    }

    int getProgressCount() {
        // In batch mode, receivedCount will not be modified
        // in single mode, sentCount may be greater than receivedCount
        // GcodePlayer takes the larger number and does not need to determine the mode
        return Math.max(receivedCount, sentCount - 1);
    }
}
