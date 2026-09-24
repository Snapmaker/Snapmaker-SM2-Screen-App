package fabscreen.libraries.legacy.lib;

import androidx.annotation.Nullable;

import com.google.gson.Gson;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;

import okio.BufferedSource;
import okio.Okio;

public class JsonHelper {
    @Nullable
    static public <T> T fromJsonFile(File file, Class<T> classOfT) {
        if (file == null || !file.exists() || file.length() == 0) {
            return null;
        }

        StringBuilder content = new StringBuilder();
        String line;
        BufferedSource source;

        try {
            source = Okio.buffer(Okio.source(new FileInputStream(file)));
            while (true) {
                line = source.readUtf8Line();
                if (line == null) {
                    break;
                }

                content.append(line);
            }
            return new Gson().fromJson(content.toString(), classOfT);
        } catch (IOException e) {
            LogHelper.log(e);
            return null;
        }
    }

    static public <T> void toJsonFile(File file, T instance, Class<T> classOfT) {
        if (file == null) {
            return;
        }

        try {
            FileWriter fileWriter = new FileWriter(file);
            new Gson().toJson(instance, classOfT, fileWriter);
            fileWriter.close();
        } catch (IOException e) {
            LogHelper.log(e);
        }
    }
}
