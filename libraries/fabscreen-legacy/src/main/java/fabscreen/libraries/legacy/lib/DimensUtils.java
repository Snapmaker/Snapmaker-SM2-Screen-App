package fabscreen.libraries.legacy.lib;

import android.content.Context;
import android.util.TypedValue;

public class DimensUtils {
    public static int dp2px(float dpValue, Context context) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                dpValue,
                context.getResources().getDisplayMetrics());
    }

    public static int dp2px(int dpValue, Context context) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                dpValue,
                context.getResources().getDisplayMetrics());
    }
}
