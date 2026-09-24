package fabscreen.libraries.legacy.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import androidx.annotation.Nullable;

import fabscreen.libraries.legacy.data.serial.SerialConnection;
import fabscreen.libraries.legacy.data.serial.fabpacket.FabPacket;

public class SerialPortService extends Service {
    private SerialConnection mSerialConnection = SerialConnection.getInstance();

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // recreate the service with the last intent that was delivered to the service.
        return START_REDELIVER_INTENT;
    }

    public class Binder extends android.os.Binder {
        public void setConnectionListener(SerialConnection.ConnectionListener listener) {
            mSerialConnection.setConnectionListener(listener);
        }

        public void setSerialDataListener(SerialConnection.SerialDataListener listener) {
            mSerialConnection.setSerialDataListener(listener);
        }

        public void connect(String device) {
            mSerialConnection.connect(device);
        }

        public void disconnect() {
            mSerialConnection.disconnect();
        }

        public void send(FabPacket packet) {
            mSerialConnection.send(packet);
        }
    }

    private Binder mBinder = new Binder();

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }
}
