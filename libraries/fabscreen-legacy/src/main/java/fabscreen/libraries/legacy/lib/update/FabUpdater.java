package fabscreen.libraries.legacy.lib.update;

import java.io.IOException;
import java.util.ArrayList;

import fabscreen.libraries.legacy.lib.file.IFile;

// FabUpdater, parse package and handle with FabUpdatePackage.

public class FabUpdater {
    private static final String TAG = "FabUpdater";

    private static final int PART_SIZE = 512;

    private FabUpdatePackage mUpdatePackage;

    private ArrayList<byte[]> mPackages;

    private byte[] mPackage;
    private int mPartCount = -1;
    private String mControllerVersion;

    private byte[] mPartBuffer = new byte[PART_SIZE];

    public FabUpdater() {
        mPackages = new ArrayList<>();
    }

    public boolean parse(IFile file) throws IOException {
        mUpdatePackage = FabUpdatePackage.parse(file);

        if (mUpdatePackage == null) {
            // Alert
            return false;
        }

        mControllerVersion = mUpdatePackage.getControllerVersion();

        mPackages = new ArrayList<>();

        if (mUpdatePackage.getModulePackages() != null) {
            mPackages.addAll(mUpdatePackage.getModulePackages());
        }

        if (mUpdatePackage.getControllerPackage() != null) {
            mPackages.add(mUpdatePackage.getControllerPackage());
        }

        return true;
    }

    public String getPackageVersion() {
        return mUpdatePackage.getPackageVersion();
    }

    public boolean isForceUpdate() {
        return mUpdatePackage.isForceUpdate();
    }

    public byte[] getScreenUpdate() {
        return mUpdatePackage.getScreenPackage();
    }

    public String getControllerVersion() {
        return mControllerVersion;
    }

    public int getPacketCount() {
        return mPackages.size();
    }

    public int getPartCount() {
        return mPartCount;
    }

    public byte[] usePacket(int index) {
        if (index < 0 || index >= mPackages.size()) {
            return null;
        }

        mPackage = mPackages.get(index);
        mPartCount = (mPackage.length - 1) / PART_SIZE + 1;

        return mPackage;
    }

    public byte[] getPart(int index) {
        if (mPackage == null || index < 0 || index >= mPartCount) {
            return null;
        }

        final int length = mPackage.length;

        byte[] buffer;

        // last one
        if (index == mPartCount - 1 && length % PART_SIZE != 0) {
            // for last part, we create new temporary buffer
            buffer = new byte[length % PART_SIZE];
        } else {
            // or we use pre-created mPartBuffer
            buffer = mPartBuffer;
        }
        System.arraycopy(mPackage, 512 * index, buffer, 0, buffer.length);
        return buffer;
    }
}
