package fabscreen.libraries.legacy.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.drawerlayout.widget.DrawerLayout;

/**
 * Intercepting and passing are used to solve sliding conflicts in the same direction
 */
public class AreaScrollDrawerLayout extends DrawerLayout {
    Context context;
    /**
     * MoreThanY defaults to 1 and is checked when used
     */
    private float minHeight = -1;

    public AreaScrollDrawerLayout(@NonNull Context context) {
        super(context);
        this.context = context;
    }

    public AreaScrollDrawerLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
    }

    public AreaScrollDrawerLayout(@NonNull Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.context = context;
    }


    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        // When the MotionEvent is at the bottom button bar height (screen height -70dp), simply return false and pass the event to the child View
        if (minHeight != -1 && ev.getY() >= minHeight) {
            return false;
        }
        return super.onInterceptTouchEvent(ev);
    }

    /**
     * Set the area below moreThanY to allow slippage
     *
     * @param minHeight Min height (min value is 0)
     */
    public void setScrollBelow(float minHeight) {
        this.minHeight = minHeight;
    }
}
