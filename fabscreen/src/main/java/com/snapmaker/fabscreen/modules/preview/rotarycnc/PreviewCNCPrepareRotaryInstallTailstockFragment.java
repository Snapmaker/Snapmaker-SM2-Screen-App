package com.snapmaker.fabscreen.modules.preview.rotarycnc;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.orhanobut.logger.Logger;
import com.snapmaker.fabscreen.R;
import fabscreen.libraries.legacy.base.BaseFragment;
import fabscreen.libraries.legacy.base.BaseViewModel;
import com.snapmaker.fabscreen.router.Router;

import butterknife.BindView;
import butterknife.OnClick;

public class PreviewCNCPrepareRotaryInstallTailstockFragment extends BaseFragment {
    public static PreviewCNCPrepareRotaryInstallTailstockFragment newInstance() {
        return new PreviewCNCPrepareRotaryInstallTailstockFragment();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setTitle(R.string.preview_cnc_rotary_install_tailstock);
    }

    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_preview_cnc_prepare_install_tailstock;
    }

    @Override
    protected BaseViewModel getViewModel() {
        return null;
    }

    @OnClick(R.id.btn_preview_cnc_prepare_install_tailstock_next)
    void onClickNext() {
        Bundle arguments = getArguments();
        if (arguments == null) {
            Logger.w("Arguments not exist.");
            return;
        }
        if (requireActivity().getIntent().getBooleanExtra("force_refresh", false)) {
            Logger.d("Refreshing print page...");
            Router.getInstance().routeToPrintPage(true).start(getContext(), Intent.FLAG_ACTIVITY_NEW_TASK);
            requireActivity().finish();
        } else {
            Router.getInstance()
                    .routeToPrintPage()
                    .start(getContext());
            requireActivity().finish();
        }
    }
}
