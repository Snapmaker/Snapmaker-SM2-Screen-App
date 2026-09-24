package fabscreen.libraries.legacy.lib;

import okio.ByteString;

public class DebugUtil {
    public static void printByteArray(byte[] bytes) {
        for (int i = 0; i < bytes.length; i++) {
            byte b = bytes[i];
            ByteString bs = ByteString.of(b);
            System.out.print(bs.hex() + " ");
        }
        System.out.println();
    }
}
