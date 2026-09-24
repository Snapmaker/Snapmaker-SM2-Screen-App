package fabscreen.libraries.legacy.server.http.handlers;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.util.Log;

import com.orhanobut.logger.Logger;
import com.yanzhenjie.andserver.annotation.GetMapping;
import com.yanzhenjie.andserver.annotation.PostMapping;
import com.yanzhenjie.andserver.annotation.RequestParam;
import com.yanzhenjie.andserver.annotation.RestController;
import com.yanzhenjie.andserver.framework.body.FileBody;
import com.yanzhenjie.andserver.framework.body.JsonBody;
import com.yanzhenjie.andserver.framework.body.StringBody;
import com.yanzhenjie.andserver.http.HttpRequest;
import com.yanzhenjie.andserver.http.HttpResponse;
import com.yanzhenjie.andserver.http.ResponseBody;
import com.yanzhenjie.andserver.util.StatusCode;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;

import fabscreen.libraries.legacy.BaseApplication;
import fabscreen.libraries.legacy.data.Constants;
import fabscreen.libraries.legacy.data.Model;
import fabscreen.libraries.legacy.lib.FileHelper;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;

@RestController
public class CameraRequestHandler extends BaseRequestHandler {
    private static final String TAG = CameraRequestHandler.class.getSimpleName();

    private static final String URI_CAPTURE_PHOTO = "/api/request_capture_photo";
    private static final String URI_GET_CAMERA_IMAGE = "/api/get_camera_image";
    private static final String URI_CAMERA_CALIBRATION = "/api/request_camera_calibration";
    private static final String URI_CAMERA_SET_CALIBRATION_MATRIX = "/api/set_camera_calibration_matrix";

    // v1
    private static final String URI_CAMERA_TAKE_PHOTO = "/api/v1/camera_take_photo";
    private static final String URI_CAMERA_CALIBRATION_MATRIX = "/api/v1/camera_calibration_matrix";
    private static final String URI_SET_CAMERA_CALIBRATION_MATRIX = "/api/v1/set_camera_calibration";
    private static final String URI_CAMERA_CALIBRATION_PHOTO = "/api/v1/camera_calibration_photo";

    // 10W Laser
    private static final String URI_10W_LASER_CAMERA_CALIBRATION = "/api/request_10w_laser_camera_calibration";
    private static final String URI_10W_LASER_CAMERA_SET_CALIBRATION_MATRIX = "/api/set_10w_laser_camera_calibration_matrix";
    private static final String URI_10W_LASER_CAMERA_CALIBRATION_PHOTO = "/api/v1/10w_laser_camera_calibration_photo";

    private CompositeDisposable disposables = new CompositeDisposable();

    // TODO: Some APIs are not being in v1 version, that will causes serious problems and hard to manage.
    //  Solution: Add new v1 API for camera capture, then test with the new API and remove old API in the future.

