package fabscreen.libraries.legacy.data;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.SystemClock;
import android.util.Log;
import android.util.SparseArray;

import com.orhanobut.logger.Logger;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

import fabscreen.libraries.legacy.data.bluetooth.BluetoothPacketBuilder;
import fabscreen.libraries.legacy.data.serial.RequestReceiver;
import fabscreen.libraries.legacy.data.serial.fabpacket.FabPacket;
import fabscreen.libraries.legacy.data.serial.fabpacket.FabPacketInputStream;
import fabscreen.libraries.legacy.lib.FabException;
import fabscreen.libraries.legacy.lib.LogHelper;
import fabscreen.libraries.legacy.receiver.BluetoothDiscoverReceiver;
import io.reactivex.Observable;
import io.reactivex.Scheduler;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;
import okio.Buffer;

// https://developer.android.com/guide/topics/connectivity/bluetooth.html
public class LaserCameraController implements ILaserCameraController, BluetoothDiscoverReceiver.BluetoothDeviceListener {
    private static final String TAG = "LaserCameraController";

    private static final UUID BLUETOOTH_MODULE_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); // unique identifier

    // bluetooth
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothDiscoverReceiver mBluetoothReceiver;
    private BluetoothSocket mBluetoothSocket;

    // receivers to handle response
    private SparseArray<RequestReceiver> mReceivers;

    // data
    private ArrayList<byte[]> mDataList;
    private int mDataSize = 0;

    // debug
    private long mRecvTime = 0;

    // rx
    private Subject<byte[]> mWriteSerializedSubject;
    private Disposable mWriteSubscription;
    private Scheduler.Worker mWorker;
    private BehaviorSubject<Boolean> mConnectedSubject = BehaviorSubject.createDefault(false);

    LaserCameraController(Context context) {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // register bluetooth receiver
        mBluetoothReceiver = new BluetoothDiscoverReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothDevice.ACTION_FOUND);
        intentFilter.addAction(BluetoothDevice.ACTION_PAIRING_REQUEST);
        intentFilter.setPriority(Integer.MAX_VALUE);
        context.registerReceiver(mBluetoothReceiver, intentFilter);

        mBluetoothReceiver.setBluetoothDeviceListener(this);

        mReceivers = new SparseArray<>();
    }

    public boolean isEnabled() {
        return mBluetoothAdapter.isEnabled();
    }

    public boolean isConnected() {
        return mConnectedSubject.getValue();
    }

    public void unregister(Context context) {
        if (mBluetoothReceiver != null) {
            context.unregisterReceiver(mBluetoothReceiver);
        }
    }

    public void updateConnectionStatus() {
        if (mBluetoothSocket == null) {
            mConnectedSubject.onNext(false);
        } else {
            mConnectedSubject.onNext(mBluetoothSocket.isConnected());
        }
    }

    public Observable<Boolean> getBluetoothConnectedObservable() {
        return mConnectedSubject;
    }

    public void setEnabled(boolean enabled) {
        if (enabled) {
            mBluetoothAdapter.enable();
        } else {
            disconnect();
            mBluetoothAdapter.disable();
        }
    }

    public String getCurrentConnectedName() {
        if (isConnected()) {
            return mBluetoothSocket.getRemoteDevice().getName();
        } else {
            return null;
        }
    }

    public int getDataSize() {
        return mDataSize;
    }

    /**
     * Start Discover Bluetooth Device
     * Start discovery will return result in BluetoothDeviceListener.
     */
    public void startDiscovery() {
        mBluetoothAdapter.startDiscovery();
    }

    /**
     * Stop Discover Bluetooth Device
     * if discovery is open, you should stop discovery before connecting any bluetooth device
     * https://developer.android.com/guide/topics/connectivity/bluetooth.html#DiscoverDevices
     */
    public void stopDiscovery() {
        if (mBluetoothAdapter.isDiscovering()) {
            mBluetoothAdapter.cancelDiscovery();
        }
    }

    @Override
    public void onDeviceDiscovered(String name, String macAddress) {
        if (name == null || macAddress == null) return;

        // receive discover bluetooth name and mac address here.
    }

    private Observable<Object> request(FabPacket packet) {
        final int key = packet.getKey();

        return Observable.create(emitter -> {
            if (!isEnabled()) {
                emitter.onError(new FabException("Bluetooth is not enabled."));
                return;
            }

            if (!isConnected()) {
                emitter.onError(new FabException("Bluetooth socket is not connected."));
                return;
            }

            RequestReceiver receiver = mReceivers.get(key);
            if (receiver == null) {
                receiver = new RequestReceiver(key);
                mReceivers.put(key, receiver);
            }

            receiver.addEmitter(emitter);

            send(packet);
        });
    }

    private Observable<Object> watch(FabPacket packet) {
        final int key = packet.getKey();

        return Observable.create(emitter -> {
            if (!isEnabled()) {
                emitter.onError(new FabException("Bluetooth is not enabled."));
                return;
            }

            if (!isConnected()) {
                emitter.onError(new FabException("Bluetooth socket is not connected."));
                return;
            }

            RequestReceiver receiver = mReceivers.get(key);
            if (receiver == null) {
                receiver = new RequestReceiver(key);
                mReceivers.put(key, receiver);
            }

            receiver.setDefaultEmitter(emitter);
        });
    }

    public void send(FabPacket packet) {
        send(packet.toByteArray());
    }

    private void send(byte[] data) {
        if (!isConnected()) {
            Log.w(TAG, "Connect to target bluetooth before send any data");
            return;
        }

        mWriteSerializedSubject.onNext(data);
    }

    /**
     * Check Camera Auto WB status (0x03 0x00)
     */
    public Observable<Boolean> checkCameraAutoWhiteBalanceActivated() {
        FabPacket packet = BluetoothPacketBuilder.isAutoWhiteBalanceActivated();
        return request(packet).map(o -> (Boolean) o);
    }

    /**
     * Set Camera Auto WB (0x05 0x00)
     */
    public Observable<Boolean> setCameraAutoWhiteBalance(boolean activated) {
        FabPacket packet = BluetoothPacketBuilder.setAutoWhiteBalanceActivated(activated);
        return request(packet).map(o -> (Boolean) o);
    }

    public Observable<Boolean> setExposeTime(int time) {
        FabPacket packet = BluetoothPacketBuilder.setExposeTime(time);
        return request(packet).map(o -> (Boolean) o);
    }

    /**
     * Set Photo Resolution (0x09 0x00)
     *
     * @param resolution a enum for available resolution,
     */
    public Observable<Boolean> setPhotoResolution(int resolution) {
        FabPacket packet = BluetoothPacketBuilder.setPhotoResolution(resolution);
        return request(packet).map(o -> (Boolean) o);
    }

    /**
     * Set Camera Quality (0x0b 0x00)
     *
     * @param value value for photo quality, range (0 - 255), 0 is the best quality.
     */
    public Observable<Boolean> setPhotoQuality(int value) {
        FabPacket packet = BluetoothPacketBuilder.setPhotoQuality(value);
        return request(packet).map(o -> (Boolean) o);
    }

    /**
     * request capture photo (0x0d 0x00)
     */
    public Observable<Boolean> requestCapturePhoto() {
        FabPacket packet = BluetoothPacketBuilder.requestCapturePhoto();
        return request(packet).map(o -> (Boolean) o);
    }

    public Observable<Boolean> requestCapturePhoto(int flashTime, int flashDelay) {
        FabPacket packet = BluetoothPacketBuilder.requestCapturePhoto(flashTime, flashDelay);
        return request(packet).map(o -> (Boolean) o);
    }

    /**
     * Laser Camera Operation: watch photo receive (0x0d 0x02)
     */
    public Observable<Bitmap> watchPhotoReceive() {
        FabPacket packet = BluetoothPacketBuilder.watchPhotoReceive();
        return watch(packet).map(o -> (Bitmap) o);
    }

    /**
     * Set Camera Lighting (0x17 0x00)
     */
    public Observable<Boolean> setCameraLighting(boolean enabled) {
        FabPacket packet = BluetoothPacketBuilder.setCameraLighting(enabled);
        return request(packet).map(o -> (Boolean) o);
    }

    public Observable<Boolean> connect(String macAddress) {
        return Observable.fromCallable(() -> {
            try {
                // Disconnect previous connection if a new connection is requesting
                if (isConnected()) {
                    disconnect();
                }

                BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(macAddress);

                mBluetoothSocket = createBluetoothSocket(device);
                // This is a blocking call
                final long start = SystemClock.elapsedRealtime();
                mBluetoothSocket.connect();
                Logger.d("Laser camera connected takes " + (SystemClock.elapsedRealtime() - start) + "ms");

                if (mBluetoothSocket.isConnected()) {
                    // Observe write data and send them in IO scheduler
                    PublishSubject<byte[]> writeSubject = PublishSubject.create();
                    mWriteSerializedSubject = writeSubject.toSerialized();

                    mWriteSubscription = mWriteSerializedSubject
                            .observeOn(Schedulers.io())
                            .subscribe((p) -> mBluetoothSocket.getOutputStream().write(p), e -> {
                                disconnect();
                                e.printStackTrace();
                            });

                    // Receive data in another IO scheduler
                    Scheduler.Worker worker = Schedulers.io().createWorker();
                    worker.schedule(LaserCameraController.this::onReceive);

                    mWorker = worker;
                    mConnectedSubject.onNext(true);
                    return true;
                } else {
                    return false;
                }
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }).subscribeOn(Schedulers.io());
    }

    private BluetoothSocket createBluetoothSocket(BluetoothDevice device) throws IOException {
        return device.createRfcommSocketToServiceRecord(BLUETOOTH_MODULE_UUID);
    }

    private void disconnect() {
        // Throw all the existing connection and subscription
        if (isConnected()) {
            if (mBluetoothSocket != null) {
                try {
                    mBluetoothSocket.close();
                    mBluetoothSocket = null;
                } catch (IOException e) {
                    mConnectedSubject.onNext(false);
                    e.printStackTrace();
                }
            }

            if (mWorker != null) {
                mWorker.dispose();
                mWorker = null;
            }

            if (mWriteSubscription != null) {
                mWriteSubscription.dispose();
                mWriteSubscription = null;
            }

            mConnectedSubject.onNext(false);
        }
    }

    private void onReceive() {
        try {
            FabPacketInputStream is = new FabPacketInputStream(mBluetoothSocket.getInputStream());

            while (true) {
                // Receive bluetooth device data here
                FabPacket packet = is.readPacket();
                SystemClock.sleep(5);

                switch (packet.getEventId()) {
                    // 0x04 request auto white balance
                    case BluetoothPacketBuilder.LASER_CAMERA_CHECK_AUTO_WHITE_BALANCE_RESPONSE_EVENT_ID:
                        // 0x06 set auto white balance
                    case BluetoothPacketBuilder.LASER_CAMERA_SET_AUTO_WHITE_BALANCE_RESPONSE_EVENT_ID:
                        // 0x08 set expose time
                    case BluetoothPacketBuilder.LASER_CAMERA_SET_EXPOSE_TIME_RESPONSE_EVENT_ID:
                        // 0x0a set photo resolution
                    case BluetoothPacketBuilder.LASER_CAMERA_SET_PHOTO_RESOLUTION_RESPONSE_EVENT_ID:
                        // 0x0c set photo quality
                    case BluetoothPacketBuilder.LASER_CAMERA_SET_PHOTO_QUALITY_RESPONSE_EVENT_ID:
                        // 0x18 set camera lighting
                    case BluetoothPacketBuilder.LASER_CAMERA_SET_CAMERA_LIGHTING_RESPONSE_EVENT_ID: {
                        sendResponse(packet, packet.getContent()[0] == 0);
                        break;
                    }

                    // 0x0e capture photo operation
                    case BluetoothPacketBuilder.LASER_CAMERA_PHOTO_RESPONSE_EVENT_ID: {
                        byte operationId = packet.getContent()[0];

                        switch (operationId) {
                            case 0:
                                // ready for receiving photo
                                mDataList = new ArrayList<>();
                                mDataSize = 0;
                                mRecvTime = SystemClock.elapsedRealtime();
                                Logger.d("Start receiving image...");
                                sendResponse(packet, packet.getContent()[0] == 0);
                                break;
                            case 1:
                                // get photo packet data
                                byte[] data = new byte[packet.getContent().length - 1];
                                Buffer buffer = new Buffer();
                                buffer.write(packet.getContent());

                                // operation id
                                buffer.readByte();
                                buffer.read(data, 0, packet.getContent().length - 1);

                                mDataList.add(data);
                                mDataSize += data.length;
                                Log.d(TAG, "received packet " + mDataList.size() + " " + (SystemClock.elapsedRealtime() - mRecvTime));
                                break;
                            case 2:
                                // merge photo data
                                Logger.d("Image received, it takes %d ms.", (SystemClock.elapsedRealtime() - mRecvTime));
                                byte[] pic = mergeData();
                                if (pic == null) {
                                    break;
                                }

                                Bitmap bitmap = BitmapFactory.decodeByteArray(pic, 0, pic.length);
                                if (bitmap != null) {
                                    Log.e(TAG, "get bitmap");
                                    sendResponse(packet, bitmap);
                                } else {
                                    Logger.w("Decode bitmap failed.");
                                }
                                break;
                            case (byte) 0xFF:
                                // error ?
                                Logger.w("Error message from laser camera.");
                                break;
                        }
                        break;
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            disconnect();
        }
    }

    private void sendResponse(FabPacket packet, Object result) {
        RequestReceiver receiver = mReceivers.get(packet.getKey());
        if (receiver != null) {
            receiver.receive(result);
        }
    }

    private byte[] mergeData() {
        byte[] pic = new byte[mDataSize];
        int offset = 0;
        if (mDataList == null) return null;

        for (byte[] d : mDataList) {
            System.arraycopy(d, 0, pic, offset, d.length);
            offset += d.length;
        }
        return pic;
    }

    private Set<BluetoothDevice> getBondDevices() {
        if (mBluetoothAdapter == null || !isEnabled()) return null;

        return mBluetoothAdapter.getBondedDevices();
    }

    public int getBondedDeviceCount() {
        if (getBondDevices() == null) {
            return 0;
        } else {
            return getBondDevices().size();
        }
    }

    public void removeBondedDeviceRecords() {
        Set<BluetoothDevice> bondedDevices = getBondDevices();
        if (bondedDevices == null) return;

        for (BluetoothDevice device : bondedDevices) {
            unBondDevice(device);
        }
    }

    /**
     * remove bluetooth device bonded(paired) record, removeBond() is not a publicly-exposed api,
     * so using reflection call to handle it.
     * https://stackoverflow.com/questions/9608140/how-to-unpair-or-delete-paired-bluetooth-device-programmatically-on-android
     */
    private void unBondDevice(BluetoothDevice device) {
        try {
            Method m = device.getClass().getMethod("removeBond", (Class[]) null);
            m.invoke(device, (Object[]) null);
        } catch (Exception e) {
            LogHelper.log(e);
        }
    }

    @Override
    public void onEmergencyStop() {
        // complete all the emitters
        for (int i = 0; i < mReceivers.size(); i++) {
            int key = mReceivers.keyAt(i);
            RequestReceiver receiver = mReceivers.get(key);
            receiver.complete();
        }
    }
}
