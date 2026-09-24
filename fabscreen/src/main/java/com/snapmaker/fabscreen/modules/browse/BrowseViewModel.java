package com.snapmaker.fabscreen.modules.browse;

import android.content.Context;

import androidx.annotation.NonNull;

import com.orhanobut.logger.Logger;
import com.snapmaker.fabscreen.FabScreenApplication;

import java.util.ArrayList;
import java.util.Collections;

import fabscreen.libraries.legacy.base.BaseViewModel;
import fabscreen.libraries.legacy.data.Constants;
import fabscreen.libraries.legacy.data.Model;
import fabscreen.libraries.legacy.lib.file.FabLocalFileManager;
import fabscreen.libraries.legacy.lib.file.FabUsbFileManager;
import fabscreen.libraries.legacy.lib.file.IFile;
import fabscreen.libraries.legacy.lib.file.IFileManager;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.BehaviorSubject;

class BrowseViewModel extends BaseViewModel {
    private static final String TAG = "BrowseViewModel";

    static final int FILTER_NONE = 0;
    static final int FILTER_NAME_ASCENDING = 1;
    static final int FILTER_NAME_DESCENDING = 2;
    static final int FILTER_DATE_ASCENDING = 3;
    static final int FILTER_DATE_DESCENDING = 4;
    static final int FILTER_SIZE_ASCENDING = 5;
    static final int FILTER_SIZE_DESCENDING = 6;

    private CompositeDisposable disposables = new CompositeDisposable();

    // Represent files on current directory, can be subscribed on view.
    private BehaviorSubject<ArrayList<IFile>> mFilesSubject = BehaviorSubject.create();

    private int mFilter = FILTER_NONE;
    private BehaviorSubject<ArrayList<IFile>> mFilteredFilesSubject = BehaviorSubject.create();

    private int mHeadType = Constants.HEAD_UNPLUGGED;

    // return count of files being selected
    private BehaviorSubject<Integer> mSelectedFilesCountSubject = BehaviorSubject.createDefault(0);
    private ArrayList<IFile> mSelectFileList = new ArrayList<>();

    private IFileManager mFileManager;

    // TODO: 2021/8/24  viewModel must never reference activity context.
    BrowseViewModel(Context context, Model model, boolean isLocal) {
        super();

        if (isLocal) {
            mFileManager = new FabLocalFileManager(context, getModel().getFilesDir().getPath());
        } else {
            mFileManager = FabScreenApplication.getInstance().getFabUsbFileManager();
        }
        mHeadType = model.getMachineController().getHeadType();

        // every time files changed, apply filter.
        disposables.add(mFilesSubject.subscribe(files -> applyFilter()));
    }

    boolean isRoot() {
        return mFileManager.isRoot();
    }

    long getTotalSpace() {
        return mFileManager.getTotalSpace();
    }

    long getUsedSpace() {
        return mFileManager.getUsedSpace();
    }

    /**
     * Mount disk (USB Disk) and initialize root directory.
     */
    Observable<Boolean> mount() {
        return mFileManager.mount();
    }

    void unMount() {
        mFileManager.unmount();
    }

    public Observable<ArrayList<IFile>> getFiles() {
        return mFilesSubject;
    }

    /**
     * List files on current directory.
     */
    Observable<ArrayList<IFile>> listFiles() {
        Logger.d("current head: %s", getModel().getMachineController().getHeadType());
        return mFileManager.listFiles()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext(files -> {
                    ArrayList<IFile> files2 = handleFiles(files);
                    mFilesSubject.onNext(files2);
                });
    }

    private ArrayList<IFile> handleFiles(ArrayList<IFile> files) {
        Collections.reverse(files);

        ArrayList<IFile> files2 = new ArrayList<>();
        for (IFile file : files) {
            String name = file.getName();
            if (name.startsWith(".") || (name.startsWith("System Volume") && file.isDirectory())) {
                continue;
            }

            final int fileType = getFileHeadType(file);
            // Allows log files to be displayed
            if (fileType == Constants.FILE_TYPE_UPDATE || isFileCompatibleForMachine(fileType) || file.isDirectory() || fileType == Constants.FILE_TYPE_LOG) {
                files2.add(file);
            } else if (mHeadType == Constants.HEAD_LASER_10W && fileType == Constants.FILE_TYPE_LASER) {
                files2.add(file);
            }
        }

        return files2;
    }

