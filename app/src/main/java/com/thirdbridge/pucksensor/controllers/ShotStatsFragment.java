package com.thirdbridge.pucksensor.controllers;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.content.ContextCompat;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.google.gson.Gson;
import com.thirdbridge.pucksensor.R;
import com.thirdbridge.pucksensor.ble.BluetoothLeService;
import com.thirdbridge.pucksensor.ble.GattInfo;
import com.thirdbridge.pucksensor.ble.Sensor;
import com.thirdbridge.pucksensor.ble.SensorDetails;
import com.thirdbridge.pucksensor.models.User;
import com.thirdbridge.pucksensor.utils.BaseFragment;
import com.thirdbridge.pucksensor.utils.Constants;
import com.thirdbridge.pucksensor.utils.IO;
import com.thirdbridge.pucksensor.utils.MathHelper;
import com.thirdbridge.pucksensor.utils.Shot;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Created by Christophe on 2015-10-14.
 * Modified by Jayson Dalpé since 2016-01-22.
 */
public class ShotStatsFragment extends BaseFragment {

	private static String TAG = ShotStatsFragment.class.getSimpleName();
    private static String FOLDER_SAVE_SHOT = "Shots";

    private static final double STAMP = 1.25;
    private static final double DRAFT_STAMP = 50/3;
    private static final int MINIMAL_G = 0; // Actually +1
    private static final boolean DEBUG = true;
    private static final String THRESHOLD_G = "THRESHOLD_G";
    private static final String CHECK_ACCEL = "CHECK_ACCEL";
    private static final String CHECK_SPEED = "CHECK_SPEED";
    private static final String CHECK_ROTATION = "CHECK_ROTATION";

    private static final int[] GRAPH_COLOR = {Color.BLUE, Color.RED};
    private static final int[] RECENT_NAME = {R.string.recent_shot1, R.string.recent_shot2, R.string.recent_shot3, R.string.recent_shot4};

    // PROTOCOL CMD
    private static final int SETTINGS_READ = 2;
    private static final int SETTINGS_NEW  = 3;
    private static final int DATA          = 4;
    private static final int DATA_READY    = 5;
    private static final int DATA_END      = 6;
    private static final int DATA_START    = 8;
    private static final int DATA_DRAFT    = 10;

    // PROTOCOL DEFAULT SETTINGS
    private static final byte VALIDITY_TOKEN = 0x3D;
    private static final int[] DEFAULT = {VALIDITY_TOKEN, 0x00, 0x00, 255, 0x00, 0x01, 0x01, 0x01}; //(2G)

    // Saving local instance
    SharedPreferences mSettings;

	private boolean mTestRunning = false;
	private User mUser;
	private boolean mPreviewTest = false;
    private boolean mProgressChange = true;
    private int[] mActualSettings = DEFAULT.clone();

	private Button mStartStopButton;
	private Button mSaveButton;
	private ProgressBar mSensorStartingProgressBar;
	private TextView mSensorStartingTextView;
	private TextView mDescriptionTextView;

	//Loading screen
	private RelativeLayout mLoadingScreenRelativeLayout;

	private boolean mTestWasRun = false;

    private Thread mBackgroundThread;
    private boolean mPause = true;

    // Menu
    private CheckBox mAccCheckB;
    private CheckBox mSpeedCheckB;
    private CheckBox mAngularCheckB;
    private TextView mPeakAccTV;
    private SeekBar mPeakAccSB;
    private ProgressBar mCalibratePB;
    private Button mCalibrateBtn;

    // Check management
    private CheckBox[] mRecentResult;
    private int mFirstCheck = 0;
    private int mSecondCheck = -1;
    private boolean mCanTouchThis = true;

    // Core
    private double[] mAccelCircularBuffer = new double[Shot.getMaxDraftData()];
    private double[] mRotCircularBuffer = new double[Shot.getMaxDraftData()];
    private int mCircularIndex = 0;
    private Shot[] mRecent = new Shot[4];
    private int mRealIndex;
    private Shot mReal;
    private long mTime = 0;

	//Accel Chart
    private LinearLayout mAccelLayout;
	private LineChart mAccelChart;
	private TextView mTopAccelXYZTextView;
	private float[] mAccelMax = {0f, 0f};


	//Speed Chart
    private LinearLayout mSpeedLayout;
	private LineChart mSpeedChart;
	private TextView mTopSpeedXYZTextView;
	private float[] mSpeedMax = {0f, 0f};
	private float mPuckSpeedXYZ = 0f;
	private Button mGenerateButton;
    private Button mHackButton;

	//Rotation Chart
    private float[] mRotationMax = {0f, 0f};
    private LinearLayout mAngularLayout;
	private LineChart mRotationChart;
	private TextView mTopRotationTextView;

	//Calibration
	private int mCalibrationTime = 1000;
	private float mTimeStep = 0f;
	private final double GRAVITY = 9.80665;
    private boolean mCalibrationDone = false;
    private float mAccelSumX = 0f;
    private float mAccelSumY = 0f;
    private float mAccelSumZ = 0f;
    private int mCalibrationDot = 0;
    private boolean mSendOnce = false;



	//Accel Chart
	private int mAccelDataSetIndexXYZ;

	//Speed Chart
	private int mSpeedDataSetIndexXYZ;

	// BLE
	private boolean mSensorReady = false;
    private int mRotationDataSetIndex;
	private BluetoothLeService mBtLeService = null;
	private BluetoothGatt mBtGatt = null;
	private List<BluetoothGattService> mServiceList = null;
	private static final int GATT_TIMEOUT = 100; // milliseconds
	private boolean mServicesRdy = false;
	private boolean mIsReceiving = false;

	// SensorTag
	private List<Sensor> mEnabledSensors = new ArrayList<>();
	private BluetoothGattService mOadService = null;
	private BluetoothGattService mConnControlService = null;

    // Calibration
    private double[] mCalibrationValue = {0,0,0};
    private int mCalibrateNb = 0;
    boolean mStartCalibration = false;


