package fabscreen.libraries.legacy.data.serial;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Log;
import android.util.SparseArray;

import androidx.annotation.Nullable;

import com.orhanobut.logger.Logger;

import java.io.EOFException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import fabscreen.libraries.legacy.data.Constants;
import fabscreen.libraries.legacy.data.ISlaveComputer;
import fabscreen.libraries.legacy.data.serial.fabpacket.FabPacket;
import fabscreen.libraries.legacy.data.serial.fabpacket.FabPacketBuilder;
import fabscreen.libraries.legacy.data.serial.fabpacket.FabPacketContent;
import fabscreen.libraries.legacy.data.serial.fabpacket.content.ExtruderModel;
import fabscreen.libraries.legacy.data.serial.fabpacket.content.ExtruderTemperature;
import fabscreen.libraries.legacy.lib.FabException;
import fabscreen.libraries.legacy.lib.LogHelper;
import fabscreen.libraries.legacy.service.SerialPortService;
import io.reactivex.Observable;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;
import okio.Buffer;
import okio.ByteString;

public class SerialController implements ISlaveComputer {
    private static final String TAG = "SerialController";

    private Context mContext;

    private CompositeDisposable mDisposables = new CompositeDisposable();

    // connection
    // Selected device
    private String mDevice;
    private SerialPortService.Binder mServiceBinder;

    // FabPacket receivers to handle responses
    private SparseArray<RequestReceiver> mReceivers;

    // connection
    private long mLastReceiveTimeMillis = 0;
    private Disposable mPollingSubscription = null;
    private Disposable mHeartbeatSubscription = null;
    private BehaviorSubject<Boolean> mConnectedSubject = BehaviorSubject.createDefault(false);
    private BehaviorSubject<FabPacketContent.MachineStatus> mMachineStatusSubject = BehaviorSubject.createDefault(FabPacketContent.MachineStatus.getDefaultInstance());

    private byte mLaser10WErrorState;

    // data - machine status
    private BehaviorSubject<Boolean> mIsStatusValidSubject = BehaviorSubject.createDefault(true);

    // data - print
//    private Subject<String> mPrintGcodeResponseSubject = PublishSubject.create();
    // data - G-code response content
    private FabPacketContent.GcodeResponse mGcodeResponse = FabPacketContent.GcodeResponse.EMPTY_GCODE_RESPONSE;

    // data - settings
    private Subject<Integer> mAutoCalibrationProgressSubject = PublishSubject.create();
    private PublishSubject<FabPacketContent.HeaderSecurity> mHeaderSecuritySubject = PublishSubject.create();
    private PublishSubject<Integer> mPausePrintSubject = PublishSubject.create();

    // data - batch G-code response content
    private PublishSubject<FabPacketContent.BatchGcodeResponse> mBatchGcodeResponseSubject = PublishSubject.create();
    // data - Listen for master status updates
    private PublishSubject<FabPacketContent.MasterState> mMasterStateSubject = PublishSubject.create();

    public SerialController(Context context) {
        mContext = context;

        mReceivers = new SparseArray<>();
    }

    @Override
    public Observable<FabPacketContent.BatchGcodeResponse> getBatchGcodeResponseSubject() {
        return mBatchGcodeResponseSubject.hide();
    }

    @Override
    public Observable<FabPacketContent.MasterState> getMasterState() {
        return mMasterStateSubject.hide();
    }

    public Observable<Boolean> getConnectedObservable() {
        return mConnectedSubject.distinctUntilChanged();
    }

    public void setHeartbeatEnabled(boolean enabled) {
        if (enabled) {
            Logger.i("Enable heartbeat.");
            mLastReceiveTimeMillis = SystemClock.elapsedRealtime();

            // Polling machine status every few seconds
            if (mPollingSubscription != null) { // in case onConnected being called twice in a row
                mPollingSubscription.dispose();
            }
            mPollingSubscription = Observable.interval(Constants.POLLING_INTERVAL, TimeUnit.MILLISECONDS)
                    .flatMap(t -> requestMachineStatus()
                            .onErrorReturnItem(FabPacketContent.MachineStatus.getDefaultInstance()))
                    .subscribe(machineStatus -> {
                        if (!machineStatus.isDefault) {
                            mMachineStatusSubject.onNext(machineStatus);
                        } else {
                            Logger.w("Not getting machine status, replace with default status.");
                        }
                    }, e -> {
                        Log.e(TAG, "Failed to request machine status.");
                        e.printStackTrace();
                    });

            // Check response interval
            if (mHeartbeatSubscription != null) {
                mHeartbeatSubscription.dispose();
            }
            mHeartbeatSubscription = Observable.interval(Constants.HEARTBEAT_INTERVAL, TimeUnit.MILLISECONDS)
                    .subscribe(t -> {
                        long time = SystemClock.elapsedRealtime();
                        if (time - mLastReceiveTimeMillis > Constants.HEARTBEAT_INTERVAL) {
                            disconnect();
                        }
                    });
        } else {
            Logger.i("Disable heartbeat.");
            if (mPollingSubscription != null) {
                mPollingSubscription.dispose();
                mPollingSubscription = null;
            }
            if (mHeartbeatSubscription != null) {
                mHeartbeatSubscription.dispose();
                mHeartbeatSubscription = null;
            }
        }
    }

    public FabPacketContent.MachineStatus getMachineStatus() {
        return mMachineStatusSubject.getValue();
    }

    public Observable<FabPacketContent.MachineStatus> getMachineStatusObservable() {
        return mMachineStatusSubject;
    }

    @Override
    public Observable<Boolean> getMachineStatusValidObservable() {
        return mIsStatusValidSubject.hide();
    }

    @Override
    public boolean isMachineStatusValid() {
        return mIsStatusValidSubject.getValue();
    }

    /**
     * Set service binder, serial controller uses this binder to communicate with serial port service.
     *
     * @param binder IBinder to bind service callbacks.
     */
    public void setServiceBinder(@Nullable IBinder binder) {
        if (binder == null) {
            mServiceBinder.setConnectionListener(null);
            mServiceBinder.setSerialDataListener(null);
            mServiceBinder = null;
        } else {
            mServiceBinder = (SerialPortService.Binder) binder;
            mServiceBinder.setConnectionListener(this::onConnection);
            mServiceBinder.setSerialDataListener(this::onReceive);
        }
    }

    public void connect(String device) {
        if (mDevice != null) {
            disconnect();
        }

        mDevice = device;

        mServiceBinder.connect(device);
    }

    private void disconnect() {
        mServiceBinder.disconnect();

        mDevice = null;

        mDisposables.clear();

        mReceivers.clear();
    }

    private Observable<Object> watch(FabPacket packet) {
        final int key = packet.getKey();

        return Observable.create(emitter -> {
            if (mServiceBinder == null) {
                emitter.onError(new IOException("Serial port service is not connected."));
                return;
            }

            if (!mConnectedSubject.getValue()) {
                emitter.onError(new IOException("Serial port is not connected."));
                return;
            }

            RequestReceiver receiver = mReceivers.get(key);
            if (receiver == null) {
                receiver = new RequestReceiver(key);
                mReceivers.put(packet.getKey(), receiver);
            }

            receiver.setDefaultEmitter(emitter);
        });
    }

    private Observable<Object> request(FabPacket packet, int timeout, Observable<Object> observable) {
        return request(packet).timeout(timeout, TimeUnit.MILLISECONDS, observable);
    }

    private Observable<Object> request(FabPacket packet, int timeout) {
        return request(packet).timeout(timeout, TimeUnit.MILLISECONDS);
    }

