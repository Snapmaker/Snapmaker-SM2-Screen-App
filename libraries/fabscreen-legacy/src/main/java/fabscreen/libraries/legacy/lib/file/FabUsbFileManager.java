package fabscreen.libraries.legacy.lib.file;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;

import com.github.mjdev.libaums.UsbMassStorageDevice;
import com.github.mjdev.libaums.fs.FileSystem;
import com.github.mjdev.libaums.fs.UsbFile;
import com.github.mjdev.libaums.fs.UsbFileInputStream;
import com.github.mjdev.libaums.partition.Partition;
import com.orhanobut.logger.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import fabscreen.libraries.legacy.lib.FabException;
import fabscreen.libraries.legacy.receiver.UsbBroadcastReceiver;
import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import io.reactivex.subjects.PublishSubject;


public class FabUsbFileManager implements IFileManager, UsbBroadcastReceiver.UsbListener {
    private final static String TAG = "FabUsbFileManager";

    private Context mContext;

    private Stack<IFile> mStack = new Stack<>();

    private UsbBroadcastReceiver mReceiver;

    private UsbMassStorageDevice mUsbDevice;

    private PublishSubject<Boolean> mDeviceAccessibleEvents = PublishSubject.create();

    private PublishSubject<Boolean> mUSBStateEvents = PublishSubject.create();

    private boolean mDevicesHang = false;

    private static final String ILLEGAL_CHAR_REGEX = "[/|\\\\:*\"<>?]";

    public FabUsbFileManager(Context context) {
        mContext = context;

        registerReceiver();
    }

    private void registerReceiver() {
        mReceiver = new UsbBroadcastReceiver();
        mReceiver.setUsbListener(this);

        IntentFilter usbDeviceStateFilter = new IntentFilter();
        usbDeviceStateFilter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        usbDeviceStateFilter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        usbDeviceStateFilter.addAction(UsbBroadcastReceiver.ACTION_USB_PERMISSION);
        mContext.registerReceiver(mReceiver, usbDeviceStateFilter);
    }

    @Override
    public Observable<Boolean> mount() {
        if (!mDevicesHang) {
            mStack.clear();
        } else {
            return Observable.fromCallable(() -> {
                // Get devices
                UsbMassStorageDevice[] devices = UsbMassStorageDevice.getMassStorageDevices(mContext);
                // Use first device as default device
                mUsbDevice = devices[0];
                mUsbDevice.init();
                return true;
            });
        }

        UsbManager usbManager = (UsbManager) mContext.getSystemService(Context.USB_SERVICE);
        if (usbManager == null) {
            return Observable.just(false);
        }

        // Get devices
        UsbMassStorageDevice[] devices = UsbMassStorageDevice.getMassStorageDevices(mContext);
        if (devices.length == 0) {
            return Observable.just(false);
        }

        // Use first device as default device
        mUsbDevice = devices[0];

        if (usbManager.hasPermission(mUsbDevice.getUsbDevice())) {
            // Ideal situation, we have USB permission
            return Observable.fromCallable(
                    () -> {
                        mUsbDevice.init();
                        return true;
                    })
                    .doOnNext(mounted -> {
                        IFile rootFile = getRootFile();
                        mStack.add(rootFile);
                    });
        } else {
            // Request permission
            PendingIntent pendingIntent = PendingIntent.getBroadcast(mContext, 0, new Intent(UsbBroadcastReceiver.ACTION_USB_PERMISSION), 0);
            usbManager.requestPermission(mUsbDevice.getUsbDevice(), pendingIntent);

            return mDeviceAccessibleEvents.take(1)
                    .map(granted -> {
                        if (granted) {
                            mUsbDevice.init();
                            return true;
                        } else {
                            return false;
                        }
                    })
                    .doOnNext(file -> {
                        IFile rootFile = getRootFile();
                        mStack.add(rootFile);
                    });
        }
    }

    @Override
    public void unmount() {
        mStack.clear();
        mDevicesHang = false;
        if (mUsbDevice != null) {
            mUsbDevice.close();
        }

    }

    @Override
    public void deviceHang() {
        mDevicesHang = true;
        mUsbDevice.close();
    }

    private void deviceRecovery() {
        if (mDevicesHang) {
            Disposable subscribe = mount().subscribe(success -> {
            }, Throwable::printStackTrace);
        }
    }

