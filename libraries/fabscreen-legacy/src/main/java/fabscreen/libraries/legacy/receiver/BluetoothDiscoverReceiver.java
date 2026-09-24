package fabscreen.libraries.legacy.receiver;

import android.app.admin.DeviceAdminReceiver;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;

/**
 * Device Owner Receiver
 * <p>
 * Use DeviceOwnerReceiver to avoid error imports.
 */
public class BluetoothDiscoverReceiver extends DeviceAdminReceiver {
    private static final String TAG = "BluetoothReceiver";

    private BluetoothDeviceListener mListener;

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (BluetoothDevice.ACTION_FOUND.equals(action)) {
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            // return name and MAC address with listener
            if (mListener != null) {
                mListener.onDeviceDiscovered(device.getName(), device.getAddress());
            }

        }

        // Silence pair target device, directly confirm pair information
        // Stop broadcast to avoid pop up system dialog
        if (BluetoothDevice.ACTION_PAIRING_REQUEST.equals(action)) {
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            try {
                device.setPairingConfirmation(true);
                abortBroadcast();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void setBluetoothDeviceListener(BluetoothDeviceListener listener) {
        this.mListener = listener;
    }

    public interface BluetoothDeviceListener {
        void onDeviceDiscovered(String name, String macAddress);
    }
}