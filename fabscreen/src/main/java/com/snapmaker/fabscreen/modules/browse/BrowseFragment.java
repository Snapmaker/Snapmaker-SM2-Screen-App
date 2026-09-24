package com.snapmaker.fabscreen.modules.browse;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.orhanobut.logger.Logger;
import com.snapmaker.fabscreen.R;
import fabscreen.libraries.legacy.base.BaseFragment;
import fabscreen.libraries.legacy.base.BaseViewModel;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.OnClick;
import fabscreen.libraries.legacy.lib.LogHelper;
import fabscreen.libraries.legacy.view.FabAlert;
import fabscreen.libraries.legacy.view.FabConfirm;
import fabscreen.libraries.legacy.view.FabInputDialog;
import fabscreen.libraries.legacy.view.PullDownMenu;
import fabscreen.libraries.legacy.view.SwipeListView;
import fabscreen.libraries.legacy.view.ViewUtils;
import fabscreen.libraries.legacy.view.bottombar.BottomBar;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.subjects.BehaviorSubject;

public class BrowseFragment extends BaseFragment {
    private static final String TAG = "BrowseFragment";

    private static final int FILE_TYPE_LOCAL = 0;
    private static final int FILE_TYPE_REMOTE = 1;
    private static final int FILE_TYPE_LIBRARY = 2;

    @BindView(R.id.top_bar_info)
    ImageButton mIbTopBarInfo;

    // List
    @BindView(R.id.view_browse_local_files)
    View mViewLocalFiles;
    @BindView(R.id.lv_browse_local_files)
    SwipeListView mLvLocalFiles;
    @BindView(R.id.view_browse_local_files_empty)
    View mViewLocalFilesEmpty;

    @BindView(R.id.view_browse_remote_files)
    View mViewRemoteFiles;
    @BindView(R.id.lv_browse_remote_files)
    SwipeListView mLvRemoteFiles;
    @BindView(R.id.view_browse_remote_files_empty)
    View mViewRemoteFilesEmpty;
    @BindView(R.id.view_browse_usb_file_unable_to_identify)
    View mViewRemoteFilesError;

    // file type bar
    @BindView(R.id.bb_browse_file_type)
    BottomBar mBbFileTypeBar;

    // multi-select bar
    @BindView(R.id.view_browse_multi_selected_bar)
    View mMultiSelectedBar;
    @BindView(R.id.btn_browse_multi_delete)
    Button mBtnMultiSelectedDelete;
    @BindView(R.id.btn_browse_multi_rename)
    Button mBtnMultiSelectedRename;

    @BindView(R.id.iv_browse_multi_delete)
    ImageView mIvMultiSelectedDelete;
    @BindView(R.id.iv_browse_multi_rename)
    ImageView mIvMultiSelectedRename;

    // file usage
    @BindView(R.id.pb_browse_space_usage)
    ProgressBar mPbSpaceUsage;
    @BindView(R.id.tv_browse_space_usage)
    TextView mTvSpaceUsage;

    /**
     * file view mode, 0 for local files, 1 for remote files
     */
    private BehaviorSubject<Integer> mFileTypeSubject = BehaviorSubject.createDefault(0);

    private BehaviorSubject<Boolean> mMultiSelectSubject = BehaviorSubject.createDefault(false);

    private BrowseListAdapter mLocalFileListAdapter;
    private BrowseListAdapter mRemoteFileListAdapter;

    private BrowseViewModel mLocalViewModel;
    private BrowseViewModel mRemoteViewModel;
    private BrowseViewModel mCurrentViewModel;

    private BrowseMenuAdapter mLocalMenuAdapter;
    private BrowseMenuAdapter mRemoteMenuAdapter;

    private FabInputDialog mInputDialog;
    private FabConfirm mConfirmDialog;
    private Disposable mDisposableFileManager;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setTitle(R.string.browse_files);

        // Separate Local and Remote methods, reuse Remote methods
        initBottomBar();
        initLocalFiles();
        initRemoteFiles();
        mountRemoteFiles();
        updateLocalFilesLists();
        updateRemoteFilesLists();

        initMenu();

        initData();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mLocalViewModel.dispose();
        mRemoteViewModel.dispose();

        mLocalFileListAdapter.dispose();
        mRemoteFileListAdapter.dispose();

