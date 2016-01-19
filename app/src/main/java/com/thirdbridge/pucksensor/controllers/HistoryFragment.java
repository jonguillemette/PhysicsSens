package com.thirdbridge.pucksensor.controllers;

import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.thirdbridge.pucksensor.R;
import com.thirdbridge.pucksensor.database.DataManager;
import com.thirdbridge.pucksensor.models.ShotTest;
import com.thirdbridge.pucksensor.models.User;
import com.thirdbridge.pucksensor.utils.BaseFragment;
import com.thirdbridge.pucksensor.utils.Constants;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Christophe on 2015-10-14.
 */
public class HistoryFragment extends BaseFragment {

    private Constants.SelectedTest selectedTest;

    private TextView mEmptyHistoryTextView;
    private RecyclerView mHistoryRecyclerView;
    private Button mStartButton;
    private ShotHistoryAdapter mShotHistoryAdapter;
    private RelativeLayout historyHeaderRelativeLayout;
    private static boolean mViewModeOnly;

    private List<ShotTest> mShotTestList;
    private User mUser;

    public static HistoryFragment newInstance(){
        HistoryFragment fragment = new HistoryFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mShotTestList = new ArrayList<>();

        for(ShotTest shotTest: DataManager.get().getShotTests()){
            mShotTestList.add(shotTest);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        View v = inflateShotTestHistory(inflater,container);
        return v;
    }


    private View inflateShotTestHistory(LayoutInflater inflater, ViewGroup container){

        View v = inflater.inflate(R.layout.fragment_history, container, false);

        mEmptyHistoryTextView = (TextView) v.findViewById(R.id.empty_history_textview);
        mHistoryRecyclerView = (RecyclerView) v.findViewById(R.id.test_history_recycler_view);
        historyHeaderRelativeLayout = (RelativeLayout) v.findViewById(R.id.stats_history_list_header);


        if(DataManager.get().getShotTests().size() == 0){
            mHistoryRecyclerView.setVisibility(View.GONE);
            historyHeaderRelativeLayout.setVisibility(View.GONE);
            mEmptyHistoryTextView.setVisibility(View.VISIBLE);
        }
        else{
            historyHeaderRelativeLayout.setVisibility(View.VISIBLE);
            mHistoryRecyclerView.setVisibility(View.VISIBLE);
            mEmptyHistoryTextView.setVisibility(View.GONE);
        }

        mHistoryRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        mShotHistoryAdapter = new ShotHistoryAdapter();

        mHistoryRecyclerView.setAdapter(mShotHistoryAdapter);

        return v;
    }


    private class ShotHistoryHolder extends RecyclerView.ViewHolder{

        private TextView usernameTextView;
        private TextView dateTextView;
        private TextView speedTextView;
        private TextView rotationTextView;
        private TextView detailsTextView;
        private RelativeLayout mTestListItemRelativeLayout;

        public ShotHistoryHolder(View itemView) {
            super(itemView);

            usernameTextView = (TextView) itemView.findViewById(R.id.username_text_view);
            dateTextView = (TextView) itemView.findViewById(R.id.date_text_view);
            speedTextView = (TextView) itemView.findViewById(R.id.speed_text_view);
            rotationTextView = (TextView) itemView.findViewById(R.id.rotation_text_view);
            detailsTextView = (TextView) itemView.findViewById(R.id.details_text_view);
            mTestListItemRelativeLayout = (RelativeLayout) itemView.findViewById(R.id.test_list_item_relativelayout);
        }

        public void bindShotTest(final ShotTest shotTest){
            usernameTextView.setText(shotTest.getUsername());
            dateTextView.setText(shotTest.getDate());
            //todo
            speedTextView.setText(shotTest.getTopSpeed() + " m/s");
            rotationTextView.setText(shotTest.getTopRotation() + " deg/s");

            if(shotTest.getDescription() != null)
                detailsTextView.setText(shotTest.getDescription());


            mTestListItemRelativeLayout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    getController().gotoPreviewShotTest(shotTest);
                }
            });

        }
    }

    private class ShotHistoryAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>{

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
            LayoutInflater layoutInflater = LayoutInflater.from(getActivity());
            View view = layoutInflater.inflate(R.layout.list_item_history,viewGroup,false);

            return new ShotHistoryHolder(view);
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int i) {
            ShotTest shotTest = mShotTestList.get(i);
            ShotHistoryHolder shotHistoryHolder = (ShotHistoryHolder) viewHolder;
            shotHistoryHolder.bindShotTest(shotTest);
        }

        @Override
        public int getItemCount() {
            return mShotTestList.size();
        }
    }

}
