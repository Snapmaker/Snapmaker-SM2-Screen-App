package com.snapmaker.fabscreen.modules.factory;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.view.View;
import android.widget.ImageView;

import com.orhanobut.logger.Logger;
import com.snapmaker.fabscreen.R;
import fabscreen.libraries.legacy.base.BaseFragment;
import fabscreen.libraries.legacy.base.BaseViewModel;

import butterknife.BindView;

public class FactoryColorFraudFragment extends BaseFragment {
    private static final String TAG = FactoryTouchPanelTestFragment.class.getSimpleName();

    @BindView(R.id.view_color_fraud)
    View mView;

    @BindView(R.id.iv_color_view_test)
    ImageView mIvColorView;

    private static final int INTERVAL = 2000;
    private Handler mHandler;
    private int mIndex = 0;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setTitle("Color Fraud");

        mHandler = new Handler();
        mHandler.postDelayed(mRunnable, 0);

        mView.setOnClickListener((v) -> back());
        mIvColorView.setOnClickListener((v) -> back());
    }

    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_factory_color_fraud;
    }

    @Override
    protected BaseViewModel getViewModel() {
        return null;
    }

    private Runnable mRunnable = new Runnable() {
        @Override
        public void run() {

            int[] colors = {
                    Color.RED,
                    Color.GREEN,
                    Color.BLUE,
                    Color.WHITE,
                    Color.BLACK
            };

            boolean isTestColorPic = (mIndex % 8) > 4;

            // We need to show these pictures for LCD panel test.
            if (isTestColorPic) {
                switch (mIndex % 8) {
                    case 5:
                        mIvColorView.setImageResource(R.drawable.pic_factory_color_test);
                        break;
                    case 6:
                        mIvColorView.setImageResource(R.drawable.pic_factory_color_test2);
                        break;
                    case 7:
                        mIvColorView.setImageResource(R.drawable.pic_factory_color_test3);
                        break;
                    default:
                }
            } else {
                int color = colors[mIndex % 8];
                mView.setBackgroundColor(color);
            }

            mIvColorView.setVisibility(isTestColorPic ? ImageView.VISIBLE : ImageView.GONE);
            mView.setVisibility( isTestColorPic? ImageView.GONE : ImageView.VISIBLE);
            mIndex++;

            mHandler.postDelayed(this, INTERVAL);
        }
    };
}
