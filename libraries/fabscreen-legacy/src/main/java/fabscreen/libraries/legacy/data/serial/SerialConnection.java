package fabscreen.libraries.legacy.data.serial;

import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import fabscreen.libraries.legacy.data.serial.fabpacket.FabPacket;
import fabscreen.libraries.legacy.data.serial.fabpacket.FabPacketInputStream;
import fabscreen.libraries.legacy.lib.serialport.SerialPort;
import io.reactivex.Scheduler;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;

/**
 * A wrapper for serial port which encapsulate input and output byte stream as FabPacket.
 */
public class SerialConnection {
    private static final String TAG = SerialConnection.class.getSimpleName();
    private static SerialConnection instance;

    private SerialPort mSerialPort;

    private Disposable mReadWorker;

    private Subject<FabPacket> mWriteSubject;
    private Subject<FabPacket> mWriteSerializedSubject;
    private Disposable mWriteSubscription;

    private SerialDataListener mSerialDataListener;
    private ConnectionListener mConnectionListener;

    public static SerialConnection getInstance() {
        if (instance == null) {
            synchronized (SerialConnection.class) {
                if (instance == null) {
                    instance = new SerialConnection();
                }
            }
        }
        return instance;
    }

    public interface ConnectionListener {
        void onConnectionChanged(boolean connected);
    }

    public void setConnectionListener(ConnectionListener listener) {
        if (mConnectionListener != null) {
            Log.e(TAG, "ConnectionListener has been set!");
            return;
        }
        mConnectionListener = listener;
    }

    public interface SerialDataListener {
        void onReceive(FabPacket packet);
    }

    public void setSerialDataListener(SerialDataListener listener) {
        if (mSerialDataListener != null) {
            Log.e(TAG, "SerialDataListener has been set!");
            return;
        }
        mSerialDataListener = listener;
    }

    private boolean isConnected() {
        return mSerialPort != null;
    }

    /**
     * Connect to given serial port device.
     */
    public void connect(String device) {
        if (isConnected()) {
            disconnect();
        }

        try {
            mSerialPort = new SerialPort(new File(device), 115200);
        } catch (IOException | InterruptedException e) {
            Log.w(TAG, "Unable to connect to serial port " + device);

            if (mConnectionListener != null) {
                mConnectionListener.onConnectionChanged(false);
            }

            return;
        }

        // Observe write data and send them with IO scheduler
        mWriteSubject = PublishSubject.create();
        mWriteSerializedSubject = mWriteSubject.toSerialized();
        mWriteSubscription = mWriteSerializedSubject
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe(p -> {
                    mSerialPort.getOutputStream().write(p.toByteArray());
                });

        // Read byte streams from serial port and parse as FabPacket
        Scheduler.Worker worker = Schedulers.io().createWorker();
        worker.schedule(() -> {
            InputStream in = mSerialPort.getInputStream();
            FabPacketInputStream is = new FabPacketInputStream(in);

            while (true) {
                try {
                    FabPacket packet = is.readPacket();
                    if (packet != null && mSerialDataListener != null) {
                        mSerialDataListener.onReceive(packet);
                    }
                } catch (IOException e) {
                    // disconnect when IOException raises
                    disconnect();

                    // close input stream
                    try {
                        is.close();
                    } catch (IOException e2) {
                        e2.printStackTrace();
                    }
                    break;
                }
            }
        });
        mReadWorker = worker;

        if (mConnectionListener != null) {
            mConnectionListener.onConnectionChanged(true);
        }
    }

    /**
     * Disconnect from serial port.
     */
    public void disconnect() {
        if (isConnected()) {
            mReadWorker.dispose();
            mReadWorker = null;

            mWriteSubscription.dispose();
            mWriteSubscription = null;

            mWriteSerializedSubject = null;
            mWriteSubject = null;

            mSerialPort.close();
            mSerialPort = null;

            if (mConnectionListener != null) {
                mConnectionListener.onConnectionChanged(false);
            }
        }
    }

    /**
     * Send packet to serial port.
     *
     * @param packet packet to be sent.
     */
    public void send(FabPacket packet) {
        if (!isConnected()) {
            Log.w(TAG, "Connect to serial port before send any data.");
            return;
        }
        if (mWriteSerializedSubject != null) {
            mWriteSerializedSubject.onNext(packet);
        }
    }
}