    // Thread running
    Runnable mRun = new Runnable() {
        @Override
        public void run() {
            while(true) {
                if (mProgressChange && mSettings != null && mBtGatt != null) {
                    long value = mSettings.getInt(THRESHOLD_G, MINIMAL_G)+1;

                    byte[] send = new byte[20];
                    send[0] = SETTINGS_NEW;
                    send[1] = 0; //Battery, don't care

                    if (value < 15) {
                        value = value * 2048 / 16;
                        mActualSettings[1] = 0;
                        mActualSettings[2] = 0;
                        mActualSettings[3] = (int) (value & 255);
                        mActualSettings[4] = (int) (value/256 & 255);
                    }
                    else
                    {
                        value = value * 2048 / 400;
                        mActualSettings[1] = (int) (value & 255);
                        mActualSettings[2] = (int) (value/256 & 255);
                        mActualSettings[3] = 0;
                        mActualSettings[4] = 0;
                    }
                    String sendValue = send[0] + ", " + send[1] + ", ";
                    for (int i=0; i<18; i++) {
                        if (i < mActualSettings.length) {
                            send[2+i] = (byte)mActualSettings[i];
                        } else {
                            send[2+i] = 0;
                        }
                        sendValue+= send[2+i] + ", ";
                    };
                    Log.i(TAG, "New settings: " + sendValue);
                    try {
                        writeBLE(send);
                        mProgressChange = false;
                    } catch(Exception e) {}
                }

                try {
                    Thread.sleep(1000);
                } catch (Exception e) {

                }
                if (mPause) {
                    break;
                }
            }
        }
    };

