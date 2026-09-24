package com.snapmaker.fabscreen.modules.common;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.orhanobut.logger.Logger;
import com.snapmaker.fabscreen.R;

import fabscreen.libraries.legacy.view.ActionButton;

import com.uber.autodispose.AutoDispose;
import com.uber.autodispose.AutoDisposeConverter;
import com.uber.autodispose.android.lifecycle.AndroidLifecycleScopeProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import fabscreen.libraries.legacy.data.Constants;
import fabscreen.libraries.legacy.data.ISlaveComputer;
import fabscreen.libraries.legacy.data.Model;
import fabscreen.libraries.legacy.data.model.ModelBoundary;
import fabscreen.libraries.legacy.data.serial.fabpacket.FabPacketContent;
import fabscreen.libraries.legacy.lib.LogHelper;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.subjects.BehaviorSubject;

public class SetOriginPagerPresenter implements DefaultLifecycleObserver {
    private Context context;
    private Model model;

    @BindView(R.id.vp_widget_set_origin_pager)
    ViewPager mVpPager;

    private ActionButton mBtnSetOriginX;
    private ActionButton mBtnSetOriginY;
    private ActionButton mBtnSetOriginZ;
    private ActionButton mBtnSetOrigin;
    private ActionButton mBtnGotoOrigin;
    private ActionButton mBtnRunBoundary;
    private ActionButton mBtnHome;

    private BehaviorSubject<Boolean> mMovingEventSubject = BehaviorSubject.createDefault(false);
    private boolean mIsDisabledZ = false;

    private MyPagerAdapter mPagerAdapter;
    private ModelBoundary mBoundary;

    public SetOriginPagerPresenter(Context context, Model model) {
        this.context = context;
        this.model = model;
    }

    protected Context getContext() {
        return context;
    }

    protected Model getModel() {
        return model;
    }

    protected <T> AutoDisposeConverter<T> bindToLifecycle() {
        LifecycleOwner owner = (LifecycleOwner) getContext();
        return AutoDispose.autoDisposable(AndroidLifecycleScopeProvider.from(owner, Lifecycle.Event.ON_DESTROY));
    }

    public Observable<Boolean> getMovingEventObservable() {
        return mMovingEventSubject.hide();
    }

    public void bindView(Lifecycle lifecycle, View view) {
        lifecycle.addObserver(this);
        ButterKnife.bind(this, view);

        final LayoutInflater inflater = LayoutInflater.from(getContext());

        List<View> views = new ArrayList<>();
        views.add(createPage1(inflater));
        views.add(createPage2(inflater));

        mPagerAdapter = new MyPagerAdapter(views);
        mVpPager.setAdapter(mPagerAdapter);

        mMovingEventSubject
                .distinctUntilChanged()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(movingEvent -> setEnabled(!movingEvent));
    }

    public void setEnabled(boolean enabled) {
        mBtnSetOriginX.setEnabled(enabled);
        mBtnSetOriginY.setEnabled(enabled);
        mBtnSetOriginZ.setEnabled(enabled && !mIsDisabledZ);
        mBtnSetOrigin.setEnabled(enabled);
        mBtnRunBoundary.setEnabled(enabled);
        mBtnGotoOrigin.setEnabled(enabled);
        mBtnHome.setEnabled(enabled);
    }

    public void setBoundary(@Nullable ModelBoundary boundary) {
        mBoundary = boundary;
        mBtnRunBoundary.setEnabled(mBoundary != null);
    }

    private View createPage1(LayoutInflater inflater) {
        View page = inflater.inflate(R.layout.widget_set_origin_pager_p1, null);

        mBtnSetOrigin = page.findViewById(R.id.btn_widget_set_origin_set_origin);
        mBtnSetOrigin.setOnClickListener(this::onClickSetOrigin);

        mBtnGotoOrigin = page.findViewById(R.id.btn_widget_set_origin_goto_origin);
        mBtnGotoOrigin.setOnClickListener(this::onClickGotoOrigin);

        mBtnRunBoundary = page.findViewById(R.id.btn_widget_set_origin_run_boundary);
        mBtnRunBoundary.setEnabled(false);
        mBtnRunBoundary.setOnClickListener(this::onClickRunBoundary);

        return page;
    }

