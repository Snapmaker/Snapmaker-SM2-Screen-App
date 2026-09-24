package com.snapmaker.fabscreen.modules.factory;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.snapmaker.fabscreen.R;
import fabscreen.libraries.legacy.base.BaseFragment;
import fabscreen.libraries.legacy.base.BaseViewModel;
import fabscreen.libraries.legacy.view.FabAlert;

import java.util.Locale;

import butterknife.BindView;
import butterknife.OnClick;
import fabscreen.libraries.legacy.data.Constants;
import fabscreen.libraries.legacy.lib.LogHelper;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.PublishSubject;

public class FactoryBluetoothFragment extends BaseFragment {
    private final static String TAG = FactoryBluetoothFragment.class.getSimpleName();
    private final static int STATUS_IDLE = 0;
    private final static int STATUS_BLUETOOTH_CONNECTED = 1;
    private final static int STATUS_BLUETOOTH_NOT_CONNECTED = 2;

    @BindView(R.id.btn_factory_bluetooth_request)
    Button mBtnBluetoothRequest;
    @BindView(R.id.btn_factory_bluetooth_camera_lighting)
    Button mBtnBluetoothLighting;

    @BindView(R.id.tv_factory_bluetooth_status)
    TextView mTvBluetoothStatus;
    @BindView(R.id.tv_factory_bluetooth_result)
    TextView mTvBluetoothResult;
    @BindView(R.id.tv_factory_bluetooth_used_time)
    TextView mTvBluetoothSpeed;

    @BindView(R.id.iv_factory_bluetooth_image)
    ImageView mIvBluetoothImage;

    private long mStartTime = 0;

    private BehaviorSubject<Integer> mBluetoothStatusSubject = BehaviorSubject.createDefault(STATUS_IDLE);
    private PublishSubject<Bitmap> mBitmapSubject = PublishSubject.create();
    private BehaviorSubject<Boolean> mCameraLightingStatus = BehaviorSubject.createDefault(false);

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setTitle("Bluetooth Test");

        initView();

        initData();
    }

    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_factory_bluetooth;
    }

    @Override
    protected BaseViewModel getViewModel() {
        return null;
    }

    private void initView() {
        mBluetoothStatusSubject
                .observeOn(AndroidSchedulers.mainThread())
                .distinctUntilChanged()
                .as(bindToLifecycle())
                .subscribe(status -> {
            mBtnBluetoothRequest.setEnabled(status == STATUS_BLUETOOTH_CONNECTED);
            mBtnBluetoothLighting.setEnabled(status == STATUS_BLUETOOTH_CONNECTED);

            switch (status) {
                case STATUS_IDLE:
                    mTvBluetoothStatus.setText("正在初始化");
                    break;
                case STATUS_BLUETOOTH_CONNECTED:
                    mTvBluetoothStatus.setText("已连接");
                    break;
                case STATUS_BLUETOOTH_NOT_CONNECTED:
                    mTvBluetoothStatus.setText("未连接");
                    break;
                default:
                    break;
            }
        });

        // flashlight status
        mCameraLightingStatus
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle() )
                .subscribe(status -> mBtnBluetoothLighting.setSelected(status));

        mBluetoothStatusSubject
                .delaySubscription(10000, Constants.TIME_UNIT)
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(status -> {
                    if (status == STATUS_BLUETOOTH_NOT_CONNECTED) {
                        mTvBluetoothResult.setText("蓝牙未连接！");
                    }
                });
    }

    private void initData() {
        // pull bluetooth controller status
        getModel().getLaserCameraController().getBluetoothConnectedObservable()
                .as(bindToLifecycle())
                .subscribe(connected -> {
                    Log.d(TAG, "Bluetooth status " + (connected ? "connected" : "disconnected"));
                    mBluetoothStatusSubject.onNext(connected ? STATUS_BLUETOOTH_CONNECTED : STATUS_BLUETOOTH_NOT_CONNECTED);
                }, Throwable::printStackTrace);

        // bitmap capture
        mBitmapSubject.observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(bitmap -> {
                    mIvBluetoothImage.setVisibility(View.VISIBLE);
                    mIvBluetoothImage.setImageBitmap(bitmap);
                });
    }

    @OnClick(R.id.btn_factory_bluetooth_request)
    void onClickRequest() {
        // reset result
        mTvBluetoothSpeed.setText("");
        mTvBluetoothResult.setText("正在测试中，请稍候...");
        mIvBluetoothImage.setVisibility(View.GONE);

        getModel().getLaserCameraController().requestCapturePhoto()
                .doOnNext(success -> {
                    mStartTime = success ? SystemClock.elapsedRealtime() : 0;
                    if (success) {
                        Log.d(TAG, "request capture photo success");
                    } else {
                        Log.d(TAG, "request capture photo failed!");
                    }
                })
                .flatMap(success -> getModel().getLaserCameraController().watchPhotoReceive())
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(bitmap -> {
                    final float usedTime = ((SystemClock.elapsedRealtime() - mStartTime) * 10 / 10.0f) / 1000.0f;
                    final int dataSize = getModel().getLaserCameraController().getDataSize();
                    final float receivedSpeed;
                    StringBuilder stringBuilder = new StringBuilder();
                    if (dataSize != 0) {
                        receivedSpeed = (dataSize / usedTime) / 1024;
                    } else {
                        receivedSpeed = 0;
                    }
                    stringBuilder.append(String.format(Locale.getDefault(), "传输速度： %.2f KB/s\n", receivedSpeed));
                    stringBuilder.append(String.format(Locale.getDefault(), "传输大小： %d KB", dataSize / 1024));
                    mTvBluetoothSpeed.setText(stringBuilder.toString());


                    // result pass if speed is higher than 20 KB per seconds
                    mTvBluetoothResult.setText(receivedSpeed > 20 ? "测试通过" : "测试不通过");
                    mBitmapSubject.onNext(bitmap);
                }, e -> {
                    LogHelper.log(e);
                    mTvBluetoothResult.setText("测试出现意外错误，已停止。");
                });

    }

    @OnClick(R.id.btn_factory_bluetooth_camera_lighting)
    void onClickLighting() {
        if (mBluetoothStatusSubject.getValue() == STATUS_BLUETOOTH_CONNECTED) {
            boolean status = !mCameraLightingStatus.getValue();
            getModel().getLaserCameraController()
                    .setCameraLighting(status)
                    .observeOn(AndroidSchedulers.mainThread())
                    .as(bindToLifecycle())
                    .subscribe(success -> {
                        if (success) {
                            mCameraLightingStatus.onNext(status);
                        } else {
                            FabAlert.alert(getContext(), "打开补光灯失败！");
                        }
                    }, Throwable::printStackTrace);
        } else {
            Log.e(TAG, "Bluetooth is not connected.");
        }
    }
}
