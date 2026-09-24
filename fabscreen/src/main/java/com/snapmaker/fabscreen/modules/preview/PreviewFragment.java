package com.snapmaker.fabscreen.modules.preview;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.orhanobut.logger.Logger;
import com.snapmaker.fabscreen.FabScreenApplication;
import com.snapmaker.fabscreen.R;
import com.snapmaker.fabscreen.modules.common.PrintDetailPanelWidgetPresenter;
import com.snapmaker.fabscreen.router.Router;

import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.OnClick;
import fabscreen.libraries.legacy.base.BaseFragment;
import fabscreen.libraries.legacy.base.BaseViewModel;
import fabscreen.libraries.legacy.data.Constants;
import fabscreen.libraries.legacy.data.model.ModelBoundary;
import fabscreen.libraries.legacy.data.print.BatchPrintController;
import fabscreen.libraries.legacy.lib.LogHelper;
import fabscreen.libraries.legacy.lib.file.FabLocalFileManager;
import fabscreen.libraries.legacy.lib.file.FabUsbFileManager;
import fabscreen.libraries.legacy.lib.file.IFile;
import fabscreen.libraries.legacy.lib.file.IFileManager;
import fabscreen.libraries.legacy.lib.parser.GcodeParser;
import fabscreen.libraries.legacy.lib.parser.IGcodeParser;
import fabscreen.libraries.legacy.lib.parser.SnapmakerParser;
import fabscreen.libraries.legacy.view.FabAlert;
import fabscreen.libraries.legacy.view.FabConfirm;
import fabscreen.libraries.legacy.view.FabScreenDialog;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;

public class PreviewFragment extends BaseFragment {
    public static final int REQUEST_CALIBRATION_RESULT = 1;

    // preview pic
    @BindView(R.id.iv_preview_picture_icon)
    ImageView mIvPreviewPictureDefault;
    @BindView(R.id.iv_preview_picture)
    ImageView mIvPreviewPicture;

    // basic info
    @BindView(R.id.tv_preview_file_name)
    TextView mTvFilename;
    @BindView(R.id.tv_preview_bounding_box)
    TextView mTvBoundingBox;

    // detail info
    @BindView(R.id.tv_preview_detail_info)
    TextView mTvDetailInfo;
    @BindView(R.id.widget_detail_panel_3dp)
    View mViewDetailPanel3DP;
    @BindView(R.id.widget_detail_panel_3dp_dual_extruder)
    View mViewDetailPanelDualExtruder;
    @BindView(R.id.widget_detail_panel_laser)
    View mViewDetailPanelLaser;
    @BindView(R.id.widget_detail_panel_cnc)
    View mViewDetailPanelCNC;
    private PrintDetailPanelWidgetPresenter mDetailPanelWidgetPresenter;

    // buttons
    @BindView(R.id.btn_preview_start_print)
    Button mBtnStartPrint;
    @BindView(R.id.btn_preview_change_settings)
    Button mBtnChangeSettings;

    private String mFilePath;
    private boolean mIsLocal;

    private int mFileType = Constants.FILE_TYPE_UNKNOWN;

    private IFileManager mFileManager;
    private IFile mParseFile;

    private int mHeadType = Constants.HEAD_UNPLUGGED;

    private IGcodeParser mGcodeParser;
    private ModelBoundary mModelBoundary = new ModelBoundary();
    private boolean mIsRotaryAvailable = false;
    private Disposable sub;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setTitle(R.string.all_preview);

        // get head type
        mHeadType = getModel().getMachineController().getHeadType();

        // deal with arguments
        if (getArguments() == null) {
            return;
        }

        Bundle arguments = getArguments();
        mFilePath = arguments.getString("file_path");
        mIsLocal = arguments.getBoolean("is_local");

        initView();