    private Observable<Object> request(FabPacket packet) {
        final int key = packet.getKey();

        return Observable.create(emitter -> {
            if (emitter.isDisposed()) {
                Logger.d("Emitter is disposed, skip sending request.");
                return;
            }

            if (mServiceBinder == null) {
                emitter.onError(new FabException("Serial port service is not connected."));
                return;
            }

            if (!mConnectedSubject.getValue()) {
                emitter.onError(new FabException("Serial port is not connected."));
                return;
            }

            RequestReceiver receiver = mReceivers.get(key);
            if (receiver == null) {
                receiver = new RequestReceiver(key);
                mReceivers.put(key, receiver);
            }
            receiver.addEmitter(emitter);

            // Check send 0x09 0x14
            if (packet.getEventId() == FabPacket.SETTINGS_REQUEST_EVENT_ID && Arrays.equals(packet.getContent(), new byte[]{(byte) 0x14})) {
                Logger.d("Request 0x09 0x14 ");
            }
            send(packet);
        });
    }

    /**
     * Send FabPacket to serial port.
     *
     * @param packet packet to be sent.
     */
    @Override
    public void send(FabPacket packet) {
        mServiceBinder.send(packet);
    }

    @Override
    public Observable<FabPacketContent.GcodeResponse> sendGcode(String gcode) {
        return request(FabPacketBuilder.gcodeRequest(gcode, 0))
                .map(o -> (FabPacketContent.GcodeResponse) o);
    }

    @Override
    public Observable<FabPacketContent.GcodeResponse> sendPrintGcode(String gcode, int lineno) {
        return request(FabPacketBuilder.printGcodeRequest(gcode, lineno))
                .map(o -> (FabPacketContent.GcodeResponse) o);
    }

    @Override
    public void sendPrintBatchGcode(int startLine, int endLine, String gcode) {
        send(FabPacketBuilder.printBatchGcodeRequest(startLine, endLine, gcode));
    }

