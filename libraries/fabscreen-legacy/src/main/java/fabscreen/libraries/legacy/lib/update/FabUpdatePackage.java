package fabscreen.libraries.legacy.lib.update;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import fabscreen.libraries.legacy.BaseApplication;
import fabscreen.libraries.legacy.lib.file.IFile;
import fabscreen.libraries.legacy.lib.file.IFileManager;
import okio.Buffer;
import okio.BufferedSource;
import okio.Okio;

public class FabUpdatePackage {
    private static final String TAG = FabUpdatePackage.class.getSimpleName();

    public static final int FLAG_FORCE_UPDATE = 1 << 1;

    private long length;
    private UpdatePackageHeader header;

    private BufferedSource source;
    private byte[] mControllerPackage;
    private ArrayList<byte[]> mModulePackages;
    private byte[] mScreenPackage;

    private String mControllerVersion;

    public static FabUpdatePackage parse(IFile file) throws IOException {
        if (file == null) {
            return null;
        }

        FabUpdatePackage updatePackage = new FabUpdatePackage();

        updatePackage.length = file.length();
        IFileManager iFileManager = file.isLocal() ?
                BaseApplication.getInstance().getFabLocalFileManager() :
                BaseApplication.getInstance().getFabUsbFileManager();
        updatePackage.source = Okio.buffer(Okio.source(iFileManager.getInputStream(file)));
        byte[] p = updatePackage.source.readByteArray(updatePackage.length);
        iFileManager.deviceHang();
        updatePackage.header = UpdatePackageHeader.parse(p);
        if (updatePackage.header == null) {
            return null;
        }

        int count = updatePackage.header.getCount();
        updatePackage.mModulePackages = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            byte type = updatePackage.header.getTypes().get(i);
            int startPos = updatePackage.header.getStartPos().get(i);
            int size = updatePackage.header.getSizes().get(i);
            switch (type) {
                case UpdatePackageHeader.TYPE_CONTROLLER_FIRMWARE: {
                    updatePackage.mControllerPackage = getPackage(p, startPos, size);
                    updatePackage.parseControllerPackage(updatePackage.mControllerPackage);
                    break;
                }
                case UpdatePackageHeader.TYPE_MODULE_FIRMWARE: {
                    updatePackage.mModulePackages.add(getPackage(p, startPos, size));
                    break;
                }
                case UpdatePackageHeader.TYPE_SCREEN: {
                    updatePackage.mScreenPackage = getPackage(p, startPos, size);
                    break;
                }
            }
        }

        return updatePackage;
    }

    private void parseControllerPackage(byte[] packet) {
        if (mControllerPackage == null) {
            return;
        }

        byte[] version = new byte[32];
        System.arraycopy(packet, 5, version, 0, version.length);
        mControllerVersion = new String(version).trim();
    }

    private static byte[] getPackage(byte[] packet, int startPos, int size) {
        byte[] pack = new byte[size];
        System.arraycopy(packet, startPos, pack, 0, size);
        return pack;
    }

    public UpdatePackageHeader getHeader() {
        return header;
    }

    public ArrayList<byte[]> getModulePackages() {
        return mModulePackages;
    }

    public byte[] getControllerPackage() {
        return mControllerPackage;
    }

    public String getControllerVersion() {
        return mControllerVersion;
    }

    public String getPackageVersion() {
        return header.version;
    }

    public byte[] getScreenPackage() {
        return mScreenPackage;
    }

    public boolean isForceUpdate() {
        return ((header.getFlag() & FLAG_FORCE_UPDATE) != 0);
    }

    public static class UpdatePackageHeader {
        public static final int TYPE_CONTROLLER_FIRMWARE = 0;
        public static final int TYPE_MODULE_FIRMWARE = 1;
        public static final int TYPE_SCREEN = 2;
        private short length;
        private String version;
        private int flag;
        private byte count;
        private List<Byte> types;
        private List<Integer> startPos;
        private List<Integer> sizes;

        public static UpdatePackageHeader parse(byte[] content) {
            UpdatePackageHeader header = new UpdatePackageHeader();

            header.types = new ArrayList<>();
            header.startPos = new ArrayList<>();
            header.sizes = new ArrayList<>();

            Buffer buffer = new Buffer();
            buffer.write(content);
            try {
                header.length = buffer.readShort();
                header.version = buffer.readString(32, Charset.forName("UTF-8")).trim();
                header.flag = buffer.readInt();
                header.count = buffer.readByte();
                for (int i = 0; i < header.count; i++) {
                    header.types.add(buffer.readByte());
                    header.startPos.add(buffer.readInt());
                    header.sizes.add(buffer.readInt());
                }
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }

            return header;
        }

        public short getLength() {
            return length;
        }

        public String getVersion() {
            return version;
        }

        public int getFlag() {
            return flag;
        }

        public byte getCount() {
            return count;
        }

        public List<Byte> getTypes() {
            return types;
        }

        public List<Integer> getStartPos() {
            return startPos;
        }

        public List<Integer> getSizes() {
            return sizes;
        }

        @Override
        public String toString() {
            return String.format(Locale.getDefault(), "length %d, version %s, flag %d, count %d, types %s, Starts %s, sizes %s",
                    length, version, flag, count,
                    types.toString(),
                    startPos.toString(), sizes.toString());
        }
    }
}
