package fabscreen.libraries.legacy.view;

import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;

import fabscreen.libraries.legacy.R;
import fabscreen.libraries.legacy.view.bottombar.BottomBarItem;

public class ViewUtils {
    public static BottomBarItem createBottomBarItem(@StringRes int titleRes, @DrawableRes int iconRes) {
        BottomBarItem item = new BottomBarItem(titleRes);
        item.setIcon(iconRes);
        item.setNormalColor(R.color.custom_grey_0);
        item.setSelectedColor(R.color.custom_blue_600);
        item.setDisabledColor(R.color.custom_grey_600);
        return item;
    }
}