    @Override
    public IFile getRootFile() {
        deviceRecovery();
        if (mUsbDevice == null) {
            return null;
        }

        List<Partition> partitions = mUsbDevice.getPartitions();
        if (partitions.size() == 0) {
            return null;
        }

        Partition partition = partitions.get(0);
        FileSystem fs = partition.getFileSystem();
        return new FabUsbFile(fs.getRootDirectory());
    }


    @Override
    public long getUsedSpace() {
        deviceRecovery();
        if (mUsbDevice == null) {
            return 0;
        }

        List<Partition> partitions = mUsbDevice.getPartitions();
        if (partitions.size() == 0) {
            return 0;
        }

        Partition partition = partitions.get(0);
        FileSystem fs = partition.getFileSystem();
        long usedSpace = fs.getOccupiedSpace();
        deviceHang();
        return usedSpace;
    }

    @Override
    public long getTotalSpace() {
        if (mDevicesHang) {
            mount();
        }
        if (mUsbDevice == null) {
            return 0;
        }

        List<Partition> partitions = mUsbDevice.getPartitions();
        if (partitions.size() == 0) {
            return 0;
        }

        Partition partition = partitions.get(0);
        FileSystem fs = partition.getFileSystem();
        long totalSpace = fs.getCapacity();
        deviceHang();
        return totalSpace;
    }

    @Override
    public Observable<IFile> search(String path) {
        deviceRecovery();
        if (mUsbDevice == null) {
            return mount().flatMap(mounted -> {
                if (mounted) {
                    return search(path);
                } else {
                    deviceHang();
                    return Observable.error(new FabException("Failed to access file from USB device."));
                }
            });
        }

        if ("/".equals(path)) {
            Observable<IFile> observable = Observable.just(getRootFile());
            deviceHang();
            return observable;
        }

        if (!path.startsWith("/")) {
            deviceHang();
            return Observable.error(new FabException("Only absolute path is supported."));
        }

        try {
            UsbFile usbFile = getFileSystem().getRootDirectory().search(path.substring(1));
            return Observable.just(new FabUsbFile(usbFile));
        } catch (IOException e) {
            deviceHang();
            return Observable.error(new FabException("Unable to open file."));
        }
    }

    /**
     * If the init is unsuccessful, the resulting mStack may be 0
     *
     * @return
     */
    @Override
    public boolean isRoot() {
        return mStack.size() <= 1;
    }

    @Override
    public IFile getCurrentDirectory() {
        return mStack.peek();
    }

    @Override
    public void gotoDirectory(IFile file) {
        mStack.push(file);
    }

    @Override
    public void popDirectory() {
        mStack.pop();
    }

    @Override
    public IFile createFile(IFile file, String name) throws IOException {
        IFile iFile = null;
        deviceRecovery();
        if (mUsbDevice == null) {
            return iFile;
        }
        UsbFile usbFile = (UsbFile) file.getFile();

        try {
            if (!usbFile.isRoot()) {
                usbFile = getFileSystem().getRootDirectory().search(usbFile.getAbsolutePath().substring(1));
                if (usbFile == null) {
                    return iFile;
                }
            } else {
                usbFile = getFileSystem().getRootDirectory();
            }
            UsbFile usbFile1 = usbFile.createFile(name);
            iFile = new FabUsbFile(usbFile1);
        } catch (IOException e) {
            deviceHang();
            throw e;
        }
        return iFile;
    }

    public FileSystem getFileSystem() {
        List<Partition> partitions = mUsbDevice.getPartitions();
        if (partitions.size() == 0) {
            deviceHang();
            return null;
        }
        Partition partition = partitions.get(0);
        return partition.getFileSystem();
    }


