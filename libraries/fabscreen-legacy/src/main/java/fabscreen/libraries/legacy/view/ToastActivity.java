package fabscreen.libraries.legacy.view;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

import fabscreen.libraries.legacy.R;
import fabscreen.libraries.legacy.lib.DimensUtils;

public class ToastActivity extends Activity {

    public static final String TOAST_MESSAGE = "TOAST_MESSAGE";
    public static final String TOAST_DRAWABLE = "TOAST_DRAWABLE";
    public static final String TOAST_SHOW_TIME = "TOAST_SHOW_TIME";
    public static final String TOAST_TITLE = "TOAST_TITLE";
    public static final int VIEW_DONE = -1;
    public static final int DEFAULT_SHOW_TIME = 2000;
    public static final int DEFAULT_Y_MARGIN = 20;
    public static final String TOAST_Y_MARGIN = "TOAST_Y_MARGIN";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.toast_image_and_text);
        onNewIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        int toastMessageId = intent.getIntExtra(TOAST_MESSAGE, VIEW_DONE);
        TextView tvToastMessage = findViewById(R.id.tv_toast_message_bottom_horizontal);
        if (toastMessageId != -1) {
            tvToastMessage.setText(toastMessageId);
            tvToastMessage.setVisibility(View.VISIBLE);
        } else {
            tvToastMessage.setVisibility(View.GONE);
        }

        int toastDrawable = intent.getIntExtra(TOAST_DRAWABLE, VIEW_DONE);
        ImageView ivToastDrawable = findViewById(R.id.iv_toast_top_horizontal);
        if (toastDrawable != -1) {
            ivToastDrawable.setImageDrawable(getDrawable(toastDrawable));
            ivToastDrawable.setVisibility(View.VISIBLE);
        } else {
            ivToastDrawable.setVisibility(View.GONE);
        }

        int toastTitle = intent.getIntExtra(TOAST_TITLE, VIEW_DONE);
        TextView tvToastTitle = findViewById(R.id.tv_toast_title_bottom_horizontal);
        if (toastTitle != -1) {
            tvToastTitle.setText(toastTitle);
            tvToastTitle.setVisibility(View.VISIBLE);
        } else {
            tvToastTitle.setVisibility(View.INVISIBLE);
        }

        int showTime = intent.getIntExtra(TOAST_SHOW_TIME, DEFAULT_SHOW_TIME);

        // Set y margin for different dialogs.
        int yMargin = intent.getIntExtra(TOAST_Y_MARGIN, DEFAULT_Y_MARGIN);
        RelativeLayout.LayoutParams ivLp = (RelativeLayout.LayoutParams) ivToastDrawable.getLayoutParams();
        ivLp.topMargin = DimensUtils.dp2px(yMargin, getApplicationContext());
        RelativeLayout.LayoutParams tvLp = (RelativeLayout.LayoutParams) tvToastMessage.getLayoutParams();
        tvLp.bottomMargin = DimensUtils.dp2px(yMargin, getApplicationContext());
        ivToastDrawable.requestLayout();

        // Auto dismiss after 2000ms
        new Handler().postDelayed(this::finish, showTime);
    }
}
