package com.snapmaker.fabscreen.modules.browse;

import android.content.Context;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.orhanobut.logger.Logger;
import com.snapmaker.fabscreen.R;
import com.snapmaker.fabscreen.router.Router;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import fabscreen.libraries.legacy.data.Constants;
import fabscreen.libraries.legacy.lib.LogHelper;
import fabscreen.libraries.legacy.lib.file.IFile;
import fabscreen.libraries.legacy.view.FabAlert;
import fabscreen.libraries.legacy.view.FabConfirm;
import fabscreen.libraries.legacy.view.FabInputDialog;
import fabscreen.libraries.legacy.view.SwipeListView;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;

public class BrowseListAdapter extends BaseAdapter {
    private final CompositeDisposable disposables = new CompositeDisposable();
    private Disposable subscribe;

    private Context mContext;
    private List<IFile> mFiles;
    private BrowseViewModel mViewModel;
    private SparseBooleanArray mIsSelected;
    private boolean mIsMultiSelected = false;
    private FabConfirm fabConfirm;
    private FabInputDialog fabInputDialog;

    BrowseListAdapter(Context context, BrowseViewModel viewModel) {
        this(context, new ArrayList<>(), viewModel);
    }

    private BrowseListAdapter(Context context, List<IFile> files, BrowseViewModel viewModel) {
        mContext = context;
        mFiles = files;
        mViewModel = viewModel;
        mIsSelected = new SparseBooleanArray();
    }

    private Context getContext() {
        return mContext;
    }

    void dispose() {
        disposables.dispose();
    }

