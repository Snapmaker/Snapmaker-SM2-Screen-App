package fabscreen.libraries.legacy.lib.file;

import android.content.Context;

import androidx.annotation.NonNull;

import com.github.mjdev.libaums.fs.UsbFile;
import com.github.mjdev.libaums.fs.UsbFileOutputStream;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

public class UsbHelper {
    private Context mContext;

    private static final String TAG = "UsbHelper";

    public UsbHelper(@NonNull Context context) {
        this.mContext = context;
    }

    // read Folder
    public ArrayList<UsbFile> getUsbFolderFileList(UsbFile usbFolder) {
//        currentFile = usbFolder;
        ArrayList<UsbFile> usbFiles = new ArrayList<>();
        try {
            usbFiles.get(0).getAbsolutePath();
            Collections.addAll(usbFiles, usbFolder.listFiles());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return usbFiles;
    }

    // not tested
    public boolean saveSDFileToUsb(File targetFile, UsbFile saveFolder, DownloadProgressListener progressListener) {
        boolean result;
        try {
            //USB文件是否存在
            boolean isExist = false;
            UsbFile saveFile = null;
            for (UsbFile usbFile : saveFolder.listFiles()) {
                if (usbFile.getName().equals(targetFile.getName())) {
                    isExist = true;
                    saveFile = usbFile;
                }
            }
            if (isExist) {
                saveFile.delete();
            }
            saveFile = saveFolder.createFile(targetFile.getName());
            FileInputStream fis = new FileInputStream(targetFile);

            int avi = fis.available();
            UsbFileOutputStream uos = new UsbFileOutputStream(saveFile);
            int bytesRead;
            byte[] buffer = new byte[1024 * 8];
            int writeCount = 0;

            while ((bytesRead = fis.read(buffer)) != -1) {
                uos.write(buffer, 0, bytesRead);
                writeCount += bytesRead;
                if (progressListener != null) {
                    progressListener.downloadProgress(writeCount * 100 / avi);
                }
            }

            uos.flush();
            fis.close();
            uos.close();
            result = true;
        } catch (final Exception e) {
            e.printStackTrace();
            result = false;
        }
        return result;
    }

    public interface DownloadProgressListener {
        void downloadProgress(int progress);
    }
}