    private View createPage2(LayoutInflater inflater) {
        View page = inflater.inflate(R.layout.widget_set_origin_pager_p2, null);

        mBtnHome = page.findViewById(R.id.btn_widget_set_origin_home);
        mBtnHome.setOnClickListener(this::onClickHome);

        mBtnSetOriginX = page.findViewById(R.id.btn_widget_set_origin_x);
        mBtnSetOriginX.setOnClickListener(this::onClickSetOriginX);

        mBtnSetOriginY = page.findViewById(R.id.btn_widget_set_origin_y);
        mBtnSetOriginY.setOnClickListener(this::onClickSetOriginY);

        mBtnSetOriginZ = page.findViewById(R.id.btn_widget_set_origin_z);
        mBtnSetOriginZ.setOnClickListener(this::onClickSetOriginZ);

        return page;
    }

    private Observable<FabPacketContent.CoordinateSystem> updateCoordinateSystem(Object response) {
        return getModel().getMachineController().updateCoordinateSystem();
    }

    public void disabledZ() {
        mIsDisabledZ = true;
        mBtnSetOriginZ.setEnabled(false);
    }

    private void onClickSetOrigin(View v) {
        mMovingEventSubject.onNext(true);
        mBtnSetOrigin.setActivated(true);

        Logger.i("Requesting set origin...");

        getModel().getSlaveComputer().setPosition(0, 0, 0, ISlaveComputer.FLAG_XYZ)
                .flatMap(this::updateCoordinateSystem)
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(success -> {
                    mMovingEventSubject.onNext(false);
                    mBtnSetOrigin.setActivated(false);
                }, e -> {
                    LogHelper.log(e);
                    mMovingEventSubject.onNext(false);
                    mBtnSetOrigin.setActivated(false);
                });
    }

    private void onClickSetOriginX(View v) {
        mMovingEventSubject.onNext(true);
        mBtnSetOriginX.setActivated(true);

        Logger.i("Requesting set origin x...");

        getModel().getSlaveComputer().setPosition(0, 0, 0, ISlaveComputer.FLAG_X)
                .flatMap(this::updateCoordinateSystem)
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(success -> {
                    mMovingEventSubject.onNext(false);
                    mBtnSetOriginX.setActivated(false);
                }, e -> {
                    LogHelper.log(e);
                    mMovingEventSubject.onNext(false);
                    mBtnSetOriginX.setActivated(false);
                });
    }

    private void onClickSetOriginY(View v) {
        mMovingEventSubject.onNext(true);
        mBtnSetOriginY.setActivated(true);

        Logger.i("Requesting set origin y...");

        getModel().getSlaveComputer().setPosition(0, 0, 0, ISlaveComputer.FLAG_Y)
                .flatMap(this::updateCoordinateSystem)
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(success -> {
                    mMovingEventSubject.onNext(false);
                    mBtnSetOriginY.setActivated(false);
                }, e -> {
                    LogHelper.log(e);
                    mMovingEventSubject.onNext(false);
                    mBtnSetOriginY.setActivated(false);
                });
    }

    private void onClickSetOriginZ(View v) {
        mMovingEventSubject.onNext(true);
        mBtnSetOriginZ.setActivated(true);

        Logger.i("Requesting set origin z...");

        getModel().getSlaveComputer().setPosition(0, 0, 0, ISlaveComputer.FLAG_Z)
                .flatMap(this::updateCoordinateSystem)
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(success -> {
                    mMovingEventSubject.onNext(false);
                    mBtnSetOriginZ.setActivated(false);
                }, e -> {
                    LogHelper.log(e);
                    mMovingEventSubject.onNext(false);
                    mBtnSetOriginZ.setActivated(false);
                });
    }

    private void onClickGotoOrigin(View v) {
        final float currentZ = (float) getModel().getMachineController().getMachineStatus().z;
        mMovingEventSubject.onNext(true);
        mBtnGotoOrigin.setActivated(true);

        Logger.i("Requesting go to origin...");

        if (currentZ > 0) {
            // Engage direction, move X Y linear module first, then Z.
            getModel().getSlaveComputer().sendGcode("G1 X0 Y0 F3000")
                    .flatMap(res -> getModel().getSlaveComputer().sendGcode("G1 Z0 F1800"))
                    .observeOn(AndroidSchedulers.mainThread())
                    .as(bindToLifecycle())
                    .subscribe(success -> {
                        mMovingEventSubject.onNext(false);
                        mBtnGotoOrigin.setActivated(false);
                    }, e -> {
                        LogHelper.log(e);
                        mMovingEventSubject.onNext(false);
                        mBtnGotoOrigin.setActivated(false);
                    });
        } else {
            // Retract direction, move Z linear module first, then X Y.
            getModel().getSlaveComputer().sendGcode("G1 Z0 F1800")
                    .flatMap(res -> getModel().getSlaveComputer().sendGcode("G1 X0 Y0 F3000"))
                    .observeOn(AndroidSchedulers.mainThread())
                    .as(bindToLifecycle())
                    .subscribe(success -> {
                        mMovingEventSubject.onNext(false);
                        mBtnGotoOrigin.setActivated(false);
                    }, e -> {
                        LogHelper.log(e);
                        mMovingEventSubject.onNext(false);
                        mBtnGotoOrigin.setActivated(false);
                    });
        }
    }

