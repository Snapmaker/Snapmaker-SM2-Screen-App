package fabscreen.libraries.legacy.view.RectEnergyBar;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import androidx.annotation.Nullable;

import fabscreen.libraries.legacy.R;

public class RectEnergyBarButton extends RelativeLayout {
    private View mBtnValue;
    private int mPosition;

    public RectEnergyBarButton(Context context) {
        this(context, null);
    }

    public RectEnergyBarButton(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RectEnergyBarButton(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public RectEnergyBarButton(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);

        initAttrs(attrs, defStyleAttr, defStyleRes);
        initialize();
    }

    private void initAttrs(@Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        // undefined
    }

    private void initialize() {
        setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT));

        LayoutInflater inflater = LayoutInflater.from(getContext());
        View view = inflater.inflate(R.layout.view_rect_energy_bar_button,this, true);

        mBtnValue = view.findViewById(R.id.btn_energy_bar_button);
    }

    void initialize(Context context) {
        mBtnValue.setClickable(false);
    }

    public void setWidth(int width) {
        ViewGroup.LayoutParams params = getLayoutParams();
        params.width = width;
        setLayoutParams(params);
    }

    public void setPosition(int position) {
        mPosition = position;
    }

    public int getPosition() {
        return mPosition;
    }

    public void show() {
        mBtnValue.setVisibility(View.VISIBLE);
    }

    public void hide() {
        mBtnValue.setVisibility(View.GONE);
    }

}
