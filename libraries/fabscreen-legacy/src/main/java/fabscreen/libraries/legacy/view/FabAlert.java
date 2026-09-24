package fabscreen.libraries.legacy.view;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import fabscreen.libraries.legacy.R;

public class FabAlert {
    private static AlertDialog sDialog;

    /**
     * Display alert.
     */
    public static void alert(Context context, int resid) {
        alert(context, context.getString(resid));
    }

    public static void alert(Context context, CharSequence text) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(context);
        final Activity activity = (Activity) context;

        // Dismiss existing dialog
        if (sDialog != null) {
            sDialog.cancel();
            sDialog.dismiss();
        }

        // Create new dialog and config
        // https://stackoverflow.com/questions/22794049/how-do-i-maintain-the-immersive-mode-in-dialogs/23207365#23207365
        sDialog = builder.create();

        final LayoutInflater inflater = activity.getLayoutInflater();
        final View view = inflater.inflate(R.layout.dialog_alert, null);
        sDialog.setView(view);

        // View display
        final TextView tvMessage = view.findViewById(R.id.tv_dialog_message);
        tvMessage.setText(text);

        sDialog.show();

        // Auto dismiss after 2000ms
        new Handler().postDelayed(() -> {
            if (sDialog != null) {
                sDialog.cancel();
                sDialog.dismiss();
                sDialog = null;
            }
        }, 2000);
    }
}
