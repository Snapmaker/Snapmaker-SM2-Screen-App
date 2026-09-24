package fabscreen.libraries.legacy.server.http.handlers;

import static fabscreen.libraries.legacy.data.remote.SessionManager.NULL_SESSION;

import android.content.Intent;
import android.text.TextUtils;

import com.alibaba.android.arouter.launcher.ARouter;
import com.orhanobut.logger.Logger;
import com.yanzhenjie.andserver.annotation.GetMapping;
import com.yanzhenjie.andserver.annotation.PostMapping;
import com.yanzhenjie.andserver.annotation.RequestParam;
import com.yanzhenjie.andserver.annotation.RestController;
import com.yanzhenjie.andserver.framework.body.JsonBody;
import com.yanzhenjie.andserver.framework.body.StringBody;
import com.yanzhenjie.andserver.http.HttpRequest;
import com.yanzhenjie.andserver.http.HttpResponse;
import com.yanzhenjie.andserver.http.ResponseBody;
import com.yanzhenjie.andserver.http.multipart.MultipartFile;
import com.yanzhenjie.andserver.util.StatusCode;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import fabscreen.libraries.legacy.BaseApplication;
import fabscreen.libraries.legacy.data.Constants;
import fabscreen.libraries.legacy.data.MachineController;
import fabscreen.libraries.legacy.data.print.MachinePrintJobState;
import fabscreen.libraries.legacy.lib.LogHelper;
import fabscreen.libraries.legacy.lib.file.FabLocalFile;
import fabscreen.libraries.legacy.lib.file.IFile;
import fabscreen.libraries.legacy.lib.parser.GcodeParser;
import fabscreen.libraries.legacy.lib.parser.IGcodeParser;
import fabscreen.libraries.legacy.route.RoutePath;
import fabscreen.libraries.legacy.server.http.HTTPEventBus;
import fabscreen.libraries.legacy.view.FabConfirm;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.subjects.SingleSubject;

@RestController
class OrcaRequestHandler extends BaseRequestHandler {
    private static final String URI_LOCAL_FILE = "/api/files/local";
    private static final String URI_CHECK_VERSION = "/api/version";
    private static final String URI_ROOT = "/";

    CompositeDisposable mDisposable = new CompositeDisposable();

