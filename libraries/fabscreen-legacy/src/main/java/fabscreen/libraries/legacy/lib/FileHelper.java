package fabscreen.libraries.legacy.lib;

import java.io.File;

public class FileHelper {
    public static boolean removeFile(File file) {
        if (file == null) return false;

        // If is file then delete it directly.
        if (file.isFile()) {
            return file.delete();
        }

        // If is directory then recursively remove child files.
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files == null) {
                return false;
            }
            for (File f : files) {
                if (!removeFile(f)) {
                    return false;
                }
            }
        }

        return true;
    }
}
