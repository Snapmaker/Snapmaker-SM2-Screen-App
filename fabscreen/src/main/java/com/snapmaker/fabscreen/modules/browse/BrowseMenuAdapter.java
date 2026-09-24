package com.snapmaker.fabscreen.modules.browse;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.snapmaker.fabscreen.R;

import java.util.List;

import io.reactivex.disposables.CompositeDisposable;

public class BrowseMenuAdapter extends BaseAdapter {
    private final CompositeDisposable disposables = new CompositeDisposable();
    private Context mContext;
    private List<String> mItems;
    private int mSelectedItem = -1;

    public interface OnItemClickListener {
        void onItemClick(View view, int position);
    }

    private OnItemClickListener mListener;

    BrowseMenuAdapter(Context context, List<String> items) {
        mContext = context;
        mItems = items;
    }

    void dispose() {
        disposables.dispose();
    }

    @Override
    public int getCount() {
        return mItems.size();
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @NonNull
    @Override
    public String getItem(int i) {
        return mItems.get(i);
    }

    void setOnItemClickListener(OnItemClickListener listener) {
        mListener = listener;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(mContext).inflate(R.layout.item_menu, parent, false);
        }

        String name = getItem(position);

        TextView tvItemName = convertView.findViewById(R.id.tv_menu_item_name);
        Button btnItem = convertView.findViewById(R.id.btn_menu_item);
        ImageView imageView = convertView.findViewById(R.id.iv_menu_item_selected);

        tvItemName.setText(name);

        imageView.setVisibility(mSelectedItem == position ? View.VISIBLE : View.INVISIBLE);

        btnItem.setOnClickListener((v) -> {
            if (mListener != null) {
                mListener.onItemClick(v, position);
            }
            if (position != 0) {
                mSelectedItem = position;
            }
        });

        return convertView;
    }
}
