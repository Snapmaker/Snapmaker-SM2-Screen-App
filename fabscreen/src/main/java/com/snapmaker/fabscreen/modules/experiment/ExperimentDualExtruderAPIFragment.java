package com.snapmaker.fabscreen.modules.experiment;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.snapmaker.fabscreen.R;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import fabscreen.libraries.legacy.base.BaseFragment;

public class ExperimentDualExtruderAPIFragment extends BaseFragment {

    private ExperimentDualExtruderAPIViewModel mViewModel;

    public static Fragment newInstance() {
        return new ExperimentDualExtruderAPIFragment();
    }

    @BindView(R.id.rv_apis)
    RecyclerView mRvAPIs;

    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_experiment_dual_extruder;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mViewModel = getFragmentScopeViewModel(ExperimentDualExtruderAPIViewModel.class);
        List<String> apis = mViewModel.getAPIs();
        APIAdapter apiAdapter = new APIAdapter(apis);
        apiAdapter.setOnItemClickListener(position -> {
            mViewModel.testAPI(position);
        });
        mRvAPIs.setLayoutManager(new LinearLayoutManager(requireContext()));
        mRvAPIs.setAdapter(apiAdapter);
    }

    static class APIAdapter extends RecyclerView.Adapter<APIAdapter.ViewHolder> {
        private final List<String> mApis;
        private OnItemClickListener mListener;

        public APIAdapter(@NonNull List<String> apis) {
            mApis = apis;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View inflate = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_experiment, parent, false);
            return new ViewHolder(inflate, mListener);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            TextView textView = (TextView) holder.itemView;
            textView.setText(position + ": " + mApis.get(position));
        }

        @Override
        public int getItemCount() {
            return mApis.size();
        }

        public static class ViewHolder extends RecyclerView.ViewHolder {

            public ViewHolder(@NonNull View itemView, OnItemClickListener listener) {
                super(itemView);
                itemView.setOnClickListener(v -> listener.onClick(getAdapterPosition()));
            }
        }

        public void setOnItemClickListener(OnItemClickListener listener) {
            mListener = listener;
        }

        interface OnItemClickListener {
            void onClick(int position);
        }
    }
}
