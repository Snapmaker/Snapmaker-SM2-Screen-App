package fabscreen.libraries.legacy.lib.crash;

import android.app.Activity;
import android.app.Application;
import android.content.Intent;
import android.os.Bundle;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class FabScreenActivityManagement implements Application.ActivityLifecycleCallbacks {
    private List<WeakReference<Activity>> mRunningActivities = new ArrayList<WeakReference<Activity>>();

    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {

    }

    @Override
    public void onActivityStarted(Activity activity) {
        WeakReference<Activity> weakReference = new WeakReference<>(activity);
        mRunningActivities.add(weakReference);
    }

    @Override
    public void onActivityResumed(Activity activity) {

    }

    @Override
    public void onActivityPaused(Activity activity) {

    }

    @Override
    public void onActivityStopped(Activity activity) {

    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {

    }

    @Override
    public void onActivityDestroyed(Activity activity) {
        for (WeakReference<Activity> activityWeakReference : mRunningActivities) {
            if (activityWeakReference == null) {
                continue;
            }
            Activity tmpActivity = activityWeakReference.get();
            if (tmpActivity == null) {
                continue;
            }
            if (tmpActivity == activity) {
                mRunningActivities.remove(activityWeakReference);
                break;
            }
        }
    }

    public List<WeakReference<Activity>> getRunningActivities() {
        return mRunningActivities;
    }

    public Activity getLastActivity() {
        if (mRunningActivities.isEmpty()) return null;
        WeakReference<Activity> lastActivityReference = mRunningActivities.get(mRunningActivities.size() - 1);
        return lastActivityReference.get();
    }

    public void removeAllActivity() {
        for (WeakReference<Activity> activityWeakReference : mRunningActivities) {
            if (activityWeakReference == null) {
                continue;
            }
            Activity tmpActivity = activityWeakReference.get();
            if (tmpActivity == null) {
                continue;
            }
            tmpActivity.finish();
        }
    }

    public ArrayList<Intent> getIntents() {
        ArrayList<Intent> intentList = new ArrayList<>();
        for (WeakReference<Activity> activityWeakReference : mRunningActivities) {
            if (activityWeakReference == null) {
                continue;
            }
            Activity tmpActivity = activityWeakReference.get();
            if (tmpActivity == null) {
                continue;
            }
            intentList.add((Intent) tmpActivity.getIntent().clone());
        }
        return intentList;
    }
}
