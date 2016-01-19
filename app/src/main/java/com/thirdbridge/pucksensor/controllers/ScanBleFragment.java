package com.thirdbridge.pucksensor.controllers;

/**
 * Created by Christophe on 2015-10-28.
 */

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.thirdbridge.pucksensor.R;
import com.thirdbridge.pucksensor.ble.BleDeviceInfo;
import com.thirdbridge.pucksensor.utils.BaseFragment;
import com.thirdbridge.pucksensor.utils.ble_utils.CustomTimer;
import com.thirdbridge.pucksensor.utils.ble_utils.CustomTimerCallback;

import java.util.List;


public class ScanBleFragment extends BaseFragment {

    private static final String TAG = ScanBleFragment.class.getSimpleName();
    private final int SCAN_TIMEOUT = 10; // Seconds
    private final int CONNECT_TIMEOUT = 10; // Seconds

    private DeviceListAdapter mDeviceAdapter = null;
    private TextView mEmptyMsg;
    private TextView mStatus;
    private Button mBtnScan = null;
    private ListView mDeviceListView = null;
    private ProgressBar mProgressBar;

    private CustomTimer mScanTimer = null;
    private CustomTimer mConnectTimer = null;
    @SuppressWarnings("unused")
    private CustomTimer mStatusTimer;
    private Context mContext;

