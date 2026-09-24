package fabscreen.libraries.legacy.view;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.PopupWindow;

import fabscreen.libraries.legacy.R;

public class PullDownMenu {
    private static PullDownMenu mMenu;

    private PopupWindow mWindow;

    public static PullDownMenu create(Context context, ListAdapter adapter) {
        if (mMenu != null) {
            mMenu.mWindow.dismiss();
        }
        final Activity activity = (Activity) context;

        mMenu = new PullDownMenu();

        final LayoutInflater inflater = activity.getLayoutInflater();
        final View view = inflater.inflate(R.layout.widget_pull_down_menu, null);

        mMenu.mWindow = new PopupWindow(view,
                WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT, true);


        // view binding
        ListView listView = view.findViewById(R.id.lv_menu_list);
        listView.setAdapter(adapter);
        return mMenu;
    }

    public void showBelowView(View view) {
        mWindow.showAsDropDown(view);
    }

    public void showBelowView(View view, int xoff, int yoff) {
        // xoff and yoff are in pixels
        mWindow.showAsDropDown(view, xoff, yoff);
    }

    public static void dismiss() {
        if (mMenu != null) {
            mMenu.mWindow.dismiss();
        }
    }
}
