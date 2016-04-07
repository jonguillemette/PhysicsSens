package com.thirdbridge.pucksensor.controllers;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.thirdbridge.pucksensor.R;
import com.thirdbridge.pucksensor.utils.BaseFragment;

/**
 * Created by Christophe on 2015-10-14.
 * Modified by Jayson Dalpé since 2016-03-22.
 */
public class ActionBarFragment extends BaseFragment {

    public static int battery_value = -1;

    private ImageButton mBleImageButton;
    private ImageButton mHomeImageButton;
    private ImageButton mListImageButton;
    private static ImageButton mBatteryImageButton;
    private TextView mActionBarTextView;
    private TextView mCurrentUserTextView;
    private TextView mCurrentBleStatusTextView;

    private static ActionBarFragment mInstance = null;

    public static ActionBarFragment getInstance(){
        if (mInstance == null) {
            mInstance = new ActionBarFragment();
        }
        return mInstance;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        super.onCreateView(inflater, container, savedInstanceState);

        View v = inflater.inflate(R.layout.fragment_action_bar, container, false);

        mHomeImageButton = (ImageButton) v.findViewById(R.id.action_bar_home_image_button);
        mBleImageButton = (ImageButton) v.findViewById(R.id.action_bar_bte_image_button);
        mListImageButton = (ImageButton) v.findViewById(R.id.action_bar_list_image_button);
        mActionBarTextView = (TextView) v.findViewById(R.id.action_bar_textview);
        mCurrentUserTextView = (TextView) v.findViewById(R.id.action_bar_current_username);
        mCurrentBleStatusTextView = (TextView) v.findViewById(R.id.action_bar_current_ble_status);
        mBatteryImageButton = (ImageButton) v.findViewById(R.id.action_bar_battery);

        mHomeImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                updateGui();
                getController().gotoHome();
            }
        });

        mListImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //TODO Update this
                //updateGui();
                //getController().gotoHistory();
            }
        });

        mBleImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                updateGui();
                getController().gotoBleScan();
            }
        });

        mBatteryImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (battery_value >= 0) {
                    String charging = "Discharging";
                    int actual_battery = battery_value;
                    if (battery_value >= 128) {
                        charging = "Charging";
                        actual_battery -= 128;
                    }
                    Toast.makeText(getActivity(), "Battery level: " + actual_battery + " " + charging, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getActivity(), "Battery level: Connect a device.", Toast.LENGTH_SHORT).show();
                }
            }
        });

        return v;
    }

    /**
     * Update the battery images and value
     * IMPORTANT: Call it each time it change, not each data receive
     * IMPORTANT: Conversion in 0-255 before sending
     * @param battery The new level of the battery
     */
    public void updateBattery(final int battery) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                battery_value = battery;
                if (battery_value > 128) {
                    int base = battery_value-128;

                    // Don't want to show 0 if a little more.
                    if (base < 15) {
                        mBatteryImageButton.setImageResource(R.drawable.stat_sys_battery_charge_anim15);
                    } else if(base < 28) {
                        mBatteryImageButton.setImageResource(R.drawable.stat_sys_battery_charge_anim28);
                    } else if (base < 43) {
                        mBatteryImageButton.setImageResource(R.drawable.stat_sys_battery_charge_anim43);
                    } else if (base < 57) {
                        mBatteryImageButton.setImageResource(R.drawable.stat_sys_battery_charge_anim57);
                    } else if (base < 71) {
                        mBatteryImageButton.setImageResource(R.drawable.stat_sys_battery_charge_anim71);
                    } else if (base < 85) {
                        mBatteryImageButton.setImageResource(R.drawable.stat_sys_battery_charge_anim85);
                    } else {
                        mBatteryImageButton.setImageResource(R.drawable.stat_sys_battery_charge_anim100);
                    }
                } else {
                    // Don't want to show 0 if a little more.
                    if (battery_value < 0) {
                        mBatteryImageButton.setImageResource(R.drawable.stat_sys_battery_0);
                    } else if (battery_value < 15) {
                        mBatteryImageButton.setImageResource(R.drawable.stat_sys_battery_15);
                    } else if(battery_value < 28) {
                        mBatteryImageButton.setImageResource(R.drawable.stat_sys_battery_28);
                    } else if (battery_value < 43) {
                        mBatteryImageButton.setImageResource(R.drawable.stat_sys_battery_43);
                    } else if (battery_value < 57) {
                        mBatteryImageButton.setImageResource(R.drawable.stat_sys_battery_57);
                    } else if (battery_value < 71) {
                        mBatteryImageButton.setImageResource(R.drawable.stat_sys_battery_71);
                    } else if (battery_value < 85) {
                        mBatteryImageButton.setImageResource(R.drawable.stat_sys_battery_85);
                    } else {
                        mBatteryImageButton.setImageResource(R.drawable.stat_sys_battery_100);
                    }
                }
            }
        });
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