    private void onClickHome(View v) {
        mMovingEventSubject.onNext(true);
        mBtnHome.setActivated(true);

        Logger.i("Requesting G28...");

        // TODO: bug here, Homing on CS#1 will result in not get correct coordinate x, y, z
        // So we switch back go CS#0, G28 and then switch back
        getModel().getMachineController().updateCoordinateSystem(0)
                .flatMap(response -> getModel().getSlaveComputer().sendGcode("G28"))
                .flatMap(response -> getModel().getMachineController().updateCoordinateSystem(1))
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(success -> {
                    mMovingEventSubject.onNext(false);
                    mBtnHome.setActivated(false);
                }, e -> {
                    LogHelper.log(e);
                    mMovingEventSubject.onNext(false);
                    mBtnHome.setActivated(false);
                });
    }

    private Observable<FabPacketContent.GcodeResponse> runBoundaryWith(int index, int speed) {
        if (index == 5) {
            String cmd = String.format(Locale.US, "G1 X0 Y0 F%d", speed);
            return getModel().getSlaveComputer().sendGcode(cmd);
        } else {
            // 0 -> 1 -> 2 -> 3 -> 4(0) -> 5(origin)
            final float[] point = mBoundary.getBoundaryPoint(index % 4);
            String cmd = String.format(Locale.US, "G1 X%.2f Y%.2f F%d", point[0], point[1], speed);

            return getModel().getSlaveComputer().sendGcode(cmd)
                    .flatMap(response -> runBoundaryWith(index + 1, speed));
        }
    }

    private void onClickRunBoundary(View v) {
        mMovingEventSubject.onNext(true);
        mBtnRunBoundary.setActivated(true);

        Logger.i("Start run boundary.");

        int headType = getModel().getMachineController().getHeadType();
        switch (headType) {
            case Constants.HEAD_LASER:
            case Constants.HEAD_LASER_2W_IR:
            case Constants.HEAD_LASER_10W:
            case Constants.HEAD_LASER_20W:
            case Constants.HEAD_LASER_40W: {
                runBoundaryWith(0, 3000)
                        .observeOn(AndroidSchedulers.mainThread())
                        .as(bindToLifecycle())
                        .subscribe(res -> {
                            mMovingEventSubject.onNext(false);
                            mBtnRunBoundary.setActivated(false);
                        }, e -> {
                            mMovingEventSubject.onNext(false);
                            mBtnRunBoundary.setActivated(false);
                            LogHelper.log(e);
                        });
                break;
            }
            case Constants.HEAD_CNC:
            case Constants.HEAD_CNC_200W: {
                runBoundaryWith(0, 1800)
                        .observeOn(AndroidSchedulers.mainThread())
                        .as(bindToLifecycle())
                        .subscribe(res -> {
                            mMovingEventSubject.onNext(false);
                            mBtnRunBoundary.setActivated(false);
                        }, e -> {
                            mMovingEventSubject.onNext(false);
                            mBtnRunBoundary.setActivated(false);
                            LogHelper.log(e);
                        });
                break;
            }
            default:
                break;
        }
    }

    class MyPagerAdapter extends PagerAdapter {
        private List<View> views;

        MyPagerAdapter(List<View> views) {
            this.views = views;
        }

        @Override
        public int getCount() {
            return views.size();
        }

        @NonNull
        @Override
        public Object instantiateItem(@NonNull ViewGroup container, int position) {
            View view = views.get(position);
            container.addView(view);
            return view;
        }

        @Override
        public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
            container.removeView((View) object);
        }

        @Override
        public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
            return view == object;
        }
    }
}
