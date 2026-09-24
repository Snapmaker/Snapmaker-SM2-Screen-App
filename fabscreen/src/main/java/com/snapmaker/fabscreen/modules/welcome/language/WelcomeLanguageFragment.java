package com.snapmaker.fabscreen.modules.welcome.language;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import android.view.View;
import android.widget.Button;

import butterknife.BindView;
import butterknife.OnClick;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.subjects.BehaviorSubject;

import com.snapmaker.fabscreen.R;
import fabscreen.libraries.legacy.base.BaseFragment;
import fabscreen.libraries.legacy.base.BaseViewModel;
import com.snapmaker.fabscreen.modules.welcome.WelcomeActivity;
import com.snapmaker.fabscreen.router.Router;

import java.util.ArrayList;
import java.util.List;

public class WelcomeLanguageFragment extends BaseFragment {
    public static WelcomeLanguageFragment newInstance() {
        return new WelcomeLanguageFragment();
    }

    @BindView(R.id.btn_language_english)
    Button mBtnLanguageEnglish;
    @BindView(R.id.btn_language_german)
    Button mBtnLanguageGerman;
    @BindView(R.id.btn_language_simplified_chinese)
    Button mBtnLanguageSimplifiedChinese;
    @BindView(R.id.btn_language_french)
    Button mBtnLanguageFrench;
    @BindView(R.id.btn_language_japanese)
    Button mBtnLanguageJapanese;
    @BindView(R.id.btn_language_spanish)
    Button mBtnLanguageSpanish;
    @BindView(R.id.btn_language_polish)
    Button mBtnLanguagePolish;

    private List<Button> mButtons;
    private BehaviorSubject<Integer> mSelectedLanguageSubject = BehaviorSubject.createDefault(0);

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mButtons = new ArrayList<>();
        mButtons.add(mBtnLanguageEnglish);
        mButtons.add(mBtnLanguageGerman);
        mButtons.add(mBtnLanguageSimplifiedChinese);
        mButtons.add(mBtnLanguageFrench);
        mButtons.add(mBtnLanguageJapanese);
        mButtons.add(mBtnLanguageSpanish);
        mButtons.add(mBtnLanguagePolish);

        // init button events
        for (int i = 0; i < mButtons.size(); i++) {
            int position = i;
            Button button = mButtons.get(position);
            button.setOnClickListener(v -> {
                mSelectedLanguageSubject.onNext(position);
            });
        }

        mSelectedLanguageSubject
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(language -> {
                    for (int i = 0; i < mButtons.size(); i++) {
                        mButtons.get(i).setSelected(i == language);
                    }
                });

        mSelectedLanguageSubject.onNext(getModel().getMultiLanguageManager().getCurrentLanguage());
    }

    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_welcome_language;
    }

    @Override
    protected BaseViewModel getViewModel() {
        return null;
    }

    @OnClick(R.id.btn_welcome_set_language_next)
    void onClickNext() {
        getModel().getMultiLanguageManager().setLanguage(getContext(), mSelectedLanguageSubject.getValue());
        getModel().getPreferences().setMachineSetupLanguage(true);
        Router.getInstance().routeToHomeActivity().start(getContext(), Intent.FLAG_ACTIVITY_NEW_TASK);
    }
}