    @PostMapping(path = URI_LOCAL_FILE)
    void uploadFile(HttpRequest request, HttpResponse response,
                    @RequestParam(name = "file") MultipartFile file) {
        String isNeedPrint = request.getParameter("print");
        if (isNeedPrint == null) {
            Logger.w("parameter \"print\" not found");
            response.setStatus(HttpResponse.SC_BAD_REQUEST);
            response.setBody(new StringBody("parameter \"print\" not found"));
            return;
        }
        if (isMachineStatePrinting() && isNeedPrint.equals("true")) {
            Logger.w("Machine is not in idle status");
            response.setStatus(HttpResponse.SC_SERVICE_UNAVAILABLE);
            response.setBody(new StringBody("Machine is not in idle status"));
            return;
        }

        if (file.isEmpty()) {
            Logger.w("Upload file is empty, stop receiving...");
            response.setBody(new StringBody("Empty file body"));
            response.setStatus(HttpResponse.SC_BAD_REQUEST);
            return;
        }

        if (file.getFilename() == null) {
            Logger.w("Upload file name is empty, stop receiving...");
            response.setBody(new StringBody("Empty file name"));
            response.setStatus(HttpResponse.SC_BAD_REQUEST);
            return;
        }

        if (getModel().getMachineController().isEmergencyStopTriggered()) {
            response.setStatus(HttpResponse.SC_SERVICE_UNAVAILABLE);
            response.setBody(new StringBody("Machine is not in idle status"));
            Logger.d("Current in emergency stop state, stop receiving file.");
            return;
        }

        File targetFile = new File(getModel().getFilesDir(), file.getFilename());
        try {
            file.transferTo(targetFile);
            HTTPEventBus.getInstance().onReceiveFile(targetFile);
        } catch (IOException e) {
            response.setStatus(HttpResponse.SC_INTERNAL_SERVER_ERROR);
            response.setBody(new StringBody("Save file failed."));
            LogHelper.log(e);
            return;
        }

        response.setBody(new StringBody("Upload successfully."));

        CountDownLatch countDownLatch = new CountDownLatch(1);
        if (isNeedPrint.equals("true") && getModel().getMachineController().getWorkType() == MachineController.WorkType.FDM) {
            if (getModel().getRemoteController().getRemotePageFlag()) {
                Logger.d("disconnect remote connection for staring print via upload...");
                getModel().getRemoteController().setCurrentSession(NULL_SESSION);
            }
            getModel().getRemoteController().connectPrintController();
            Disposable sub = preparePrint(targetFile).flatMap(result -> {
                if (result) {
                    return startPrint();
                } else {
                    return Observable.just(false);
                }
            }).subscribe(success -> {
                AndroidSchedulers.mainThread().scheduleDirect(this::gotoPrintSilently, 1000, TimeUnit.MILLISECONDS);

                if (success) {
                    response.setStatus(StatusCode.SC_OK);
                } else {
                    response.setBody(new StringBody("Prepare and start print failed, if problem persist, please restart machine and try again."));
                    response.setStatus(StatusCode.SC_SERVICE_UNAVAILABLE);
                }
                countDownLatch.countDown();
            }, e -> {
                response.setStatus(HttpResponse.SC_INTERNAL_SERVER_ERROR);
                countDownLatch.countDown();
            });
            mDisposable.add(sub);
        } else {
            Logger.d("upload complete, print skip.");
            countDownLatch.countDown();
        }

        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            response.setBody(new StringBody("Interrupted."));
            response.setStatus(HttpResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    private void gotoPrintSilently() {
        ARouter.getInstance().build(RoutePath.PRINT_ACTIVITY).withFlags(Intent.FLAG_ACTIVITY_NEW_TASK).navigation(BaseApplication.getInstance().getViewSub().getContext());
    }

    @GetMapping(URI_CHECK_VERSION)
    void checkVersion(HttpResponse response) {
        JSONObject versionResponse = new JSONObject();
        try {
            versionResponse.put("api", "0.1");
            versionResponse.put("server", "1.2.3");
            versionResponse.put("text", "OctoPrint 1.2.3/Screen Dummy");
            versionResponse.put("machineConnection", true);
            final ResponseBody body = new JsonBody(versionResponse);
            response.setBody(body);
            response.setStatus(StatusCode.SC_OK);
        } catch (JSONException e) {
            LogHelper.log(e);
        }
    }

    @GetMapping(value = URI_ROOT, produces = "text/html; charset=utf-8")
    void getRoot(HttpResponse response) {
        String machineName = getModel().getPreferences().getMachineName();
        int machineModel = getModel().getMachineController().getMachineModel();
        String modelName = "N/A";
        switch (machineModel) {
        case Constants.MACHINE_MODEL_SNAPMAKER_A150: {
            modelName = "A150";
            break;
        }
        case Constants.MACHINE_MODEL_SNAPMAKER_A250: {
            modelName = "A250";
            break;
        }
        case Constants.MACHINE_MODEL_SNAPMAKER_A350: {
            modelName = "A350";
            break;
        }
        default:
            break;
        }
        String ip = getIPAddress();
        String macAddress = getMacAddr();
        String firmwareVersion = getCurrentVersion();

        String html = "<!DOCTYPE html>"
                + "<html><head><meta charset=\"utf-8\"><meta name=\"viewport\" content=\"width=device-width,initial-scale=1\"><title>" + (machineName != null ? machineName : "Device Info") + "</title>"
                + "<style>"
                + "*{margin:0;padding:0;box-sizing:border-box;}"
                + "body{font-family:'Roboto','Noto Sans SC',sans-serif;background:#121212;color:#fff;min-height:100vh;}"
                + ".app-bar{background:#1e1e1e;padding:16px 24px;box-shadow:0 2px 4px rgba(0,0,0,0.5);position:sticky;top:0;z-index:100;}"
                + ".app-bar-content{max-width:1200px;margin:0 auto;display:flex;align-items:center;justify-content:space-between;}"
                + ".app-title{font-size:20px;font-weight:500;display:flex;align-items:center;gap:12px;}"
                + ".logo{width:32px;height:32px;background:linear-gradient(135deg,#2196F3,#1976D2);border-radius:8px;display:flex;align-items:center;justify-content:center;font-weight:700;font-size:18px;}"
                + ".status-badge{background:#1b5e20;color:#69f0ae;padding:4px 12px;border-radius:12px;font-size:12px;font-weight:500;display:flex;align-items:center;gap:6px;}"
                + ".status-indicator{width:8px;height:8px;background:#69f0ae;border-radius:50%;animation:pulse 2s infinite;}"
                + "@keyframes pulse{0%,100%{opacity:1;}50%{opacity:0.4;}}"
                + ".container{max-width:1200px;margin:0 auto;padding:24px;}"
                + ".section{background:#1e1e1e;border-radius:8px;margin-bottom:16px;overflow:hidden;}"
                + ".section-header{padding:16px 20px;border-bottom:1px solid #2c2c2c;display:flex;align-items:center;gap:12px;}"
                + ".section-icon{width:24px;height:24px;color:#2196F3;}"
                + ".section-title{font-size:16px;font-weight:500;color:#fff;}"
                + ".section-content{padding:20px;}"
                + ".info-grid{display:grid;grid-template-columns:repeat(auto-fill,minmax(280px,1fr));gap:16px;}"
                + ".info-item{background:#2c2c2c;border-radius:8px;padding:16px;transition:all 0.2s;border:1px solid transparent;}"
                + ".info-item:hover{background:#333;border-color:#2196F3;transform:translateY(-2px);}"
                + ".info-label{font-size:12px;color:#9e9e9e;text-transform:uppercase;letter-spacing:0.5px;margin-bottom:8px;font-weight:500;}"
                + ".info-value{font-size:16px;color:#fff;font-weight:400;word-break:break-word;font-family:'Roboto Mono','Courier New',monospace;}"
                + ".footer{text-align:center;padding:24px;color:#757575;font-size:13px;}"
                + ".footer a{color:#2196F3;text-decoration:none;}"
                + ".footer a:hover{text-decoration:underline;}"
                + "@media(max-width:768px){.container{padding:16px;}.info-grid{grid-template-columns:1fr;}.app-bar{padding:12px 16px;}}"
                + "svg{fill:currentColor;}"
                + "</style></head>"
                + "<body>"
                + "<div class=\"app-bar\">"
                + "<div class=\"app-bar-content\">"
                + "<div class=\"app-title\">"
                + "<div class=\"logo\">A</div>"
                + "<span>" + (machineName != null ? machineName : "SM2.0 Device") + "</span>"
                + "</div>"
                + "<div class=\"status-badge\">"
                + "<span class=\"status-indicator\"></span>"
                + "<span>READY</span>"
                + "</div>"
                + "</div>"
                + "</div>"
                + "<div class=\"container\">"
                + "<div class=\"section\">"
                + "<div class=\"section-header\">"
                + "<svg class=\"section-icon\" viewBox=\"0 0 24 24\"><path d=\"M13,2.05V5.08C16.39,5.57 19,8.47 19,12C19,12.9 18.82,13.75 18.5,14.54L21.12,16.07C21.68,14.83 22,13.45 22,12C22,6.82 18.05,2.55 13,2.05M12,19C8.13,19 5,15.87 5,12C5,8.47 7.61,5.57 11,5.08V2.05C5.94,2.55 2,6.81 2,12C2,17.52 6.47,22 12,22C14.3,22 16.39,21.2 18.07,19.93L15.54,18.35C14.77,18.74 13.91,19 13,19H12M12,6A6,6 0 0,0 6,12C6,14.97 8.16,17.43 11,17.93V14.89C9.26,14.43 8,12.86 8,11A4,4 0 0,1 12,7A4,4 0 0,1 16,11C16,12.86 14.74,14.43 13,14.89V17.93C15.84,17.43 18,14.97 18,12A6,6 0 0,0 12,6Z\"/></svg>"
                + "<span class=\"section-title\">设备信息</span>"
                + "</div>"
                + "<div class=\"section-content\">"
                + "<div class=\"info-grid\">"
                + "<div class=\"info-item\">"
                + "<div class=\"info-label\">设备名称</div>"
                + "<div class=\"info-value\">" + (machineName != null ? machineName : "未知") + "</div>"
                + "</div>"
                + "<div class=\"info-item\">"
                + "<div class=\"info-label\">设备型号</div>"
                + "<div class=\"info-value\">" + (modelName != null ? modelName : "未知") + "</div>"
                + "</div>"
                + "<div class=\"info-item\">"
                + "<div class=\"info-label\">固件版本</div>"
                + "<div class=\"info-value\">" + (firmwareVersion != null ? firmwareVersion : "未知") + "</div>"
                + "</div>"
                + "</div>"
                + "</div>"
                + "</div>"
                + "<div class=\"section\">"
                + "<div class=\"section-header\">"
                + "<svg class=\"section-icon\" viewBox=\"0 0 24 24\"><path d=\"M17,3A2,2 0 0,1 19,5V15A2,2 0 0,1 17,17H13V19H14A1,1 0 0,1 15,20H22V22H15A1,1 0 0,1 14,23H10A1,1 0 0,1 9,22H2V20H9A1,1 0 0,1 10,19H11V17H7C5.89,17 5,16.1 5,15V5A2,2 0 0,1 7,3H17M17,5H7V15H17V5Z\"/></svg>"
                + "<span class=\"section-title\">网络信息</span>"
                + "</div>"
                + "<div class=\"section-content\">"
                + "<div class=\"info-grid\">"
                + "<div class=\"info-item\">"
                + "<div class=\"info-label\">IP 地址</div>"
                + "<div class=\"info-value\">" + (ip != null && !ip.isEmpty() ? ip : "未知") + "</div>"
                + "</div>"
                + "<div class=\"info-item\">"
                + "<div class=\"info-label\">MAC 地址</div>"
                + "<div class=\"info-value\">" + (macAddress != null && !macAddress.isEmpty() ? macAddress : "未知") + "</div>"
                + "</div>"
                + "</div>"
                + "</div>"
                + "</div>"
                + "</div>"
                + "<div class=\"footer\">"
                + "Powered by <a href=\"#\">Snapmaker 2.0 Server beta</a>"
                + "</div>"
                + "</body></html>";

        response.setHeader("Content-Type", "text/html; charset=utf-8");
        response.setStatus(StatusCode.SC_OK);
        response.setBody(new StringBody(html));
    }

    public Observable<Boolean> preparePrint(File file) {

        int fileType = Constants.FILE_TYPE_3DP;

        MachineController.WorkType workType = getModel().getMachineController().getWorkType();
        boolean isFileTypeConflict = false;
        switch (workType) {
            case FDM:
                isFileTypeConflict = fileType != Constants.FILE_TYPE_3DP;
                break;
            case LASER:
                isFileTypeConflict = fileType != Constants.FILE_TYPE_LASER;
                break;
            case CNC:
                isFileTypeConflict = fileType != Constants.FILE_TYPE_CNC;
                break;
            default:
                isFileTypeConflict = true;
                break;

        }
        if (isFileTypeConflict) {
            Logger.w("prepare print failed, file type conflict");
            return Observable.just(false);
        }

        final CountDownLatch countDownLatch = new CountDownLatch(1);
        IFile iFile = new FabLocalFile(file);
        getModel().getRemoteController().setFile(iFile);
        getModel().getRemoteController().setFileType(fileType);
        getModel().getRemoteController().setFileName(file.getName());
        // Workspace
        getModel().getWorkspace().setPrintSource(Constants.PRINT_SOURCE_LUBAN);
        getModel().getWorkspace().setRemotePrintToWorkSpace(file);

        final IGcodeParser parser = new GcodeParser();
        parser.startParse(iFile, fileType);

        // Wait file parsing to finish
        Disposable sub = parser.getParseProgressObservable()
                .throttleLast(100, TimeUnit.MILLISECONDS)
                .distinctUntilChanged()
                .subscribe(progress -> {
                    Logger.d("progress = " + progress);
                    if (progress == 100) {
                        getModel().getRemoteController().reset();
                        getModel().getRemoteController().setTotalLines(parser.getTotalLinesCount());
                        getModel().getRemoteController().setEstimatedTime(parser.getEstimatedTime());

                        getModel().getWorkspace().setEstimatedTime(parser.getEstimatedTime());
                        getModel().getWorkspace().setFileTotalLineCount(parser.getTotalLinesCount());
                        countDownLatch.countDown();
                    }
                }, e -> {
                    Logger.w("Prepare print failed, parse file error.");
                    countDownLatch.countDown();
                });
        mDisposable.add(sub);

        try {
            countDownLatch.await();
            return Observable.just(true);
        } catch (InterruptedException e) {
            return Observable.just(false);
        }
    }

    public Observable<Boolean> startPrint() {
        SingleSubject<Boolean> singleSubject = SingleSubject.create();

        Disposable sub = getModel().getRemoteController().start()
                .subscribe(retCode -> {
                    if (retCode == 0) {
                        singleSubject.onSuccess(true);
                    } else {
                        singleSubject.onSuccess(false);
                    }
                }, e -> {
                    LogHelper.log(e);
                    singleSubject.onSuccess(false);
                });
        mDisposable.add(sub);

        return singleSubject.toObservable();
    }


    private String getIPAddress() {
        // check ip address
        String addressString = "Not Connected";
        try {
            List<NetworkInterface> interfaceList = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface networkInterface : interfaceList) {
                List<InetAddress> addresses = Collections.list(networkInterface.getInetAddresses());
                for (InetAddress address : addresses) {
                    if (!address.isLoopbackAddress()) {
                        String sAddr = address.getHostAddress();
                        boolean isIPv4 = sAddr.indexOf(':') < 0;

                        if (isIPv4) {
                            addressString = sAddr;
                        }
                    }
                }
            }
        } catch (SocketException e) {
            LogHelper.log(e);
        }
        return addressString;
    }

    private String getMacAddr() {
        return getModel().getNetworkController().getMacAddress();
    }

    public String getCurrentVersion() {
        String lastUpdateVersion = getModel().getPreferences().getLastUpdatePackageVersion();
        String curVersion;
        if (TextUtils.isEmpty(lastUpdateVersion)) {
            curVersion = lastUpdateVersion;
        } else {
            curVersion = lastUpdateVersion;
            String[] versionSplits = lastUpdateVersion.split("_");
            if (versionSplits.length > 1) {
                curVersion = versionSplits[1];
            }
        }
        return curVersion;
    }

    public boolean isMachineStatePrinting() {
        int machinePrintStatus = getModel().getMachineController().getMachineStatus().printerStatus;
        final boolean isMachineStatusPrinting = machinePrintStatus == 3 || machinePrintStatus == 4;
        final boolean isPrintJobStatePrinting = MachinePrintJobState.isStatePrinting(getModel().getPrintController().getPrintJobState().getValue());
        return isMachineStatusPrinting && isPrintJobStatePrinting;
    }
}
