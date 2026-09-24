package fabscreen.libraries.legacy.base;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.ViewModelProvider;

import com.google.firebase.analytics.FirebaseAnalytics;
import com.orhanobut.logger.Logger;
import com.uber.autodispose.AutoDispose;
import com.uber.autodispose.AutoDisposeConverter;
import com.uber.autodispose.android.lifecycle.AndroidLifecycleScopeProvider;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Optional;
import fabscreen.libraries.legacy.BaseApplication;
import fabscreen.libraries.legacy.R2;
import fabscreen.libraries.legacy.data.Constants;
import fabscreen.libraries.legacy.data.Model;
import io.reactivex.disposables.CompositeDisposable;


public abstract class BaseFragment extends Fragment {
    protected final CompositeDisposable disposables = new CompositeDisposable();

    private View mRootView;

    @Nullable
    @BindView(R2.id.top_bar_title)
    TextView mTvTopBarTitle;
    @Nullable
    @BindView(R2.id.top_bar_back)
    public Button mBtnTopBarBack;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Logger.d("Route: Create " + getClass().getSimpleName());
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if (mRootView == null) {
            mRootView = inflater.inflate(getLayoutResID(), container, false);
            ButterKnife.bind(this, mRootView);
        }
        return mRootView;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        disposables.clear();
    }

    /**
     * Abstract fragment's layout resource ID, where subclasses doesn't need to inflate the view themselves.
     */
    protected abstract int getLayoutResID();

    @NonNull
    protected ViewModelProvider getViewModelProvider() {
        if (getActivity() != null) {
            return new ViewModelProvider(requireActivity());
        } else {
            throw new IllegalStateException("call getViewModelProvider() outside of lifecycle");
        }
    }

    protected ViewModelProvider getViewFragmentScopeViewModelProvider() {
        return new ViewModelProvider(this);
    }

    protected <T extends BaseViewModel> T getFragmentScopeViewModel(Class<T> clazz) {
        return new ViewModelProvider(this).get(clazz);
    }

    protected <T extends BaseViewModel> T getActivityScopeViewModel(Class<T> clazz) {
        return new ViewModelProvider(requireActivity()).get(clazz);
    }

    protected BaseViewModel getViewModel() {
        return null;
    }

    /**
     * Auto-disposing on
     */
    protected <T> AutoDisposeConverter<T> bindToLifecycle() {
        return AutoDispose.autoDisposable(AndroidLifecycleScopeProvider.from(this, Lifecycle.Event.ON_DESTROY));
    }

    @Optional
    @OnClick(R2.id.top_bar_back)
    protected void back() {
        if (getActivity() != null) {
            Logger.d("Route: Back from " + getClass().getSimpleName());
            getActivity().onBackPressed();
        }
    }

    @Optional
    @OnClick(R2.id.top_bar_info)
    protected void info() {
        // Info button is hidden by default.
        // Implement button behavior based on the page.
    }

    protected void setTitle(CharSequence title) {
        if (mTvTopBarTitle != null) {
            mTvTopBarTitle.setText(title);
        }
    }

    protected void setTitle(String title) {
        if (mTvTopBarTitle != null) {
            mTvTopBarTitle.setText(title);
        }
    }

    protected void setTitle(int resid) {
        if (mTvTopBarTitle != null) {
            mTvTopBarTitle.setText(resid);
        }
    }

    @NonNull
    protected Model getModel() {
        return BaseApplication.getInstance().getModel();
    }

    protected FirebaseAnalytics getFirebaseAnalytics() {
        if (getActivity() != null) {
            return ((BaseActivity) getActivity()).getFirebaseAnalytics();
        } else {
            return null;
        }
    }

    /**
     * Navigate home and clear activities above home Activity.
     *
     * @param clazz the target home Activity class.
     *              Different apps may(not must) have different home Activities.
     */
    protected <T extends Activity> void backToHome(Class<T> clazz) {
        Logger.d("Route: Back to home.");
        Activity activity = getActivity();
        if (activity != null) {
            Intent intent = new Intent(activity, clazz);
            intent.putExtra(Constants.KEY_IS_FORCE_BACK_HOME, true);
            intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            activity.startActivity(intent);
        }
    }

    protected void showDialog(Dialog dialog) {
        if (dialog == null || requireActivity().isFinishing()) return;
        dialog.show();
    }

    protected void dismissDialog(Dialog dialog) {
        if (dialog == null) return;
        dialog.dismiss();
    }

    protected void finishWithResultOk() {
        requireActivity().setResult(Activity.RESULT_OK);
        requireActivity().finish();
    }
}
