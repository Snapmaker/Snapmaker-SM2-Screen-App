package com.snapmaker.fabscreen.modules.experiment;

import android.content.Context;
import android.os.Bundle;
import android.os.SystemClock;
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
import com.snapmaker.fabscreen.FabScreenApplication;
import com.snapmaker.fabscreen.R;
import fabscreen.libraries.legacy.base.BaseFragment;
import fabscreen.libraries.legacy.base.BaseViewModel;
import fabscreen.libraries.legacy.view.SwipeListView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import butterknife.BindView;
import fabscreen.libraries.legacy.data.Constants;
import fabscreen.libraries.legacy.lib.LogHelper;
import fabscreen.libraries.legacy.lib.file.IFile;
import fabscreen.libraries.legacy.lib.file.IFileManager;
import io.reactivex.Scheduler;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.BehaviorSubject;
import okio.BufferedSink;
import okio.BufferedSource;
import okio.Okio;

public class ExperimentCopyTestFragment extends BaseFragment {
    private IFileManager mRemoveFileManager;
    private BehaviorSubject<ArrayList<IFile>> mFilesSubject = BehaviorSubject.create();
    private BehaviorSubject<IFile> mFileSubject = BehaviorSubject.create();
    private BehaviorSubject<String> mResultSubject = BehaviorSubject.createDefault("Ready");

    private CopyTestAdapter mRemoteFileListAdapter;
    private Scheduler.Worker mCopyWorker;

    @BindView(R.id.lv_browse_remote_files)
    SwipeListView mLvRemoteFiles;
    @BindView(R.id.view_experiment_remote_files_empty)
    View mViewRemoteFilesEmpty;
    @BindView(R.id.tv_copy_test_desc)
    TextView mTvDesc;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initRemoteListView();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mRemoveFileManager != null) {
            mRemoveFileManager = null;
        }
        if (mCopyWorker != null) {
            mCopyWorker.dispose();
            mCopyWorker = null;
        }
    }

    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_experiment_copy_test;
    }

    @Override
    protected BaseViewModel getViewModel() {
        return null;
    }

    private void initRemoteListView() {
        // init remote files
        mRemoveFileManager = FabScreenApplication.getInstance().getFabUsbFileManager();
        mRemoveFileManager.mount()
                .doOnNext(success -> {
                    if (!success) {
                        mLvRemoteFiles.setVisibility(View.GONE);
                    }
                })
                .filter(success -> success)
                .flatMap(success -> mRemoveFileManager.listFiles())
                .as(bindToLifecycle())
                .subscribe(files -> {
                    ArrayList<IFile> files2 = handleFiles(files);
                    mFilesSubject.onNext(files2);
                }, LogHelper::log);

        mFilesSubject
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(files -> {
                    mRemoteFileListAdapter.setItem(files);
                    mRemoteFileListAdapter.notifyDataSetChanged();
                });

        mRemoteFileListAdapter = new CopyTestAdapter(getContext(), new ArrayList<IFile>());
        mRemoteFileListAdapter.setListener(file -> {
            if (!file.isDirectory()) {
                if (mCopyWorker == null) {
                    mCopyWorker = Schedulers.io().createWorker();
                }
                mFileSubject.onNext(file);
                mCopyWorker.schedule(this::copyFileToWorkspace);

            }
        });
        mLvRemoteFiles.setAdapter(mRemoteFileListAdapter);
        mLvRemoteFiles.setEmptyView(mViewRemoteFilesEmpty);

        mResultSubject
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(result -> mTvDesc.setText(result));
    }

    private void copyFileToWorkspace() {
        IFile file = mFileSubject.getValue();
        long startTime = SystemClock.elapsedRealtime();
        String filePath = getModel().getFilesDir().getPath();
        long fileLength = file.length();

        File newFile = new File(filePath, "testCopy.gcode");
        IFileManager iFileManager = file.isLocal() ?
                FabScreenApplication.getInstance().getFabLocalFileManager() :
                FabScreenApplication.getInstance().getFabUsbFileManager();
        try {
            BufferedSource bufferedSource = Okio.buffer(Okio.source(iFileManager.getInputStream(file)));
            BufferedSink bufferedSink = Okio.buffer(Okio.sink(new FileOutputStream(newFile)));
            int len;
            int totalBytes = 0;
            byte[] buffer = new byte[1024 * 16];
            while ((len = bufferedSource.read(buffer)) != -1) {
                bufferedSink.write(buffer, 0, len);
                totalBytes += len;
                if (totalBytes % 10240 == 0) {
                    mResultSubject.onNext("progress " + ((float) (100 * totalBytes / fileLength))
                            + " current " + totalBytes / (1024 * 1024) + " MB");
                }
            }
            bufferedSink.close();
            bufferedSource.close();

            long endTime = SystemClock.elapsedRealtime();
            int usingTime = (int) (endTime - startTime) / 1000;
            mResultSubject.onNext("Copy completed, using " + usingTime + " s"
                    + " avg speed " + (fileLength / (1024 * 1024)) / usingTime + " MB/s");
            Logger.d("Copy completed, using %d s", (endTime - startTime) / 1000);

        } catch (IOException e) {
            LogHelper.log(e);
        } finally {
            iFileManager.deviceHang();
        }

    }

    private ArrayList<IFile> handleFiles(ArrayList<IFile> files) {
        Collections.reverse(files);

        ArrayList<IFile> files2 = new ArrayList<>();
        for (IFile file : files) {
            String name = file.getName();
            if (name.startsWith(".")) {
                continue;
            }
            if (file.isDirectory()) {
                continue;
            }
            files2.add(file);
        }
        return files2;
    }

    static class CopyTestAdapter extends BaseAdapter {
        private Context mContext;
        private List<IFile> mFiles;
        private onClickFileListener mListener;

        CopyTestAdapter(Context context, ArrayList<IFile> files) {
            mContext = context;
            mFiles = files;
        }

        @Override
        public int getCount() {
            return mFiles.size();
        }

        public void setItem(ArrayList<IFile> files) {
            mFiles = files;
        }

        @Override
        public IFile getItem(int i) {
            return mFiles.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        public void setListener(onClickFileListener listener) {
            mListener = listener;
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

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(mContext).inflate(R.layout.item_file, parent, false);
            }

            ImageView ivFileIcon = convertView.findViewById(R.id.iv_file_item_icon);
            Button btnItem = convertView.findViewById(R.id.btn_file_item);
            TextView tvFileName = convertView.findViewById(R.id.tv_file_item_name);
            TextView tvFileDesc = convertView.findViewById(R.id.tv_file_item_desc);
            CheckBox cbFileSelected = convertView.findViewById(R.id.cb_file_item_selected);
            ImageView ivDirArrow = convertView.findViewById(R.id.iv_file_item_arrow);

            IFile file = getItem(position);
            final boolean isDirectory = file.isDirectory();
            final int fileType = getFileHeadType(file);

            btnItem.setOnClickListener(view -> {
                if (isDirectory) {
                    // TODO: copy directory ?
                } else {
                    // copyFile
                    mListener.onClickFile(file);
                }
            });

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


            // set file name
            tvFileName.setText(file.getName());

            // format file size and last modified time
            tvFileDesc.setText(getFileDescription(file));

            ivDirArrow.setVisibility(ImageView.GONE);
            cbFileSelected.setVisibility(CheckBox.GONE);

            return convertView;
        }

        public interface onClickFileListener {
            void onClickFile(IFile file);
        }
    }
}
