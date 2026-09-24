package com.snapmaker.fabscreen.modules.settings.language;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.snapmaker.fabscreen.R;
import fabscreen.libraries.legacy.base.BaseFragment;
import fabscreen.libraries.legacy.base.BaseViewModel;
import com.snapmaker.fabscreen.router.Router;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.OnClick;
import fabscreen.libraries.legacy.data.MultiLanguageManager;

public class SettingsLanguageFragment extends BaseFragment {
    public static SettingsLanguageFragment getInstance() {
        return new SettingsLanguageFragment();
    }

    @BindView(R.id.btn_settings_language_save)
    Button mBtnSave;

    @BindView(R.id.lv_settings_language)
    ListView mLvLanguageList;

    private SettingsLanguageAdapter mAdapter;
    private ArrayList<LanguageItem> mLanguages;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setTitle(R.string.all_language);
        initView();
    }

    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_settings_language;
    }

    @Override
    protected BaseViewModel getViewModel() {
        return null;
    }

    void initView() {
        disableSave();

        // init languages
        mLanguages = new ArrayList<>();
        mLanguages.add(new LanguageItem(MultiLanguageManager.LANGUAGE_DEFAULT, "English"));
        mLanguages.add(new LanguageItem(MultiLanguageManager.LANGUAGE_GERMAN, "Deustch"));
        mLanguages.add(new LanguageItem(MultiLanguageManager.LANGUAGE_SIMPLIFIED_CHINESE, "简体中文"));
        mLanguages.add(new LanguageItem(MultiLanguageManager.LANGUAGE_FRENCH, "Français"));
        mLanguages.add(new LanguageItem(MultiLanguageManager.LANGUAGE_JAPANESE, "日本語"));
        mLanguages.add(new LanguageItem(MultiLanguageManager.LANGUAGE_SPANISH, "español"));
        mLanguages.add(new LanguageItem(MultiLanguageManager.LANGUAGE_POLISH, "Polski"));
//        mLanguages.add(new LanguageItem(MultiLanguageManager.LANGUAGE_KOREAN, "한국어"));
//        mLanguages.add(new LanguageItem(MultiLanguageManager.LANGUAGE_ITALIAN, "Italiano"));

        int currentLanguage = getModel().getMultiLanguageManager().getCurrentLanguage();
        mAdapter = new SettingsLanguageAdapter(getContext());
        mAdapter.setOnSelectedLanguageListener(item -> {
            if (item.getLanguage() == currentLanguage) {
                disableSave();
            } else {
                enableSave();
            }

            // update list view
            setSelectedLanguage(item.getLanguage());
            mAdapter.notifyDataSetChanged();
        });

        mAdapter.setItems(mLanguages);
        mLvLanguageList.setAdapter(mAdapter);

        // set current language selected
        setSelectedLanguage(currentLanguage);
        mAdapter.notifyDataSetChanged();
    }

    void disableSave() {
        if (getContext() != null) {
            mBtnSave.setTextColor(ContextCompat.getColor(getContext(), R.color.custom_grey_600));
        }
        mBtnSave.setEnabled(false);
    }

    void enableSave() {
        if (getContext() != null) {
            mBtnSave.setTextColor(ContextCompat.getColor(getContext(), R.color.custom_blue_400));
        }
        mBtnSave.setEnabled(true);
    }

    void setSelectedLanguage(int language) {
        if (mLanguages == null) return;

        for (LanguageItem l : mLanguages) {
            l.setSelected(l.getLanguage() == language);
        }
    }

    int getSelectedLanguage() {
        if (mLanguages == null) return -1;
        int selectedLanguage = MultiLanguageManager.LANGUAGE_UNKNOWN;

        for (LanguageItem l : mLanguages) {
            if (l.isSelected()) {
                selectedLanguage = l.getLanguage();
                return selectedLanguage;
            }
        }
        return selectedLanguage;
    }

    @OnClick(R.id.btn_settings_language_save)
    void onClickSave() {
        int selectedLanguage = getSelectedLanguage();
        if (selectedLanguage != MultiLanguageManager.LANGUAGE_UNKNOWN) {
            getModel().getMultiLanguageManager().setLanguage(getContext(), selectedLanguage);
            // TODO: Can we change language without restarting the whole app?
            Router.getInstance().routeToHomeActivity().start(getContext(), Intent.FLAG_ACTIVITY_CLEAR_TOP);
        }
    }

    public static class LanguageItem {
        private int language;
        private String languageName;
        private boolean isSelected;

        public LanguageItem(int language, String languageName) {
            this.language = language;
            this.languageName = languageName;
            this.isSelected = false;
        }

        public int getLanguage() {
            return language;
        }

        public String getLanguageName() {
            return languageName;
        }

        public void setSelected(boolean selected) {
            isSelected = selected;
        }

        public boolean isSelected() {
            return isSelected;
        }
    }

    static class SettingsLanguageAdapter extends BaseAdapter {
        private Context mContext;
        private List<LanguageItem> mItems;
        private OnLanguageSelectedListener mListener;

        public SettingsLanguageAdapter(Context context) {
            mContext = context;
        }

        public void setOnSelectedLanguageListener(OnLanguageSelectedListener listener) {
            mListener = listener;
        }

        @Override
        public int getCount() {
            return mItems.size();
        }

        @Override
        public LanguageItem getItem(int position) {
            return mItems.get(position);
        }

        public void setItems(ArrayList<LanguageItem> items) {
            mItems = items;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(mContext).inflate(R.layout.item_settings_langauge, parent, false);
            }
            LanguageItem item = getItem(position);

            Button btnLanguage = convertView.findViewById(R.id.btn_settings_language_name);
            ImageView ivSelected = convertView.findViewById(R.id.iv_settings_language_selected);

            btnLanguage.setText(item.getLanguageName());
            btnLanguage.setOnClickListener(v -> mListener.onSelectedItem(item));

            ivSelected.setVisibility(item.isSelected() ? ImageView.VISIBLE : ImageView.GONE);

            return convertView;
        }
    }

    public interface OnLanguageSelectedListener {
        void onSelectedItem(LanguageItem item);
    }
}
