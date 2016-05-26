package com.thirdbridge.pucksensor.controllers;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckedTextView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.PopupWindow;
import android.widget.Spinner;
import android.widget.Toast;

import com.thirdbridge.pucksensor.R;
import com.thirdbridge.pucksensor.ble.BluetoothLeService;
import com.thirdbridge.pucksensor.ble.GattInfo;
import com.thirdbridge.pucksensor.ble.Sensor;
import com.thirdbridge.pucksensor.ble.SensorDetails;
import com.thirdbridge.pucksensor.models.User;
import com.thirdbridge.pucksensor.utils.BaseFragment;
import com.thirdbridge.pucksensor.utils.IO;
import com.thirdbridge.pucksensor.utils.JSONParser;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Created by Christophe on 2015-10-14.
 * Modified by Jayson Dalpe since 2016-01-26
 */
public class HomeFragment extends BaseFragment {
    private static String TAG = HomeFragment.class.getSimpleName();
    private final static String PREF_USER = "PREF_USER";

    // Saving local instance
    SharedPreferences mSettings;

    private Button mShotTextButton;
    private Button mStickHandlingButton;
    private Button mCalibrationButton;
    private Button mFreeRoamingButton;
    private Button mAnalysisButton;
    private ImageButton mNewUserImageButton;
    private Spinner mUserSpinner;
    private Button mSaveUserButton;
    private Button mCancelUserButton;
    private EditText mFirstNameEditText;
    private EditText mLastNameEditText;

    private List<User> mUsers;
    private List<String> mUserNames;
    private ArrayAdapter<User> mSpinnerAdapter;

    // Bluetooth
    // BLE
    private boolean mSensorReady = false;
    private int mRotationDataSetIndex;
    private BluetoothLeService mBtLeService = null;
    private BluetoothGatt mBtGatt = null;
    private List<BluetoothGattService> mServiceList = null;
    private static final int GATT_TIMEOUT = 100; // milliseconds
    private boolean mServicesRdy = false;
    private boolean mIsReceiving = false;
    private boolean mWasInitialise = false;
    private boolean mCommunicationDone = false;

    // SensorTag
    private List<Sensor> mEnabledSensors = new ArrayList<Sensor>();
    private BluetoothGattService mOadService = null;
    private BluetoothGattService mConnControlService = null;


    //Battery
    private int mBatteryLevel = -1;

    //Interface for other fragment
    public interface  BluetoothListener {
        void onBluetoothCommand(byte[] values);
    }

    public void addBluetoothListener(BluetoothListener listener) {

        mListener.add(listener);
    }

    public void removeBluetoothListener(BluetoothListener listener) {
        mListener.remove(listener);
    }

    private List<BluetoothListener> mListener = new ArrayList<BluetoothListener>();

    boolean mPause = false;
    Thread mBackgroundThread;
    Runnable mRun = new Runnable() {
        @Override
        public void run() {
            while(true) {
                try {
                    Thread.sleep(300);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                if (getController().isBleDeviceConnected() && !mWasInitialise) {
                    mWasInitialise = true;
                    mServicesRdy = false;
                    initializeBluetooth();
                }

                if (!getController().isBleDeviceConnected()){
                    if (mBatteryLevel != -1) {
                        mCommunicationDone = false;
                        mBatteryLevel = -1;
                        ActionBarFragment.getInstance().updateBattery(mBatteryLevel);
                    }
                    mWasInitialise = false;
                }

                if (mPause) {
                    break;
                }
            }
        }
    };

    private static HomeFragment mFragment = null;

    public static HomeFragment getInstance(){
        if (mFragment == null) {
            mFragment = new HomeFragment();
        }
        return mFragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, final ViewGroup container, Bundle savedInstanceState) {

        super.onCreateView(inflater, container, savedInstanceState);

        View v = inflater.inflate(R.layout.fragment_home, container, false);

        mShotTextButton = (Button) v.findViewById(R.id.shot_test_button);
        mStickHandlingButton = (Button) v.findViewById(R.id.handling_test_button);
        mCalibrationButton = (Button) v.findViewById(R.id.calibration_button);
        mFreeRoamingButton = (Button) v.findViewById(R.id.roaming_test_button);
        mAnalysisButton = (Button) v.findViewById(R.id.analysis_test_button);
        mNewUserImageButton = (ImageButton) v.findViewById(R.id.new_user_image_button);
        mUserSpinner = (Spinner) v.findViewById(R.id.user_spinner);

        mSettings = getActivity().getSharedPreferences("StatPuck", 0);
        populateSpinner();

        mUserSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                if (mUsers.get(position) != null) {
                    getController().setCurrentUsername(mUsers.get(position).getName());
                    SharedPreferences.Editor editor = mSettings.edit();
                    editor.putString(PREF_USER, mUsers.get(position).getId());
                    editor.commit();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
                getController().setCurrentUsername("");
            }

        });