    public static ScanBleFragment newInstance(){
        return new ScanBleFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.i(TAG, "onCreateView");

        View view = inflater.inflate(R.layout.fragment_scan, container, false);

        mContext = getController().getApplicationContext();

        mStatus = (TextView) view.findViewById(R.id.status);
        mBtnScan = (Button) view.findViewById(R.id.btn_scan);
        mDeviceListView = (ListView) view.findViewById(R.id.device_list);
        mDeviceListView.setClickable(true);
        mDeviceListView.setOnItemClickListener(mDeviceClickListener);
        mEmptyMsg = (TextView)view.findViewById(R.id.no_device);

        // Progress bar to use during scan and connection
        mProgressBar = (ProgressBar) view.findViewById(R.id.pb_busy);
        mProgressBar.setMax(SCAN_TIMEOUT);

        // Alert parent activity
        getController().onScanViewReady(view);

        return view;
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "onDestroy");
        super.onDestroy();
    }

    public void setStatus(String txt) {
        mStatus.setText(txt);
        mStatus.setTextAppearance(mContext, R.style.statusStyle_Success);
    }

    public void setStatus(String txt, int duration) {
        setStatus(txt);
        mStatusTimer = new CustomTimer(null, duration, mClearStatusCallback);
    }

    public void setError(String txt) {
        setBusy(false);
        stopTimers();
        mStatus.setText(txt);
        mStatus.setTextAppearance(mContext, R.style.statusStyle_Failure);
    }

    public void notifyDataSetChanged() {
        List<BleDeviceInfo> deviceList = getController().getDeviceInfoList();
        if (mDeviceAdapter == null) {
            mDeviceAdapter = new DeviceListAdapter(getController(),deviceList);
        }
        mDeviceListView.setAdapter(mDeviceAdapter);
        mDeviceAdapter.notifyDataSetChanged();
        if (deviceList.size() > 0) {
            mEmptyMsg.setVisibility(View.GONE);
        } else {
            mEmptyMsg.setVisibility(View.VISIBLE);
        }
    }

    public void setBusy(boolean f) {
        if (mProgressBar == null)
            return;
        if (f) {
            mProgressBar.setVisibility(View.VISIBLE);
        } else {
            stopTimers();
            mProgressBar.setVisibility(View.GONE);
        }
    }

    public void updateGui(boolean scanning) {
        if (mBtnScan == null)
            return; // UI not ready
        setBusy(scanning);

        if (scanning) {
            mScanTimer = new CustomTimer(mProgressBar, SCAN_TIMEOUT, mPgScanCallback);
            mStatus.setTextAppearance(mContext, R.style.statusStyle_Busy);
            mBtnScan.setText("Stop");
            mStatus.setText("Scanning...");
            mEmptyMsg.setText(R.string.nodevice);
            getController().updateGuiState();
        } else {
            // Indicate that scanning has stopped
            mStatus.setTextAppearance(mContext, R.style.statusStyle_Success);
            mBtnScan.setText("Scan");
            mEmptyMsg.setText(R.string.scan_advice);
            getController().setProgressBarIndeterminateVisibility(false);
            mDeviceAdapter.notifyDataSetChanged();
        }
    }

    public void setDeviceConnected(int deviceIntex, boolean connected){
        int viewCount = mDeviceListView.getChildCount();

        for(int i=0; i<viewCount; i++){
            View view = mDeviceListView.getChildAt(i);

            if(view.getTag().equals(deviceIntex)){
                Button connectButton = (Button) view.findViewById(R.id.btnConnect);

                if(connected)
                    connectButton.setText("Disconnect");
                else{
                    connectButton.setText("Connect");
                }
            }
        }
    }

    // Listener for device list
    private OnItemClickListener mDeviceClickListener = new OnItemClickListener() {
        public void onItemClick(AdapterView<?> parent, View view, int pos, long id) {
            mConnectTimer = new CustomTimer(mProgressBar, CONNECT_TIMEOUT, mPgConnectCallback);
            getController().onDeviceClick(pos);
        }
    };

    // Listener for progress timer expiration
    private CustomTimerCallback mPgScanCallback = new CustomTimerCallback() {
        public void onTimeout() {
            getController().onScanTimeout();
        }

        public void onTick(int i) {
        }
    };

    // Listener for connect/disconnect expiration
    private CustomTimerCallback mPgConnectCallback = new CustomTimerCallback() {
        public void onTimeout() {
            getController().onConnectTimeout();
        }

        public void onTick(int i) {
        }
    };

    // Listener for connect/disconnect expiration
    private CustomTimerCallback mClearStatusCallback = new CustomTimerCallback() {
        public void onTimeout() {
            getController().runOnUiThread(new Runnable() {
                public void run() {
                    setStatus("");
                }
            });
            mStatusTimer = null;
        }

        public void onTick(int i) {
        }
    };

    private void stopTimers() {
        if (mScanTimer != null) {
            mScanTimer.stop();
            mScanTimer = null;
        }
        if (mConnectTimer != null) {
            mConnectTimer.stop();
            mConnectTimer = null;
        }
    }

    //
    // CLASS DeviceAdapter: handle device list
    //
    class DeviceListAdapter extends BaseAdapter {
        private List<BleDeviceInfo> mDevices;
        private LayoutInflater mInflater;
        private Button mConnectButton;

        public DeviceListAdapter(Context context, List<BleDeviceInfo> devices) {
            mInflater = LayoutInflater.from(context);
            mDevices = devices;
        }

        public int getCount() {
            return mDevices.size();
        }

        public Object getItem(int position) {
            return mDevices.get(position);
        }

        public long getItemId(int position) {
            return position;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            ViewGroup vg;

            if (convertView != null) {
                vg = (ViewGroup) convertView;
            } else {
                vg = (ViewGroup) mInflater.inflate(R.layout.element_device, null);
                mConnectButton = (Button) vg.findViewById(R.id.btnConnect);
                vg.setTag(position);
            }

            BleDeviceInfo deviceInfo = mDevices.get(position);
            BluetoothDevice device = deviceInfo.getBluetoothDevice();
            int rssi = deviceInfo.getRssi();
            String descr = device.getName() + "\n" + device.getAddress() + "\nRssi: " + rssi + " dBm";
            ((TextView) vg.findViewById(R.id.descr)).setText(descr);

            if(device.getName() != null && device.getName().equals("Physics Sensor"))
                ((ImageView) vg.findViewById(R.id.device_image)).setImageDrawable(ContextCompat.getDrawable(getContext(),R.drawable.puck));

            return vg;
        }

    }

}

