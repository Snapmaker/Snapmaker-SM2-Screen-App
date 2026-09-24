package com.snapmaker.fabscreen.modules.dualextrudercalibration.entries;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.snapmaker.fabscreen.R;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class EntriesAdapter extends RecyclerView.Adapter<EntriesAdapter.ViewHolder> {
    private final List<EntryData> mEntryList;
    private ItemClickListener mListener;

    public EntriesAdapter(@NonNull List<EntryData> list) {
        mEntryList = list;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_calibration_entry, parent, false);
        return new ViewHolder(view, mListener);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(mEntryList.get(position));
    }

    @Override
    public int getItemCount() {
        return mEntryList.size();
    }

    public void setOnItemClickListener(ItemClickListener listener) {
        mListener = listener;
    }

    interface ItemClickListener {
        void onItemClick(EntryData entry);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private EntryData mEntry;
        @BindView(R.id.iv_icon)
        ImageView mIvIcon;
        @BindView(R.id.tv_title)
        TextView mTvTitle;
        @BindView(R.id.v_badge)
        View mVBadge;

        public ViewHolder(@NonNull View itemView, ItemClickListener listener) {
            super(itemView);
            ButterKnife.bind(this, itemView);

            itemView.setOnClickListener(v -> {
                if (mEntry == null) return;
                listener.onItemClick(mEntry);
            });
        }

        public void bind(EntryData entry) {
            mEntry = entry;
            mIvIcon.setImageResource(entry.icon);
            mTvTitle.setText(entry.title);
            mVBadge.setVisibility(entry.showBadge ? View.VISIBLE : View.INVISIBLE);
        }
    }
}