        final PopupWindow popupWindow = inflateNewUserPopup(container);

        mNewUserImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ((EditText) popupWindow.getContentView().findViewById(R.id.first_name)).setText("");
                ((EditText) popupWindow.getContentView().findViewById(R.id.last_name)).setText("");
                popupWindow.setFocusable(true);
                popupWindow.update();
                popupWindow.showAtLocation(container, Gravity.TOP, 0, 200);
            }
        });

        mShotTextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mCommunicationDone) {
                    if (mUserSpinner.getSelectedItemPosition() != -1) {
                        if (getController().isBleDeviceConnected()) {
                            getController().gotoShotStats((User) mUserSpinner.getSelectedItem());
                        }
                    }
                } else {
                    if (getController().isBleDeviceConnected())
                        Toast.makeText(getActivity(), "Device is connecting...", Toast.LENGTH_LONG).show();
                    else
                        Toast.makeText(getActivity(), "No device connected", Toast.LENGTH_LONG).show();
                }
            }
        });

        mStickHandlingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mCommunicationDone) {
                    if (mUserSpinner.getSelectedItemPosition() != -1) {
                        if (getController().isBleDeviceConnected()) {
                            getController().gotoStickHand((User) mUserSpinner.getSelectedItem());
                        }
                    }
                } else {
                    if (getController().isBleDeviceConnected())
                        Toast.makeText(getActivity(), "Device is connecting...", Toast.LENGTH_LONG).show();
                    else
                        Toast.makeText(getActivity(), "No device connected", Toast.LENGTH_LONG).show();
                }
            }
        });

        mCalibrationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mCommunicationDone) {
                    if (mUserSpinner.getSelectedItemPosition() != -1) {
                        if (getController().isBleDeviceConnected()) {
                            getController().gotoCalibration();
                        }
                    }
                } else {
                    if (getController().isBleDeviceConnected())
                        Toast.makeText(getActivity(), "Device is connecting...", Toast.LENGTH_LONG).show();
                    else
                        Toast.makeText(getActivity(), "No device connected", Toast.LENGTH_LONG).show();
                }
            }
        });

        mFreeRoamingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mCommunicationDone) {
                    if (mUserSpinner.getSelectedItemPosition() != -1) {
                        if (getController().isBleDeviceConnected()) {
                            getController().gotoFreeRoaming();
                        }
                    }
                } else {
                    if (getController().isBleDeviceConnected())
                        Toast.makeText(getActivity(), "Device is connecting...", Toast.LENGTH_LONG).show();
                    else
                        Toast.makeText(getActivity(), "No device connected", Toast.LENGTH_LONG).show();
                }
            }
        });

        mAnalysisButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mUserSpinner.getSelectedItemPosition() != -1) {
                    getController().gotoAnalysis((User) mUserSpinner.getSelectedItem());
                }
            }
        });

