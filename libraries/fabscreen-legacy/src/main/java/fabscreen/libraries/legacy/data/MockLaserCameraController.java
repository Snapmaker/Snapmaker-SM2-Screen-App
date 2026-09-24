package fabscreen.libraries.legacy.data;

import android.content.Context;
import android.graphics.Bitmap;

import io.reactivex.Observable;

public class MockLaserCameraController implements ILaserCameraController {

    MockLaserCameraController() {

    }

    @Override
    public void unregister(Context context) {

    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public void setEnabled(boolean enabled) {

    }

    @Override
    public boolean isConnected() {
        return true;
    }

    @Override
    public String getCurrentConnectedName() {
        return "?";
    }

    @Override
    public int getDataSize() {
        return 0;
    }

    @Override
    public Observable<Boolean> connect(String macAddress) {
        return Observable.just(true);
    }

    @Override
    public void updateConnectionStatus() {

    }

    @Override
    public Observable<Boolean> getBluetoothConnectedObservable() {
        return Observable.just(true);
    }

    @Override
    public Observable<Boolean> requestCapturePhoto() {
        return Observable.just(true);
    }

    @Override
    public Observable<Boolean> requestCapturePhoto(int flashTime, int flashDelay) {
        return Observable.just(true);
    }

    @Override
    public Observable<Bitmap> watchPhotoReceive() {
        return Observable.just(Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888));
    }

    @Override
    public Observable<Boolean> setCameraAutoWhiteBalance(boolean activated) {
        return Observable.just(true);
    }

    @Override
    public Observable<Boolean> checkCameraAutoWhiteBalanceActivated() {
        return Observable.just(true);
    }

    @Override
    public Observable<Boolean> setCameraLighting(boolean enabled) {
        return Observable.just(true);
    }

    @Override
    public Observable<Boolean> setPhotoQuality(int value) {
        return Observable.just(true);
    }

    @Override
    public Observable<Boolean> setPhotoResolution(int resolution) {
        return Observable.just(true);
    }

    @Override
    public Observable<Boolean> setExposeTime(int time) {
        return Observable.just(true);
    }

    @Override
    public int getBondedDeviceCount() {
        return 0;
    }

    @Override
    public void removeBondedDeviceRecords() {
    }

    @Override
    public void onEmergencyStop() {

    }
}