        mLocalMenuAdapter.dispose();
        mRemoteMenuAdapter.dispose();
    }

    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_file_list;
    }

    @Override
    protected BaseViewModel getViewModel() {
        return null;
    }

    private void initBottomBar() {
        mBbFileTypeBar.addItem(ViewUtils.createBottomBarItem(R.string.browse_local, R.drawable.btn_file_local_normal_32x28));
        mBbFileTypeBar.addItem(ViewUtils.createBottomBarItem(R.string.browse_usb, R.drawable.btn_file_usb_normal_32x28));
        mBbFileTypeBar.setBarBackgroundColor(R.color.bottom_bar_background);
        mBbFileTypeBar.initialize();

        mBbFileTypeBar.selectTab(0);
        mBbFileTypeBar.setOnTabSelectedListener(position -> mFileTypeSubject.onNext(position));
    }

    private BrowseMenuAdapter.OnItemClickListener mMenuListener = new BrowseMenuAdapter.OnItemClickListener() {
        @Override
        public void onItemClick(View view, int position) {
            if (position == 0) {
                Logger.i("Multi-select mode toggled.");
            } else {
                Logger.i("Switching filter " + position);
            }

            switch (position) {
                case 0:
                    // toggle multi select mode
                    mMultiSelectSubject.onNext(true);
                    break;
                case 1:
                    mCurrentViewModel.setFilter(BrowseViewModel.FILTER_NAME_ASCENDING);
                    break;
                case 2:
                    mCurrentViewModel.setFilter(BrowseViewModel.FILTER_DATE_DESCENDING);
                    break;
                default:
                    break;
            }

            PullDownMenu.dismiss();
        }
    };

    private void initMenu() {
        ArrayList<String> menuItems = new ArrayList<>();
        menuItems.add(getResources().getString(R.string.all_edit));
        menuItems.add(getResources().getString(R.string.browse_menu_sort_name_ascending));
        menuItems.add(getResources().getString(R.string.browse_menu_sort_date_descending));

        mLocalMenuAdapter = new BrowseMenuAdapter(getContext(), menuItems);
        mLocalMenuAdapter.setOnItemClickListener(mMenuListener);
        mRemoteMenuAdapter = new BrowseMenuAdapter(getContext(), menuItems);
        mRemoteMenuAdapter.setOnItemClickListener(mMenuListener);
    }

    private void updateRemoteFilesLists() {
        mRemoteViewModel.watchFilteredFiles()
                .as(bindToLifecycle())
                .subscribe(files -> {
                    mRemoteFileListAdapter.setItems(files);
                    mRemoteFileListAdapter.notifyDataSetChanged();
                    updateSpaceUsage();
                });

        mRemoteFileListAdapter = new BrowseListAdapter(getContext(), mRemoteViewModel);
        mLvRemoteFiles.setAdapter(mRemoteFileListAdapter);
        if (mLvRemoteFiles.getEmptyView() == null) {
            mLvRemoteFiles.setEmptyView(mViewRemoteFilesEmpty);
        }
    }

    private void updateLocalFilesLists() {
        // update file lists
        mLocalViewModel.watchFilteredFiles()
                .as(bindToLifecycle())
                .subscribe(files -> {
                    mLocalFileListAdapter.setItems(files);
                    mLocalFileListAdapter.notifyDataSetChanged();
                    updateSpaceUsage();
                });

        // view
        mLocalFileListAdapter = new BrowseListAdapter(getContext(), mLocalViewModel);
        mLvLocalFiles.setAdapter(mLocalFileListAdapter);
        mLvLocalFiles.setEmptyView(mViewLocalFilesEmpty);
    }

    private void mountRemoteFiles() {
        mRemoteViewModel.mount()
                .doOnNext(success -> {
                    if (success) {
                        mBbFileTypeBar.enableTab(1);
                    } else {
                        mBbFileTypeBar.disableTab(1);
                    }
                })
                .filter(success -> success)
                .flatMap(success -> mRemoteViewModel.listFiles())
                .as(bindToLifecycle())
                .subscribe(
                        iFiles -> {
                            // If the USB drive is mount, delete the abnormal view
                            mViewRemoteFilesError.setVisibility(View.GONE);
                            mViewRemoteFilesEmpty.setVisibility(View.GONE);
                            mLvRemoteFiles.setEmptyView(null);
                        },
                        e -> {
                            // todo: Two Exceptions are now known: IOException is caused by the NTFS format, and NullPointerException is caused by the EXFAT format
                            LogHelper.log(e);
                            mBbFileTypeBar.enableTab(1);
                            // If the USB drive is abnormal, display the error view
                            mViewRemoteFilesError.setVisibility(View.VISIBLE);
                            mViewRemoteFilesEmpty.setVisibility(View.GONE);
                            mLvRemoteFiles.setEmptyView(mViewRemoteFilesError);
                        });

    }

    private void initRemoteFiles() {
        // init remote files
        mRemoteViewModel = new BrowseViewModel(getContext(), getModel(), false);
        mRemoteViewModel.subscribeFileManagerState()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(isAttach -> {
                            mLvRemoteFiles.setVisibility(View.GONE);
                            mViewRemoteFiles.setVisibility(View.GONE);
                            if (!isAttach) {
                                if (mFileTypeSubject.getValue() == FILE_TYPE_REMOTE) {
                                    mMultiSelectSubject.onNext(false);
                                }
                                mBbFileTypeBar.selectTab(0);
                            }
                            mountRemoteFiles();
                            updateRemoteFilesLists();
                            // When the USB status changes, refresh the current page to the local page
                            mFileTypeSubject.onNext(FILE_TYPE_LOCAL);
                        }
                );
    }

    private void initLocalFiles() {
        // init local files
        mLocalViewModel = new BrowseViewModel(getContext(), getModel(), true);
        mLocalViewModel.mount()
                .filter(success -> success)
                .flatMap(success -> mLocalViewModel.listFiles())
                .as(bindToLifecycle())
                .subscribe(files -> {
                    // Do nothing
                }, Throwable::printStackTrace);
    }

    private void initData() {
        // file type
        mFileTypeSubject
                .as(bindToLifecycle())
                .subscribe((mode) -> {
                    // switch view model
                    mCurrentViewModel = (mode == FILE_TYPE_LOCAL ? mLocalViewModel : mRemoteViewModel);
                    mViewLocalFiles.setVisibility(mode == FILE_TYPE_LOCAL ? View.VISIBLE : View.GONE);
                    mViewRemoteFiles.setVisibility(mode == FILE_TYPE_REMOTE ? View.VISIBLE : View.GONE);
                    updateSpaceUsage();
                    updateObserverFileManagerState(mode == FILE_TYPE_REMOTE);
                });

        // multi-select
        mIbTopBarInfo.setVisibility(View.VISIBLE);
        mMultiSelectSubject
                .as(bindToLifecycle())
                .subscribe(isMultiSelected -> {
                    mBbFileTypeBar.setVisibility(isMultiSelected ? View.GONE : View.VISIBLE);
                    mMultiSelectedBar.setVisibility(isMultiSelected ? View.VISIBLE : View.GONE);
                    mIbTopBarInfo.setImageResource(isMultiSelected ? R.drawable.ic_file_menu_selected_40x40 : R.drawable.ic_top_bar_more_50x50);

                    mLocalFileListAdapter.setMultiSelected(isMultiSelected);
                    mRemoteFileListAdapter.setMultiSelected(isMultiSelected);

                    // disable all swipe
                    mLvLocalFiles.closeAllItems();
                    mLvRemoteFiles.closeAllItems();
                });

        // change button status with selected counts
        mLocalViewModel.getSelectedCount()
                .as(bindToLifecycle())
                .subscribe(count -> {
                    // Temporary workaround here.
                    // Solution: We should implement square button with image, just like ActionButton does.
                    mBtnMultiSelectedDelete.setEnabled(count > 0);
                    mIvMultiSelectedDelete.setEnabled(count > 0);
                    mBtnMultiSelectedRename.setEnabled(count == 1);
                    mIvMultiSelectedRename.setEnabled(count == 1);
                });

        mRemoteViewModel.getSelectedCount()
                .as(bindToLifecycle())
                .subscribe(count -> {
                    mBtnMultiSelectedDelete.setEnabled(count > 0);
                    mIvMultiSelectedDelete.setEnabled(count > 0);
                    mBtnMultiSelectedRename.setEnabled(count == 1);
                    mIvMultiSelectedRename.setEnabled(count == 1);
                });

        // init
        mFileTypeSubject.onNext(FILE_TYPE_LOCAL);
    }

    private void updateObserverFileManagerState(boolean isRemote) {
        if (mDisposableFileManager != null && !mDisposableFileManager.isDisposed()) {
            mDisposableFileManager.dispose();
            mDisposableFileManager = null;
        }
        if (isRemote) {
            mDisposableFileManager = mCurrentViewModel.subscribeFileManagerState().observeOn(AndroidSchedulers.mainThread())
                    .as(bindToLifecycle())
                    .subscribe(isAttach -> {
                                if (!isAttach) {
                                    if (mConfirmDialog != null && mConfirmDialog.isShowing()) {
                                        mConfirmDialog.dismiss();
                                    }
                                    if (mInputDialog != null && mInputDialog.isShowing()) {
                                        mInputDialog.dismiss();
                                    }

                                }
                            }
                    );
        }
    }

    @Override
    protected void info() {
        if (mMultiSelectSubject.getValue()) {
            mMultiSelectSubject.onNext(false);
            Logger.i("Multi-select canceled.");
        } else {
            // show menu
            Logger.i("Opening info menu.");
            PullDownMenu.create(getContext(), mFileTypeSubject.getValue() == FILE_TYPE_LOCAL ? mLocalMenuAdapter : mRemoteMenuAdapter)
                    .showBelowView(mIbTopBarInfo, -360 + mIbTopBarInfo.getWidth(), 10);
        }
    }

    @OnClick(R.id.top_bar_back)
    void onBack() {
        if (getActivity() == null) {
            return;
        }

        if (!mCurrentViewModel.isRoot()) {
            mCurrentViewModel.popDirectory()
                    .as(bindToLifecycle())
                    .subscribe(files -> { /**/ }, e -> super.back());
        } else {
            super.back();
        }
    }

    @OnClick(R.id.btn_browse_multi_delete)
    void onClickMultiDelete() {
        mConfirmDialog = FabConfirm.create(getContext())
                .setDescription(R.string.browse_delete_items_desc)
                .setCancel(R.string.all_cancel, (dialog, position) -> dialog.dismiss())
                .setConfirm(R.string.all_delete, (dialog, position) -> {
                    mCurrentViewModel.deleteSelectedFiles()
                            .doOnNext(success -> mCurrentViewModel.removeAllSelectedFile())
                            .as(bindToLifecycle())
                            .subscribe(success -> mMultiSelectSubject.onNext(false));
                    dialog.dismiss();
                });
        mConfirmDialog.show();
    }

    @OnClick(R.id.btn_browse_multi_rename)
    void onClickMultiRename() {
        ArrayList<String> selectedFilenames = mCurrentViewModel.getSelectedFileNames();
        if (selectedFilenames == null || selectedFilenames.size() != 1) return;
        int index = selectedFilenames.get(0).lastIndexOf(".");
        String sourceName = (index == -1) ? selectedFilenames.get(0) : selectedFilenames.get(0).substring(0, index);
        String extension = (index == -1) ? "" : selectedFilenames.get(0).substring(index + 1).toLowerCase();
        mInputDialog = FabInputDialog.create(getContext())
                .setEditText(sourceName)
                .setTitle(R.string.browse_rename_dialog_title)
                .setButton(getString(R.string.all_confirm), ((dialog, which) -> {
                    String targetFilename = FabInputDialog.getsInstance().getEditTextContent().trim();
                    if (targetFilename.isEmpty() || targetFilename.equals(sourceName)) {
                        return;
                    }

                    mCurrentViewModel.renameSelectedFile((index == -1) ? targetFilename : targetFilename + "." + extension)
                            .doOnNext(success -> mCurrentViewModel.removeAllSelectedFile())
                            .as(bindToLifecycle())
                            .subscribe(success -> {
                                Logger.i("Rename file %s.", success ? "succeed" : "failed");
                                mMultiSelectSubject.onNext(false);
                            }, e -> {
                                LogHelper.log(e);
                                FabAlert.alert(getContext(), e.getMessage());
                                mMultiSelectSubject.onNext(false);
                            });
                }));
        mInputDialog.show();
    }

    private void updateSpaceUsage() {
        long totalSpace = mCurrentViewModel.getTotalSpace();
        long usageSpace = mCurrentViewModel.getUsedSpace();

        totalSpace /= 1024 * 1024;
        usageSpace /= 1024 * 1024;

        // TODO: i18n
        mTvSpaceUsage.setText(getString(R.string.browse_space_usage_desc, totalSpace - usageSpace, totalSpace));

        // usage
        float progress = 0;
        if (totalSpace != 0) {
            progress = (usageSpace / (float) totalSpace) * 100.0f;
        }
        mPbSpaceUsage.setProgress((int) progress);
    }
}