/*
        WebSettings wSettings;
        wSettings = mWebView.getSettings();
        wSettings.setJavaScriptEnabled(true);
        mWebView.loadUrl("file:///storage/emulated/0/Statpuck/index.html");
*/

        return v;
    }


    private void populateSpinner(){
        mUsers = new ArrayList<User>();

        File root = new File(this.getContext().getFilesDir(), "users");
        if (!root.exists()) {
            root.mkdirs();
            return;
        }

        File[] users = root.listFiles();

        String prefUser = mSettings.getString(PREF_USER, "");
        int id = 0;
        for(int i=0; i<users.length; i++){
            User newUser = User.depackageForm(IO.loadFile(users[i]));
            mUsers.add(newUser);
            if (prefUser.trim().equalsIgnoreCase(newUser.getId().trim())) {
                id = i;
            }
        }

        mSpinnerAdapter = new UserArrayAdapter(getController(), R.layout.spinner_dropdown_item, mUsers);
        mUserSpinner.setAdapter(mSpinnerAdapter);
        mUserSpinner.setSelection(id);
    }

    private PopupWindow inflateNewUserPopup(ViewGroup container){

        LayoutInflater layoutInflater
                = (LayoutInflater)getController().getBaseContext()
                .getSystemService(getController().LAYOUT_INFLATER_SERVICE);

        final View popupView = layoutInflater.inflate(R.layout.new_user_popup, container, false);

        mSaveUserButton = (Button) popupView.findViewById(R.id.save_user_button);
        mCancelUserButton = (Button) popupView.findViewById(R.id.cancel_button);
        mFirstNameEditText = (EditText) popupView.findViewById(R.id.first_name);
        mLastNameEditText = (EditText) popupView.findViewById(R.id.last_name);

        final PopupWindow popupWindow = new PopupWindow(
                popupView,
                1000,
                ViewGroup.LayoutParams.WRAP_CONTENT);

        mSaveUserButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!mFirstNameEditText.getText().toString().matches("") || !mLastNameEditText.getText().toString().matches("")) {
                    User newUser = new User(UUID.randomUUID().toString(), mFirstNameEditText.getText().toString(), mLastNameEditText.getText().toString());
                    IO.saveFile(newUser.packageForm(), new File(getContext().getFilesDir().getAbsolutePath() +  "/users/" + newUser.getId() + ".user"));
                    mUsers.add(newUser);
                    populateSpinner();
                    mSpinnerAdapter.notifyDataSetChanged();
                    mUserSpinner.setSelection(mSpinnerAdapter.getCount() - 1);
                    popupWindow.dismiss();
                    getController().setCurrentUsername(mFirstNameEditText.getText().toString() + " " + mLastNameEditText.getText().toString());

                    SharedPreferences.Editor editor = mSettings.edit();
                    editor.putString(PREF_USER, newUser.getId());
                    editor.commit();
                }
            }
        });

        mCancelUserButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                popupWindow.dismiss();
            }
        });

        return popupWindow;
    }


    // Bluetooth connexion
    @Override
    public void onResume() {
        Log.d(TAG, "onResume");
        super.onResume();
        if (!mIsReceiving) {
            getController().registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
            mIsReceiving = true;
        }
        mPause = false;
        mBackgroundThread = new Thread(mRun);
        mBackgroundThread.start();
    }

    @Override
    public void onPause() {
        Log.d(TAG, "onPause");
        super.onPause();
        if (mIsReceiving) {
            getController().unregisterReceiver(mGattUpdateReceiver);
            mIsReceiving = false;
        }

        mPause = true;
        try {
            mBackgroundThread.join();
        } catch (Exception e) {

        }
    }

    public boolean IsBluetoothReady() {
        return mSensorReady;
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter fi = new IntentFilter();
        fi.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        fi.addAction(BluetoothLeService.ACTION_DATA_NOTIFY);
        fi.addAction(BluetoothLeService.ACTION_DATA_WRITE);
        fi.addAction(BluetoothLeService.ACTION_DATA_READ);
        return fi;
    }

    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            int status = intent.getIntExtra(BluetoothLeService.EXTRA_STATUS, BluetoothGatt.GATT_SUCCESS);

            if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    displayServices();
                    checkOad();
                } else {
                    Toast.makeText(getController().getApplication(), "Service discovery failed", Toast.LENGTH_LONG).show();
                    return;
                }
            } else if (BluetoothLeService.ACTION_DATA_NOTIFY.equals(action)) {
                // Notification
                byte[] value = intent.getByteArrayExtra(BluetoothLeService.EXTRA_DATA);
                String uuidStr = intent.getStringExtra(BluetoothLeService.EXTRA_UUID);
                mCommunicationDone = true;
                if ((value[1] & 0xFF) != mBatteryLevel) {
                    mBatteryLevel = (value[1] & 0xFF);
                    ActionBarFragment.getInstance().updateBattery(mBatteryLevel);
                }
                for (BluetoothListener listener: mListener) {
                    if (listener != null) {
                        listener.onBluetoothCommand(value);
                    }
                }
            } else if (BluetoothLeService.ACTION_DATA_WRITE.equals(action)) {
                // Data written
                String uuidStr = intent.getStringExtra(BluetoothLeService.EXTRA_UUID);

                onCharacteristicWrite(uuidStr, status);
            } else if (BluetoothLeService.ACTION_DATA_READ.equals(action)) {
                // Data read
                String uuidStr = intent.getStringExtra(BluetoothLeService.EXTRA_UUID);
                byte[] value = intent.getByteArrayExtra(BluetoothLeService.EXTRA_DATA);
            }

            if (status != BluetoothGatt.GATT_SUCCESS) {
                setError("GATT error code: " + status);
            }
        }
    };

    private void onCharacteristicWrite(String uuidStr, int status) {
        Log.d(TAG, "onCharacteristicWrite: " + uuidStr);
        boolean begin = mBtGatt.beginReliableWrite();
        Log.i(TAG, "Begin successfull: " + begin);
        if (begin) {
            Log.i(TAG, "Execute successfull: " + mBtGatt.executeReliableWrite());
        }
    }

    private void displayServices() {

        mServicesRdy = true;

        try {
            mServiceList = mBtLeService.getSupportedGattServices();
        } catch (Exception e) {
            e.printStackTrace();
            mServicesRdy = false;
        }

        // Characteristics descriptor readout done
        if (mServicesRdy) {
            setStatus("Service discovery complete");
            enableSensors(true);
            enableNotifications(true);
        } else {
            setError("Failed to read services");
        }
    }

    private void enableSensors(boolean enable) {
        for (Sensor sensor : mEnabledSensors) {
            Log.i(TAG, "Going to enable sensor " + sensor.getService().toString());
            UUID confUuid = sensor.getConfig();

            // Skip keys
            if (confUuid == null)
                break;

            for (int i = 0; i < mBtGatt.getServices().size(); i++) {
                Log.i(TAG, String.format("Going to display service %d %s", i, mBtGatt.getServices().get(i).getUuid()));
            }

            mSensorReady = true;
        }
    }

    private void enableNotifications(boolean enable) {
        for (Sensor sensor : mEnabledSensors) {
            UUID servUuid = sensor.getService();
            UUID dataUuid = sensor.getData();
            BluetoothGattService serv = mBtGatt.getService(servUuid);

            if(serv != null) {
                BluetoothGattCharacteristic charac = serv.getCharacteristic(dataUuid);

                mBtLeService.setCharacteristicNotification(charac, enable);
                mBtLeService.waitIdle(GATT_TIMEOUT);
            }
            else{
                mSensorReady = false;
            }
        }
    }

    private void initializeBluetooth() {
        // BLE
        mBtLeService = BluetoothLeService.getInstance();
        mServiceList = new ArrayList<BluetoothGattService>();

        // Initialize sensor list
        updateSensorList();

        if (!mIsReceiving) {
            mIsReceiving = true;
            getController().registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        }

        // Create GATT object
        mBtGatt = BluetoothLeService.getBtGatt();

        // Start service discovery
        if (!mServicesRdy && mBtGatt != null) {
            if (mBtLeService.getNumServices() == 0)
                discoverServices();
            else
                displayServices();
        }

    }

    private void checkOad() {
        // Check if OAD is supported (needs OAD and Connection Control service)
        mOadService = null;
        mConnControlService = null;

        for (int i = 0; i < mServiceList.size() && (mOadService == null || mConnControlService == null); i++) {
            BluetoothGattService srv = mServiceList.get(i);
            if (srv.getUuid().equals(GattInfo.OAD_SERVICE_UUID)) {
                mOadService = srv;
            }
            if (srv.getUuid().equals(GattInfo.CC_SERVICE_UUID)) {
                mConnControlService = srv;
            }
        }
    }

    private void discoverServices() {
        if (mBtGatt.discoverServices()) {
            mServiceList.clear();
            setStatus("Service discovery started");
        } else {
            setError("Service discovery start failed");
        }
    }

    boolean isEnabledByPrefs(final Sensor sensor) {
        String preferenceKeyString = "pref_" + sensor.name().toLowerCase(Locale.ENGLISH) + "_on";

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getController());

        Boolean defaultValue = true;
        return prefs.getBoolean(preferenceKeyString, defaultValue);
    }

    //
    // Application implementation
    //
    private void updateSensorList() {
        mEnabledSensors.clear();

        for (int i = 0; i < Sensor.SENSOR_LIST.length; i++) {
            Sensor sensor = Sensor.SENSOR_LIST[i];
            if (isEnabledByPrefs(sensor)) {
                mEnabledSensors.add(sensor);
            }
        }
    }

    public void writeBLE(byte[] values) throws Exception{
        if (mBtGatt != null && mSensorReady) {
            Log.i(TAG, "Everything ready");
            if (mBtGatt.getService(SensorDetails.UUID_PUCK_ACC_SERV) == null) {
                throw new Exception();
            }
            BluetoothGattCharacteristic carac = mBtGatt.getService(SensorDetails.UUID_PUCK_ACC_SERV).getCharacteristic(SensorDetails.UUID_PUCK_WRITE);
            if (carac == null) {
                throw new Exception();
            }
            Log.i(TAG, "Service: " + carac + " " + carac.toString());
            carac.setValue(values);
            Log.i(TAG, "Sending successfull: " + mBtGatt.writeCharacteristic(carac));

        } else {
            throw new Exception();
        }
    }


    private void setError(String txt) {
        Log.i(TAG, String.format("GOT ERROR %s", txt));
    }

    private void setStatus(String txt) {
        Log.i(TAG, String.format("GOT STATUS %s", txt));
    }
}
