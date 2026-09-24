package fabscreen.libraries.legacy.lib;

public class StringHelper {
    public static boolean isAlphabetic(char c) {
        return ('a' <= c && c <= 'z') || ('A' <= c && c <= 'Z');
    }
}
