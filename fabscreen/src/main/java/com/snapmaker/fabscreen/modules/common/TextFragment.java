package com.snapmaker.fabscreen.modules.common;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.view.View;
import android.widget.TextView;
import butterknife.BindView;
import com.snapmaker.fabscreen.R;
import fabscreen.libraries.legacy.base.BaseFragment;
import fabscreen.libraries.legacy.base.BaseViewModel;

public class TextFragment extends BaseFragment {

    @BindView(R.id.tv_center_text)
    TextView mTvCenterText;

    private String mTitle;
    private String mText;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setTitle(mTitle);
        mTvCenterText.setText(mText);
    }

    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_text;
    }

    @Override
    protected BaseViewModel getViewModel() {
        return null;
    }

    public void setTitle(String title) {
        super.setTitle(title);
        mTitle = title;
    }

    public void setText(String text) {
        mText = text;
    }
}
