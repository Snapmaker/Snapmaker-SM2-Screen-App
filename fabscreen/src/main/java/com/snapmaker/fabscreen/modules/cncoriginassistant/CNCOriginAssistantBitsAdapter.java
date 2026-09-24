package com.snapmaker.fabscreen.modules.cncoriginassistant;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.snapmaker.fabscreen.R;
import fabscreen.libraries.legacy.view.SquareActionButton;

import java.util.ArrayList;
import java.util.List;

public class CNCOriginAssistantBitsAdapter extends BaseAdapter {
    private Context mContext;
    private List<CNCOriginAssistantBitItem> mItems;
    private int mSelectedPosition;
    private OnItemClickListener mOnItemClickListener;

    public CNCOriginAssistantBitsAdapter(Context context) {
        mContext = context;
        mItems = new ArrayList<>();
        mSelectedPosition = -1;
    }

    @Override
    public int getCount() {
        return mItems.size();
    }

    @Override
    public CNCOriginAssistantBitItem getItem(int position) {
        return mItems.get(position);
    }

    public void setItems(ArrayList<CNCOriginAssistantBitItem> items) {
        mItems = items;
    }

    public void setSelectedPosition(int position) {
        mSelectedPosition = position;
        notifyDataSetChanged();
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        mOnItemClickListener = listener;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        CNCOriginAssistantBitItem item = getItem(position);

        if (convertView == null) {
            int viewLayout = item.isDefaultBit() ?
                    R.layout.item_origin_assistant_default_bit :
                    R.layout.item_origin_assistant_custom_bit;
            convertView = LayoutInflater.from(mContext).inflate(viewLayout, parent, false);
        }

        if (item.isDefaultBit()) {
            SquareActionButton squareActionButton = convertView.findViewById(R.id.sab_origin_assistant_default_bit);
            TextView tvBitDesc = convertView.findViewById(R.id.tv_origin_assistant_default_bit_desc);

            // bit button
            squareActionButton.setTitle(item.getBitName());
            squareActionButton.setSelected(mSelectedPosition == position);
            squareActionButton.setBackground(mContext.getDrawable(item.getBitResId()));
            squareActionButton.setOnClickListener(v -> {
                mOnItemClickListener.onItemDefaultBitClick(v, position);
            });
        } else {
            SquareActionButton squareActionButton = convertView.findViewById(R.id.sab_origin_assistant_custom_bit);
            squareActionButton.setTitle(item.getBitName());

            squareActionButton.setOnClickListener(v -> {
                mOnItemClickListener.onItemCustomBitClick(v, position);
            });
        }

        return convertView;
    }

    public interface OnItemClickListener {
        void onItemDefaultBitClick(View view, int position);
        void onItemCustomBitClick(View view, int position);
    }

}
