package fabscreen.libraries.legacy.data;

import android.content.Context;
import android.graphics.Bitmap;

import io.reactivex.Observable;

public interface ILaserCameraController {
    void unregister(Context context);

    boolean isEnabled();
    void setEnabled(boolean enabled);

    boolean isConnected();
    String getCurrentConnectedName();
    int getDataSize();
    Observable<Boolean> connect(String macAddress);
    void updateConnectionStatus();
    Observable<Boolean> getBluetoothConnectedObservable();

    Observable<Boolean> requestCapturePhoto();
    Observable<Boolean> requestCapturePhoto(int flashTime, int flashDelay);
    Observable<Bitmap> watchPhotoReceive();
    Observable<Boolean> setCameraAutoWhiteBalance(boolean activated);
    Observable<Boolean> checkCameraAutoWhiteBalanceActivated();
    Observable<Boolean> setCameraLighting(boolean enabled);
    Observable<Boolean> setPhotoQuality(int value);
    Observable<Boolean> setPhotoResolution(int resolution);
    Observable<Boolean> setExposeTime(int time);

    int getBondedDeviceCount();
    void removeBondedDeviceRecords();

    void onEmergencyStop();
}