    public Observable<FabPacketContent.GcodeResponse> sendGcode(String gcode, boolean replyContent) {
        FabPacket packet = FabPacketBuilder.gcodeRequest(gcode, 0);
        byte[] bytes = packet.toByteArray();
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X ", b));
        }
        Logger.d("G-code Request Hex: %s", sb.toString());
        return request(FabPacketBuilder.gcodeRequest(gcode, 0))
                .flatMap(o1 -> watch(FabPacketBuilder.extendGcodeRequest())
                        .map(o2 -> (FabPacketContent.GcodeResponse) o2));
    }


    /**
     * Status Request: Get Machine Status. (0x07 0x01)
     */
    @Override
    public Observable<FabPacketContent.MachineStatus> requestMachineStatus() {
        FabPacket packet = FabPacketBuilder.statusRequestMachineStatus();
        return request(packet, 1000).map(o -> (FabPacketContent.MachineStatus) o);
    }

    /**
     * Status Request: Get Abnormal Status. (0x07 0x02)
     */
    @Override
    public Observable<FabPacketContent.MachineErrors> getMachineErrors() {
        FabPacket packet = FabPacketBuilder.statusRequestMachineAbnormalStatus();
        return request(packet).map(o -> (FabPacketContent.MachineErrors) o);
    }

    @Override
    public Observable<FabPacketContent.MachineErrors> getMachineErrors(int timeout) {
        FabPacket packet = FabPacketBuilder.statusRequestMachineAbnormalStatus();
        return request(packet, timeout).map(o -> (FabPacketContent.MachineErrors) o);
    }

    @Override
    public Observable<FabPacketContent.MachineErrors> watchMachineErrors() {
        FabPacket packet = FabPacketBuilder.statusRequestMachineAbnormalStatus();
        return watch(packet).map(o -> (FabPacketContent.MachineErrors) o);
    }

    @Override
    public Observable<Boolean> watchWaitEvents() {
        FabPacket packet = FabPacketBuilder.statusWaitRequest();
        return watch(packet).map(o -> true);
    }

    /**
     * Status request: start online print. (0x07 0x03)
     */
    @Override
    public Observable<Integer> start() {
        FabPacket packet = FabPacketBuilder.statusRequestMachineStartPrint();
        return request(packet).map(o -> (Integer) o);
    }

    @Override
    public Observable<Integer> useBatchGcodeMode(int check) {
        Logger.d("send useBatchGcodeMode " + check);
        FabPacket packet = FabPacketBuilder.setgcodeBatchSending(check);
        return request(packet, 1000).map(o -> (Integer) o);
    }

    /**
     * Status request: pause print. (0x07 0x04)
     */
    @Override
    public Observable<Integer> pause() {
        FabPacket packet = FabPacketBuilder.statusRequestMachinePausePrint();
        return request(packet).map(o -> (Integer) o);
    }

    /**
     * Status request: resume print. (0x07 0x05)
     */
    @Override
    public Observable<Integer> resume() {
        FabPacket packet = FabPacketBuilder.statusRequestMachineResumePrint();
        return request(packet).map(o -> (Integer) o);
    }

    /**
     * Status request: stop print. (0x07 0x06)
     */
    @Override
    public Observable<Integer> stop() {
        FabPacket packet = FabPacketBuilder.statusRequestMachineStopPrint();
        return request(packet).map(o -> (Integer) o);
    }

    /**
     * Status request: finish print. (0x07 0x07)
     */
    @Override
    public Observable<Integer> finish() {
        FabPacket packet = FabPacketBuilder.statusRequestMachineFinishPrint();
        return request(packet).map(o -> (Integer) o);
    }

    /**
     * Status request: Get line number. (0x07 0x08)
     */
    @Override
    public Observable<Integer> getLineNumber() {
        FabPacket packet = FabPacketBuilder.statusRequestLineNumber();
        return request(packet).map(o -> (Integer) o);
    }

    @Override
    public Observable<Integer> resetErrorFlag() {
        FabPacket packet = FabPacketBuilder.statusRequestResetErrorFlag();
        return request(packet).map(o -> (Integer) o);
    }

    /**
     * Status request: Resume print. (0x07 0x0b)
     */
    @Override
    public Observable<Integer> resumeFromPowerOutage() {
        FabPacket packet = FabPacketBuilder.statusRequestResumePrint();
        return request(packet).map(o -> (Integer) o);
    }

    @Override
    public Observable<FabPacketContent.CoordinateSystem> requestCoordinateSystem() {
        FabPacket packet = FabPacketBuilder.statusRequestCoodinateSystem();
        return request(packet).map(o -> (FabPacketContent.CoordinateSystem) o);
    }

    @Override
    public Observable<FabPacketContent.CoordinateSystem> requestCoordinateSystem(int timeout) {
        FabPacket packet = FabPacketBuilder.statusRequestCoodinateSystem();
        return request(packet, timeout).map(o -> (FabPacketContent.CoordinateSystem) o);
    }

    /**
     * Settings: Set Workspace.
     */
    @Override
    public Observable<Boolean> setWorkspace(int xSize, int xHomeOffset, int xMaxDir, int xStepperDir,
                                            int ySize, int yHomeOffset, int yMaxDir, int yStepperDir,
                                            int zSize, int zHomeOffset, int zMaxDir, int zStepperDir) {
        FabPacket packet = FabPacketBuilder.setWorkspace(
                xSize, xHomeOffset, xMaxDir, xStepperDir,
                ySize, yHomeOffset, yMaxDir, yStepperDir,
                zSize, zHomeOffset, zMaxDir, zStepperDir);

        // time out up to 10 second
        return request(packet, 10 * 1000).map(o -> (Boolean) o);
    }

    /**
     * Settings: Start Auto Calibration (0x09 0x02).
     */
    @Override
    public Observable<Boolean> startAutoCalibration() {
        FabPacket packet = FabPacketBuilder.startAutoCalibration();
        return request(packet).map(o -> (Boolean) o);
    }

    @Override
    public Observable<Boolean> startAutoCalibration(int grid) {
        FabPacket packet = FabPacketBuilder.startAutoCalibration(grid);
        return request(packet).map(o -> (Boolean) o);
    }

    @Override
    public Observable<Integer> getAutoCalibrationProgress() {
        return mAutoCalibrationProgressSubject;
    }

    /**
     * Settings: Start Manual Calibration. (0x09 0x04)
     */
    @Override
    public Observable<Boolean> startManualCalibration() {
        FabPacket packet = FabPacketBuilder.startManualCalibration();
        return request(packet).map(o -> (Boolean) o);
    }

    @Override
    public Observable<Boolean> startManualCalibration(int grid) {
        FabPacket packet = FabPacketBuilder.startManualCalibration(grid);
        return request(packet).map(o -> (Boolean) o);
    }

    /**
     * Settings: Goto Manual Calibration Point. (0x09 0x05)
     */
    @Override
    public Observable<Boolean> gotoCalibrationPoint(int point) {
        FabPacket packet = FabPacketBuilder.gotoCalibrationPoint(point);
        return request(packet).map(o -> (Boolean) o);
    }

    /**
     * Settings: Move Manual Calibration Point. (0x09 0x06)
     */
    @Override
    public Observable<Boolean> moveCalibrationPoint(double offset) {
        FabPacket packet = FabPacketBuilder.moveCalibrationPoint(offset);
        return request(packet).map(o -> (Boolean) o);
    }

    // 0x09 0x07
    @Override
    public Observable<Boolean> saveCalibration() {
        FabPacket packet = FabPacketBuilder.saveCalibration();
        return request(packet).map(o -> (Boolean) o);
    }

    /**
     * Settings: Exit Calibration. (0x09 0x08)
     */
    @Override
    public Observable<Boolean> exitCalibration() {
        FabPacket packet = FabPacketBuilder.exitCalibration();
        return request(packet).map(o -> (Boolean) o);
    }

    /**
     * Settings: Reset Calibration. (0x09 0x09)
     */
    @Override
    public Observable<Boolean> resetCalibration() {
        FabPacket packet = FabPacketBuilder.resetCalibration();
        return request(packet).map(o -> (Boolean) o);
    }

    /**
     * Settings: Get Laser Focal Length (0x09 0x0a)
     */
    @Override
    public Observable<Float> getLaserFocalLength() {
        FabPacket packet = FabPacketBuilder.getLaserFocalLength();
        return request(packet).map(o -> (Float) o);
    }

    @Override
    public Observable<Float> getLaserFocalLength(int timeout) {
        FabPacket packet = FabPacketBuilder.getLaserFocalLength();
        return request(packet, timeout).map(o -> (Float) o);
    }

    /**
     * Settings: Set Laser Focus (0x09 0x0b)
     */
    @Override
    public Observable<Boolean> setLaserFocalLength(float focalLength) {
        FabPacket packet = FabPacketBuilder.setLaserFocalLength(focalLength);
        return request(packet).map(o -> (Boolean) o);
    }

    /**
     * Setting: Start Laser Focus (0x09 0x0c)
     */
    @Override
    public Observable<Boolean> startLaserFocusSetting(float xPosition, float yPosition, float zPosition) {
        FabPacket packet = FabPacketBuilder.startLaserFocusSetting(xPosition, yPosition, zPosition);
        return request(packet).map(o -> (Boolean) o);
    }

    /**
     * Setting: Start Laser Fine Tune (0x09 0x0d)
     */
    @Override
    public Observable<Boolean> startLaserFineTune() {
        FabPacket packet = FabPacketBuilder.startLaserFineTune();
        return request(packet).map(o -> (Boolean) o);
    }

    @Override
    public Observable<Boolean> startLaserFineTune(float zOffset) {
        FabPacket packet = FabPacketBuilder.startLaserFineTune(zOffset);
        return request(packet).map(o -> (Boolean) o);
    }

    // 0x09 0x0e
    @Override
    public Observable<Integer> fastCalibration() {
        FabPacket packet = FabPacketBuilder.fastCalibration();
        return request(packet).map(o -> (Integer) o);
    }

    // 0x09 0x0f
    @Override
    public Observable<Integer> requestAdjustSetting(int type, float value) {
        FabPacket packet = FabPacketBuilder.requestAdjustSettings((byte) type, value);
        return request(packet).map(o -> (Integer) o);
    }

    @Override
    public Observable<Integer> requestAdjustSettingFeedRate(float value) {
        return requestAdjustSetting(0, value);
    }

    @Override
    public Observable<Integer> requestAdjustSettingNozzleTemp(float value) {
        return requestAdjustSetting(1, value);
    }

    @Override
    public Observable<Integer> requestAdjustSettingNozzleTemp(int which, float value) {
        return requestAdjustSetting(which == 0 ? 1 : 7, value);
    }

    @Override
    public Observable<Integer> requestAdjustSettingHeatedBedTemp(float value) {
        return requestAdjustSetting(2, value);
    }

    @Override
    public Observable<Integer> requestAdjustSettingLaserPower(float value) {
        return requestAdjustSetting(3, value);
    }

    @Override
    public Observable<Integer> requestAdjustSettingZOffset(float value) {
        return requestAdjustSetting(4, value);
    }

    @Override
    public Observable<Integer> requestAdjustSettingZOffset(int which, float value) {
        return requestAdjustSetting(which == 0 ? 4 : 8, value);
    }

    public Observable<Integer> requestAdjustSettingFlowRate(int which, float value) {
        return requestAdjustSetting(which == 0 ? 9 : 10, value);
    }

    @Override
    public Observable<Integer> requestAdjustSettingCNCPower(float value) {
        return requestAdjustSetting(5, value);
    }

    @Override
    public Observable<FabPacketContent.AdjustSettings> getAdjustSetting(int type) {
        FabPacket packet = FabPacketBuilder.getAdjustSettings((byte) type);
        return request(packet).map(o -> (FabPacketContent.AdjustSettings) o);
    }

    @Override
    public Observable<Boolean> setAFAssistLightState(int state) {
        FabPacket packet = FabPacketBuilder.setAFAssistLightState((byte) state);
        return request(packet).map(o -> (byte) o == 0);
    }

    @Override
    public Observable<FabPacketContent.AdjustSettings> getAdjustSettingFeedRate() {
        return getAdjustSetting(0);
    }

    @Override
    public Observable<FabPacketContent.AdjustSettings> getAdjustSettingLaserPower() {
        return getAdjustSetting(1);
    }

    @Override
    public Observable<FabPacketContent.AdjustSettings> getAdjustSettingZOffset() {
        return getAdjustSetting(4);
    }

    @Override
    public Observable<FabPacketContent.AdjustSettings> getAdjustSettingCNCPower() {
        return getAdjustSetting(5);
    }

    @Override
    public Observable<FabPacketContent.MachineSize> getMachineSize() {
        FabPacket packet = FabPacketBuilder.getMachineSize();
        return request(packet, 500).map(o -> (FabPacketContent.MachineSize) o);
    }

    @Override
    public Observable<Boolean> checkCalibrationEverSucceeded() {
        FabPacket packet = FabPacketBuilder.checkCalibrationEverSucceeded();
        return request(packet).map(o -> (Boolean) o);
    }


    public Observable<FabPacketContent.ExtendKitInfo> requestExtendKitInfo() {
        FabPacket packet = FabPacketBuilder.requestExtendKitInfo();
        return request(packet).map(o -> (FabPacketContent.ExtendKitInfo) o);
    }

    // TODO: refactor this.
    public Observable<Byte> setExtendKitInfo(boolean isQuickSwapInstalled, byte extendKitConf) {
        FabPacket packet = FabPacketBuilder.setExtendKitInfo((byte)(isQuickSwapInstalled ? 1 : 0), extendKitConf);
        return request(packet).map(o -> (byte) o);
    }

    /**
     * Movement: G28 Z (0x0b 0x01)
     */
    @Override
    public Observable<Boolean> gotoZHome() {
        FabPacket packet = FabPacketBuilder.gotoZHome();
        return request(packet).map(o -> (Boolean) o);
    }

    @Override
    public Observable<Boolean> setPosition(float x, float y, float z, int flag) {
        return setPosition(x, y, z, 0, flag);
    }

    @Override
    public Observable<Boolean> setPosition(float x, float y, float z, float b, int flag) {
        String cmd = "G92";
        if ((flag & ISlaveComputer.FLAG_X) > 0) {
            cmd += String.format(Locale.US, " X%.2f", x);
        }
        if ((flag & ISlaveComputer.FLAG_Y) > 0) {
            cmd += String.format(Locale.US, " Y%.2f", y);
        }
        if ((flag & ISlaveComputer.FLAG_Z) > 0) {
            cmd += String.format(Locale.US, " Z%.2f", z);
        }
        if ((flag & ISlaveComputer.FLAG_B) > 0) {
            cmd += String.format(Locale.US, " B%.2f", b);
        }

        FabPacket packet = FabPacketBuilder.gcodeRequest(cmd, 0);
        return request(packet).map(o -> true).delay(2500, TimeUnit.MILLISECONDS);
    }

    /**
     * Movement: Absolute axis movement (0x0b 0x02)
     */
    @Override
    public Observable<Boolean> gotoAbsolutePosition(float x, float y, float z) {
        FabPacket packet = FabPacketBuilder.gotoAbsolutePosition(x, y, z);
        return request(packet).map(o -> (Boolean) o);
    }

    @Override
    public Observable<Boolean> gotoAbsolutePosition(float x, float y, float z, float f) {
        FabPacket packet = FabPacketBuilder.gotoAbsolutePosition(x, y, z, f);
        return request(packet).map(o -> (Boolean) o);
    }

    /**
     * Movement: Relative axis movement (0x0b 0x03)
     */
    public Observable<Boolean> gotoRelativePosition(float x, float y, float z) {
        FabPacket packet = FabPacketBuilder.gotoRelativePosition(x, y, z);
        return request(packet).map(o -> (Boolean) o);
    }

    public Observable<Boolean> gotoRelativePosition(float x, float y, float z, float f) {
        FabPacket packet = FabPacketBuilder.gotoRelativePosition(x, y, z, f);
        return request(packet).map(o -> (Boolean) o);
    }

    /**
     * Movement: Request Extrusion (0x0b 0x04)
     */
    @Override
    public Observable<Boolean> requestExtrusion(int type, float lengthIn, float speedIn, float lengthOut, float speedOut) {
        FabPacket packet = FabPacketBuilder.requestExtrusion(type, lengthIn, speedIn, lengthOut, speedOut);
        return request(packet).map(o -> (Boolean) o);
    }

    //0x09 0x2d
    @Override
    public Observable<Boolean> setCrossLineLaserIndicator(boolean active) {
        FabPacket packet = FabPacketBuilder.setCrossLineLaserIndicator(active);
        return request(packet).map(o -> (Boolean) o);
    }

    //0x09 0x2e
    @Override
    public Observable<Boolean> getCrossLineLaserIndicatorStatus() {
        FabPacket packet = FabPacketBuilder.getCrossLineLaserIndicator();
        return request(packet).map(o -> (Boolean) o);
    }

    @Override
    public Observable<Boolean> setFireSensorSensitivity(int sensitivity) {
        FabPacket packet = FabPacketBuilder.setFireSensorSensitivity(sensitivity);
        return request(packet).map(o -> (Boolean) o);
    }

    @Override
    public Observable<Short> getFireSensorSensitivity() {
        FabPacket packet = FabPacketBuilder.getFireSensorSensitivity();
        return request(packet).map(o -> (Short) o);
    }

    @Override
    public Observable<Boolean> setCrossLineIndicatorOffset(float xOffset, float yOffset) {
        FabPacket packet = FabPacketBuilder.setCrossLineIndicatorOffset(xOffset, yOffset);
        return request(packet).map(o -> (Boolean) o);
    }

    @Override
    public Observable<FabPacketContent.CrossLineIndicatorOffset> getCrossLineIndicatorOffset() {
        FabPacket packet = FabPacketBuilder.getCrossLineIndicatorOffset();
        return request(packet).map(o -> (FabPacketContent.CrossLineIndicatorOffset) o);
    }

    public Observable<Float> getLaserIndicatorPower() {
        FabPacket packet = FabPacketBuilder.getLaserIndicatorPower();
        return request(packet).map(o -> (Float) o);
    }

    public Observable<Boolean> setLaserIndicatorPower(float power) {
        FabPacket packet = FabPacketBuilder.setLaserIndicatorPower(power);
        return request(packet).map(o -> (Boolean) o);
    }

    /**
     * Add-on: Get enclosure status (0x11 0x01)
     */
    public Observable<FabPacketContent.EnclosureStatus> getEnclosureStatus() {
        FabPacket packet = FabPacketBuilder.getEnclosureStatus();
        return request(packet).map(o -> (FabPacketContent.EnclosureStatus) o);
    }

    /**
     * Add-on: Set enclosure led (0x11 0x02)
     *
     * @param value led value (0 - 100)
     */
    public Observable<Boolean> setEnclosureLed(int value) {
        FabPacket packet = FabPacketBuilder.setEnclosureLed(value);
        return request(packet).map(o -> (Boolean) o);
    }

    /**
     * Add-on: Set enclosure fan on/off (0x11 0x03)
     *
     * @param value fan value (0 - 100)
     */
    public Observable<Boolean> setEnclosureFan(int value) {
        FabPacket packet = FabPacketBuilder.setEnclosureFan(value);
        return request(packet).map(o -> (Boolean) o);
    }

    /**
     * Add-on: Set enclosure door detection (0x11 0x04)
     */
    public Observable<Boolean> setEnclosureDoorDetection(boolean enabled) {
        FabPacket packet = FabPacketBuilder.setEnclosureDoorDetection(enabled);
        return request(packet).map(o -> (Boolean) o);
    }

    /**
     * Add-on: request rotary module status (0x11 0x08)
     */
    public Observable<Byte> requestRotaryModuleStatus() {
        FabPacket packet = FabPacketBuilder.requestRotaryModuleStatus();
        return request(packet).map(o -> (Byte) o);
    }

    /**
     * Byte 0 emergency stop connected & idle
     * 1 emergency stop not connected or not available
     * 2 emergency stop triggered
     **/
    public Observable<Byte> watchEmergencyStopStatus() {
        FabPacket packet = FabPacketBuilder.requestEmergencyStopStatus();
        return watch(packet).map(o -> (Byte) o);
    }

    public Observable<Byte> requestEmergencyStopStatus() {
        FabPacket packet = FabPacketBuilder.requestEmergencyStopStatus();
        return request(packet).map(o -> (Byte) o);
    }

    @Override
    public Observable<FabPacketContent.AirPurifierStatus> requestAirPurifierAddOnStatus() {
        FabPacket packet = FabPacketBuilder.requestAirPurifierAddOnStatus();
        return request(packet).map(o -> (FabPacketContent.AirPurifierStatus) o);
    }

    @Override
    public Observable<FabPacketContent.AirPurifierStatus> watchAirPurifierAddOnStatus() {
        FabPacket packet = FabPacketBuilder.requestAirPurifierAddOnStatus();
        return watch(packet).map(o -> (FabPacketContent.AirPurifierStatus) o);
    }

    @Override
    public Observable<FabPacketContent.AirPurifierFan> requestAirPurifierFan() {
        FabPacket packet = FabPacketBuilder.requestAirPurifierFanStatus();
        return request(packet).map(o -> (FabPacketContent.AirPurifierFan) o);
    }

    @Override
    public Observable<Boolean> setAirPurifierEnabled(boolean enabled) {
        FabPacket packet = FabPacketBuilder.setAirPurifierFanEnabled(enabled);
        return request(packet).map(o -> (Boolean) o);
    }

    @Override
    public Observable<Boolean> setAirPurifierFanSpeedLevel(int level) {
        FabPacket packet = FabPacketBuilder.setAirPurifierFanSpeed(level);
        return request(packet).map(o -> (Boolean) o);
    }

    @Override
    public Observable<Integer> getAirPurifierFilterLifeTime() {
        FabPacket packet = FabPacketBuilder.requestAirPurifierFilterLifeTime();
        return request(packet).map(o -> (Integer) o);
    }

    @Override
    public Observable<Integer> watchAirPurifierFilterLifeTime() {
        FabPacket packet = FabPacketBuilder.requestAirPurifierFilterLifeTime();
        return watch(packet).map(o -> (Integer) o);
    }

    @Override
    public Observable<FabPacketContent.HeaderSecurity> requestHeaderSecurityStatus() {
        FabPacket packet = FabPacketBuilder.requestHeaderSecurityStatus();
        return request(packet).map(o -> (FabPacketContent.HeaderSecurity) o);
    }

    @Override
    public Observable<FabPacketContent.HeaderSecurity> watchHeaderSecurityStatus() {
        return mHeaderSecuritySubject.hide()
                .doOnNext(headerSecurity -> Logger.w("Head Safety Report: "
                        + headerSecurity.toString()));
    }

    @Override
    public Observable<Integer> requestHeaderOnlineSyncId(int timeout) {
        FabPacket packet = FabPacketBuilder.requestHeaderOnlineSyncId();
        return request(packet, timeout).map(o -> (Integer) o);
    }

    @Override
    public Observable<Boolean> setHeaderOnlineSyncId(int headerId) {
        FabPacket packet = FabPacketBuilder.setHeaderOnlineSyncId(headerId);
        return request(packet).map(o -> (Boolean) o);
    }

    @Override
    public void setAbnormalTemperatureRange(int protectTemperature, int recoveryTemperature) {
        FabPacket packet = FabPacketBuilder.setAbnormalTemperatureRange(protectTemperature, recoveryTemperature);
        send(packet);
    }

    @Override
    public Observable<Boolean> setPrintOffsetWithCrossLine(boolean enabled) {
        FabPacket packet = FabPacketBuilder.setPrintOffsetWithCrossLine(enabled);
        return request(packet).map(o -> (Boolean) o);
    }


    @Override
    public Observable<Integer> watchPrintPauseState() {
        return mPausePrintSubject.hide();
    }

    public Observable<String> getControllerVersion() {
        FabPacket packet = FabPacketBuilder.checkControllerVersion();
        return request(packet).map(o -> (String) o);
    }

    public Observable<Boolean> startUpdate() {
        FabPacket packet = FabPacketBuilder.startUpdate();
        return request(packet).map(o -> (Boolean) o);
    }

    public Observable<Short> watchPacketIndexRequest() {
        FabPacket packet = FabPacketBuilder.requestUpdatePackage();
        return watch(packet).map(o -> (Short) o);
    }

    public void sendUpdatePackage(byte opCode, short index, byte[] content) {
        FabPacket packet = FabPacketBuilder.sendUpdatePackage(opCode, index, content);
        send(packet);
    }

    public void requestModuleVersion() {
        FabPacket packet = FabPacketBuilder.requestModuleVersion();
        send(packet);
    }

    public Observable<FabPacketContent.ModuleVersion> watchModuleVersion() {
        FabPacket packet = FabPacketBuilder.requestModuleVersion();
        return watch(packet).map(o -> (FabPacketContent.ModuleVersion) o);
    }

    /**
     * Laser Camera Operation: set Camera Wi-Fi
     */
    public Observable<Boolean> setupLaserNetwork(String SSID, String password) {
        FabPacket packet = FabPacketBuilder.setupLaserNetwork(SSID, password);
        return request(packet).map(o -> (Boolean) o);
    }

    /**
     * Laser Camera Operation: Get laser status.
     */
    public Observable<FabPacketContent.LaserWifiStatus> getLaserWifiStatus() {
        FabPacket packet = FabPacketBuilder.getLaserStatus();
        return request(packet).map(o -> (FabPacketContent.LaserWifiStatus) o);
    }

    public Observable<FabPacketContent.LaserBtStatus> getLaserBluetoothStatus() {
        FabPacket packet = FabPacketBuilder.getLaserBtStatus();
        return request(packet).map(o -> (FabPacketContent.LaserBtStatus) o);
    }

    private void onMachineValidValueChanged(boolean isValid) {
        if (mIsStatusValidSubject == null ||mIsStatusValidSubject.getValue() == null) return;

        boolean current = mIsStatusValidSubject.getValue();
        if (isValid == current) return;

        mIsStatusValidSubject.onNext(isValid);
    }

    /**
     * Receive connection changes.
     *
     * @param connected whether serial port is connected
     */
    private void onConnection(boolean connected) {
        mConnectedSubject.onNext(connected);

        Logger.i("onConnection, connected = " + connected);

        if (connected) {
            mConnectedSubject.onNext(true);

            setHeartbeatEnabled(true);

            // Save connected device
            SharedPreferences sharedPref = mContext.getSharedPreferences("com.snapmaker.fabscreen.PREFERENCE_DEFAULT", Context.MODE_PRIVATE);
            sharedPref.edit().putString("SERIAL_PORT_PATH", mDevice).apply();
        } else {
            mDevice = null;
            mConnectedSubject.onNext(false);
            mMachineStatusSubject.onNext(FabPacketContent.MachineStatus.getDefaultInstance());
            onMachineValidValueChanged(true); // reset subject

            setHeartbeatEnabled(false);
        }
    }

    /**
     * Receive data from serial connection.
     * <p>
     * Note that this method is NOT called on UI thread.
     *
     * @param packet received packet
     */
    private void onReceive(FabPacket packet) {
        mLastReceiveTimeMillis = SystemClock.elapsedRealtime();
        // Too many logs, comment it out unless you are debugging the heartbeat.
//        Log.i(TAG, "Receive event " + packet.getEventId() + ": " + packet + " time: " + mLastReceiveTimeMillis);

        switch (packet.getEventId()) {
            // 0x02 G-code response
            // 0x04 File print G-code response
            case FabPacket.GCODE_RESPONSE_EVENT_ID:
            case FabPacket.PRINT_GCODE_RESPONSE_EVENT_ID: {
                FabPacketContent.GcodeResponse response = FabPacketContent.GcodeResponse.parse(packet.getContent());
                if (response != null) {
                    mGcodeResponse.setLineNo(response.getLineNo());
                    sendResponse(packet, response);
                } else {
                    Logger.e("response is null, data is not parsed properly.");
                }
                break;
            }
            case FabPacket.GCODE_RESPONSE_EXTEND_EVENT_ID: {
                byte operationId = packet.getContent()[0];
                switch (operationId) {
                    case 0x01:
                        Buffer buffer = new Buffer();
                        buffer.write(packet.getContent());
                        try {
                            // skip operation ID
                            buffer.readByte();
                            mGcodeResponse.mergeContent(buffer.readUtf8());
                        } catch (IOException e) {
                            Logger.e("Write G-code content failed.");
                        }
                        break;
                    case 0x02:
                        // Merge G-code response data
                        if (mGcodeResponse != null) {
                            sendResponse(packet, mGcodeResponse);
                            // erase Last Gcode Response
                            mGcodeResponse = new FabPacketContent.GcodeResponse();

                        }
                        break;
                }
                break;
            }


            // 0x08 Status Sync
            case FabPacket.STATUS_RESPONSE_EVENT_ID: {
                byte subEventId = packet.getContent()[0];
                if (packet.getContent().length == 1) break;
                switch (subEventId) {
                    case 0x01:
                        FabPacketContent.MachineStatus machineStatus = FabPacketContent.MachineStatus.parse(packet.getContent());
                        if (machineStatus != null) {
                            sendResponse(packet, machineStatus);
                            onMachineValidValueChanged(true);
                        } else {
                            Logger.e("Invalid machine status payload detected.");
                            onMachineValidValueChanged(false);
                        }
                        break;
                    case 0x02:
                        // TODO:
                        FabPacketContent.MachineErrors machineErrors = FabPacketContent.MachineErrors.parse(packet.getContent());
                        if (machineErrors != null) {
                            RequestReceiver receiver = mReceivers.get(packet.getKey());
                            if (receiver != null) {
                                receiver.receive(machineErrors);
                            }
                        }
                        break;
                    case 0x04:
                        // Processing 04 pause
                        mPausePrintSubject.onNext((packet.getContent()[1] & 0xff));
                    case 0x03:
                    case 0x05:
                    case 0x06:
                    case 0x07:
                    case 0x0a:
                    case 0x0b:
                    case 0x0c:
                    case 0x12: {
                        RequestReceiver receiver = mReceivers.get(packet.getKey());
                        if (receiver != null) {
                            receiver.receive(packet.getContent()[1] & 0xff);
                        } else {
                            mMasterStateSubject.onNext(FabPacketContent.MasterState.parse(packet.getContent()));
                        }
                        break;
                    }
                    case 0x08: {
                        // TODO
                        Buffer buffer = new Buffer();
                        buffer.write(packet.getContent());
                        try {
                            buffer.readByte();
                            buffer.readByte(); // isPowerPanic
                            buffer.readByte(); // isLocal
                            final int lineno = buffer.readInt();

                            sendResponse(packet, lineno);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        break;
                    }
                    case 0x09: {
                        Buffer buffer = new Buffer();
                        buffer.write(packet.getContent());
                        try {
                            buffer.readByte();
                            final int progress = buffer.readInt();
                            RequestReceiver receiver = mReceivers.get(packet.getKey());
                            if (receiver != null) {
                                receiver.receive(progress);
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        break;
                    }
                    case 0x0e: {
                        FabPacketContent.CoordinateSystem coordinateSystem = FabPacketContent.CoordinateSystem.parse(packet.getContent());
                        sendResponse(packet, coordinateSystem);
                        break;
                    }
                    case 0x0f: {
                        byte logLevel = packet.getContent()[1];
                        LogHelper.setFirmwareLogLevel(logLevel);
                        break;
                    }
                    case 0x10: {
                        Buffer buffer = new Buffer();
                        buffer.write(packet.getContent());

                        try {
                            buffer.readByte();
                            int level = (int) buffer.readByte();
                            buffer.readByte();
                            String msg = buffer.readString(Charset.forName("ASCII")).trim();
                            LogHelper.firmwareLog(level, msg);
                        } catch (IOException e) { /* */ }
                        break;
                    }
                    case 0x11: {
                        RequestReceiver receiver = mReceivers.get(packet.getKey());
                        FabPacketContent.HeaderSecurity headerSecurity = FabPacketContent.HeaderSecurity.parse(packet.getContent());
                        if (receiver != null) {
                            receiver.receive(headerSecurity);
                        }
                        mHeaderSecuritySubject.onNext(headerSecurity);
                        break;
                    }
                    case 0x13:
                        sendResponse(packet, ExtruderModel.parse(packet.getContent()));
                        break;
                    case 0x14:
                        byte[] content = packet.getContent();
                        sendResponse(packet, Arrays.asList(content[1] == 0, content[2] == 0));
                        break;
                    case 0x15:
                        sendResponse(packet, ExtruderTemperature.parse(packet.getContent()));
                        break;
                }
                break;
            }

            // 0x0a Setting Response
            case FabPacket.SETTINGS_RESPONSE_EVENT_ID: {
                byte subEventId = packet.getContent()[0];
                switch (subEventId) {
                    case 0x01:
                    case 0x0b:
                    case 0x0c:
                    case 0x13:
                    case 0x0d: {
                        sendResponse(packet, packet.getContent()[1] == 0);
                        break;
                    }
                    case 0x02:
                        if (packet.getContent()[1] != 0) {
                            // Auto calibration fail.
                            RequestReceiver receiver = mReceivers.get(packet.getKey());
                            receiver.receive(false);
                            break;
                        }
                    case 0x04:
                    case 0x05:
                    case 0x06:
                    case 0x07:
                    case 0x08:
                    case 0x09: {
                        RequestReceiver receiver = mReceivers.get(packet.getKey());
                        if (receiver != null) {
                            receiver.receive(true);
                        }
                        break;
                    }
                    case 0x03: {
                        if (packet.getContent()[1] != 0) {
                            // TODO: handle error response
                        }
                        mAutoCalibrationProgressSubject.onNext((packet.getContent()[2] & 0xFF));
                        break;
                    }
                    case 0x0a: {
                        Buffer buffer = new Buffer();
                        buffer.write(packet.getContent());
                        try {
                            buffer.readByte(); // sub-event
                            buffer.readByte(); // success
                            final int focalLengthX1000 = buffer.readInt();
                            sendResponse(packet, focalLengthX1000 / 1000f);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        break;
                    }
                    case 0x0e:
                    case 0x0f: {
                        sendResponse(packet, (int) packet.getContent()[1]);
                        break;
                    }
                    case 0x10: {
                        FabPacketContent.AdjustSettings settings = FabPacketContent.AdjustSettings.parse(packet.getContent());
                        sendResponse(packet, settings);
                        break;
                    }
                    case 0x11: {
                        // Light control result
                        sendResponse(packet, packet.getContent()[1]);
                        break;
                    }
                    case 0x12: {
                        Buffer buffer = new Buffer();
                        buffer.write(packet.getContent());
                        try {
                            buffer.readByte();
                            sendResponse(packet, buffer.readInt());
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        break;
                    }
                    case 0x14: {
                        FabPacketContent.MachineSize machineSize = FabPacketContent.MachineSize.parse(packet.getContent());
                        Logger.d("Response 0x0a 0x14");
                        if (machineSize != null) {
                            sendResponse(packet, machineSize);
                        } else {
                            Logger.d("Response 0x0a 0x14 machine size is null, packet is: ", new String(packet.toByteArray()));
                        }
                        break;
                    }
                    case 0x15: {
                        sendResponse(packet, packet.getContent()[1] == 1);
                        break;
                    }
                    case 0x21: {
                        sendResponse(packet, (int) packet.getContent()[1] & 0xFF);
                        break;
                    }
                    case 0x18:
                    case 0x1a:
                    case 0x1b:
                    case 0x1d:
                    case 0x1e:
                    case 0x1f:
                    case 0x20:
                    case 0x22:
                    case 0x23:
                    case 0x24:
                    case 0x25:
                    case 0x2d:
                        sendResponse(packet, packet.getContent()[1] == 0);
                        break;
                    case 0x19:
                        Logger.d("0a19-content: %s", ByteString.of(packet.getContent()).hex());
                        List<Float> offsets = new ArrayList<>();
                        Buffer buffer = new Buffer();
                        buffer.write(packet.getContent());
                        try {
                            buffer.skip(1);
                            offsets.add(buffer.readInt() / 1000f);
                            offsets.add(buffer.readInt() / 1000f);
                            offsets.add(buffer.readInt() / 1000f);
                        } catch (EOFException e) {
                            LogHelper.log(e);
                        }
                        sendResponse(packet, offsets);
                        break;
                    case 0x1c:
                        /*byte[] content = packet.getContent();*/
                        // Response will always be true.
                        sendResponse(packet, /*Arrays.asList(content[1] & 0xff, content[2] & 0xff, content[3] & 0xff)*/true);
                        break;
                    case 0x2b:
                        FabPacketContent.ExtendKitInfo info = FabPacketContent.ExtendKitInfo.parse(packet.getContent());
                        if (info != null) {
                            sendResponse(packet, info);
                        }
                        break;
                    case 0x2c:
                        Buffer extendKitBuffer = new Buffer();
                        extendKitBuffer.write(packet.getContent());
                        try {
                            extendKitBuffer.skip(1);
                            byte result = extendKitBuffer.readByte();
                            sendResponse(packet, result);
                        } catch (EOFException e) {
                            LogHelper.log(e);
                        }
                        break;
                    case 0x26:
                        sendResponse(packet, (int) packet.getContent()[1] & 0xFF);
                        break;
                    case 0x2e:
                        Buffer crossLineBuffer = new Buffer();
                        crossLineBuffer.write(packet.getContent());
                        try {
                            crossLineBuffer.readByte(); // sub-event
                            crossLineBuffer.readByte(); // success
                            sendResponse(packet, crossLineBuffer.readByte() == 0);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        break;
                    case 0x2f:
                        Buffer fireSensorSensitivityBuffer = new Buffer();
                        fireSensorSensitivityBuffer.write(packet.getContent());
                        try {
                            fireSensorSensitivityBuffer.readByte(); // sub-event
                            sendResponse(packet, fireSensorSensitivityBuffer.readByte() == 0);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        break;
                    case 0x30:
                        Buffer setFireSensorResponseBuffer = new Buffer();
                        setFireSensorResponseBuffer.write(packet.getContent());
                        try {
                            setFireSensorResponseBuffer.readByte(); // operationId
                            setFireSensorResponseBuffer.readByte();
                            sendResponse(packet, setFireSensorResponseBuffer.readShort());
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        break;
                    case 0x32:
                        sendResponse(packet, packet.getContent()[1] == 0);
                        break;
                    case 0x33:
                        FabPacketContent.CrossLineIndicatorOffset crossLineIndicatorOffset = FabPacketContent.CrossLineIndicatorOffset.parse(packet.getContent());
                        sendResponse(packet, crossLineIndicatorOffset);
                        break;
                    case 0x34:
                        sendResponse(packet, packet.getContent()[1] == 0);
                        break;
                    case 0x35:
                        Buffer laserIndicatorPowerBuffer = new Buffer();
                        laserIndicatorPowerBuffer.write(packet.getContent());
                        try {
                            laserIndicatorPowerBuffer.skip(1); // sub-event
                            boolean isSuccess = laserIndicatorPowerBuffer.readByte() == 0; // success

                            if (isSuccess) {
                                sendResponse(packet, ((float) laserIndicatorPowerBuffer.readInt() / 1000f));
                            } else {
                                Logger.e("Get Laser Indicator failed, using default value");
                                sendResponse(packet, 0f);
                            }

                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        break;
                    case 0x36:
                        sendResponse(packet, packet.getContent()[1] == 0);
                        break;
                }
                break;
            }

            // 0x0c Movement Response
            case FabPacket.MOVEMENT_RESPONSE_EVENT_ID: {
                byte subEventId = packet.getContent()[0];
                switch (subEventId) {
                    case 0x01:
                    case 0x02:
                    case 0x03:
                    case 0x04:
                    case 0x05:
                    case 0x06:
                        sendResponse(packet, packet.getContent()[1] == 0);
                        break;
                }
                break;
            }

            // 0x0e Laser Camera Operation Response
            case FabPacket.LASER_CAMERA_OPERATION_RESPONSE_EVENT_ID: {
                byte subEventId = packet.getContent()[0];
                switch (subEventId) {
                    case 0x01: {
                        sendResponse(packet, packet.getContent()[1] == 0);
                        break;
                    }

                    case 0x02: {
                        FabPacketContent.LaserWifiStatus status = FabPacketContent.LaserWifiStatus.parse(packet.getContent());
                        if (status != null) {
                            sendResponse(packet, status);
                        }
                        break;
                    }
                    case 0x07: {
                        FabPacketContent.LaserBtStatus status = FabPacketContent.LaserBtStatus.parse(packet.getContent());
                        if (status != null) {
                            sendResponse(packet, status);
                        }
                        break;
                    }
                }
                break;
            }

            // 0x12 Add-on Operation Response
            case FabPacket.ADD_ON_OPERATION_RESPONSE_EVENT_ID: {
                byte subEventId = packet.getContent()[0];
                switch (subEventId) {
                    case 0x00: {
                        // TODO: add-on module list response, it's not defined yet.
                        break;
                    }
                    case 0x01: {
                        FabPacketContent.EnclosureStatus status = FabPacketContent.EnclosureStatus.parse(packet.getContent());
                        if (status != null) {
                            sendResponse(packet, status);
                        }
                        break;
                    }
                    case 0x02:
                    case 0x03:
                    case 0x04:
                    case 0x0B:
                    case 0x0C: {
                        sendResponse(packet, packet.getContent()[1] == 0);
                        break;
                    }
                    case 0x07:
                    case 0x08: {
                        sendResponse(packet, packet.getContent()[1]);
                        break;
                    }
                    case 0x09: {
                        FabPacketContent.AirPurifierStatus status = FabPacketContent.AirPurifierStatus.parse(packet.getContent());
                        if (status != null) {
                            sendResponse(packet, status);
                        }
                        break;
                    }
                    case 0x0A: {
                        FabPacketContent.AirPurifierFan airPurifierFan = FabPacketContent.AirPurifierFan.parse(packet.getContent());
                        if (airPurifierFan != null) {
                            sendResponse(packet, airPurifierFan);
                        }
                        break;
                    }
                    case 0x0D: {
                        Buffer buffer = new Buffer();
                        try {
                            buffer.write(packet.getContent());
                            buffer.readByte(); // operation id
                            int filterLifeTime = buffer.readByte();
                            sendResponse(packet, filterLifeTime);
                        } catch (IOException e) {
                            LogHelper.log(e);
                        }
                        break;
                    }
                }
                break;
            }
            // 0x14 Batch Gcode Response
            case FabPacket.PRINT_BATCH_GCODE_RESPONSE_EVENT_ID: {
                FabPacketContent.BatchGcodeResponse batchGcodeResponse = FabPacketContent.BatchGcodeResponse.parse(packet.getContent());
                if (batchGcodeResponse != null) {
                    mBatchGcodeResponseSubject.onNext(batchGcodeResponse);
                }
                break;
            }
            // 0xaa Update
            case FabPacket.UPDATE_RESPONSE_EVENT_ID: {
                byte subEventId = packet.getContent()[0];
                switch (subEventId) {
                    case 0x00: {
                        sendResponse(packet, packet.getContent()[1] == 0);
                        break;
                    }
                    case 0x01: {
                        Buffer buffer = new Buffer();
                        short packageIndex = 0;
                        try {
                            buffer.write(packet.getContent());
                            buffer.readByte(); // subEventID
                            packageIndex = buffer.readShort();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        sendResponse(packet, packageIndex);
                        break;
                    }
                    case 0x02: {
                        break;
                    }
                    case 0x03: {
                        Buffer buffer = new Buffer();
                        buffer.write(packet.getContent());
                        String version = "";
                        try {
                            buffer.readByte();
                            // FIXME: readString could cause problem，the return string contains 00 in the end of data
                            version = buffer.readString(FabPacketContent.UTF_8).trim();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        sendResponse(packet, version);
                        break;
                    }

                    case 0x07: {
                        FabPacketContent.ModuleVersion moduleVersion = FabPacketContent.ModuleVersion.parse(packet.getContent());
                        if (moduleVersion != null) {
                            sendResponse(packet, moduleVersion);
                        }
                        break;
                    }
                }
                break;
            }
        }
    }

    private void sendResponse(FabPacket packet, Object result) {
        RequestReceiver receiver = mReceivers.get(packet.getKey());
        if (receiver != null) {
            if (result != null) {
                receiver.receive(result);
            } else {
                receiver.error(new FabException("Unable to parse response body."));
            }
        }
    }

    public void onEmergencyStop() {
        // disable heartbeat
        setHeartbeatEnabled(false);
        // complete all the emitters
        for (int i = 0; i < mReceivers.size(); i++) {
            int key = mReceivers.keyAt(i);
            RequestReceiver receiver = mReceivers.get(key);
            receiver.complete();
        }
    }

    @Override
    public Observable<List<ExtruderModel>> getExtruderModel() {
        FabPacket packet = FabPacketBuilder.getExtruderModel();
        //noinspection unchecked
        return request(packet).map(o -> (List<ExtruderModel>) o);
    }

    @Override
    public Observable<List<Boolean>> getExtrudersHaveFilament() {
        FabPacket packet = FabPacketBuilder.getExtrudersHaveFilament();
        //noinspection unchecked
        return request(packet).map(o -> (List<Boolean>) o);
    }

    @Override
    public Observable<List<ExtruderTemperature>> getExtruderTemperatures() {
        FabPacket packet = FabPacketBuilder.getExtruderTemperatures();
        //noinspection unchecked
        return request(packet).map(o -> (List<ExtruderTemperature>) o);
    }

    @Override
    public Observable<Boolean> setWorkSpeed(int which, float value) {
        FabPacket packet = FabPacketBuilder.setWorkSpeed(which, value);
        return request(packet).map(o -> (Integer) o == 0);
    }

    @Override
    public Observable<Boolean> setExtruderTemperature(int which, float value) {
        FabPacket packet = FabPacketBuilder.setExtruderTemperature(which, value);
        return request(packet).map(o -> (Integer) o == 0);
    }

    @Override
    public Observable<Boolean> setLiveZOffset(int which, float value) {
        FabPacket packet = FabPacketBuilder.setLiveZOffset(which, value);
        return request(packet).map(o -> (Integer) o == 0);
    }

    @Override
    public Observable<Boolean> setFlowRate(int which, float value) {
        FabPacket packet = FabPacketBuilder.setFeedRate(which, value);
        return request(packet).map(o -> (Integer) o==0);
    }

    @Override
    public Observable<Float> getWorkSpeed(int which) {
        FabPacket packet = FabPacketBuilder.getWorkSpeed(which);
        return request(packet).map(o -> ((FabPacketContent.AdjustSettings) o).value);
    }

    @Override
    public Observable<Float> getLiveZOffset(int which) {
        FabPacket packet = FabPacketBuilder.getLiveZOffset(which);
        return request(packet).map(o -> ((FabPacketContent.AdjustSettings) o).value);
    }

    @Override
    public Observable<Boolean> switchToExtruder(int which) {
        FabPacket packet = FabPacketBuilder.switchToExtruder(which);
        return request(packet).map(o -> (Boolean) o);
    }

    @Override
    public Observable<List<Float>> getExtruderOffset() {
        FabPacket packet = FabPacketBuilder.getExtruderOffset();
        //noinspection unchecked
        return request(packet).map(o -> (List<Float>) o);
    }

    @Override
    public Observable<Boolean> confirmExtruderPosition(int which) {
        FabPacket packet = FabPacketBuilder.confirmExtruderPosition(which);
        return request(packet).map(o -> (Boolean) o);
    }

    @Override
    public Observable<Boolean> setExtruderOffset(int direction, float offset) {
        FabPacket packet = FabPacketBuilder.setExtruderOffset(direction, offset);
        return request(packet).map(o -> (Boolean) o);
    }

    @Override
    public Observable<Boolean> setToolheadFanSpeed(int which, int speed) {
        FabPacket packet = FabPacketBuilder.setToolheadFanSpeed(which, speed);
        return request(packet).map(o -> (Boolean) o);
    }

    @Override
    public Observable<Boolean> calibrateSensorTouchBed(int which, boolean isAuto) {
        FabPacket packet = FabPacketBuilder.calibrateSensorTouchBed(which, isAuto);
        return request(packet).map(o -> (Boolean) o);
    }

    @Override
    public Observable<Boolean> calibrateSensorManualConfirm(int which) {
        FabPacket packet = FabPacketBuilder.calibrateSensorManualConfirm(which);
        return request(packet).map(o -> (Boolean) o);
    }

    @Override
    public Observable<Boolean> calibrationSensorRequestAbort() {
        FabPacket packet = FabPacketBuilder.calibrationSensorRequestAbort();
        return request(packet).map(o -> (Boolean) o);
    }

    @Override
    public Observable<Boolean> probeBedPosition(int which, boolean isAuto) {
        FabPacket packet = FabPacketBuilder.probeBedPosition(which, isAuto);
        return request(packet).map(o -> (Boolean) o);
    }

    @Override
    public Observable<Boolean> saveBedPosition() {
        FabPacket packet = FabPacketBuilder.saveBedPosition();
        return request(packet).map(o -> (Boolean) o);
    }

    @Override
    public Observable<Boolean> startDualExtruderAutoLeveling(int grid) {
        FabPacket packet = FabPacketBuilder.startDualExtruderAutoLeveling(grid);
        return request(packet).map(o -> (Boolean) o);
    }

    @Override
    public Observable<Integer> dualExtruderAutoLevelPoint(int point) {
        FabPacket packet = FabPacketBuilder.dualExtruderAutoLevelPoint(point);
        return request(packet).map(o -> (Integer) o);
    }

    @Override
    public Observable<Boolean> finishDualExtruderAutoLeveling() {
        FabPacket packet = FabPacketBuilder.stopDualExtruderAutoLeveling();
        return request(packet).map(o -> (Boolean) o);
    }

    @Override
    public Observable<Boolean> startDualExtruderManualLeveling(int grid) {
        FabPacket packet = FabPacketBuilder.startDualExtruderManualLeveling(grid);
        return request(packet).map(o -> (Boolean) o);
    }

    @Override
    public Observable<Boolean> dualExtruderManualLevelPoint(int point) {
        FabPacket packet = FabPacketBuilder.dualExtruderManualLevelPoint(point);
        return request(packet).map(o -> (Boolean) o);
    }

    @Override
    public Observable<Boolean> finishDualExtruderManualLeveling() {
        FabPacket packet = FabPacketBuilder.dualExtruderStopManualLeveling();
        return request(packet).map(o -> (Boolean) o);
    }

    @Override
    public Observable<Integer> getActivatedExtruder() {
        FabPacket packet = FabPacketBuilder.getActivatedExtruder();
        return request(packet).map(o -> (Integer) o);
    }

    @Override
    public Observable<Boolean> extrudeInfinitely(int direction, int speed) {
        FabPacket packet = FabPacketBuilder.extrudeInfinitely(direction, speed);
        return request(packet).map(o -> (Boolean) o);
    }

    @Override
    public Observable<Boolean> stopMovement() {
        FabPacket packet = FabPacketBuilder.stopMovement();
        return request(packet).map(o -> (Boolean) o);
    }
}
