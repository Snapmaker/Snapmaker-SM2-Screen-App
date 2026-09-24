package fabscreen.libraries.legacy.lib;

import fabscreen.libraries.legacy.BaseApplication;
import fabscreen.libraries.legacy.R;

public class DateHelper {
    public static String formatTime(double time) {
        int hour = (int) (time) / 3600;
        int minute = ((int) (time) % 3600) / 60;
        int second = ((int) (time) % 60);

        return BaseApplication.getInstance().getString(R.string.date_helper_format_split_by_colon, hour, minute, second);
    }

    public static String formatTime2(double time) {
        int hour = (int) (time) / 3600;
        int minute = ((int) (time) % 3600) / 60;
        int second = ((int) (time) % 60);

        if (hour < 1) {
            return BaseApplication.getInstance().getString(R.string.date_helper_format_time_minute_second, minute, second);
        } else {
            return BaseApplication.getInstance().getString(R.string.date_helper_format_time_hour_minute, hour, minute);
        }
    }
}