    /**
     * Take a photo and return the captured photo.
     */
    @PostMapping(path = URI_CAMERA_TAKE_PHOTO)
    void takePhoto(HttpRequest request, HttpResponse response) {

        if (!getModel().getLaserCameraController().isConnected()) {
            response.setStatus(HttpResponse.SC_NOT_FOUND);
            return;
        }

        CountDownLatch countDownLatch = new CountDownLatch(1);

        final File file = new File(getModel().getCacheDir(), "capture.jpg");

        Disposable sub = getModel().getLaserCameraController().requestCapturePhoto()
                .flatMap(success -> getModel().getLaserCameraController().watchPhotoReceive())
                .subscribe(bitmap -> {
                    Logger.i("Receive bitmap");
                    Matrix m = new Matrix();
                    m.postRotate(getModel().getMachineController().getHeadType() == Constants.HEAD_LASER_10W ? 90 : 270);
                    bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), m, true);

                    FileOutputStream fos = new FileOutputStream(file);
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
                    fos.close();

                    countDownLatch.countDown();
                });
        disposables.add(sub);

        try {
            countDownLatch.await();
            FileBody body = new FileBody(file);
            response.setBody(body);
        } catch (InterruptedException e) {
            response.setStatus(HttpResponse.SC_NOT_FOUND);
        }
    }

    /**
     * Request Camera Calibration Matrix, given by detected points and corners.
     */
    @GetMapping(path = URI_CAMERA_CALIBRATION)
    void getCameraCalibrationMatrix(HttpRequest request, HttpResponse response) {

        Model model = BaseApplication.getInstance().getModel();
        String result = model.getPreferences().getCameraCalibration();

        if (result != null) {
            final StringBody body = new StringBody(result);
            response.setBody(body);
        } else {
            response.setStatus(HttpResponse.SC_NO_CONTENT);
        }
    }

    /**
     * Request 10W Laser Camera Calibration Matrix, given by detected points and corners.
     */
    @GetMapping(path = URI_10W_LASER_CAMERA_CALIBRATION)
    void get10WLaserCameraCalibrationMatrix(HttpRequest request, HttpResponse response) {

        Model model = BaseApplication.getInstance().getModel();
        String result = model.getPreferences().get10WLaserCameraCalibration();
        if (result != null) {
            final StringBody body = new StringBody(result);
            response.setBody(body);
        } else {
            response.setStatus(HttpResponse.SC_NO_CONTENT);
        }
    }


    @PostMapping(path = URI_CAMERA_SET_CALIBRATION_MATRIX)
    void setCameraCalibrationMatrix(HttpRequest request, HttpResponse response, @RequestParam("matrix") String matrix) {

        Model model = BaseApplication.getInstance().getModel();
        try {
            matrix = URLDecoder.decode(matrix, "GBK");
        } catch (UnsupportedEncodingException e) {
            response.setStatus(StatusCode.SC_INTERNAL_SERVER_ERROR);
        }

        if (matrix != null) {
            model.getPreferences().setCameraCalibration(matrix);
            response.setStatus(StatusCode.SC_OK);
        } else {
            response.setStatus(StatusCode.SC_BAD_REQUEST);
        }

    }

    @PostMapping(path = URI_10W_LASER_CAMERA_SET_CALIBRATION_MATRIX)
    void set10WLaserCameraCalibrationMatrix(HttpRequest request, HttpResponse response, @RequestParam("matrix") String matrix) {

        Model model = BaseApplication.getInstance().getModel();
        try {
            matrix = URLDecoder.decode(matrix, "GBK");
        } catch (UnsupportedEncodingException e) {
            response.setStatus(StatusCode.SC_INTERNAL_SERVER_ERROR);
        }

        if (matrix != null) {
            model.getPreferences().set10WLaserCameraCalibration(matrix);
            response.setStatus(StatusCode.SC_OK);
        } else {
            response.setStatus(StatusCode.SC_BAD_REQUEST);
        }

    }


    /**
     * Request Camera Calibration Photo.
     */
    @GetMapping(path = URI_CAMERA_CALIBRATION_PHOTO)
    void getCameraCalibrationPhoto(HttpRequest request, HttpResponse response) {

        Model model = BaseApplication.getInstance().getModel();
        File file = new File(model.getCacheDir(), "calibration.jpg");
        if (file.exists()) {
            final ResponseBody body = new FileBody(file);
            response.setBody(body);
        } else {
            response.setStatus(HttpResponse.SC_NOT_FOUND);
        }
    }

    /**
     * Request 10W Laser Camera Calibration Photo.
     */
    @GetMapping(path = URI_10W_LASER_CAMERA_CALIBRATION_PHOTO)
    void get10WLaserCameraCalibrationPhoto(HttpRequest request, HttpResponse response) {
        Model model = BaseApplication.getInstance().getModel();
        File file = new File(model.getCacheDir(), "10WLaserCalibration.jpg");
        if (file.exists()) {
            final ResponseBody body = new FileBody(file);
            response.setBody(body);
        } else {
            response.setStatus(HttpResponse.SC_NOT_FOUND);
        }
    }

    @GetMapping(URI_CAPTURE_PHOTO)
    void capture(HttpRequest request, HttpResponse response,
                 @RequestParam("index") int index, @RequestParam("x") float x, @RequestParam("y") float y,
                 @RequestParam("z") float z, @RequestParam("feedRate") int f, @RequestParam(name = "photoQuality", required = false, defaultValue = "-1") int quality) {

        Model model = BaseApplication.getInstance().getModel();
        Logger.i(String.format(Locale.getDefault(), "capture photo index %d x %.2f y %.2f z %.2f f %d", index, x, y, z, f));
        JSONObject json = new JSONObject();
        if (model.getLaserCameraController().isConnected()) {

            if (index == 0) {
                clearSubscription();

                for (int i = 0; i < 9; i++) {
                    File file = new File(model.getCacheDir(), i + ".jpg");

                    if (file.exists()) {
                        FileHelper.removeFile(file);
                    }
                }
            }
            if (!(quality >= -1 && quality <= 255)) {
                try {
                    json.put("status", false);
                    final ResponseBody body = new JsonBody(json);
                    response.setBody(body);
                    return;
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            final CountDownLatch countDownLatch = new CountDownLatch(1);
            Disposable subscription = model.getSlaveComputer().gotoAbsolutePosition(x, y, z, f)
                    // The default value is used, and the factory default value is restored upon startup
                    .flatMap(success -> quality != -1 ? getModel().getLaserCameraController().setPhotoQuality(quality) : Observable.just(success))
                    .flatMap(success -> {
                                Logger.d("setPhotoQuality " + success + "\tquality " + quality);
                                return model.getLaserCameraController().requestCapturePhoto();
                            }
                    )
                    .doOnNext(success -> {
                        json.put("status", success);
                    })
                    .flatMap(success -> model.getLaserCameraController().watchPhotoReceive())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(bitmap -> {
                        Matrix m = new Matrix();
                        m.postRotate(getModel().getMachineController().getHeadType() == Constants.HEAD_LASER_10W ? 90 : 270);
                        bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), m, true);

                        File file = new File(model.getCacheDir(), index + "_temp.jpg");
                        FileOutputStream fos = new FileOutputStream(file);
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
                        fos.close();
                        file.renameTo(new File(model.getCacheDir(), index + ".jpg"));

                        countDownLatch.countDown();
                    }, e -> {
                        e.printStackTrace();
                        json.put("status", false);
                    });
            disposables.add(subscription);

            try {
                countDownLatch.await();

                final ResponseBody body = new JsonBody(json);
                response.setBody(body);
            } catch (InterruptedException e) {
                try {
                    json.put("status", false);
                    final ResponseBody body = new JsonBody(json);
                    response.setBody(body);
                } catch (JSONException e2) {
                    e2.printStackTrace();
                }
            }
        } else {
            try {
                json.put("status", false);
                final ResponseBody body = new JsonBody(json);
                response.setBody(body);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    @GetMapping(URI_GET_CAMERA_IMAGE)
    void getCameraPhoto(HttpRequest request, HttpResponse response, @RequestParam("index") int index) {

        Log.d(TAG, "get image " + index + " .jpg");
        File file = new File(getModel().getCacheDir(), index + ".jpg");
        if (file.exists()) {
            final ResponseBody body = new FileBody(file);
            response.setBody(body);
        } else {
            response.setStatus(HttpResponse.SC_NOT_FOUND);
        }
    }

    // Test

    private void clearSubscription() {
        if (disposables == null) return;

        disposables.clear();
    }
}
