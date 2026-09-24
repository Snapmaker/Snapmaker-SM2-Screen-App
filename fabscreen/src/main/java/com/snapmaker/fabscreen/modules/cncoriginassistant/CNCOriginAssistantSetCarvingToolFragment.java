package com.snapmaker.fabscreen.modules.cncoriginassistant;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.GridView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.snapmaker.fabscreen.R;
import fabscreen.libraries.legacy.base.BaseFragment;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.OnClick;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.subjects.BehaviorSubject;

public class CNCOriginAssistantSetCarvingToolFragment extends BaseFragment implements CNCOriginAssistantBitsAdapter.OnItemClickListener {
    public static CNCOriginAssistantSetCarvingToolFragment newInstance() {
        return new CNCOriginAssistantSetCarvingToolFragment();
    }

    @BindView(R.id.gv_origin_assistant_bits)
    GridView mGvBits;

    @BindView(R.id.btn_cnc_origin_assistant_set_carving_tool_next)
    Button mBtnNext;

    private CNCOriginAssistantViewModel mViewModel;
    private float mBitDiameter = -1;
    private float mBitLength = -1;
    private ArrayList<CNCOriginAssistantBitItem> mBitsList;
    private CNCOriginAssistantBitsAdapter mBitAdapter;

    private BehaviorSubject<CNCOriginAssistantBitItem> mSelectedItemSubject = BehaviorSubject.create();

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mViewModel = getViewModel();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setTitle(R.string.cnc_origin_assistant_carving_tool);

        initData();
        initAdapter();

        mBtnNext.setEnabled(false);
        mSelectedItemSubject
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(item -> {
                    boolean isDefaultBit = item.isDefaultBit();
                    mBtnNext.setEnabled(isDefaultBit);

                    if (isDefaultBit) {
                        mBitDiameter = item.getBitDiameter();
                        mBitLength = item.getBitLength();
                    }
                });
    }

    private void initData() {
        mBitsList = new ArrayList<>();

        mBitsList.add(new CNCOriginAssistantBitItem(CNCOriginAssistantBitItem.TYPE_ITEM_BIT_DEFAULT,
                getResources().getString(R.string.cnc_origin_assistant_carving_v_bit),
                R.drawable.btn_cnc_origin_assistant_carving_v_bit_70x70, 3.175f, 50f));
        mBitsList.add(new CNCOriginAssistantBitItem(CNCOriginAssistantBitItem.TYPE_ITEM_BIT_DEFAULT,
                getResources().getString(R.string.cnc_origin_assistant_flat_end_mill),
                R.drawable.btn_cnc_origin_assistant_flat_end_mill_70x70, 3.175f, 50f));
        mBitsList.add(new CNCOriginAssistantBitItem(CNCOriginAssistantBitItem.TYPE_ITEM_BIT_DEFAULT,
                getResources().getString(R.string.cnc_origin_assistant_ball_end_mill),
                R.drawable.btn_cnc_origin_assistant_ball_end_mill_70x70, 3.175f, 50f));
        mBitsList.add(new CNCOriginAssistantBitItem(CNCOriginAssistantBitItem.TYPE_ITEM_BIT_DEFAULT,
                getResources().getString(R.string.cnc_origin_assistant_straight_groove_v_bit),
                R.drawable.btn_cnc_origin_assistant_groove_v_bit_70x70, 3.175f, 50f));

        // custom bit button
        mBitsList.add(new CNCOriginAssistantBitItem(CNCOriginAssistantBitItem.TYPE_ITEM_BIT_CUSTOM,
                getResources().getString(R.string.cnc_origin_assistant_custom_bit),
                R.drawable.btn_cnc_origin_assistant_custom_bit_70x70, -1, -1));
    }

    private void initAdapter() {
        mBitAdapter = new CNCOriginAssistantBitsAdapter(getContext());
        mBitAdapter.setOnItemClickListener(this);
        mBitAdapter.setItems(mBitsList);
        mGvBits.setAdapter(mBitAdapter);

        mBitAdapter.notifyDataSetChanged();
    }

    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_cnc_origin_assistant_set_carving_tool;
    }

    @Override
    protected CNCOriginAssistantViewModel getViewModel() {
        return getViewModelProvider().get(CNCOriginAssistantViewModel.class);
    }

    @OnClick(R.id.btn_cnc_origin_assistant_set_carving_tool_next)
    void onClickNext() {
        mViewModel.setBitDiameterInput(String.valueOf(mBitDiameter));
        mViewModel.setBitLengthInput(String.valueOf(mBitLength));

        if (getActivity() != null) {
            ((CNCOriginAssistantActivity) getActivity()).gotoCNCOriginAssistantSetOriginIntroFragment();
        }
    }

    @Override
    public void onItemDefaultBitClick(View view, int position) {
        mSelectedItemSubject.onNext(mBitAdapter.getItem(position));
        mBitAdapter.setSelectedPosition(position);
    }

    @Override
    public void onItemCustomBitClick(View view, int position) {
        CNCOriginAssistantBitItem item = mBitAdapter.getItem(position);
        mSelectedItemSubject.onNext(item);
        mBitAdapter.setSelectedPosition(position);

        if (getActivity() != null) {
            ((CNCOriginAssistantActivity) getActivity()).gotoCNCOriginAssistantCustomBitFragment();
        }
    }
}
