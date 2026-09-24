package com.snapmaker.fabscreen.modules.cncoriginassistant;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.snapmaker.fabscreen.R;
import fabscreen.libraries.legacy.base.BaseFragment;
import fabscreen.libraries.legacy.base.BaseViewModel;

import butterknife.BindView;
import butterknife.OnClick;

public class CNCOriginAssistantSetOriginIntroFragment extends BaseFragment {
    public static CNCOriginAssistantSetOriginIntroFragment newInstance() {
        return new CNCOriginAssistantSetOriginIntroFragment();
    }

    @BindView(R.id.iv_cnc_origin_assistant_intro_cover)
    ImageView mIvCover;
    @BindView(R.id.tv_cnc_origin_assistant_intro_content)
    TextView mTvContent;
    @BindView(R.id.btn_cnc_origin_assistant_intro_start)
    Button mBtnNext;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setTitle(R.string.cnc_origin_assistant);
        mIvCover.setImageResource(R.drawable.pic_cnc_origin_assistant_set_origin_intro_desc_360x240);
        mTvContent.setText(R.string.cnc_origin_assistant_set_origin_intro_content);
        mBtnNext.setText(R.string.all_next);
    }

    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_cnc_origin_assistant_intro;
    }

    @Override
    protected BaseViewModel getViewModel() {
        return null;
    }

    @OnClick(R.id.btn_cnc_origin_assistant_intro_start)
    void onClickNext() {
        if (getActivity() != null) {
            ((CNCOriginAssistantActivity) getActivity()).gotoCNCOriginAssistantSafetyGogglesFragment();
        }
    }
}