    Runnable mStartCalibrationRunnable = new Runnable() {
        @Override
        public void run() {
            mActualSettings[5] = 0;
            mActualSettings[6] = 0;
            mActualSettings[7] = 0;

            mProgressChange = true;

            try {
                // Sleep fo 1sec (setting thread delay) + 50 ms (IC max delay for settings)
                Thread.sleep(1050);
            } catch (Exception e) {}

            mCalibrationValue[0] = 0;
            mCalibrationValue[1] = 0;
            mCalibrationValue[2] = 0;
            mCalibrateNb = 0;
            mStartCalibration = true;

            while (mCalibrateNb< 100) {}

            mStartCalibration = false;
            mCalibrationValue[0] = (mCalibrationValue[0] / mCalibrateNb) % 256;
            mCalibrationValue[1] = (mCalibrationValue[1] / mCalibrateNb) % 256;
            mCalibrationValue[2] = (mCalibrationValue[2] / mCalibrateNb) % 256;

            mActualSettings[5] = (int)mCalibrationValue[0];
            mActualSettings[6] = (int)mCalibrationValue[1];
            mActualSettings[7] = (int)mCalibrationValue[2];

            mProgressChange = true;

            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mCalibrateBtn.setVisibility(View.VISIBLE);
                    mCalibratePB.setVisibility(View.GONE);
                }
            });
        }
    };

	public static ShotStatsFragment newInstance(User user) {
		Bundle args = new Bundle();
		args.putString(Constants.CURRENT_USER, new Gson().toJson(user, User.class));

		ShotStatsFragment fragment = new ShotStatsFragment();
		fragment.setArguments(args);

		return fragment;
	}


	@Override
	public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

		if (getArguments().containsKey(Constants.CURRENT_USER)) {
			mUser = new Gson().fromJson(getArguments().getString(Constants.CURRENT_USER), User.class);
		} else {
			mPreviewTest = true;
		}

		if (!mPreviewTest) {
			initializeBluetooth();
		}
	}

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

	@Override
	public View onCreateView(LayoutInflater inflater, final ViewGroup container, Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);

		View v = inflater.inflate(R.layout.fragment_stats, container, false);

		mSensorStartingProgressBar = (ProgressBar) v.findViewById(R.id.starting_sensors_progressbar);
		mSensorStartingTextView = (TextView) v.findViewById(R.id.starting_sensors_textview);
		mStartStopButton = (Button) v.findViewById(R.id.start_button);
		mSaveButton = (Button) v.findViewById(R.id.save_button);
		mDescriptionTextView = (TextView) v.findViewById(R.id.stats_description_textview);
		mGenerateButton = (Button) v.findViewById(R.id.generate_button);
        mHackButton = (Button) v.findViewById(R.id.demo_start_button);

		mLoadingScreenRelativeLayout = (RelativeLayout) v.findViewById(R.id.loading_screen_relative_layout);

        // Menu
        mAccCheckB = (CheckBox) v.findViewById(R.id.show_acceleration_check);
        mSpeedCheckB = (CheckBox) v.findViewById(R.id.show_speed_check);
        mAngularCheckB = (CheckBox) v.findViewById(R.id.show_angular_check);
        mPeakAccTV = (TextView) v.findViewById(R.id.peak_acc_number);
        mPeakAccSB = (SeekBar) v.findViewById(R.id.peak_acc_seekbar);
        mCalibrateBtn = (Button) v.findViewById(R.id.calibrate_button);
        mCalibratePB = (ProgressBar) v.findViewById(R.id.calibrate_progress);
        mCalibratePB.setIndeterminate(true);
        mCalibratePB.setVisibility(View.GONE);

        mSettings = getActivity().getSharedPreferences("StatPuck", 0);

        String value = "" + (mSettings.getInt(THRESHOLD_G, MINIMAL_G)+1);
        mPeakAccTV.setText(value);
        mPeakAccSB.setProgress(Integer.parseInt(mPeakAccTV.getText().toString())-1);


        // Main structure
        mAccelLayout = (LinearLayout) v.findViewById(R.id.accel_layout);
        mSpeedLayout = (LinearLayout) v.findViewById(R.id.speed_layout);
        mAngularLayout = (LinearLayout) v.findViewById(R.id.angular_layout);

        mRecentResult = new CheckBox[4];
        mRecentResult[0] = (CheckBox) v.findViewById(R.id.recent_result1);
        mRecentResult[1] = (CheckBox) v.findViewById(R.id.recent_result2);
        mRecentResult[2] = (CheckBox) v.findViewById(R.id.recent_result3);
        mRecentResult[3] = (CheckBox) v.findViewById(R.id.recent_result4);

        for (int i=0; i<mRecentResult.length; i++) {
            mRecentResult[i].setText(RECENT_NAME[i]);
        }

        mRecentResult[0].setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (mCanTouchThis)
                    checkRecent(0, isChecked);
            }
        });

        mRecentResult[1].setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (mCanTouchThis)
                    checkRecent(1, isChecked);
            }
        });

        mRecentResult[2].setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (mCanTouchThis)
                    checkRecent(2, isChecked);
            }
        });

        mRecentResult[3].setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (mCanTouchThis)
                    checkRecent(3, isChecked);
            }
        });

		//Accel chart
		mAccelChart = (LineChart) v.findViewById(R.id.accel_stats_chart);
		mTopAccelXYZTextView = (TextView) v.findViewById(R.id.top_accel_xyz_textview);

        mCalibrateBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCalibratePB.setVisibility(View.VISIBLE);
                mCalibrateBtn.setVisibility(View.GONE);

                Thread t = new Thread(mStartCalibrationRunnable);
                t.start();

            }
        });

        if (DEBUG) {
            mHackButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mSensorStartingProgressBar.setVisibility(View.GONE);
                    mSensorStartingTextView.setVisibility(View.GONE);
                    mStartStopButton.setVisibility(View.VISIBLE);
                    mSaveButton.setVisibility(View.VISIBLE);
                }
            });


            mGenerateButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                }
            });
        } else {
            mHackButton.setVisibility(View.GONE);
            mGenerateButton.setVisibility(View.GONE);
        }


        mPeakAccSB.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                mPeakAccTV.setText("" + (progress+1));
                SharedPreferences.Editor editor = mSettings.edit();
                editor.putInt(THRESHOLD_G, progress);
                editor.commit();
                mProgressChange = true;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

		//Speed Chart
		mSpeedChart = (LineChart) v.findViewById(R.id.speed_stats_chart);
		mTopSpeedXYZTextView = (TextView) v.findViewById(R.id.top_speed_xyz_textview);

		//Rotation Chart
		mRotationChart = (LineChart) v.findViewById(R.id.rotation_stats_chart);
		mTopRotationTextView = (TextView) v.findViewById(R.id.top_rotation_textview);


		mStartStopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (DEBUG || mSensorReady) {
                    mCalibrateBtn.setVisibility(View.VISIBLE);
                    mSaveButton.setBackground(ContextCompat.getDrawable(getContext(), R.drawable.small_button_shadow));

                    if (mTestRunning) {
                        mTestRunning = false;
                        mStartStopButton.setText(getString(R.string.reset));
                        mStartStopButton.setCompoundDrawablesWithIntrinsicBounds(R.drawable.reset, 0, 0, 0);
                        populateStatisticsFields();
                    } else if (mTestWasRun) {
                        mTestWasRun = false;
                        getController().reloadShotStatsFragment();
                        if (!getController().isBleDeviceConnected())
                            mStartStopButton.setEnabled(false);
                    } else {
                        mTestWasRun = true;

                        if (mAccelChart.getLineData() != null) {
                            mAccelChart.getLineData().clearValues();
                            mAccelChart.getData().getDataSets().clear();
                            mAccelChart.getData().getXVals().clear();
                        }
                        if (mSpeedChart.getLineData() != null) {
                            mSpeedChart.getLineData().clearValues();
                            mSpeedChart.getData().getDataSets().clear();
                            mSpeedChart.getData().getXVals().clear();
                        }
                        if (mRotationChart.getLineData() != null) {
                            mRotationChart.getLineData().clearValues();
                            mRotationChart.getData().getDataSets().clear();
                            mRotationChart.getData().getXVals().clear();
                        }

                        mAccelChart.clear();
                        mSpeedChart.clear();
                        mRotationChart.clear();



                        setupAccelChart();
                        setupSpeedChart();
                        setupRotationChart();

                        mTopAccelXYZTextView.setText("");

                        mTopSpeedXYZTextView.setText("");

                        mTopRotationTextView.setText("");

                        mStartStopButton.setText(getString(R.string.stopTest));
                        mStartStopButton.setCompoundDrawablesWithIntrinsicBounds(android.R.drawable.ic_delete, 0, 0, 0);

                        mTestRunning = true;
                        byte[] send = {DATA_READY, 0x00};
                        try {
                            writeBLE(send);
                        } catch (Exception e) {}
                    }
                }
            }
        });

		mSaveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Save on a known place every shot shown
                File rootsd = Environment.getExternalStorageDirectory();
                File root = new File(rootsd.getAbsolutePath(), FOLDER_SAVE_SHOT);
                if (!root.exists()) {
                    root.mkdirs();
                }

                List<Integer> id = new ArrayList<Integer>();
                if (mFirstCheck != -1) {
                    id.add(mFirstCheck);
                }
                if (mSecondCheck != -1) {
                    id.add(mSecondCheck);
                }

                for (int i=0; i<id.size(); i++) {
                    Pair<String, String> saveData = mRecent[id.get(i)].packageFormCSV();
                    File file = new File(root, saveData.first);
                    IO.saveFile(saveData.second, file);
                    Toast.makeText(getContext(), "Save " + getString(RECENT_NAME[id.get(i)]) + " in " + file, Toast.LENGTH_LONG).show();

                    Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                    intent.setData(Uri.fromFile(file));
                    getActivity().sendBroadcast(intent);
                }


            }
        });

        mAccCheckB.setChecked(mSettings.getBoolean(CHECK_ACCEL, mAccCheckB.isChecked()));
        mSpeedCheckB.setChecked(mSettings.getBoolean(CHECK_SPEED, mSpeedCheckB.isChecked()));
        mAngularCheckB.setChecked(mSettings.getBoolean(CHECK_ROTATION, mAngularCheckB.isChecked()));
        showAcceleration(mAccCheckB.isChecked());
        showSpeed(mSpeedCheckB.isChecked());
        showAngularSpeed(mAngularCheckB.isChecked());

        mAccCheckB.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                showAcceleration(isChecked);
                SharedPreferences.Editor editor = mSettings.edit();
                editor.putBoolean(CHECK_ACCEL, isChecked);
                editor.commit();
            }
        });

        mSpeedCheckB.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                showSpeed(isChecked);
                SharedPreferences.Editor editor = mSettings.edit();
                editor.putBoolean(CHECK_SPEED, isChecked);
                editor.commit();
            }
        });

        mAngularCheckB.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                showAngularSpeed(isChecked);
                SharedPreferences.Editor editor = mSettings.edit();
                editor.putBoolean(CHECK_ROTATION, isChecked);
                editor.commit();
            }
        });

		return v;
	}

    private void writeBLE(byte[] values) throws Exception{
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

    private void checkRecent(int id, boolean isChecked) {
        if (isChecked) {
            if (mFirstCheck == -1) {
                mFirstCheck = id;
            } else if (mSecondCheck == -1) {
                mSecondCheck = id;
            } else {
                mFirstCheck = mSecondCheck;
                mSecondCheck = id;
            }
        } else {
            if (mFirstCheck == id) {
                mFirstCheck = -1;
            }
            if (mSecondCheck == id) {
                mSecondCheck = -1;
            }
        }

        mCanTouchThis = false; // Prevent infinite loop
        mRecentResult[0].setChecked(false);
        mRecentResult[1].setChecked(false);
        mRecentResult[2].setChecked(false);
        mRecentResult[3].setChecked(false);

        if (mFirstCheck != -1) {
            mRecentResult[mFirstCheck].setChecked(true);
        }
        if (mSecondCheck != -1) {
            mRecentResult[mSecondCheck].setChecked(true);
        }
        mCanTouchThis = true;

        populateCharts();
        populateStatisticsFields();
    }

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

		if (mPreviewTest) {

			DisplayMetrics displaymetrics = new DisplayMetrics();
			getController().getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
			int screenWidth = displaymetrics.widthPixels;
			int screenHeight = displaymetrics.heightPixels;

			RelativeLayout.LayoutParams layout_description = new RelativeLayout.LayoutParams(screenWidth,
					screenHeight);

			mLoadingScreenRelativeLayout.setLayoutParams(layout_description);

			mLoadingScreenRelativeLayout.setVisibility(View.VISIBLE);
			mDescriptionTextView.setVisibility(View.VISIBLE);
			//mDescriptionTextView.setText("Test performed by user:" + mShotTest.getUsername() + " on " + mShotTest.getDate() + "\r\n\r\n" + mShotTest.getDescription());

			mStartStopButton.setVisibility(View.GONE);
			mSaveButton.setVisibility(View.GONE);
			mSensorStartingProgressBar.setVisibility(View.GONE);
			mSensorStartingTextView.setVisibility(View.GONE);

			Handler handler = new Handler();
			handler.postDelayed(new Runnable() {
				public void run() {
					setupAccelChart();
					setupSpeedChart();
					setupRotationChart();
					populateStatisticsFields();
				}
			}, 1000);
		}

		if (mSensorReady) {
			activateTestButtons();
		}
	}

	private void activateTestButtons() {
		if (mSensorStartingProgressBar != null && mSensorStartingTextView != null && mStartStopButton != null && mSaveButton != null) {
			mSensorStartingProgressBar.setVisibility(View.GONE);
			mSensorStartingTextView.setVisibility(View.GONE);
			mStartStopButton.setVisibility(View.VISIBLE);
			mSaveButton.setVisibility(View.VISIBLE);
        }
	}

	private void deactivateTestButtons(){
		mStartStopButton.setEnabled(false);
		mSaveButton.setEnabled(false);
	}

	private void populateStatisticsFields() {

		if (mAccelChart != null) {
            float max = 0;
            for (int i=0; i<mAccelMax.length; i++) {
                if (mAccelMax[i] > max) {
                    max = mAccelMax[i];
                }
            }
            YAxis leftAxis = mAccelChart.getAxisLeft();
            leftAxis.setAxisMaxValue(Math.abs(max));
            leftAxis.setAxisMinValue(0);
			//accel
			mTopAccelXYZTextView.setText(MathHelper.round(mAccelMax[0], 2) + "g and " + MathHelper.round(mAccelMax[1], 2) + "g");
            mAccelChart.fitScreen();
            mAccelChart.notifyDataSetChanged();

		}

		if (mSpeedChart != null) {
            float max = 0;
            for (int i=0; i<mSpeedMax.length; i++) {
                if (mSpeedMax[i] > max) {
                    max = mSpeedMax[i];
                }
            }
            YAxis leftAxis = mSpeedChart.getAxisLeft();
            leftAxis.setAxisMaxValue(Math.abs(max));
            leftAxis.setAxisMinValue(0);
			//speed
			mTopSpeedXYZTextView.setText(MathHelper.round(mSpeedMax[0], 2) + " m/s and " + MathHelper.round(mSpeedMax[1], 2) + "m/s");
            mSpeedChart.fitScreen();
            mSpeedChart.notifyDataSetChanged();

		}

		if (mRotationChart != null) {
            float max = 0;
            for (int i=0; i<mRotationMax.length; i++) {
                if (mRotationMax[i] > max) {
                    max = mRotationMax[i];
                }
            }
            YAxis leftAxis = mRotationChart.getAxisLeft();
            leftAxis.setAxisMaxValue(Math.abs(max));
            leftAxis.setAxisMinValue(0);
			//rotation
			mTopRotationTextView.setText(MathHelper.round(mRotationMax[0], 2) + " degrees/s and" + MathHelper.round(mRotationMax[1], 2) + "degrees/s");
            mRotationChart.fitScreen();
            mRotationChart.notifyDataSetChanged();
		}
	}

	private void setupAccelChart() {
		mAccelChart.setNoDataTextDescription("");

		mAccelChart.setAutoScaleMinMaxEnabled(true);

		// enable touch gestures
		mAccelChart.setTouchEnabled(false);

		// enable scaling and dragging
		mAccelChart.setDragEnabled(false);
		mAccelChart.setScaleEnabled(false);
		mAccelChart.setDrawGridBackground(false);

		// if disabled, scaling can be done on x- and y-axis separately
		mAccelChart.setPinchZoom(true);

		// set an alternative background color
		mAccelChart.setBackgroundColor(Color.WHITE);

		LineData data = new LineData();
		data.setValueTextColor(Color.BLACK);

		// add empty data
		mAccelChart.setData(data);

		mAccelChart.setDescription("");

		// get the legend (only possible after setting data)
		Legend l = mAccelChart.getLegend();

		// modify the legend ...
		// l.setPosition(LegendPosition.LEFT_OF_CHART);
        l.setForm(Legend.LegendForm.LINE);
		l.setTextColor(Color.BLACK);

		XAxis xl = mAccelChart.getXAxis();
		xl.setTextColor(Color.BLACK);
		xl.setDrawGridLines(false);
		xl.setAvoidFirstLastClipping(true);
		xl.setSpaceBetweenLabels(5);
		xl.setPosition(XAxis.XAxisPosition.BOTTOM);
		xl.setEnabled(true);

		YAxis leftAxis = mAccelChart.getAxisLeft();
		leftAxis.setTextColor(Color.BLACK);
		leftAxis.setAxisMaxValue(1f);
		leftAxis.setAxisMinValue(-1f);
		leftAxis.setStartAtZero(false);
		leftAxis.setDrawGridLines(false);
		leftAxis.setEnabled(false);

		YAxis rightAxis = mAccelChart.getAxisRight();
		rightAxis.setEnabled(false);

		if (mPreviewTest) {
			mAccelChart.getAxisLeft().setDrawGridLines(true);
			mAccelChart.getAxisLeft().setEnabled(true);
		}
	}

	private void addAccelEntry(String xValue, int xValueInt, float yValue, boolean newData, int idName, int id) {
		LineData data = mAccelChart.getData();

		if (data != null) {
			LineDataSet currentLineDataSet = null;

            if (!data.getXVals().contains(xValue + " ms"))
                data.addXValue(xValue + " ms");

            if (newData) {
                mAccelMax[id] = 0;
                currentLineDataSet = createSet(getActivity().getString(RECENT_NAME[idName]), GRAPH_COLOR[id], 1f, 1f, true);
                data.addDataSet(currentLineDataSet);
                mAccelDataSetIndexXYZ = data.getIndexOfDataSet(currentLineDataSet);


            }
            data.addEntry(new Entry(yValue, xValueInt), mAccelDataSetIndexXYZ);

            if (Math.abs(yValue) > Math.abs(mAccelMax[id])) {
                mAccelMax[id] = Math.abs(yValue);
            }
		}
	}

	private void setupSpeedChart() {
		mSpeedChart.setNoDataTextDescription("");

		mSpeedChart.setAutoScaleMinMaxEnabled(true);

		// enable touch gestures
		mSpeedChart.setTouchEnabled(false);

		// enable scaling and dragging
		mSpeedChart.setDragEnabled(true);
		mSpeedChart.setScaleEnabled(true);
		mSpeedChart.setDrawGridBackground(false);

		// if disabled, scaling can be done on x- and y-axis separately
		mSpeedChart.setPinchZoom(true);

		// set an alternative background color
		mSpeedChart.setBackgroundColor(Color.WHITE);

		LineData data = new LineData();
		data.setValueTextColor(Color.BLACK);

		// add empty data
		mSpeedChart.setData(data);

		mSpeedChart.setDescription("");

		// get the legend (only possible after setting data)
		Legend l = mSpeedChart.getLegend();

		// modify the legend ...
		// l.setPosition(LegendPosition.LEFT_OF_CHART);
		l.setForm(Legend.LegendForm.LINE);
		l.setTextColor(Color.BLACK);

		XAxis xl = mSpeedChart.getXAxis();
		xl.setTextColor(Color.BLACK);
		xl.setDrawGridLines(false);
		xl.setAvoidFirstLastClipping(true);
		xl.setSpaceBetweenLabels(5);
		xl.setPosition(XAxis.XAxisPosition.BOTTOM);
		xl.setEnabled(true);

		YAxis leftAxis = mSpeedChart.getAxisLeft();
		leftAxis.setTextColor(Color.BLACK);
		leftAxis.setAxisMaxValue(1f);
		leftAxis.setAxisMinValue(-1f);
		leftAxis.setStartAtZero(false);
		leftAxis.setDrawGridLines(false);
		leftAxis.setEnabled(false);

		YAxis rightAxis = mSpeedChart.getAxisRight();
		rightAxis.setEnabled(false);

		if (mPreviewTest) {
			mSpeedChart.getAxisLeft().setDrawGridLines(true);
			mSpeedChart.getAxisLeft().setEnabled(true);
		}
	}

	private void addSpeedEntry(String xValue, int xValueInt, float yValue, boolean newData, int idName, int id) {

		LineData data = mSpeedChart.getData();

		if (data != null) {
			LineDataSet currentLineDataSet = null;

			// add a new x-value first
			if (mPreviewTest) {
				if (!data.getXVals().contains(xValue))
					data.addXValue(xValue);
			} else {
				if (!data.getXVals().contains(xValue + " ms"))
					data.addXValue(xValue + " ms");
			}

            if (newData) {
                mSpeedMax[id] = 0;
                currentLineDataSet = createSet(getActivity().getString(RECENT_NAME[idName]), GRAPH_COLOR[id], 1f, 1f, true);
                data.addDataSet(currentLineDataSet);
                mSpeedDataSetIndexXYZ = data.getIndexOfDataSet(currentLineDataSet);
            }
            data.addEntry(new Entry(yValue, xValueInt), mSpeedDataSetIndexXYZ);

            if (Math.abs(yValue) > Math.abs(mSpeedMax[id])) {
                mSpeedMax[id] = Math.abs(yValue);
            }
		}
	}

	private void setupRotationChart() {
		mRotationChart.setNoDataTextDescription("");

		mRotationChart.setAutoScaleMinMaxEnabled(true);

		// enable touch gestures
		mRotationChart.setTouchEnabled(false);

		// enable scaling and dragging
		mRotationChart.setDragEnabled(true);
		mRotationChart.setScaleEnabled(true);
		mRotationChart.setDrawGridBackground(false);

		// if disabled, scaling can be done on x- and y-axis separately
		mRotationChart.setPinchZoom(true);

		// set an alternative background color
		mRotationChart.setBackgroundColor(Color.WHITE);

		LineData data = new LineData();
		data.setValueTextColor(Color.BLACK);

		// add empty data
		mRotationChart.setData(data);
		mRotationChart.setDescription("");

		// get the legend (only possible after setting data)
		Legend l = mRotationChart.getLegend();

		// modify the legend ...
		// l.setPosition(LegendPosition.LEFT_OF_CHART);
		l.setForm(Legend.LegendForm.LINE);
		l.setTextColor(Color.BLACK);

		XAxis xl = mRotationChart.getXAxis();
		xl.setTextColor(Color.BLACK);
		xl.setDrawGridLines(false);
		xl.setAvoidFirstLastClipping(true);
		xl.setSpaceBetweenLabels(5);
		xl.setPosition(XAxis.XAxisPosition.BOTTOM);
		xl.setEnabled(true);

		YAxis leftAxis = mRotationChart.getAxisLeft();
		leftAxis.setTextColor(Color.BLACK);
		leftAxis.setAxisMaxValue(100f);
		leftAxis.setAxisMinValue(-100f);
		leftAxis.setStartAtZero(false);
		leftAxis.setDrawGridLines(false);
		leftAxis.setEnabled(false);

		YAxis rightAxis = mRotationChart.getAxisRight();
		rightAxis.setEnabled(false);

		if (mPreviewTest) {
			mRotationChart.getAxisLeft().setDrawGridLines(true);
			mRotationChart.getAxisLeft().setEnabled(true);
		}
	}

	private void addRotationEntry(String xValue, int xValueInt, float yValue, boolean newData, int idName, int id) {
		LineData data = mRotationChart.getData();

		if (data != null) {
            LineDataSet currentLineDataSet = null;

            if (!data.getXVals().contains(xValue + " ms"))
                data.addXValue(xValue + " ms");

            if (newData) {
                mRotationMax[id] = 0;
                currentLineDataSet = createSet(getActivity().getString(RECENT_NAME[idName]), GRAPH_COLOR[id], 1f, 1f, true);
                data.addDataSet(currentLineDataSet);
                mRotationDataSetIndex = data.getIndexOfDataSet(currentLineDataSet);
            }

            if (Math.abs(yValue) > Math.abs(mRotationMax[id])) {
                mRotationMax[id] = Math.abs(yValue);
            }

			// add a new x-value first
            data.addEntry(new Entry(yValue, xValueInt), mRotationDataSetIndex);

		}
	}


	private LineDataSet createSet(String setTitle, int lineColor, float lineWidth, float circleSize, boolean fullLine) {
		LineDataSet set = new LineDataSet(null, setTitle);
        set.setAxisDependency(YAxis.AxisDependency.LEFT);
		set.setColor(lineColor);
		set.setCircleColor(lineColor);
		set.setLineWidth(lineWidth);
		set.setCircleSize(circleSize);
		set.setFillAlpha(65);
		set.setFillColor(lineColor);
		set.setHighLightColor(lineColor);//Color.rgb(244, 117, 117));
		set.setValueTextColor(Color.BLACK);
		set.setValueTextSize(9f);
		set.setDrawValues(false);
		if (!fullLine)
			set.enableDashedLine(10, 10, 0);
		return set;
	}

	public void stopCurrentTest() {
		if (mTestRunning) {
			mTestRunning = false;
			mStartStopButton.setText(getString(R.string.reset));
			mStartStopButton.setCompoundDrawablesWithIntrinsicBounds(R.drawable.reset, 0, 0, 0);
			populateStatisticsFields();
		}
	}

	public void updateGui() {
		if (!getController().isBleDeviceConnected()) {
			mStartStopButton.setBackground(ContextCompat.getDrawable(getContext(), R.drawable.disabled_small_button_shadow));
			mStartStopButton.setCompoundDrawablesWithIntrinsicBounds(R.drawable.btn_check_buttonless_on_disabled, 0, 0, 0);
		} else {
			mStartStopButton.setBackground(ContextCompat.getDrawable(getContext(), R.drawable.small_button_shadow));
			mStartStopButton.setCompoundDrawablesWithIntrinsicBounds(R.drawable.btn_check_buttonless_on, 0, 0, 0);
		}
	}


	// ////////////////////////////////////////////////////////////////////////////////////////////////
	//
	// BLE  methods
	//
	// ////////////////////////////////////////////////////////////////////////////////////////////////

	private void initializeBluetooth() {
		// BLE
		mBtLeService = BluetoothLeService.getInstance();
		mServiceList = new ArrayList<>();

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
				onCharacteristicChanged(uuidStr, value);
			} else if (BluetoothLeService.ACTION_DATA_WRITE.equals(action)) {
				// Data written
				String uuidStr = intent.getStringExtra(BluetoothLeService.EXTRA_UUID);
				onCharacteristicWrite(uuidStr, status);
			} else if (BluetoothLeService.ACTION_DATA_READ.equals(action)) {
				// Data read
				String uuidStr = intent.getStringExtra(BluetoothLeService.EXTRA_UUID);
				byte[] value = intent.getByteArrayExtra(BluetoothLeService.EXTRA_DATA);
				//onCharacteristicsRead(uuidStr,value,status);
			}

			if (status != BluetoothGatt.GATT_SUCCESS) {
				setError("GATT error code: " + status);
			}
		}
	};


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

	boolean isEnabledByPrefs(final Sensor sensor) {
		String preferenceKeyString = "pref_" + sensor.name().toLowerCase(Locale.ENGLISH) + "_on";

		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getController());

		Boolean defaultValue = true;
		return prefs.getBoolean(preferenceKeyString, defaultValue);
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

	private void setError(String txt) {
		Log.i(TAG, String.format("GOT ERROR %s", txt));
	}

	private void setStatus(String txt) {
		Log.i(TAG, String.format("GOT STATUS %s", txt));
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
			activateTestButtons();
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
				deactivateTestButtons();
			}
		}
	}

	private void onCharacteristicWrite(String uuidStr, int status) {
		Log.d(TAG, "onCharacteristicWrite: " + uuidStr);
        boolean begin = mBtGatt.beginReliableWrite();
        Log.i(TAG, "Begin successfull: " + begin);
        if (begin) {
            Log.i(TAG, "Execute successfull: " + mBtGatt.executeReliableWrite());
        }
	}

	private void onCharacteristicChanged(String uuidStr, byte[] value) {

        double[] accelHigh = getAccelHigh(value);
        double[] accelLow = getAccelLow(value);
        double[] gyro = getGyro(value);
        if (value[0] != SETTINGS_READ) {
            if (value[0] == DATA_DRAFT && mStartCalibration) {
                mCalibrationValue[0] += (double)(short)value[2];
                mCalibrationValue[1] += (double)(short)value[4];
                mCalibrationValue[2] += (double)(short)value[6];
                mCalibrateNb ++;

                mCalibrationValue[0] += (double)(short)value[8];
                mCalibrationValue[1] += (double)(short)value[10];
                mCalibrationValue[2] += (double)(short)value[12];
                mCalibrateNb ++;

                mCalibrationValue[0] += (double)(short)value[14];
                mCalibrationValue[1] += (double)(short)value[16];
                mCalibrationValue[2] += (double)(short)value[18];
                mCalibrateNb ++;
            } else {
                double[] realAccel = new double[accelLow.length];
                for (int i = 0; i < realAccel.length; i++) {
                    if (accelLow[i] >= 15) {
                        realAccel[i] = accelHigh[i];
                    } else {
                        realAccel[i] = accelLow[i];
                    }
                }
                if (mTestRunning) {
                    calculationMethod(realAccel, gyro, value[0]);
                }
            }
            Log.i(TAG, "Check first: AH: " + accelHigh[0] + " AL: " + accelLow[0] + " Gyro: " + gyro[0]);
        } else {
            if (value[2] != VALIDITY_TOKEN && !mSendOnce) {
                // Settings don't care, send default ones
                byte[] send = new byte[20];
                send[0] = SETTINGS_NEW;
                send[1] = 0; //Battery, don't care
                for (int i=0; i<18; i++) {
                    if (i < DEFAULT.length) {
                        send[2+i] = (byte)mActualSettings[i];
                    } else {
                        send[2+i] = 0;
                    }
                }
                try {
                    writeBLE(send);
                    mSendOnce = true;
                } catch (Exception e) {

                }
            }

            for (int i=0; i<mActualSettings.length; i++) {
                mActualSettings[i] = value[2+i];
            }
        }

        String val = "";
        for (int i=0; i<value.length; i++) {
            val += value[i] + ", ";
        }
        Log.i(TAG, "Value: " + val);

	}

    /**
     * Method to calibrate and see is data can be present to the screen as a "shot".
     * @param acceleration The X, Y and Z acceleration all in one.
     * @param rotation The angular speed
     * @param mode The Mode of transmission
     */
    private void calculationMethod(double[] acceleration, double rotation[], byte mode) {
        if (!mCalibrationDone) {
            // Calibration process (no need now)
            mCalibrationDone = true;

            mAccelChart.getAxisLeft().setDrawGridLines(true);
            mSpeedChart.getAxisLeft().setDrawGridLines(true);
            mRotationChart.getAxisLeft().setDrawGridLines(true);
            mAccelChart.getAxisLeft().setEnabled(true);
            mSpeedChart.getAxisLeft().setEnabled(true);
            mRotationChart.getAxisLeft().setEnabled(true);

            mTime = 0;
        } else {
            if (mTime % 100 == 0) {
                Log.d(TAG, "Time: " + System.currentTimeMillis());
            }
            mTime++;

            if (mode == DATA_DRAFT) {
                for (int i=0; i<acceleration.length; i++) {
                    // Use circular buffer
                    mAccelCircularBuffer[mCircularIndex] = acceleration[i];
                    mRotCircularBuffer[mCircularIndex] = rotation[i];
                    mCircularIndex++;
                    if (mCircularIndex >= mAccelCircularBuffer.length) {
                        mCircularIndex = 0;
                    }
                }
            } else if (mode == DATA_START) {
                Log.i(TAG, "START!");
                //Draw data
                double[] sendAcc = new double[mAccelCircularBuffer.length];
                double[] sendRot = new double[mAccelCircularBuffer.length];
                mCircularIndex ++;
                if (mCircularIndex >= mAccelCircularBuffer.length) {
                    mCircularIndex = 0;
                }

                for (int i = 0; i < sendRot.length; i++) {
                    sendAcc[i] = mAccelCircularBuffer[(mCircularIndex + i) % mAccelCircularBuffer.length];
                    sendRot[i] = mRotCircularBuffer[(mCircularIndex + i) % mAccelCircularBuffer.length];
                }
                mRecent[3] = mRecent[2];
                mRecent[2] = mRecent[1];
                mRecent[1] = mRecent[0];
                mRecent[0] = new Shot(Shot.getMaxDraftData(), mUser, true);

                mReal = new Shot(Shot.getMaxData(), mUser, false);
                mRealIndex = 0;

                for (int i = 0; i < sendRot.length; i++) {
                    mRecent[0].setAccelerationXYZ(sendAcc[i], i);
                    mRecent[0].setRotation(sendRot[i], i);
                }

                for (int i=0; i<acceleration.length; i++) {
                    // Use circular buffer
                    mReal.setAccelerationXYZ(acceleration[i], i);
                    mReal.setRotation(rotation[i], i);
                    mRealIndex ++;
                }

                populateCharts();
                populateStatisticsFields();
                mCircularIndex = 0;
            } else if (mode == DATA) {
                for (int i=0; i<acceleration.length; i++) {
                    // Use circular buffer
                    mReal.setAccelerationXYZ(acceleration[i], mRealIndex+i);
                    mReal.setRotation(rotation[i], mRealIndex+i);
                    mRealIndex ++;
                }
            } else if (mode == DATA_END){
                for (int i=0; i<acceleration.length; i++) {
                    // Use circular buffer
                    mReal.setAccelerationXYZ(acceleration[i], mRealIndex+i);
                    mReal.setRotation(rotation[i], mRealIndex+i);
                    mRealIndex ++;
                }

                mRecent[0] = mReal;
                populateCharts();
                populateStatisticsFields();
                mCircularIndex = 0;
            }
        }
    }

    private void populateCharts() {
        double time = 0.0f; // Use stamp

        mAccelChart.clear();
        LineData aData = new LineData();
        aData.setValueTextColor(Color.BLACK);
        mAccelChart.setData(aData);
        mAccelMax[0] = 0f;
        mAccelMax[1] = 0f;

        mSpeedChart.clear();
        LineData sData = new LineData();
        sData.setValueTextColor(Color.BLACK);
        mSpeedChart.setData(sData);
        mSpeedMax[0] = 0f;
        mSpeedMax[1] = 0;

        mRotationChart.clear();
        LineData rData = new LineData();
        rData.setValueTextColor(Color.BLACK);
        mRotationChart.setData(rData);
        mRotationMax[0] = 0f;
        mRotationMax[1] = 0f;

        //See which one we can do
        List<Integer> idData = new ArrayList<Integer>();
        if (mFirstCheck!= -1 && mRecent[mFirstCheck] != null) {
            idData.add(mFirstCheck);
        }
        if (mSecondCheck != -1 && mRecent[mSecondCheck] != null) {
            idData.add(mSecondCheck);
        }

        for (int id=0; id<idData.size(); id++) {
            Log.d(TAG, "ID: " + idData.get(id) + " using color " + GRAPH_COLOR[id]);
            boolean newSetRequired = true;

            mPuckSpeedXYZ = 0f;
            Log.i(TAG, "ABC");
            for (int i = 0; i < mRecent[idData.get(id)].getRotations().length; i++) {
                Log.i(TAG, "ABC: " +  mRecent[idData.get(id)].getAccelerations()[i]);
                mTimeStep = mRecent[idData.get(id)].isDraft() ? (float) DRAFT_STAMP : (float) STAMP;

                if (mRecent[idData.get(id)].isCooked()) {
                    addAccelEntry(i * mTimeStep + "", i, (float) mRecent[idData.get(id)].getAccelerations()[i], newSetRequired, idData.get(id), id);
                } else {
                    double accelX = mRecent[idData.get(id)].getAccelerationsXYZ()[i].x;
                    double accelY = mRecent[idData.get(id)].getAccelerationsXYZ()[i].y;
                    double accelZ = mRecent[idData.get(id)].getAccelerationsXYZ()[i].z;
                    double accelXYZ = Math.sqrt(Math.pow(accelX, 2) + Math.pow(accelY, 2) + Math.pow(accelZ, 2));


                    mPuckSpeedXYZ = (float) Math.sqrt(Math.pow((accelX) * GRAVITY * mTimeStep, 2) +
                            Math.pow((accelY) * GRAVITY * mTimeStep, 2) + Math.pow((accelZ) * GRAVITY * mTimeStep, 2)) / 1000f + mPuckSpeedXYZ;

                    //Complete recent data
                    mRecent[idData.get(id)].setAccelerationXYZ(accelXYZ, i);
                    addAccelEntry(i * mTimeStep + "", i, (float) accelXYZ, newSetRequired, idData.get(id), id);
                }


                mRecent[idData.get(id)].setSpeedXYZ(mPuckSpeedXYZ, i);
                addSpeedEntry(i * mTimeStep + "", i, mPuckSpeedXYZ, newSetRequired, idData.get(id), id);
                addRotationEntry(i * mTimeStep + "", i, (float) mRecent[idData.get(id)].getRotations()[i], newSetRequired, idData.get(id), id);

                newSetRequired = false;
            }
            mRecent[idData.get(id)].setMax(mAccelMax[id], mSpeedMax[id], mRotationMax[id]);

        }
    }

    private void showAcceleration(boolean show) {
        if (show) {
            mAccelLayout.setVisibility(View.VISIBLE);
        } else {
            mAccelLayout.setVisibility(View.GONE);
        }
    }

    private void showSpeed(boolean show) {
        if (show) {
            mSpeedLayout.setVisibility(View.VISIBLE);
        } else {
            mSpeedLayout.setVisibility(View.GONE);
        }
    }

    private void showAngularSpeed(boolean show) {
        if (show) {
            mAngularLayout.setVisibility(View.VISIBLE);
        } else {
            mAngularLayout.setVisibility(View.GONE);
        }
    }

    private double[] getAccelHigh(byte[] values) {
        // According to protocol, byte 2-19
        double[] retValue;
        retValue = new double[3];
        int value = (values[2] & 0xFF);
        value |= (values[3] & 0xFF) << 8;
        retValue[0] = ((double)value * 400)/2048;

        value = (values[8] & 0xFF);
        value |= (values[9] & 0xFF) << 8;
        retValue[1] = ((double)value * 400)/2048;

        value = (values[14] & 0xFF);
        value |= (values[15] & 0xFF) << 8;
        retValue[2] = ((double)value * 400)/2048;
        return retValue;
    }

    private double[] getAccelLow(byte[] values) {
        // According to protocol, byte 2-19
        double[] retValue;
        retValue = new double[3];

        int value = (values[4] & 0xFF);
        value |= (values[5] & 0xFF) << 8;
        retValue[0] = ((double)value * 16)/2048;

        value = (values[10] & 0xFF);
        value |= (values[11] & 0xFF) << 8;
        retValue[1] = ((double)value * 16)/2048;

        value = (values[16] & 0xFF);
        value |= (values[17] & 0xFF) << 8;
        retValue[2] = ((double)value * 16)/2048;
        return retValue;
    }

    private double[] getGyro(byte[] values) {
        // According to protocol, byte 2-19
        double[] retValue;
        retValue = new double[3];

        int value = (values[6] & 0xFF);
        value |= (values[7] & 0xFF) << 8;
        retValue[0] = ((double)value * 2000)/32767;

        value = (values[12] & 0xFF);
        value |= (values[13] & 0xFF) << 8;
        retValue[1] = ((double)value * 2000)/32767;

        value = (values[18] & 0xFF);
        value |= (values[19] & 0xFF) << 8;
        retValue[2] = ((double)value * 2000)/32767;
        return retValue;
    }
}
