package com.thirdbridge.pucksensor.controllers;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import com.thirdbridge.pucksensor.R;
import com.thirdbridge.pucksensor.utils.BaseFragment;

/**
 * Created by Christophe on 2015-10-14.
 */
public class ActionBarFragment extends BaseFragment {

    private ImageButton bleImageButton;
    private ImageButton homeImageButton;
    private ImageButton listImageButton;
    private TextView mActionBarTextView;
    private TextView mCurrentUserTextView;
    private TextView mCurrentBleStatusTextView;

    public static ActionBarFragment newInstance(){
        return new ActionBarFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        super.onCreateView(inflater, container, savedInstanceState);

        View v = inflater.inflate(R.layout.fragment_action_bar, container, false);

        homeImageButton = (ImageButton) v.findViewById(R.id.action_bar_home_image_button);
        bleImageButton = (ImageButton) v.findViewById(R.id.action_bar_bte_image_button);
        listImageButton = (ImageButton) v.findViewById(R.id.action_bar_list_image_button);
        mActionBarTextView = (TextView) v.findViewById(R.id.action_bar_textview);
        mCurrentUserTextView = (TextView) v.findViewById(R.id.action_bar_current_username);
        mCurrentBleStatusTextView = (TextView) v.findViewById(R.id.action_bar_current_ble_status);

        homeImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
				updateGui();
                getController().gotoHome();
            }
        });

        listImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
				updateGui();
                getController().gotoHistory();
            }
        });

        bleImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
				updateGui();
                getController().gotoBleScan();
            }
        });

        return v;
    }

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		updateGui();
	}

    public void setActionBarTitle(String title){
        mActionBarTextView.setText(title);
    }

    public void setActionBarUsername(String username) { mCurrentUserTextView.setText(username); }


    public void updateGui(){
        if(!getController().isBleDeviceConnected()){
            mCurrentBleStatusTextView.setText(getResources().getString(R.string.noBleDeviceConnected));
        }
        else{
            mCurrentBleStatusTextView.setText(getResources().getString(R.string.bleDeviceConnected));
        }
    }
}
