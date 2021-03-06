package com.thirdbridge.pucksensor.activity;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.IBinder;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.google.android.youtube.player.YouTubeInitializationResult;
import com.google.android.youtube.player.YouTubePlayer;
import com.google.android.youtube.player.YouTubePlayerFragment;
import com.thirdbridge.pucksensor.R;
import com.thirdbridge.pucksensor.ble.BleDeviceInfo;
import com.thirdbridge.pucksensor.ble.BluetoothLeService;
import com.thirdbridge.pucksensor.controllers.ActionBarFragment;
import com.thirdbridge.pucksensor.controllers.AnalysisFragment;
import com.thirdbridge.pucksensor.controllers.CalibrationFragment;
import com.thirdbridge.pucksensor.controllers.FreeRoamingFragment;
import com.thirdbridge.pucksensor.controllers.HistoryFragment;
import com.thirdbridge.pucksensor.controllers.HomeFragment;
import com.thirdbridge.pucksensor.controllers.ScanBleFragment;
import com.thirdbridge.pucksensor.controllers.ShotStatsFragment;
import com.thirdbridge.pucksensor.controllers.StickHandlingFragment;
import com.thirdbridge.pucksensor.models.User;
import com.thirdbridge.pucksensor.utils.Constants;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Christophe on 2015-10-14.
 * Modified by Jayson Dalpé since 2016-02-01.
 */
public class ContentActivity extends FragmentActivity implements YouTubePlayer.OnInitializedListener{

	private static final String DEVELOPER_KEY = "AIzaSyCcshqZXdH-AJu3EePHMOv4QfwkInXIpVQ";

	private static String TAG = ContentActivity.class.getSimpleName();


	// TODO Need better management
	private enum VisibleFragment {BLE, HOME, HISTORY, STATS, STICK_HAND, YOUTUBE, CALIBRATION, FREE_ROAMING, ANALYSIS}

	private VisibleFragment mVisibleFragment;
	private Constants.SelectedTest mSelectedTest;

	private FrameLayout mActionBarFragmentContainer;
	private ActionBarFragment mActionBarFragment;

	private FrameLayout mHomeFragmentContainer;
	private HomeFragment mHomeFragment;

	private FrameLayout mScanFragmentContainer;
	private ScanBleFragment mScanFragment;

	private FrameLayout mHistoryFragmentContainer;
	private HistoryFragment mHistoryFragment;

	private FrameLayout mStatsFragmentContainer;
	private ShotStatsFragment mShotStatsFragment;

	private FrameLayout mStickHandFragmentContainer;
	private StickHandlingFragment mStickHandFragment;

	private FrameLayout mYoutubeFragmentContainer;
	private YouTubePlayerFragment mYoutubeFragment;
    private YouTubePlayer mPlayer;
	private String mVideo;
    private boolean mOnce = false;

	private FrameLayout mCalibrationFragmentContainer;
	private CalibrationFragment mCalibrationFragment;

	private FrameLayout mFreeRoamingFragmentContainer;
	private FreeRoamingFragment mFreeRoamingFragment;

	private FrameLayout mAnalysisFragmentContainer;
	private AnalysisFragment mAnalysisFragment;

    @Override
    public void onInitializationSuccess(YouTubePlayer.Provider provider, YouTubePlayer youTubePlayer, boolean b) {
        Log.i(TAG, "SUCCEED!");
        mPlayer = youTubePlayer;
        mPlayer.loadVideo(mVideo);

    }

    @Override
    public void onInitializationFailure(YouTubePlayer.Provider provider, YouTubeInitializationResult youTubeInitializationResult) {
        Log.wtf(TAG, "FAILLLLED!!!!");
    }

	private User mSelectedUser;

	// BLE management
	private boolean mBleDeviceConnected = false;
	private boolean mBleSupported = true;
	private boolean mScanning = false;
	private int mNumDevs = 0;
	private int mConnIndex = NO_DEVICE;
	private List<BleDeviceInfo> mDeviceInfoList;
	private static BluetoothManager mBluetoothManager;
	private BluetoothAdapter mBtAdapter = null;
	private BluetoothDevice mBluetoothDevice = null;
	private BluetoothLeService mBluetoothLeService = null;
	private BleConnectionTerminatedReceiver mBleConnectionTerminatedReceiver;
	private IntentFilter mFilter;
	private String[] mDeviceFilter = {};//null;