    @Override
    public int getCount() {
        return mFiles.size();
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @NonNull
    @Override
    public IFile getItem(int i) {
        return mFiles.get(i);
    }

    void setItems(List<IFile> files) {
        mFiles = files;
        initMultiSelectedList();
    }

    private void initMultiSelectedList() {
        for (int i = 0; i < mFiles.size(); i++) {
            mIsSelected.put(i, false);
        }
    }

    void setMultiSelected(boolean isMultiSelected) {
        if (mIsMultiSelected == isMultiSelected) {
            return;
        }

        mIsMultiSelected = isMultiSelected;
        if (isMultiSelected) {
            initMultiSelectedList();
            mViewModel.clearSelectedCount();
        }

        notifyDataSetChanged();
    }

    private static int getFileHeadType(IFile file) {
        String suffix = file.getName()
                .substring(file.getName().lastIndexOf(".") + 1)
                .toLowerCase();
        switch (suffix) {
            case "gcode":
                return Constants.FILE_TYPE_3DP;
            case "nc":
                return Constants.FILE_TYPE_LASER;
            case "cnc":
                return Constants.FILE_TYPE_CNC;
            case "bin":
                return Constants.FILE_TYPE_UPDATE;
            default:
                return Constants.FILE_TYPE_UNKNOWN;
        }
    }

    private static String getFileDescription(IFile file) {
        String unit = "bytes";
        long fileLength = -1;
        if (!file.isDirectory()) {
            fileLength = file.length();
            if (fileLength > 1024) {
                fileLength /= 1024;
                unit = "KB";
                if (fileLength > 1024) {
                    fileLength /= 1024;
                    unit = "MB";
                }
            }
        }

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd  hh:mm", Locale.getDefault());
        String lastModified = sdf.format(new Date(file.lastModified()));

        if (fileLength == -1) {
            return String.format(Locale.getDefault(), "%s", lastModified);
        } else {
            return String.format(Locale.getDefault(), "%d %s   %s", fileLength, unit, lastModified);
        }
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(mContext).inflate(R.layout.item_file, parent, false);
        }

        SwipeListView listView = (SwipeListView) parent;

        // view binding
        ImageView ivFileIcon = convertView.findViewById(R.id.iv_file_item_icon);
        Button btnItem = convertView.findViewById(R.id.btn_file_item);
        Button btnSwipeRename = convertView.findViewById(R.id.btn_file_item_rename);
        Button btnSwipeDelete = convertView.findViewById(R.id.btn_file_item_delete);
        TextView tvFileName = convertView.findViewById(R.id.tv_file_item_name);
        TextView tvFileDesc = convertView.findViewById(R.id.tv_file_item_desc);
        ImageView ivDirArrow = convertView.findViewById(R.id.iv_file_item_arrow);
        CheckBox cbFileSelected = convertView.findViewById(R.id.cb_file_item_selected);

        // get file
        IFile file = getItem(position);
        final boolean isDirectory = file.isDirectory();
        final int fileType = getFileHeadType(file);

        // whole item button
        btnItem.setEnabled(!mIsMultiSelected);
        btnItem.setOnClickListener(v -> {
            Logger.i(String.format("Click file directory %s path %s", file.isDirectory(), file.getPath()));
            if (isDirectory) {
                disposables.add(
                        mViewModel.gotoDirectory(file).subscribe(files -> { /**/ }, LogHelper::log)
                );
            } else {
                // Click on update file will lead to an firmware update
                switch (fileType) {
                    case Constants.FILE_TYPE_3DP:
                    case Constants.FILE_TYPE_LASER:
                    case Constants.FILE_TYPE_CNC:
                        Router.getInstance()
                                .routeToPreviewPage(file.isLocal(), file.getPath())
                                .start(mContext);
                        break;
                    case Constants.FILE_TYPE_UPDATE:
                        FabConfirm.create(mContext)
                                .setDescription(R.string.dialog_browse_file_update_confirm_desc)
                                .setConfirm(R.string.all_confirm, (dialog, which) -> {
                                    dialog.dismiss();
                                    BrowseActivity activity = (BrowseActivity) mContext;
                                    activity.gotoUpdateFragment(file.getPath(), file.isLocal());
                                })
                                .setCancel(R.string.all_cancel, (dialog, which) -> {
                                    dialog.dismiss();
                                })
                                .show();

                        break;
                    case Constants.FILE_TYPE_UNKNOWN:
                        FabAlert.alert(getContext(), R.string.browse_alert_file_not_support);
                        break;
                    default:
                        break;
                }
            }
        });

        // set fileTypeImage, Arrow checkBox visibility
        if (isDirectory) {
            ivFileIcon.setImageResource(R.drawable.ic_file_type_folder_32x32);
        } else {
            switch (fileType) {
                case Constants.FILE_TYPE_3DP:
                    ivFileIcon.setImageResource(R.drawable.ic_file_type_3dp_32x32);
                    break;
                case Constants.FILE_TYPE_LASER:
                    ivFileIcon.setImageResource(R.drawable.ic_file_type_laser_32x32);
                    break;
                case Constants.FILE_TYPE_CNC:
                    ivFileIcon.setImageResource(R.drawable.ic_file_type_cnc_32x32);
                    break;
                case Constants.FILE_TYPE_UPDATE:
                    ivFileIcon.setImageResource(R.drawable.ic_file_type_update_32x32);
                    break;
                default:
                    ivFileIcon.setImageResource(R.drawable.ic_file_type_unknown_32x32);
                    break;
            }
        }
        ivDirArrow.setVisibility(!mIsMultiSelected && isDirectory ? View.VISIBLE : View.INVISIBLE);

        // set file name
        tvFileName.setText(file.getName());

        // format file size and last modified time
        tvFileDesc.setText(getFileDescription(file));

        // on checkbox selected
        cbFileSelected.setVisibility(mIsMultiSelected ? View.VISIBLE : View.INVISIBLE);
        cbFileSelected.setChecked(mIsSelected.get(position));
        cbFileSelected.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (buttonView.isPressed()) {
                mIsSelected.put(position, isChecked);
                if (isChecked) {
                    mViewModel.addSelectedFile(file);
                    Logger.d("Select file " + position);
                } else {
                    mViewModel.removeSelectedFile(file);
                    Logger.d("Deselect file " + position);
                }
            }
        });

        btnSwipeRename.setOnClickListener((v) -> {
            listView.closeAllItems();

            Logger.d("Try to rename file " + file.getName());

            int index = file.getName().lastIndexOf(".");
            String sourceName = file.isDirectory() ? file.getName() : file.getName().substring(0, index);
            String extension = file.getName().substring(index + 1).toLowerCase();
            if (fabInputDialog != null && fabInputDialog.isShowing()) {
                fabInputDialog.dismiss();
            }
            fabInputDialog = FabInputDialog.create(mContext)
                    .setTitle(R.string.browse_rename_dialog_title)
                    .setEditText(sourceName)
                    .setButton(mContext.getString(R.string.all_confirm), ((dialog, which) -> {
                        String targetFilename = FabInputDialog.getsInstance().getEditTextContent().trim();
                        if (targetFilename.equals(sourceName) || targetFilename.isEmpty()) {
                            // todo
                            return;
                        }
                        disposables.add(
                                mViewModel.renameFile(file, file.isDirectory() ? targetFilename : targetFilename + "." + extension)
                                        .subscribe(success -> {
                                            Logger.d("Rename file %s.", success ? "succeed" : "failed");
                                            if (!success) {
                                                FabAlert.alert(mContext, mContext.getString(R.string.browse_alert_rename_failed, file.getName()));
                                            }
                                        }, e -> {
                                            LogHelper.log(e);
                                            FabAlert.alert(mContext, mContext.getString(R.string.browse_alert_rename_failed, file.getName()));
                                        })
                        );
                    }));
            if (!file.isLocal()) {
                if (subscribe != null && !subscribe.isDisposed()) {
                    subscribe.dispose();
                }
                subscribe = mViewModel.subscribeFileManagerState().observeOn(AndroidSchedulers.mainThread())
                        .subscribe(isAttach -> {
                                    if (!isAttach) {
                                        fabInputDialog.dismiss();
                                    }
                                }
                        );
            }
            fabInputDialog.show();

        });

        btnSwipeDelete.setOnClickListener((v) -> {
            listView.closeAllItems();

            Logger.d("Try to delete file " + file.getName());
            if (fabConfirm != null && fabConfirm.isShowing()) {
                fabConfirm.dismiss();
            }
            fabConfirm = FabConfirm.create(getContext())
                    .setDescription(R.string.browse_delete_item_desc)
                    .setCancel(R.string.all_cancel, (dialog, i) -> dialog.dismiss())
                    .setConfirm(R.string.all_delete, (dialog, i) -> {
                        disposables.add(
                                mViewModel.removeFile(file)
                                        .subscribe(success -> {
                                            Logger.d("Delete file %s.", success ? "succeed" : "failed");
                                            if (!success) {
                                                FabAlert.alert(mContext, mContext.getString(R.string.browse_alert_delete_failed, file.getName()));
                                            }
                                        }, e -> {
                                            LogHelper.log(e);
                                            FabAlert.alert(mContext, mContext.getString(R.string.browse_alert_delete_failed, file.getName()));
                                        })
                        );
                        dialog.dismiss();
                    });
            fabConfirm.show();
            if (!file.isLocal()) {
                if (subscribe != null && !subscribe.isDisposed()) {
                    subscribe.dispose();
                }
                subscribe = mViewModel.subscribeFileManagerState().observeOn(AndroidSchedulers.mainThread())
                        .subscribe(isAttach -> {
                                    if (!isAttach) {
                                        if (fabConfirm != null && fabConfirm.isShowing()) {
                                            fabConfirm.dismiss();
                                        }
                                    }
                                }
                        );
            }
        });

        return convertView;
    }
}