    private int getFileHeadType(IFile file) {
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
            case "log":
                return Constants.FILE_TYPE_LOG;
            default:
                return Constants.FILE_TYPE_UNKNOWN;
        }
    }

    private boolean isFileCompatibleForMachine(int fileType) {
        switch (fileType) {
            case Constants.FILE_TYPE_3DP:
                return (mHeadType == Constants.HEAD_3DP
                        || mHeadType == Constants.HEAD_3DP_DUAL_EXTRUDER);
            case Constants.FILE_TYPE_LASER:
                return (mHeadType == Constants.HEAD_LASER
                        || mHeadType == Constants.HEAD_LASER_10W
                        || mHeadType == Constants.HEAD_LASER_20W
                        || mHeadType == Constants.HEAD_LASER_40W
                        || mHeadType == Constants.HEAD_LASER_2W_IR);
            case Constants.FILE_TYPE_CNC:
                return mHeadType == Constants.HEAD_CNC
                        || mHeadType == Constants.HEAD_CNC_200W;
            case Constants.FILE_TYPE_UNKNOWN:
                return false;
            case Constants.FILE_TYPE_UPDATE:
            default:
                return true;
        }
    }

    /**
     * Goto subdirectory.
     *
     * @param file The subdirectory.
     */
    Observable<Boolean> gotoDirectory(IFile file) {
        if (!file.isDirectory()) {
            return Observable.just(false);
        }

        mFileManager.gotoDirectory(file);

        return listFiles().map(files -> true);
    }

    Observable<Boolean> popDirectory() {
        mFileManager.popDirectory();

        return listFiles().map(files -> true);
    }

    Observable<Boolean> removeFile(IFile file) {
        return Observable.fromCallable(
                () -> {
                    mFileManager.removeFile(file);
                    return true;
                })
                .flatMap(success -> {
                    if (success) {
                        return listFiles().map(files -> true);
                    } else {
                        return Observable.just(false);
                    }
                });
    }

    Observable<Boolean> renameFile(IFile fabFile, @NonNull String name) {
        return Observable.fromCallable(
                () -> {
                    mFileManager.renameFile(fabFile, name);
                    return true;
                })
                .flatMap(success -> {
                    if (success) {
                        return listFiles().map(files -> true);
                    } else {
                        return Observable.just(false);
                    }
                });
    }

    // -- filter

    void setFilter(int filter) {
        if (filter != mFilter) {
            mFilter = filter;

            applyFilter();
        }
    }

    private void applyFilter() {
        ArrayList<IFile> files = mFilesSubject.getValue();

        switch (mFilter) {
            case FILTER_NAME_ASCENDING:
                Collections.sort(files, (o1, o2) -> o1.getName().compareToIgnoreCase(o2.getName()));
                break;
            case FILTER_DATE_DESCENDING:
                Collections.sort(files, (o1, o2) -> Long.compare(o2.lastModified(), o1.lastModified()));
                break;
            case FILTER_NONE:
            default:
                break;
        }
        mFilteredFilesSubject.onNext(files);
    }

    Observable<ArrayList<IFile>> watchFilteredFiles() {
        return mFilteredFilesSubject;
    }

    // -- select

    Observable<Integer> getSelectedCount() {
        return mSelectedFilesCountSubject;
    }

    void clearSelectedCount() {
        mSelectedFilesCountSubject.onNext(0);
    }

    void addSelectedFile(IFile file) {
        mSelectFileList.add(file);
        mSelectedFilesCountSubject.onNext(mSelectFileList.size());
    }

    void removeSelectedFile(IFile file) {
        // does remove can apply with IFile?
        mSelectFileList.remove(file);
        mSelectedFilesCountSubject.onNext(mSelectFileList.size());
    }

    void removeAllSelectedFile() {
        if (mSelectFileList.size() != 0) {
            mSelectFileList.clear();
        }
        mSelectedFilesCountSubject.onNext(mSelectFileList.size());
    }

    ArrayList<String> getSelectedFileNames() {
        if (mSelectFileList.size() == 0) return null;

        ArrayList<String> filenames = new ArrayList<>();
        for (IFile file : mSelectFileList) {
            filenames.add(file.getName());
        }
        return filenames;
    }

    Observable<Boolean> deleteSelectedFiles() {
        return Observable.fromCallable(
                () -> {
                    for (IFile file : mSelectFileList) {
                        mFileManager.removeFile(file);
                    }
                    return true;
                })
                .flatMap(success -> listFiles().map(files -> true));
    }

    Observable<Boolean> renameSelectedFile(String filename) {
        if (mSelectFileList.size() != 1) {
            return Observable.just(false);
        }

        IFile file = mSelectFileList.get(0);

        return Observable.fromCallable(
                () -> {
                    mFileManager.renameFile(file, filename);
                    return true;
                })
                .flatMap(success -> listFiles().map(files -> true));
    }

    void dispose() {
        if (mFileManager instanceof FabUsbFileManager) {
            mFileManager = null;
        } else {
            mFileManager.close();
        }
        disposables.dispose();
    }

    public Observable<Boolean> subscribeFileManagerState() {
        return mFileManager.getFileManagerStateObservable();
    }
}
