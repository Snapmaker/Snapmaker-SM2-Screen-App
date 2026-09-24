package fabscreen.libraries.legacy.data.print;

@Deprecated
public interface PrintListener {
    void onStartSuccess();

    void onStartFailed(int retCode);

    void onPauseSuccess();

    void onPauseFailed(int retCode);

    void onResumeSuccess();

    void onResumeFailed(int retCode);

    void onResumeFromPowerOutageSuccess();

    void onResumeFromPowerOutageFailed(int retCode);

    void onStopSuccess();

    void onStopFailed(int retCode);

    void onFinishSuccess();

    void onFinishFailed(int retCode);

}