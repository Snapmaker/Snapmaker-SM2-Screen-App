package fabscreen.libraries.legacy.lib;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.orhanobut.logger.AndroidLogAdapter;
import com.orhanobut.logger.CsvFormatStrategy;
import com.orhanobut.logger.DiskLogAdapter;
import com.orhanobut.logger.FormatStrategy;
import com.orhanobut.logger.LogStrategy;
import com.orhanobut.logger.Logger;
import com.orhanobut.logger.PrettyFormatStrategy;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class LogHelper {
    private static final int MAX_BYTES = 1000 * 1024; // 1000K (~10000 lines)

    private static final int LEVEL_TRACE = 0;
    private static final int LEVEL_VERBOSE = 1;
    private static final int LEVEL_INFO = 2;
    private static final int LEVEL_WARNING = 3;
    private static final int LEVEL_ERROR = 4;
    private static final int LEVEL_FATAL = 5;
    private static final int LEVEL_CLOSED = 6;

    private static int firmwareLogLevel = LEVEL_INFO;

    static public void configureLogger(Context context) {
        String diskPath = context.getCacheDir().getAbsolutePath();
        String folder = diskPath + File.separatorChar + "log";

        // Logcat
        FormatStrategy formatStrategy = PrettyFormatStrategy.newBuilder()
                .showThreadInfo(false)
                .methodCount(0)
                .tag("Logger")
                .build();

        Logger.addLogAdapter(new AndroidLogAdapter(formatStrategy));

        // Disk
        // screen log strategy
        HandlerThread handlerThread = new HandlerThread("Logger." + folder);
        handlerThread.start();

        Handler writeHandler = new WriteHandler(handlerThread.getLooper(), folder);
        LogStrategy diskLogStrategy = new DiskLogStrategy(writeHandler);

        FormatStrategy diskFormatStrategy = CsvFormatStrategy.newBuilder()
                .logStrategy(diskLogStrategy)
                .tag("SC")
                .build();
        Logger.addLogAdapter(new DiskLogAdapter(diskFormatStrategy));

        cleanLogFiles(folder, "SC");
        cleanLogFiles(folder, "FW");
    }

    static public void setFirmwareLogLevel(int level) {
        firmwareLogLevel = level;
    }

    static public void firmwareLog(int level, String msg) {
        if (level < firmwareLogLevel) {
            return;
        }

        int loggerLevel = 2;
        switch (level) {
            case LEVEL_TRACE:
            case LEVEL_VERBOSE:
                loggerLevel = Logger.VERBOSE;
                break;
            case LEVEL_INFO:
                loggerLevel = Logger.INFO;
                break;
            case LEVEL_WARNING:
                loggerLevel = Logger.WARN;
                break;
            case LEVEL_ERROR:
                loggerLevel = Logger.ERROR;
                break;
            case LEVEL_FATAL:
                loggerLevel = Logger.ASSERT;
                break;
        }

        Logger.log(loggerLevel, "FW", msg, null);
    }

    /**
     * Log Throwable just as Throwable::printStackTrace() did.
     */
    static public void log(Throwable e) {
        Logger.e(e.toString());

        StackTraceElement[] trace = e.getStackTrace();
        for (StackTraceElement traceElement : trace) {
            Logger.e("\tat " + traceElement);
        }
    }

    static private File getLogFile(@NonNull String folderName, @NonNull String fileName) {
        File folder = new File(folderName);
        if (!folder.exists()) {
            folder.mkdirs();
        }

        int fileNo = 0;
        File file;
        File existingFile = null;

        while (true) {
            file = new File(folder, String.format("%s_%s.log", fileName, fileNo));
            if (!file.exists()) {
                break;
            }
            existingFile = file;
            fileNo++;
        }

        if (existingFile != null && existingFile.length() < MAX_BYTES) {
            file = existingFile;
        }

        return file;
    }

    static private void cleanLogFiles(@NonNull String folder, @NonNull String fileName) {
        int fileNo = 0;
        File file;
        File existingFile = null;

        while (true) {
            file = new File(folder, String.format("%s_%s.log", fileName, fileNo));
            if (!file.exists()) {
                break;
            }
            existingFile = file;
            fileNo++;
        }

        if (existingFile != null && existingFile.length() < MAX_BYTES) {
            fileNo--;
        }

        // Keep latest 3 log files, and clean others' content
        for (int i = 0; i < fileNo - 2; i++) {
            file = new File(folder, String.format("%s_%s.log", fileName, i));
            FileWriter fileWriter = null;
            try {
                fileWriter = new FileWriter(file, false);
                fileWriter.close();
            } catch (IOException e) {
                if (fileWriter != null) {
                    try {
                        fileWriter.close();
                    } catch (IOException e1) { /* fail silently */ }
                }
            }
        }
    }

    static class DiskLogStrategy implements LogStrategy {
        @NonNull
        private final Handler handler;

        DiskLogStrategy(@NonNull Handler handler) {
            this.handler = handler;
        }

        @Override
        public void log(int priority, @Nullable String tag, @NonNull String message) {
            int arg1 = "SC".equals(tag) ? 0 : 1;
            handler.sendMessage(handler.obtainMessage(priority, arg1, 0, message));
        }
    }

    static class WriteHandler extends Handler {
        private final String folder;

        WriteHandler(@NonNull Looper looper, @NonNull String folder) {
            super(looper);
            this.folder = folder;
        }

        @Override
        public void handleMessage(Message msg) {
            String content = (String) msg.obj;

            String fileName = (msg.arg1 == 0) ? "SC" : "FW";

            FileWriter fileWriter = null;
            File logFile = getLogFile(folder, fileName);

            try {
                fileWriter = new FileWriter(logFile, true);

                writeLog(fileWriter, content);

                fileWriter.flush();
                fileWriter.close();
            } catch (IOException e) {
                if (fileWriter != null) {
                    try {
                        fileWriter.flush();
                        fileWriter.close();
                    } catch (IOException e1) { /* fail silently */ }
                }
            }
        }

        private void writeLog(@NonNull FileWriter fileWriter, @NonNull String content) throws IOException {
            fileWriter.append(content);
        }
    }
}