    @Override
    public Observable<ArrayList<IFile>> listFiles() {
        deviceRecovery();
        if (mUsbDevice == null) {
            deviceHang();
            return Observable.error(new IOException("USB Device not being initialized."));
        }

        final IFile file = mStack.peek();
        UsbFile usbFile = (UsbFile) file.getFile();

        // List files on current directory
        ArrayList<IFile> files = new ArrayList<>();
        try {
            List<Partition> partitions = mUsbDevice.getPartitions();
            if (partitions.size() == 0) {
                deviceHang();
                return Observable.error(new IOException("USB Device can't get partitions."));
            }
            Partition partition = partitions.get(0);
            FileSystem fs = partition.getFileSystem();
            if (!usbFile.isRoot()) {
                usbFile = fs.getRootDirectory().search(usbFile.getAbsolutePath().substring(1));
                if (usbFile == null) {
                    return Observable.error(new IOException("Failed to list file on current directory."));
                }
            } else {
                usbFile = fs.getRootDirectory();
            }
            for (UsbFile usbFile1 : usbFile.listFiles()) {
                files.add(new FabUsbFile(usbFile1));
            }
        } catch (IOException e) {
            deviceHang();
            return Observable.error(new IOException("Failed to list file on current directory."));
        }
        Observable<ArrayList<IFile>> arrayListObservable = Observable.just(files);
        deviceHang();
        return arrayListObservable;
    }

    private ArrayList<UsbFile> readDevice(UsbMassStorageDevice device) {
        ArrayList<UsbFile> usbFiles = new ArrayList<>();

        Partition partition = device.getPartitions().get(0);
        FileSystem fs = partition.getFileSystem();

        UsbFile root = fs.getRootDirectory();

        try {
            Collections.addAll(usbFiles, root.listFiles());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return usbFiles;
    }

    @Override
    public void close() {
        mContext.unregisterReceiver(mReceiver);
        if (mUsbDevice != null) {
            mUsbDevice.close();
            mUsbDevice = null;
        }
    }

    @Override
    public Observable<Boolean> getFileManagerStateObservable() {
        return mUSBStateEvents.hide();
    }

    @Override
    public void removeFile(IFile file) throws IOException {
        deviceRecovery();
        if (mUsbDevice == null) {
            deviceHang();
            throw new IOException("USB Device not being initialized.");
        }
        UsbFile mFile = getFileSystem().getRootDirectory().search(file.getPath().substring(1));
        if (mFile == null) {
            deviceHang();
            throw new IOException("file could not be found.");
        }
        mFile.delete();
        deviceHang();
    }

    @Override
    public void renameFile(IFile file, String name) throws IOException {
        if (name.isEmpty() || name.length() > 255) {
            throw new IOException("Filename too long.");
        }

        // Check if there is any illegal characters exists
        final Pattern illegalCharacters = Pattern.compile(ILLEGAL_CHAR_REGEX);
        Matcher matcher = illegalCharacters.matcher(name);
        if (matcher.find()) {
            throw new IOException("String name contains illegal character!");
        }
        deviceRecovery();
        if (mUsbDevice == null) {
            deviceHang();
            throw new IOException("USB Device not being initialized.");
        }
        UsbFile usbFile = getFileSystem().getRootDirectory().search(file.getPath().substring(1));
        if (usbFile == null || usbFile.getParent() == null) {
            deviceHang();
            return;
        }
        usbFile.setName(name);
        deviceHang();
    }

    @Override
    public IFile getParent() {
        return null;
    }

    @Override
    public InputStream getInputStream(IFile file) throws IOException {
        deviceRecovery();
        if (mUsbDevice == null) {
            deviceHang();
            throw new IOException("USB Device not being initialized.");
        }
        if (file.isDirectory()) {
            deviceHang();
            throw new IOException("Could not get InputStream, " + this.getClass().getName() + " isn't a file.");
        } else {
            UsbFile usbFile = getFileSystem().getRootDirectory().search(file.getPath().substring(1));
            if (usbFile == null)
                throw new IOException("Could not get InputStream, " + this.getClass().getName() + " isn't a file.");
            return new UsbFileInputStream(usbFile);
        }
    }

    @Override
    public void deviceAttached(UsbDevice usbDevice) {
        Logger.d("Device attached " + usbDevice);
        mUSBStateEvents.onNext(true);
    }

    @Override
    public void deviceDetached(UsbDevice usbDevice) {
        Logger.d("Device detached " + usbDevice);
        mUSBStateEvents.onNext(false);
        unmount();
    }

    @Override
    public void devicePermissionGranted(UsbDevice usbDevice) {
        if (mUsbDevice != null && mUsbDevice.getUsbDevice().getDeviceId() == usbDevice.getDeviceId()) {
            mDeviceAccessibleEvents.onNext(true);
        }
    }

    @Override
    public void devicePermissionDenied(UsbDevice usbDevice) {
        mDeviceAccessibleEvents.onNext(false);
    }
}