        // Turn off laser indicator
        int indicatorMode = getModel().getPreferences().getLaserIndicatorMode();
        switch (indicatorMode) {
            case 1:
                getModel().getSlaveComputer().sendGcode("M5")
                        .observeOn(AndroidSchedulers.mainThread())
                        .as(bindToLifecycle())
                        .subscribe();
            case 0:
            default:
                getModel().getSlaveComputer().setCrossLineLaserIndicator(false)
                        .observeOn(AndroidSchedulers.mainThread())
                        .as(bindToLifecycle())
                        .subscribe();
        }

        mDetailPanelWidgetPresenter = new PrintDetailPanelWidgetPresenter();
        mDetailPanelWidgetPresenter.bind(view);

        // if file is not match with toolHead
        if (!fileMatchesHead(mFileType, mHeadType) && mFileType != Constants.FILE_TYPE_UNKNOWN) {
            FabAlert.alert(getContext(), R.string.preview_alert_file_type_not_match);
            AndroidSchedulers.mainThread().scheduleDirect(this::back, 2000, Constants.TIME_UNIT);
        } else {
            initFile();
        }

        mIsRotaryAvailable = getModel().getMachineController().isRotaryModuleAvailable();

        Logger.d("preview fragment view created...");
    }

    private boolean fileMatchesHead(int fileType, int headType) {
        switch (fileType) {
            case Constants.FILE_TYPE_3DP:
                return headType == Constants.HEAD_3DP || headType == Constants.HEAD_3DP_DUAL_EXTRUDER;
            case Constants.FILE_TYPE_LASER:
                return headType == Constants.HEAD_LASER
                        || headType == Constants.HEAD_LASER_2W_IR
                        || headType == Constants.HEAD_LASER_10W
                        || headType == Constants.HEAD_LASER_20W
                        || headType == Constants.HEAD_LASER_40W;
            case Constants.FILE_TYPE_CNC:
                return headType == Constants.HEAD_CNC || headType == Constants.HEAD_CNC_200W;
            case Constants.FILE_TYPE_LOG:
            case Constants.FILE_TYPE_UPDATE:
            case Constants.FILE_TYPE_UNKNOWN:
            default:
                return false;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        updateView();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mGcodeParser.destroy();
        if (mFileManager != null) {
            if (mFileManager instanceof FabUsbFileManager) {
                mFileManager = null;
            } else {
                mFileManager.close();
            }
        }
    }

    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_preview;
    }

    @Override
    protected BaseViewModel getViewModel() {
        return null;
    }

    private void initView() {
        int index = mFilePath.lastIndexOf('/');
        String filename = mFilePath.substring(index + 1);

        index = filename.lastIndexOf('.');
        String extension = filename.substring(index + 1).toLowerCase();

        // basic
        mTvFilename.setSelected(true);
        mTvFilename.setText(filename);
        mTvBoundingBox.setVisibility(View.INVISIBLE);

        // detail
        switch (extension) {
            case "gcode":
                mFileType = Constants.FILE_TYPE_3DP;
                mIvPreviewPictureDefault.setImageResource(R.drawable.ic_file_preview_3d_120x120);
                mViewDetailPanel3DP.setVisibility(View.INVISIBLE);
                mViewDetailPanelDualExtruder.setVisibility(View.INVISIBLE);
                mViewDetailPanelLaser.setVisibility(View.GONE);
                mViewDetailPanelCNC.setVisibility(View.GONE);
                break;
            case "nc":
                mFileType = Constants.FILE_TYPE_LASER;
                mIvPreviewPictureDefault.setImageResource(R.drawable.ic_file_preview_laser_120x120);
                mViewDetailPanel3DP.setVisibility(View.GONE);
                mViewDetailPanelDualExtruder.setVisibility(View.GONE);
                mViewDetailPanelLaser.setVisibility(View.INVISIBLE);
                mViewDetailPanelCNC.setVisibility(View.GONE);
                break;
            case "cnc":
                mFileType = Constants.FILE_TYPE_CNC;
                mIvPreviewPictureDefault.setImageResource(R.drawable.ic_file_preview_cnc_120x120);
                mViewDetailPanel3DP.setVisibility(View.GONE);
                mViewDetailPanelDualExtruder.setVisibility(View.GONE);
                mViewDetailPanelLaser.setVisibility(View.GONE);
                mViewDetailPanelCNC.setVisibility(View.INVISIBLE);
                break;
            case "sm":
            default:
                mFileType = Constants.FILE_TYPE_UNKNOWN;
                mViewDetailPanel3DP.setVisibility(View.GONE);
                mViewDetailPanelDualExtruder.setVisibility(View.GONE);
                mViewDetailPanelLaser.setVisibility(View.GONE);
                mViewDetailPanelCNC.setVisibility(View.GONE);
                break;
        }

        // buttons
        mBtnStartPrint.setEnabled(false);
        mBtnStartPrint.setText((mHeadType == Constants.HEAD_3DP || mHeadType == Constants.HEAD_3DP_DUAL_EXTRUDER) ? R.string.all_start : R.string.all_ready);

        mBtnChangeSettings.setEnabled(false);
    }

    private void initFile() {
        // get file
        if (mIsLocal) {
            mFileManager = new FabLocalFileManager(getContext(), getModel().getFilesDir().getPath());
        } else {
            mFileManager = FabScreenApplication.getInstance().getFabUsbFileManager();
            sub = mFileManager.getFileManagerStateObservable()
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(isAttach -> {
                        if (!isAttach) {
                            back();
                        }
                    }, LogHelper::log);
            disposables.add(sub);
        }
        if (mFileType == Constants.FILE_TYPE_UNKNOWN) {
            mGcodeParser = new SnapmakerParser();
        } else {
            mGcodeParser = new GcodeParser();
        }

        mFileManager.search(mFilePath)
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(file -> {
                    mParseFile = file;
                    getModel().getPrintController().reset();

                    mGcodeParser.startParse(file, mFileType);
                }, e -> {
                    LogHelper.log(e);
                    FabAlert.alert(getContext(), R.string.preview_alert_failed_to_open_file);
                });

        // TODO: watch changes on show parameters
        // Listen to parsing progress
        mGcodeParser.getParseProgressObservable()
                .throttleLast(100, TimeUnit.MILLISECONDS)
                .distinctUntilChanged()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(progress -> {
                    // Return -1 when parsing file failed.
                    if (progress == -1) {
                        FabAlert.alert(getContext(), R.string.preview_alert_failed_to_parse_file);
                        Logger.w("Failed to parse file.");
                        AndroidSchedulers.mainThread().scheduleDirect(this::back, 3000, TimeUnit.MILLISECONDS);
                        return;
                    }

                    if (progress != 100) {
                        mTvDetailInfo.setText(getString(R.string.preview_parsing_file_desc, progress));
                        mViewDetailPanel3DP.setVisibility(View.INVISIBLE);
                        mViewDetailPanelDualExtruder.setVisibility(View.INVISIBLE);
                    } else {
                        mTvBoundingBox.setVisibility(View.VISIBLE);
                        mTvDetailInfo.setVisibility(View.INVISIBLE);

                        // update fileType
                        mFileType = mGcodeParser.getFileType();

                        if (mFileType == Constants.FILE_TYPE_3DP) {
                            if (mHeadType == Constants.HEAD_3DP) {
                                mViewDetailPanel3DP.setVisibility(View.VISIBLE);
                            } else {
                                mViewDetailPanelDualExtruder.setVisibility(View.VISIBLE);
                            }

                        } else if (mFileType == Constants.FILE_TYPE_LASER) {
                            mViewDetailPanelLaser.setVisibility(View.VISIBLE);
                        } else if (mFileType == Constants.FILE_TYPE_CNC) {
                            mViewDetailPanelCNC.setVisibility(View.VISIBLE);
                        }

                        // check again when sm file is parsed.
                        if (!fileMatchesHead(mFileType, mHeadType)) {
                            FabAlert.alert(getContext(), R.string.preview_alert_file_type_not_match);
                            AndroidSchedulers.mainThread().scheduleDirect(this::back, 2000, Constants.TIME_UNIT);
                        }

                        mBtnStartPrint.setEnabled(true);
                        mBtnChangeSettings.setEnabled(true);

                        handleParsedResult();
                        updateView();
                    }
                }, e -> {
                    LogHelper.log(e);
                    FabAlert.alert(getContext(), R.string.preview_alert_failed_to_parse_file);
                });
    }

    private void updateView() {
        switch (mFileType) {
            case Constants.FILE_TYPE_3DP: {
                double nozzleTemperature = getModel().getPrintController().getOverrideInitialNozzleTemperature();
                double heatedBedTemperature = getModel().getPrintController().getOverrideInitialHeatedBedTemperature();
                double workSpeed = getModel().getPrintController().getOverrideFeedRate();

                mTvBoundingBox.setText(getString(R.string.preview_bounding_box_format_x_y_z,
                        mModelBoundary.getMaxX() - mModelBoundary.getMinX(),
                        mModelBoundary.getMaxY() - mModelBoundary.getMinY(),
                        mModelBoundary.getMaxZ() - mModelBoundary.getMinZ()));

                if (mHeadType == Constants.HEAD_3DP) {
                    mDetailPanelWidgetPresenter.setNozzleTemp(nozzleTemperature);
                    mDetailPanelWidgetPresenter.setHeatedBedTemp(heatedBedTemperature);
                    mDetailPanelWidgetPresenter.setWorkSpeedPercentage(workSpeed);
                    mDetailPanelWidgetPresenter.setEstimatedTime(mGcodeParser.getEstimatedTime());
                } else if (mHeadType == Constants.HEAD_3DP_DUAL_EXTRUDER) {
                    mDetailPanelWidgetPresenter.setNozzleTemp(0, getModel().getPrintController().getOverrideInitialNozzleTemperature(0));
                    mDetailPanelWidgetPresenter.setNozzleTemp(1, getModel().getPrintController().getOverrideInitialNozzleTemperature(1));
                    mDetailPanelWidgetPresenter.setHeatedBedTemp(heatedBedTemperature);
                    mDetailPanelWidgetPresenter.setWorkSpeedPercentage(workSpeed);
                    mDetailPanelWidgetPresenter.setEstimatedTime(mGcodeParser.getEstimatedTime());
                }

                Logger.d("Update result: nozzle = %.1f, heated bead = %.1f, work speed = %.1f, estimated time =%fs",
                        nozzleTemperature,
                        heatedBedTemperature,
                        workSpeed,
                        mGcodeParser.getEstimatedTime());
                break;
            }
            case Constants.FILE_TYPE_LASER: {
                final double laserPower = getModel().getPrintController().getOverrideLaserPower();
                final double workSpeed = getModel().getPrintController().getOverrideFeedRate();

                if (mIsRotaryAvailable) {
                    mTvBoundingBox.setText(getString(R.string.preview_bounding_box_format_d_l,
                            mGcodeParser.getDiameter(),
                            mModelBoundary.getMaxY() - mModelBoundary.getMinY()));
                } else {
                    mTvBoundingBox.setText(getString(R.string.preview_bounding_box_format_x_y,
                            mModelBoundary.getMaxX() - mModelBoundary.getMinX(),
                            mModelBoundary.getMaxY() - mModelBoundary.getMinY()));
                }

                mDetailPanelWidgetPresenter.setLaserPower(laserPower);
                mDetailPanelWidgetPresenter.setWorkSpeedPercentage(workSpeed);
                mDetailPanelWidgetPresenter.setEstimatedTime(mGcodeParser.getEstimatedTime());// FIXME: 2021/9/7 NPE?
                Logger.d("Update result: laser power = %.1f, work speed = %.1f, estimated time = %fs",
                        laserPower,
                        workSpeed,
                        mGcodeParser.getEstimatedTime());
                break;
            }
            case Constants.FILE_TYPE_CNC: {
                final double workSpeed = getModel().getPrintController().getOverrideFeedRate();

                if (mIsRotaryAvailable) {
                    mTvBoundingBox.setText(getString(R.string.preview_bounding_box_format_d_l,
                            mGcodeParser.getDiameter(),
                            mModelBoundary.getMaxY() - mModelBoundary.getMinY()));
                } else {
                    mTvBoundingBox.setText(getString(R.string.preview_bounding_box_format_x_y,
                            mModelBoundary.getMaxX() - mModelBoundary.getMinX(),
                            mModelBoundary.getMaxY() - mModelBoundary.getMinY()));
                }

                final int cncSpindleMaxSpeed = getModel().getMachineController().getHeadType() == Constants.HEAD_CNC ?  12000 : 18000;
                mDetailPanelWidgetPresenter.setSpindleSpeed(cncSpindleMaxSpeed); // fixed
                mDetailPanelWidgetPresenter.setWorkSpeedPercentage(workSpeed);
                mDetailPanelWidgetPresenter.setEstimatedTime(mGcodeParser.getEstimatedTime());
                Logger.d("Update result: work speed = %.1f, estimated time = %fs",
                        workSpeed,
                        mGcodeParser.getEstimatedTime());
                break;
            }
        }
    }

    private void handleParsedResult() {
        mModelBoundary = mGcodeParser.getBoundary();
        getModel().getPrintController().setModelBoundary(mModelBoundary);
        // FIXME: what if G-code needs to run x y b, what dimensions of boundary should touchscreen executed?
        if (mIsRotaryAvailable) {
            mModelBoundary.setDimension(ModelBoundary.DIMENSION_BY);
        } else {
            mModelBoundary.setDimension(ModelBoundary.DIMENSION_XY);
        }
        Logger.d(mModelBoundary.toString());

        // show gcode thumbnail
        Bitmap thumbnail = mGcodeParser.getGcodeThumbnail();
        if (thumbnail != null) {
            mIvPreviewPicture.setImageBitmap(thumbnail);
            mIvPreviewPictureDefault.setVisibility(ImageView.GONE);
            mIvPreviewPicture.setVisibility(ImageView.VISIBLE);
        }

        switch (mFileType) {
            case Constants.FILE_TYPE_3DP: {
                final float heatedBedTemperature = mGcodeParser.getBedTargetTemperature();
                getModel().getPrintController().setOverrideInitialHeatedBedTemperature(heatedBedTemperature);

                final float nozzleTemperature = mGcodeParser.getNozzleTargetTemperature();
                getModel().getPrintController().setOverrideInitialNozzleTemperature(nozzleTemperature);
                if (mHeadType == Constants.HEAD_3DP_DUAL_EXTRUDER) {
                    final float nozzle1Temperature = mGcodeParser.getNozzle1TargetTemperature();
                    getModel().getPrintController().setOverrideInitialNozzleTemperature(0, nozzleTemperature);
                    getModel().getPrintController().setOverrideInitialNozzleTemperature(1, nozzle1Temperature);
                }

                getModel().getPrintController().setOverrideFeedRate(100);
                break;
            }
            case Constants.FILE_TYPE_LASER: {
                final float laserPower = mGcodeParser.getLaserPower();
                if (laserPower == 0) {
                    getModel().getPrintController().setOverrideLaserPower(100);
                } else {
                    getModel().getPrintController().setOverrideLaserPower(laserPower);
                }
                getModel().getPrintController().setOverrideFeedRate(100);
                break;
            }
            case Constants.FILE_TYPE_CNC: {
                getModel().getPrintController().setOverrideFeedRate(100);
                break;
            }
            default:
                break;
        }
    }

    private AlertDialog showLoadingWorkspaceDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext(), R.style.AppTheme_Dialog);
        AlertDialog dialog = builder.create();
        dialog.setCanceledOnTouchOutside(false);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(R.color.default_theme_dark_mask);
            dialog.getWindow().setLayout(280 * 2, WindowManager.LayoutParams.WRAP_CONTENT);
        }

        LayoutInflater inflater = LayoutInflater.from(getContext());
        View view = inflater.inflate(R.layout.dialog_preview_copy_to_workspace_loading, null);
        dialog.setView(view);
        dialog.show();
        return dialog;
    }

    @OnClick(R.id.btn_preview_start_print)
    void onStartPrint() {
        if ((mHeadType != Constants.HEAD_3DP && mHeadType != Constants.HEAD_3DP_DUAL_EXTRUDER)) {
            realStartPrint();
            return;
        }
        if (getModel().getPrintController() instanceof BatchPrintController) {
            // Check if ever calibrated for 3dp
            getModel().getSlaveComputer().checkCalibrationEverSucceeded()
                    .observeOn(AndroidSchedulers.mainThread())
                    .as(bindToLifecycle())
                    .subscribe(calibrated -> {
                        if (calibrated) {
                            if (mHeadType == Constants.HEAD_3DP_DUAL_EXTRUDER) {
                                checkDualExtruderExtractionLimit();
                            } else {
                                realStartPrint();
                            }
                        } else {
                            warnNotCalibrated();
                        }
                    }, LogHelper::log);
        } else {
            realStartPrint();
        }
    }

    private void warnNotCalibrated() {
        FabConfirm.create(getContext())
                .setIcon(R.drawable.pic_dialog_warning_72x72)
                .setDescription(R.string.preview_warning_not_calibrated)
                .setCanceledOnTouchOutSide(false)
                .setConfirm(R.string.all_calibrate, (dialog, which) -> {
                    dialog.dismiss();
                    // Calibrating and wait for a result.
                    if (mHeadType == Constants.HEAD_3DP) {
                        Router.getInstance()
                                .routeToCalibrationPage(false)
                                .startForResult(this, REQUEST_CALIBRATION_RESULT);
                    } else if (mHeadType == Constants.HEAD_3DP_DUAL_EXTRUDER) {
                        Router.getInstance().routeToDualExtruderCalibration().startForResult(this, REQUEST_CALIBRATION_RESULT);
                    }
                })
                .setCancel(R.string.all_continue, (dialog, which) -> {
                    dialog.dismiss();
                    // Ignore calibration absent.
                    realStartPrint();
                })
                .show();
    }

    private void checkDualExtruderExtractionLimit() {
        final float extruder0Retraction = mGcodeParser.getExtruder0RetractionDistance();
        final float extruder1Retraction = mGcodeParser.getExtruder1RetractionDistance();

        boolean isRetractionOverLimit = extruder0Retraction > 2f || extruder1Retraction > 2f;
        if (isRetractionOverLimit) {
            Logger.d("Retraction exceed detected.");
            showRetractionLimitDialog();
        } else {
            realStartPrint();
        }
    }

    private void showRetractionLimitDialog() {
        FabConfirm.create(requireContext()).setIcon(R.drawable.pic_dialog_warning_72x72)
                .setDescription(R.string.dialog_dual_extruder_gcode_retraction_exceed_warning)
                .setConfirm(R.string.all_continue, (dialog, which) -> {
                    dialog.dismiss();
                    Logger.i("Continue to print.");
                    realStartPrint();
                })
                .setCancel(R.string.all_cancel, (dialog, which) -> {
                    dialog.dismiss();
                })
                .show();
    }

    private void realStartPrint() {
        // Save information of the file we gonna print
        getModel().getWorkspace().setPrintSource(Constants.PRINT_SOURCE_SCREEN);
        getModel().getWorkspace().setEstimatedTime(mGcodeParser.getEstimatedTime());
        getModel().getWorkspace().setFileTotalLineCount(mGcodeParser.getTotalLinesCount());

        // Start copying file into workspace, dismiss dialog when finished.
        getModel().getWorkspace().checkAvailableSpace(mParseFile)
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(result -> {
                    switch (result) {
                        case 0:
                            addFileIntoWorkspace(mParseFile);
                            break;
                        case 1:
                            // available space less than 300MB
                            FabScreenDialog.create(requireContext())
                                    .setIcon(R.drawable.pic_dialog_warning_72x72)
                                    .setTitle(R.string.dialog_add_file_into_workspace_low_system_storage_title)
                                    .setDescription(R.string.dialog_add_file_into_workspace_low_system_storage_desc)
                                    .setCancel(R.string.all_close, (dialog, which) -> dialog.dismiss())
                                    .setConfirm(R.string.all_continue, (dialog, which) -> {
                                        dialog.dismiss();
                                        addFileIntoWorkspace(mParseFile);
                                    })
                                    .show();
                            break;
                        case 2:
                            // not enough available space
                            FabScreenDialog.create(requireContext())
                                    .setIcon(R.drawable.pic_dialog_failed_72x72)
                                    .setTitle(R.string.dialog_add_file_into_workspace_insufficient_system_storage_title)
                                    .setDescription(R.string.dialog_add_file_into_workspace_insufficient_system_storage_desc)
                                    .setConfirm(R.string.all_confirm, (dialog, which) -> dialog.dismiss())
                                    .show();
                            break;
                        case -1:
                        default:
                            Logger.e("File not exist.");
                            break;
                    }
                });
    }

    private void addFileIntoWorkspace(IFile targetFile) {
        PreviewActivity activity = (PreviewActivity) getActivity();
        if (activity == null) return;
        if (sub != null && !sub.isDisposed()) {
            sub.dispose();
            sub = null;
        }

        // Show loading dialog while copying.
        AlertDialog dialog = showLoadingWorkspaceDialog();

        getModel().getWorkspace().addFileToWorkspace(mParseFile)
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(success -> {
                    if (success) {
                        dialog.dismiss();
                        switch (mHeadType) {
                            case Constants.HEAD_3DP:
                            case Constants.HEAD_3DP_DUAL_EXTRUDER: {
                                boolean forceRefresh = requireActivity().getIntent().getBooleanExtra("force_refresh", false);
                                if (forceRefresh) {
                                    Logger.d("Force refreshing print...");
                                    Router.getInstance().routeToPrintPage(true).start(getContext(), Intent.FLAG_ACTIVITY_NEW_TASK);
                                    requireActivity().finish();
                                } else {
                                    Router.getInstance()
                                            .routeToPrintPage()
                                            .start(getContext());
                                    requireActivity().finish();
                                }
                                break;
                            }
                            case Constants.HEAD_LASER:
                            case Constants.HEAD_LASER_10W: {
                                if (getModel().getMachineController().isRotaryModuleAvailable()) {
//                                    activity.gotoLaserRotarySetupModeFragment();
                                    activity.gotoLaserRotarySetMaterial();
                                } else {
                                    activity.gotoLaserPrepareModeFragment();
                                }
                                break;
                            }
                            case Constants.HEAD_LASER_20W:
                            case Constants.HEAD_LASER_40W: {
                                if (getModel().getMachineController().isRotaryModuleAvailable()) {
                                    activity.gotoLaserRotarySetMaterial();
                                } else {
                                    activity.gotoLaser40wPrepareModeFragment();
                                }
                                break;
                            }
                            case Constants.HEAD_LASER_2W_IR:
                                if (getModel().getMachineController().isRotaryModuleAvailable()) {
                                    activity.gotoLaserRotarySetMaterial();
                                } else {
                                    activity.gotoLaser2wPrepareModeFragment();
                                }
                                break;
                            case Constants.HEAD_CNC:
                            case Constants.HEAD_CNC_200W: {
                                if (getModel().getMachineController().isRotaryModuleAvailable()) {
                                    activity.gotoCNCPrepareRotaryOriginRemindFragment();
                                } else {
                                    activity.gotoCNCPrepareSafetyGogglesFragment();
                                }
                                break;
                            }
                        }
                    }
                }, e -> {
                    LogHelper.log(e);
                    dialog.dismiss();
                });
    }

    @OnClick(R.id.btn_preview_change_settings)
    void onClickChangeSettings() {
        Router.getInstance().routeToPrintSettingsPage().start(getContext());
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CALIBRATION_RESULT && resultCode == Activity.RESULT_OK) {
            realStartPrint();
        }
    }
}
