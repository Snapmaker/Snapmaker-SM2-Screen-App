package fabscreen.libraries.legacy.base;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;

import com.google.firebase.analytics.FirebaseAnalytics;
import com.orhanobut.logger.Logger;
import com.uber.autodispose.AutoDispose;
import com.uber.autodispose.AutoDisposeConverter;
import com.uber.autodispose.android.lifecycle.AndroidLifecycleScopeProvider;

import fabscreen.libraries.legacy.BaseApplication;
import fabscreen.libraries.legacy.BuildConfig;
import fabscreen.libraries.legacy.data.Model;
import io.reactivex.disposables.CompositeDisposable;

public abstract class BaseActivity extends FragmentActivity {
    protected final CompositeDisposable disposables = new CompositeDisposable();
    private FirebaseAnalytics mFirebaseAnalytics;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);
    }

    @Override
    protected void onStart() {
        super.onStart();

        BaseApplication.getInstance().getViewSub().enter(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        BaseApplication.getInstance().getViewSub().leave(this);
    }

    /**
     * onDestroy
     * <p>
     * Release disposables.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        disposables.clear();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (BuildConfig.DEBUG) {
            // Hide nav bars on AVD.
            View decorView = getWindow().getDecorView();
            decorView.setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }
    }

    protected <T> AutoDisposeConverter<T> bindToLifecycle() {
        return AutoDispose.autoDisposable(AndroidLifecycleScopeProvider.from(this));
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    /**
     * Method for communications between fragments.
     *
     * @param resultCode int
     * @param data       Intent that contains result
     */
    public void onFragmentResult(int resultCode, @Nullable Intent data) {
    }

    public void addFragment(int containerId, @NonNull Fragment fragment) {
        this.addFragment(null, containerId, fragment);
    }

    /**
     * Add fragment to container.
     */
    public void addFragment(String stackName, int containerId, @NonNull Fragment fragment) {
        FragmentManager fragmentManager = getSupportFragmentManager();

        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.add(containerId, fragment);

        final int fragmentCount = fragmentManager.getFragments().size();
        if (fragmentCount > 0) {
            // hide the fragment on top now
            Fragment topFragment = fragmentManager.getFragments().get(fragmentCount - 1);
            transaction.hide(topFragment);
            transaction.addToBackStack(stackName);
        }
        transaction.commit();
    }

    /**
     * Replace fragment on top of container.
     */
    public void replaceFragment(int containerId, @NonNull Fragment fragment) {
        FragmentManager fragmentManager = getSupportFragmentManager();

        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.replace(containerId, fragment);
        transaction.commit();
    }

    /**
     * Pop fragment on top of stack.
     */
    public void popFragment() {
        FragmentManager fragmentManager = getSupportFragmentManager();

        fragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
    }

    @NonNull
    protected Model getModel() {
        return BaseApplication.getInstance().getModel();
    }

    @NonNull
    protected FirebaseAnalytics getFirebaseAnalytics() {
        return mFirebaseAnalytics;
    }

    protected <T extends BaseViewModel> T getViewModel(Class<T> modelClass) {
        return new ViewModelProvider(this).get(modelClass);
    }
}