	// GUI
	private Intent mDeviceIntent;
	private static final int STATUS_DURATION = 5;

	// Requests to other activities
	private static final int REQ_ENABLE_BT = 0;
	private static final int REQ_DEVICE_ACT = 1;

	// Housekeeping
	private static final int NO_DEVICE = -1;
	private boolean mInitialised = false;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		String[] permi = {"android.permission.ACCESS_FINE_LOCATION", "android.permission.ACCESS_COARSE_LOCATION"};
		requestPermissions(permi, 0);
		setContentView(R.layout.fragment_main_container);

		initializeScanFragment();
		initializeHomeFragment();
		initializeActionBarFragment();

		mVisibleFragment = VisibleFragment.BLE;

		initializeBleConnection();
	}


	@Override
	protected void onResume() {
		Log.d(TAG, "onResume");
		super.onResume();
		if (mBleConnectionTerminatedReceiver == null)
			mBleConnectionTerminatedReceiver = new BleConnectionTerminatedReceiver();
		IntentFilter intentFilter = new IntentFilter(Constants.BLE_CONNECTION_TERMINATED);
		registerReceiver(mBleConnectionTerminatedReceiver, intentFilter);
	}

	@Override
	protected void onDestroy() {
		Log.d(TAG, "Destroy");
		super.onDestroy();
		if (mBluetoothLeService != null) {
			scanLeDevice(false);
			mBluetoothLeService.close();
			unregisterReceiver(mReceiver);
            unbindService(mServiceConnection);
			mBluetoothLeService = null;
		}

		if (mBleConnectionTerminatedReceiver != null)
			unregisterReceiver(mBleConnectionTerminatedReceiver);
	}


	private void initializeActionBarFragment() {
		mActionBarFragmentContainer = (FrameLayout) findViewById(R.id.action_bar_fragment_container);
		mActionBarFragment = ActionBarFragment.getInstance();

		FragmentManager fm = getSupportFragmentManager();
		FragmentTransaction ft = fm.beginTransaction();
		ft.replace(R.id.action_bar_fragment_container, mActionBarFragment);
		ft.commit();
    }

	private void initializeScanFragment() {
		mScanFragmentContainer = (FrameLayout) findViewById(R.id.scan_fragment_container);
		mScanFragment = ScanBleFragment.newInstance();

		FragmentManager fm = getSupportFragmentManager();
		FragmentTransaction ft = fm.beginTransaction();
		ft.replace(R.id.scan_fragment_container, mScanFragment);
		ft.commit();
	}

	private void initializeHomeFragment() {
		mHomeFragmentContainer = (FrameLayout) findViewById(R.id.home_fragment_container);
		mHomeFragment = HomeFragment.getInstance();

		FragmentManager fm = getSupportFragmentManager();
		FragmentTransaction ft = fm.beginTransaction();
		ft.replace(R.id.home_fragment_container, mHomeFragment);
		ft.commit();
	}

	private void initializeHistoryFragment() {
		mHistoryFragmentContainer = (FrameLayout) findViewById(R.id.history_fragment_container);
		mHistoryFragment = HistoryFragment.newInstance();

		FragmentManager fm = getSupportFragmentManager();
		FragmentTransaction ft = fm.beginTransaction();
		ft.replace(R.id.history_fragment_container, mHistoryFragment);
		ft.commit();
	}

	private void initializeShotStatsFragment(User user) {
		mStatsFragmentContainer = (FrameLayout) findViewById(R.id.stats_fragment_container);
		mShotStatsFragment = ShotStatsFragment.newInstance(user);

		FragmentManager fm = getSupportFragmentManager();
		FragmentTransaction ft = fm.beginTransaction();
		ft.replace(R.id.stats_fragment_container, mShotStatsFragment);
		ft.commit();
	}

	private void initializeStickHandFragment(User user) {
		mStickHandFragmentContainer = (FrameLayout) findViewById(R.id.stick_hand_fragment_container);
		mStickHandFragment = StickHandlingFragment.newInstance(user);

		FragmentManager fm = getSupportFragmentManager();
		FragmentTransaction ft = fm.beginTransaction();
		ft.replace(R.id.stick_hand_fragment_container, mStickHandFragment);
		ft.commit();
	}

	private void initializeCalibrationFragment() {
		mCalibrationFragmentContainer = (FrameLayout) findViewById(R.id.calibration_fragment_container);
		mCalibrationFragment = CalibrationFragment.newInstance();

		FragmentManager fm = getSupportFragmentManager();
		FragmentTransaction ft = fm.beginTransaction();
		ft.replace(R.id.calibration_fragment_container, mCalibrationFragment);
		ft.commit();
	}

	private void initializeFreeRoamingFragment() {
		mFreeRoamingFragmentContainer = (FrameLayout) findViewById(R.id.freeroaming_fragment_container);
		mFreeRoamingFragment = FreeRoamingFragment.newInstance();

		FragmentManager fm = getSupportFragmentManager();
		FragmentTransaction ft = fm.beginTransaction();
		ft.replace(R.id.freeroaming_fragment_container, mFreeRoamingFragment);
		ft.commit();
	}

	private void initializeAnalysisFragment(User user) {
		mAnalysisFragmentContainer = (FrameLayout) findViewById(R.id.analysis_fragment_container);
		mAnalysisFragment = AnalysisFragment.newInstance(user);

		FragmentManager fm = getSupportFragmentManager();
		FragmentTransaction ft = fm.beginTransaction();
		ft.replace(R.id.analysis_fragment_container, mAnalysisFragment);
		ft.commit();
	}

	private void initializeYoutubeFragment(String video) {
		mYoutubeFragmentContainer = (FrameLayout) findViewById(R.id.youtube_fragment_container);
		mYoutubeFragment = (YouTubePlayerFragment) getFragmentManager().findFragmentById(R.id.youtube_fragment);



        mVideo = video;
        if (!mOnce) {
            mYoutubeFragment.initialize(DEVELOPER_KEY, this);
            Button btn = (Button) findViewById(R.id.youtube_cancel);
            btn.setText(R.string.back);
            btn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    backfromYoutube();
                }
            });

            mOnce = true;
        } else {
            mPlayer.loadVideo(mVideo);
        }
	}



	public void reloadShotStatsFragment(){
		final FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
		ft.detach(mShotStatsFragment);
		ft.attach(mShotStatsFragment);
		ft.commit();
	}

	public void reloadStickHandFragment(){
		final FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
		ft.detach(mStickHandFragment);
		ft.attach(mStickHandFragment);
		ft.commit();
	}


	public void gotoBleScan() {

		mScanFragmentContainer.setVisibility(View.VISIBLE);

		if (mHomeFragmentContainer != null)
			mHomeFragmentContainer.setVisibility(View.GONE);

		if (mHistoryFragmentContainer != null)
			mHistoryFragmentContainer.setVisibility(View.GONE);

		if (mStatsFragmentContainer != null)
			mStatsFragmentContainer.setVisibility(View.GONE);

		if (mStickHandFragmentContainer != null)
			mStickHandFragmentContainer.setVisibility(View.GONE);

		if (mYoutubeFragmentContainer != null)
			mYoutubeFragmentContainer.setVisibility(View.GONE);
        if (mVisibleFragment == VisibleFragment.YOUTUBE) {
            backfromYoutube();
        }

		if (mCalibrationFragmentContainer != null)
			mCalibrationFragmentContainer.setVisibility(View.GONE);

		if (mFreeRoamingFragmentContainer != null)
			mFreeRoamingFragmentContainer.setVisibility(View.GONE);

		if (mAnalysisFragmentContainer != null)
			mAnalysisFragmentContainer.setVisibility(View.GONE);

		mActionBarFragment.setActionBarTitle("BLE Device Discovery");


		setVisibleFragment(VisibleFragment.BLE);
	}

	public void gotoHome() {
		mHomeFragmentContainer.setVisibility(View.VISIBLE);

		if (mScanFragmentContainer != null)
			mScanFragmentContainer.setVisibility(View.GONE);

		if (mHistoryFragmentContainer != null)
			mHistoryFragmentContainer.setVisibility(View.GONE);

		if (mStatsFragmentContainer != null)
			mStatsFragmentContainer.setVisibility(View.GONE);

		if (mStickHandFragmentContainer != null)
			mStickHandFragmentContainer.setVisibility(View.GONE);

        if (mYoutubeFragmentContainer != null)
            mYoutubeFragmentContainer.setVisibility(View.GONE);
        if (mVisibleFragment == VisibleFragment.YOUTUBE) {
            backfromYoutube();
        }

		if (mCalibrationFragmentContainer != null)
			mCalibrationFragmentContainer.setVisibility(View.GONE);

		if (mFreeRoamingFragmentContainer != null)
			mFreeRoamingFragmentContainer.setVisibility(View.GONE);

		if (mAnalysisFragmentContainer != null)
			mAnalysisFragmentContainer.setVisibility(View.GONE);

		mActionBarFragment.setActionBarTitle("Home");

		setVisibleFragment(VisibleFragment.HOME);
	}

	public void gotoHistory() {
		initializeHistoryFragment();
		mHomeFragmentContainer.setVisibility(View.GONE);

		if (mStatsFragmentContainer != null)
			mStatsFragmentContainer.setVisibility(View.GONE);

		mHistoryFragmentContainer.setVisibility(View.VISIBLE);

		if (mFreeRoamingFragmentContainer != null)
			mFreeRoamingFragmentContainer.setVisibility(View.GONE);

		mActionBarFragment.setActionBarTitle("Shot Test History");
		setVisibleFragment(VisibleFragment.HISTORY);
	}

	public void gotoShotStats(User user) {
		initializeShotStatsFragment(user);
		mHomeFragmentContainer.setVisibility(View.GONE);
		if(mHistoryFragmentContainer != null)
			mHistoryFragmentContainer.setVisibility(View.GONE);
		mStatsFragmentContainer.setVisibility(View.VISIBLE);
		setVisibleFragment(VisibleFragment.STATS);
		mActionBarFragment.setActionBarTitle("Shot Test Statistics");
	}

	public void gotoStickHand(User user) {
		initializeStickHandFragment(user);
		mHomeFragmentContainer.setVisibility(View.GONE);
		if(mHistoryFragmentContainer != null)
			mHistoryFragmentContainer.setVisibility(View.GONE);
		mStickHandFragmentContainer.setVisibility(View.VISIBLE);
		setVisibleFragment(VisibleFragment.STICK_HAND);
		mActionBarFragment.setActionBarTitle("Stick Handling");
	}

	public void gotoCalibration() {
		initializeCalibrationFragment();
		mHomeFragmentContainer.setVisibility(View.GONE);
		if(mHistoryFragmentContainer != null)
			mHistoryFragmentContainer.setVisibility(View.GONE);
		mCalibrationFragmentContainer.setVisibility(View.VISIBLE);
		setVisibleFragment(VisibleFragment.CALIBRATION);
		mActionBarFragment.setActionBarTitle("Calibration & Settings");
	}

    public void gotoFreeRoaming() {
        initializeFreeRoamingFragment();
        mHomeFragmentContainer.setVisibility(View.GONE);
        if(mHistoryFragmentContainer != null)
            mHistoryFragmentContainer.setVisibility(View.GONE);
        mFreeRoamingFragmentContainer.setVisibility(View.VISIBLE);
        setVisibleFragment(VisibleFragment.FREE_ROAMING);
        mActionBarFragment.setActionBarTitle("Freeroaming");
    }

	public void gotoAnalysis(User user) {
		initializeAnalysisFragment(user);
		mHomeFragmentContainer.setVisibility(View.GONE);
		if(mHistoryFragmentContainer != null)
			mHistoryFragmentContainer.setVisibility(View.GONE);
		mAnalysisFragmentContainer.setVisibility(View.VISIBLE);
		setVisibleFragment(VisibleFragment.ANALYSIS);
		mActionBarFragment.setActionBarTitle("Analysis");
	}

	public void backfromYoutube() {
		mYoutubeFragmentContainer.setVisibility(View.GONE);
        mPlayer.pause();
	}

	public void gotoYoutube(String video) {
		initializeYoutubeFragment(video);
		mYoutubeFragmentContainer.setVisibility(View.VISIBLE);
	}


	public void setVisibleFragment(VisibleFragment visibleFragment) {
		this.mVisibleFragment = visibleFragment;
	}

	public void setCurrentUsername(String username) {
		mActionBarFragment.setActionBarUsername(username);
	}

	@Override
	public void onBackPressed() {

		switch (mVisibleFragment) {
			case BLE:
				//go to device home screen
				Intent intent = new Intent(Intent.ACTION_MAIN);
				intent.addCategory(Intent.CATEGORY_HOME);
				intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				startActivity(intent);
			case HOME:
				gotoBleScan();
				break;
			case HISTORY:
				gotoHome();
				break;
			case STATS:
				gotoHome();
				break;
			case STICK_HAND:
				gotoHome();
				break;
			case CALIBRATION:
				gotoHome();
				break;
			case ANALYSIS:
				gotoHome();
				break;
            case YOUTUBE:
                backfromYoutube();
			default:
				break;
		}
	}


	// ////////////////////////////////////////////////////////////////////////////////////////////////
	//
	// BLE related methods
	// ////////////////////////////////////////////////////////////////////////////////////////////////

	private void initializeBleConnection() {

		// Use this check to determine whether BLE is supported on the device. Then
		// you can selectively disable BLE-related features.
		if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
			Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_LONG).show();
			mBleSupported = false;
		}

		// Initializes a Bluetooth adapter. For API level 18 and above, get a
		// reference to BluetoothAdapter through BluetoothManager.
		mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
		mBtAdapter = mBluetoothManager.getAdapter();

		// Checks if Bluetooth is supported on the device.
		if (mBtAdapter == null) {
			Toast.makeText(this, R.string.bt_not_supported, Toast.LENGTH_LONG).show();
			mBleSupported = false;
		}

		// Initialize device list container and device filter
		mDeviceInfoList = new ArrayList<BleDeviceInfo>();
		Resources res = getResources();
		mDeviceFilter = res.getStringArray(R.array.device_filter);


		// Register the BroadcastReceiver
		mFilter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
		mFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
		mFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
	}


	public void onScanViewReady(View view) {
		// Initial state of widgets
		updateGuiState();

		if (!mInitialised) {
			// Broadcast receiver
			registerReceiver(mReceiver, mFilter);

			if (mBtAdapter.isEnabled()) {
				// Start straight away
				startBluetoothLeService();
			} else {
				// Request BT adapter to be turned on
				Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
				startActivityForResult(enableIntent, REQ_ENABLE_BT);
			}
			mInitialised = true;
		} else {
			mScanFragment.notifyDataSetChanged();
		}
	}

	public void onBtnScan(View view) {
		if (mScanning) {
			stopScan();
		} else {
            if(mConnIndex != NO_DEVICE) {
                mBluetoothLeService.disconnect(mBluetoothDevice.getAddress());
            }
			startScan();
		}
	}

	void onConnect() {
		if (mNumDevs > 0) {
			int connState = mBluetoothManager.getConnectionState(mBluetoothDevice, BluetoothGatt.GATT);

			switch (connState) {
				case BluetoothGatt.STATE_CONNECTED:
					mBluetoothLeService.disconnect(null);
					break;
				case BluetoothGatt.STATE_DISCONNECTED:
					boolean ok = mBluetoothLeService.connect(mBluetoothDevice.getAddress());
					if (!ok) {
						setError("Connect failed");
					}
					break;
				default:
					setError("Device busy (connecting/disconnecting)");
					break;
			}
		}
	}

	private void startScan() {
		// Start device discovery
		if (mBleSupported) {
			mNumDevs = 0;
			mDeviceInfoList.clear();
			mScanFragment.notifyDataSetChanged();
			scanLeDevice(true);
			mScanFragment.updateGui(mScanning);
			if (!mScanning) {
				setError("Device discovery start failed");
				setBusy(false);
			}
		} else {
			setError("BLE not supported on this device");
		}

	}

	private void stopScan() {
		mScanning = false;
		mScanFragment.updateGui(false);
		scanLeDevice(false);
	}

	private void startDeviceActivity() {
		gotoHome();
	}

	private void stopDeviceActivity() {
		finishActivity(REQ_DEVICE_ACT);
	}

	public void onDeviceClick(int pos) {

		if (mScanning)
			stopScan();

		setBusy(true);

		mBluetoothDevice = mDeviceInfoList.get(pos).getDevice();

		if (mConnIndex == NO_DEVICE) {
			mScanFragment.setStatus("Connecting");
			mConnIndex = pos;
			onConnect();
		} else {
			mScanFragment.setStatus("Disconnecting");
			if (mConnIndex != NO_DEVICE) {
				mBluetoothLeService.disconnect(mBluetoothDevice.getAddress());
			}
            startScan();
		}
	}

	public void onScanTimeout() {
		runOnUiThread(new Runnable() {
			public void run() {
				stopScan();
			}
		});
	}

	public void onConnectTimeout() {
		runOnUiThread(new Runnable() {
			public void run() {
				setError("Connection timed out");
			}
		});
		if (mConnIndex != NO_DEVICE) {
			mBluetoothLeService.disconnect(mBluetoothDevice.getAddress());
			mConnIndex = NO_DEVICE;
		}
	}

	public BluetoothDevice getBluetoothDevice() {
		return mBluetoothDevice;
	}

	// ////////////////////////////////////////////////////////////////////////////////////////////////
	//
	// GUI methods
	// ////////////////////////////////////////////////////////////////////////////////////////////////

	public void updateGuiState() {
		boolean mBtEnabled = mBtAdapter.isEnabled();

		if (mBtEnabled) {
			if (mScanning) {
				// BLE Host connected
				if (mConnIndex != NO_DEVICE) {
					String txt = mBluetoothDevice.getName() + " connected";
					mScanFragment.setStatus(txt);
				} else {
					mScanFragment.setStatus(mNumDevs + " devices");
				}
			}
		} else {
			mDeviceInfoList.clear();
			mScanFragment.notifyDataSetChanged();
		}
	}

	private void setBusy(boolean f) {
		mScanFragment.setBusy(f);
	}

	void setError(String txt) {
		mScanFragment.setError(txt);
	}

	private BleDeviceInfo createDeviceInfo(BluetoothDevice device, int rssi) {
		BleDeviceInfo deviceInfo = new BleDeviceInfo(device, rssi);

		return deviceInfo;
	}

	private boolean checkDeviceFilter(BluetoothDevice device) {
		int  n = mDeviceFilter.length;
        if (n > 0) {
            boolean found = false;
            for (int i=0; i<n && !found; i++) {
                if(device != null && device.getName() != null) {
                    found = device.getName().equals(mDeviceFilter[i]);
                }
            }
            return found;
        }
		// Allow all devices if the device filter is empty
		return true;
	}

	private void addDevice(BleDeviceInfo device) {
		mNumDevs++;
		mDeviceInfoList.add(device);
		mScanFragment.notifyDataSetChanged();
		if (mNumDevs > 1)
			mScanFragment.setStatus(mNumDevs + " devices");
		else
			mScanFragment.setStatus("1 device");
	}

	private boolean deviceInfoExists(String address) {
		for (int i = 0; i < mDeviceInfoList.size(); i++) {
			if (mDeviceInfoList.get(i).getDevice().getAddress().equals(address)) {
				return true;
			}
		}
		return false;
	}

	private BleDeviceInfo findDeviceInfo(BluetoothDevice device) {
		for (int i = 0; i < mDeviceInfoList.size(); i++) {
			if (mDeviceInfoList.get(i).getDevice().getAddress().equals(device.getAddress())) {
				return mDeviceInfoList.get(i);
			}
		}
		return null;
	}

	private boolean scanLeDevice(boolean enable) {
		if (enable) {
			mScanning = mBtAdapter.startLeScan(mLeScanCallback);
		} else {
			mScanning = false;
			mBtAdapter.stopLeScan(mLeScanCallback);
		}
		return mScanning;
	}

	public List<BleDeviceInfo> getDeviceInfoList() {
		return mDeviceInfoList;
	}

	private void startBluetoothLeService() {
		boolean f;

		Intent bindIntent = new Intent(this, BluetoothLeService.class);
		startService(bindIntent);
		f = bindService(bindIntent, mServiceConnection, Context.BIND_AUTO_CREATE);
		if (f)
			Log.d(TAG, "BluetoothLeService - success");
		else {
			finish();
		}
	}

	// ////////////////////////////////////////////////////////////////////////////////////////////////
	//
	// Broadcasted actions from Bluetooth adapter and BluetoothLeService
	// ////////////////////////////////////////////////////////////////////////////////////////////////

	private BroadcastReceiver mReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			final String action = intent.getAction();

			Log.i(TAG,"Got action: " + action);

			if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
				// Bluetooth adapter state change
				switch (mBtAdapter.getState()) {
					case BluetoothAdapter.STATE_ON:
						mConnIndex = NO_DEVICE;
						startBluetoothLeService();
						break;
					case BluetoothAdapter.STATE_OFF:
						Toast.makeText(context, R.string.app_closing, Toast.LENGTH_LONG).show();
						finish();
						break;
					default:
						Log.w(TAG, "Action STATE CHANGED not processed ");
						break;
				}

				updateGuiState();
			} else if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
				// GATT connect
				int status = intent.getIntExtra(BluetoothLeService.EXTRA_STATUS, BluetoothGatt.GATT_FAILURE);
				if (status == BluetoothGatt.GATT_SUCCESS) {
					setBusy(false);
					startDeviceActivity();
					mScanFragment.setDeviceConnected(mConnIndex, true);
					mBleDeviceConnected = true;
                    mDeviceInfoList.clear();
                    mScanFragment.updateGuiDisconnect();
                    updateBleGUI();
				} else
					setError("Connect failed. Status: " + status);
			} else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
				// GATT disconnect
				int status = intent.getIntExtra(BluetoothLeService.EXTRA_STATUS, BluetoothGatt.GATT_FAILURE);
				stopDeviceActivity();
				if (status == BluetoothGatt.GATT_SUCCESS) {
					setBusy(false);
					mScanFragment.setStatus(mBluetoothDevice.getName() + " disconnected", STATUS_DURATION);
					mBleDeviceConnected = true;
					updateBleGUI();
				} else {
					setError("Disconnect failed. Status: " + status);
				}
				mScanFragment.setDeviceConnected(mConnIndex, false);
				mConnIndex = NO_DEVICE;
				mBluetoothLeService.close();
			} else {
				Log.w(TAG, "Unknown action: " + action);
			}

		}
	};

	// Code to manage Service life cycle.
	private final ServiceConnection mServiceConnection = new ServiceConnection() {

		public void onServiceConnected(ComponentName componentName, IBinder service) {
			mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
			if (!mBluetoothLeService.initialize()) {
				Log.e(TAG, "Unable to initialize BluetoothLeService");
				finish();
				return;
			}
			final int n = mBluetoothLeService.numConnectedDevices();
			if (n > 0) {
				runOnUiThread(new Runnable() {
					public void run() {
						setError("Multiple connections!");
					}
				});
			} else {
				startScan();
				Log.i(TAG, "BluetoothLeService connected");
			}
		}

		public void onServiceDisconnected(ComponentName componentName) {
			mBluetoothLeService = null;
			Log.i(TAG, "BluetoothLeService disconnected");
		}
	};

	// Device scan callback.
	// NB! Nexus 4 and Nexus 7 (2012) only provide one scan result per scan
	private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {

		public void onLeScan(final BluetoothDevice device, final int rssi, byte[] scanRecord) {
			runOnUiThread(new Runnable() {
				public void run() {
					// Filter devices
					if (checkDeviceFilter(device)) {
						if (!deviceInfoExists(device.getAddress())) {
							// New device
							BleDeviceInfo deviceInfo = createDeviceInfo(device, rssi);
							addDevice(deviceInfo);
						} else {
							// Already in list, update RSSI info
							BleDeviceInfo deviceInfo = findDeviceInfo(device);
							deviceInfo.setRssi(rssi);
							mScanFragment.notifyDataSetChanged();
						}
					}
				}

			});
		}
	};

	private class BleConnectionTerminatedReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent.getAction().equals(Constants.BLE_CONNECTION_TERMINATED)) {
				if (mVisibleFragment.equals(VisibleFragment.STATS)) {
					mShotStatsFragment.stopCurrentTest();
				}
				mBleDeviceConnected = false;
				updateBleGUI();
			}
		}
	}

	private void updateBleGUI() {
		if (mActionBarFragment != null)
			mActionBarFragment.updateGui();
		if (mShotStatsFragment != null)
			mShotStatsFragment.updateGui();
	}

	public boolean isBleDeviceConnected() {
		return mBleDeviceConnected;
	}

	// ////////////////////////////////////////////////////////////////////////////////////////////////
	//
	// END BLE RELATED METHODS
	//
	// ////////////////////////////////////////////////////////////////////////////////////////////////

}
