package com.thirdbridge.pucksensor.controllers;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.content.ContextCompat;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Pair;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
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
import com.thirdbridge.pucksensor.database.DataManager;
import com.thirdbridge.pucksensor.models.ShotTest;
import com.thirdbridge.pucksensor.models.User;
import com.thirdbridge.pucksensor.utils.BaseFragment;
import com.thirdbridge.pucksensor.utils.Constants;
import com.thirdbridge.pucksensor.utils.IO;
import com.thirdbridge.pucksensor.utils.MathHelper;
import com.thirdbridge.pucksensor.utils.Shot;
import com.thirdbridge.pucksensor.utils.ble_utils.Point2D;
import com.thirdbridge.pucksensor.utils.ble_utils.Point3D;

import java.io.File;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
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

	private static final int DUMMY_DATA = 0;
	private static final int XYZ_DATA = 1;
	private static final int X_DATA = 2;
	private static final int Y_DATA = 3;
	private static final int Z_DATA = 4;

    private static final double STAMP = 2.0; // This is the delta time between sending, need to calculate instead TODO
    private static final int MINIMAL_G = 0; // Actually +1
    private static final float MINIMAL_NOISE_G = 0.1f;
    private static final boolean DEBUG = true;
    private static final String THRESHOLD_G = "THRESHOLD_G";
    private static final String CHECK_ACCEL = "CHECK_ACCEL";
    private static final String CHECK_SPEED = "CHECK_SPEED";
    private static final String CHECK_ROTATION = "CHECK_ROTATION";
    private static final String CHECK_NOT_FILTER = "CHECK_NOT_FILTER";

    private static final int[] GRAPH_COLOR = {Color.BLUE, Color.RED};
    private static final int[] RECENT_NAME = {R.string.recent_shot1, R.string.recent_shot2, R.string.recent_shot3, R.string.recent_shot4};


    private static final int SETTINGS_READ = 2;
    private static final int SETTINGS_NEW  = 3;
    private static final int DATA          = 4;
    private static final int DATA_READY    = 5;
    private static final int DATA_END      = 6;
    private static final int DATA_START    = 8;
    private static final int DATA_DRAFT    = 10;
    private static final byte VALIDITY_TOKEN = 0x3D;
    private static final int[] DEFAULT = {VALIDITY_TOKEN, 0x00, 0x00, 255, 0x00, 0x01, 0x01, 0x01}; //LOW 128 (2G)



    // Saving local instance
    SharedPreferences mSettings;

	private boolean mTestRunning = false;
	private User mUser;
	private ShotTest mShotTest;
	private boolean mPreviewTest = false;
	private double mTestStartTime = -1.0000;
	private double lastDataTime = -1.0000;

	private Button mStartStopButton;
	private Button mSaveButton;
	private ProgressBar mSensorStartingProgressBar;
	private TextView mSensorStartingTextView;
	private TextView mDescriptionTextView;

	//Loading screen
	private RelativeLayout mLoadingScreenRelativeLayout;

	private boolean mTestWasRun = false;
	private int dataCounter = 0;

    // Menu
    private CheckBox mAccCheckB;
    private CheckBox mSpeedCheckB;
    private CheckBox mAngularCheckB;
    private CheckBox mFilterCheckB;
    private TextView mPeakAccTV;
    private SeekBar mPeakAccSB;

    //TODO
    private CheckBox[] mRecentResult;
    private int mFirstCheck = 0;
    private int mSecondCheck = -1;
    private boolean mCanTouchThis = true;

    // Core
    private boolean mShotDetected = false;
    private int mIdDatas = 0;
    private Point3D[] mAccDatas = new Point3D[Shot.getMaxData()];
    private double[] mRotDatas = new double[Shot.getMaxData()];
    private Shot[] mRecent = new Shot[4];
    private boolean mNotFilter;
    private int mPauseGraph = 2000; //Sample
    private int mIterGraph = 0;
    private boolean mSendData = false;
    private boolean mLastStage;
    private long mDelta = -1;
    private long mTime = 0;

	//Accel Chart
    private LinearLayout mAccelLayout;
	private LineChart mAccelChart;
	private TextView mTopAccelXYZTextView;
	private float[] mAccelMax = {0f, 0f};
	private String mAccelData;
	private boolean mAccelDataVisibleX = false;
	private boolean mAccelDataVisibleY = false;
	private boolean mAccelDataVisibleZ = false;
	private boolean mCalculatedAverageOffset = false;

	//Speed Chart
    private LinearLayout mSpeedLayout;
	private LineChart mSpeedChart;
	private TextView mTopSpeedXYZTextView;
	private float[] mSpeedMax = {0f, 0f};
	private float mPuckSpeedXYZ = 0f;
	private float mPuckSpeedOffset = 0f;
	private String mSpeedData;
	private boolean mSpeedDataVisibleX = false;
	private boolean mSpeedDataVisibleY = false;
	private boolean mSpeedDataVisibleZ = false;
	private Button mGenerateButton;
    private Button mHackButton;

	//Rotation Chart
    private float[] mRotationMax = {0f, 0f};
    private LinearLayout mAngularLayout;
	private LineChart mRotationChart;
	private TextView mTopRotationTextView;
	private String mRotationData;

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
	private BluetoothDevice mBluetoothDevice = null;
	private BluetoothGatt mBtGatt = null;
	private List<BluetoothGattService> mServiceList = null;
	private static final int GATT_TIMEOUT = 100; // milliseconds
	private boolean mServicesRdy = false;
	private boolean mIsReceiving = false;

	// SensorTag
	private List<Sensor> mEnabledSensors = new ArrayList<>();
	private BluetoothGattService mOadService = null;
	private BluetoothGattService mConnControlService = null;
	private DecimalFormat decimal = new DecimalFormat("+0.00;-0.00");



	public static ShotStatsFragment newInstance(User user) {
		Bundle args = new Bundle();
		args.putString(Constants.CURRENT_USER, new Gson().toJson(user, User.class));

		ShotStatsFragment fragment = new ShotStatsFragment();
		fragment.setArguments(args);

		return fragment;
	}

	public static ShotStatsFragment newPreviewStatsInstance(ShotTest shotTest) {
		Bundle args = new Bundle();
		args.putString(Constants.TEST_DATA, new Gson().toJson(shotTest, ShotTest.class));

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
			mShotTest = new Gson().fromJson(getArguments().getString(Constants.TEST_DATA), ShotTest.class);
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
	}

	@Override
	public void onPause() {
		Log.d(TAG, "onPause");
		super.onPause();
		if (mIsReceiving) {
			getController().unregisterReceiver(mGattUpdateReceiver);
			mIsReceiving = false;
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
        mFilterCheckB = (CheckBox) v.findViewById(R.id.filter_off_check);
        mPeakAccTV = (TextView) v.findViewById(R.id.peak_acc_number);
        mPeakAccSB = (SeekBar) v.findViewById(R.id.peak_acc_seekbar);

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

		// DEBUGGING DATA
        mFilterCheckB.setChecked(mSettings.getBoolean(CHECK_NOT_FILTER, mFilterCheckB.isChecked()));
        mNotFilter = mFilterCheckB.isChecked();
        mLastStage = mNotFilter;

        mFilterCheckB.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                SharedPreferences.Editor editor = mSettings.edit();
                editor.putBoolean(CHECK_NOT_FILTER, isChecked);
                editor.commit();
                mNotFilter = isChecked;
            }
        });

        if (DEBUG) {
            mHackButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    /*mSensorStartingProgressBar.setVisibility(View.GONE);
                    mSensorStartingTextView.setVisibility(View.GONE);
                    mStartStopButton.setVisibility(View.VISIBLE);
                    mSaveButton.setVisibility(View.VISIBLE);*/

                }
            });


            mGenerateButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mAccelChart.getAxisLeft().setDrawGridLines(true);
                    mSpeedChart.getAxisLeft().setDrawGridLines(true);
                    mRotationChart.getAxisLeft().setDrawGridLines(true);
                    mAccelChart.getAxisLeft().setEnabled(true);
                    mSpeedChart.getAxisLeft().setEnabled(true);
                    mRotationChart.getAxisLeft().setEnabled(true);
                    int item = 1500;
                    Point3D[] acc = new Point3D[item];
                    double[] rot = new double[item];

                    for (int i = 0; i < item; i++) {
                        if (i < 200) {
                            acc[i] = new Point3D(Math.random() * 0.8, 0.0f, 0.0f);
                        } else if (i < 500) {
                            acc[i] = new Point3D((i - 200f) * (1f / 300f) * 10, 0.0f, 0.0f);
                        } else if (i < 1000) {
                            acc[i] = new Point3D((1 - ((i - 500f) * (1f / 500f))) * 10, 0.0f, 0.0f);
                        } else if (i < 1200) {
                            acc[i] = new Point3D(((i - 1000f) * (1f / 200f)) * 20, 0.0f, 0.0f);
                        } else {
                            acc[i] = new Point3D((1 - ((i - 1200f) * (1f / 300f))) * 20, 0.0f, 0.0f);
                        }
                        rot[i] = Math.random() * i;
                    }
                    onDummyChanged(acc, rot);
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

                long value = progress + 1;
                //TODO Use noise found
                int[] newSettings = {VALIDITY_TOKEN, 0x00, 0x00, 0x00, 0x00, 0x10, 0x10, 0x10};
                byte[] send = new byte[20];
                send[0] = SETTINGS_NEW;
                send[1] = 0; //Battery, don't care
                for (int i=0; i<18; i++) {
                    if (i < newSettings.length) {
                        send[2+i] = (byte)newSettings[i];
                    } else {
                        send[2+i] = 0;
                    }
                }
                if (value < 15) {
                    value = value * 2048 / 16;
                    send[5] = (byte)(value & 255);
                    send[6] = (byte) (value/256 & 255);
                    Log.i(TAG, "New value: " + send[5] + ", " + send[6]);
                }
                else
                {
                    value = value * 2048 / 400;
                    send[7] = (byte)(value & 255);
                    send[8] = (byte) (value/256 & 255);
                }
                writeBLE(send);
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

                    mSaveButton.setBackground(ContextCompat.getDrawable(getContext(), R.drawable.small_button_shadow));

                    if (mTestRunning) {
                        mTestRunning = false;
                        mStartStopButton.setText(getString(R.string.reset));
                        mStartStopButton.setCompoundDrawablesWithIntrinsicBounds(R.drawable.reset, 0, 0, 0);
                        mAccelData = new Gson().toJson(mAccelChart.getLineData(), LineData.class);
                        mSpeedData = new Gson().toJson(mSpeedChart.getLineData(), LineData.class);
                        mRotationData = new Gson().toJson(mRotationChart.getLineData(), LineData.class);
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


                        mPuckSpeedOffset = 0f;
                        mCalculatedAverageOffset = false;

                        setupAccelChart();
                        setupSpeedChart();
                        setupRotationChart();

                        mTopAccelXYZTextView.setText("");

                        mTopSpeedXYZTextView.setText("");

                        mTopRotationTextView.setText("");

                        mStartStopButton.setText(getString(R.string.stopTest));
                        mStartStopButton.setCompoundDrawablesWithIntrinsicBounds(android.R.drawable.ic_delete, 0, 0, 0);

                        lastDataTime = 0;
                        dataCounter = 0;
                        mTestRunning = true;
                        mTestStartTime = System.currentTimeMillis();
                        byte[] send = {DATA_READY, 0x00};
                        writeBLE(send);
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

    private void writeBLE(byte[] values) {
        if (mBtGatt != null) {
            Log.i(TAG, "Everything ready");
            BluetoothGattCharacteristic carac =  mBtGatt.getService(SensorDetails.UUID_PUCK_ACC_SERV).getCharacteristic(SensorDetails.UUID_PUCK_WRITE);
            Log.i(TAG, "Service: " + carac + " " + carac.toString());
            carac.setValue(values);
            Log.i(TAG, "Sending successfull: " + mBtGatt.writeCharacteristic(carac));
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
			mDescriptionTextView.setText("Test performed by user:" + mShotTest.getUsername() + " on " + mShotTest.getDate() + "\r\n\r\n" + mShotTest.getDescription());

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

	private void dismissLoadingScreen() {
		mLoadingScreenRelativeLayout.setVisibility(View.GONE);
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
			setSavedAccelData();
			mAccelChart.getAxisLeft().setDrawGridLines(true);
			mAccelChart.getAxisLeft().setEnabled(true);
		}
	}

	private void setSavedAccelData() {
        // To another job
		LineData shotTestAccelData = new Gson().fromJson(mShotTest.getAccelData(), LineData.class);

		for (int i = 0; i < shotTestAccelData.getDataSetCount(); i++) {

			List<Entry> yEntries = shotTestAccelData.getDataSetByIndex(i).getYVals();
			List<String> xValues = shotTestAccelData.getXVals();
			String dataType = shotTestAccelData.getDataSetByIndex(i).getLabel();


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
			setSavedSpeedData();
			mSpeedChart.getAxisLeft().setDrawGridLines(true);
			mSpeedChart.getAxisLeft().setEnabled(true);
		}
	}

	private void setSavedSpeedData() {

		LineData shotTestSpeedData = new Gson().fromJson(mShotTest.getSpeedData(), LineData.class);

		for (int i = 0; i < shotTestSpeedData.getDataSetCount(); i++) {

			List<Entry> yEntries = shotTestSpeedData.getDataSetByIndex(i).getYVals();
			List<String> xValues = shotTestSpeedData.getXVals();

			String dataType = shotTestSpeedData.getDataSetByIndex(i).getLabel();

		}

		dismissLoadingScreen();
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
			setSavedRotationData();
			mRotationChart.getAxisLeft().setDrawGridLines(true);
			mRotationChart.getAxisLeft().setEnabled(true);
		}
	}

	private void setSavedRotationData() {
		LineData shotTestRotationData = new Gson().fromJson(mShotTest.getRotationData(), LineData.class);

		for (int i = 0; i < shotTestRotationData.getDataSetCount(); i++) {
			List<Entry> yEntries = shotTestRotationData.getDataSetByIndex(i).getYVals();
			List<String> xValues = shotTestRotationData.getXVals();


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
			mAccelData = new Gson().toJson(mAccelChart.getLineData(), LineData.class);
			mSpeedData = new Gson().toJson(mSpeedChart.getLineData(), LineData.class);
			mRotationData = new Gson().toJson(mRotationChart.getLineData(), LineData.class);
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
		mBluetoothDevice = getController().getBluetoothDevice();
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


	BluetoothGattService getOadService() {
		return mOadService;
	}

	BluetoothGattService getConnControlService() {
		return mConnControlService;
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
			UUID servUuid = sensor.getService();
			UUID confUuid = sensor.getConfig();
			UUID dataUuid = sensor.getData();

			// Skip keys
			if (confUuid == null)
				break;

			for (int i = 0; i < mBtGatt.getServices().size(); i++) {
				Log.i(TAG, String.format("Going to display service %d %s", i, mBtGatt.getServices().get(i).getUuid()));
			}

			mSensorReady = true;
			activateTestButtons();/*

            BluetoothGattService serv = mBtGatt.getService(servUuid);

            if(serv != null) {
                BluetoothGattCharacteristic charac = serv.getCharacteristic(dataUuid);
                byte value = enable ? sensor.getEnableSensorCode() : Sensor.DISABLE_SENSOR_CODE;
                mBtLeService.writeCharacteristic(charac, value);
                mBtLeService.waitIdle(GATT_TIMEOUT);

                mSensorReady = true;
                activateTestButtons();
            }*/
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
        // Parsing system:
        switch(value[0]) {
            default:
            case SETTINGS_READ:
                if (value[2] != VALIDITY_TOKEN && !mSendOnce) {
                    // Settings don't care, send default ones
                    byte[] send = new byte[20];
                    send[0] = SETTINGS_NEW;
                    send[1] = 0; //Battery, don't care
                    for (int i=0; i<18; i++) {
                        if (i < DEFAULT.length) {
                            send[2+i] = (byte)DEFAULT[i];
                        } else {
                            send[2+i] = 0;
                        }
                    }
                    writeBLE(send);
                    mSendOnce = true;
                }
                break;
            case DATA:

                break;
            case DATA_DRAFT:

                break;
            case DATA_END:

                break;
            case DATA_START:

                break;
        }
        String val = "";
        for (int i=0; i<value.length; i++) {
            val += value[i] + ", ";
        }
        Log.i(TAG, "Value: " + val);
		Point3D accelerationLowSensor;
		Point2D accelerationHighSensor;
		Point3D acceleration;
		double accelX;
		double accelY;
		double accelZ;
		double rotation;

		if (uuidStr.equals(SensorDetails.UUID_PUCK_DATA.toString())) {
			accelerationLowSensor = Sensor.PUCK_ACCELEROMETER.convertLowAccel(value);
			accelerationHighSensor = Sensor.PUCK_ACCELEROMETER.convertHighAccel(value);

			if(accelerationLowSensor.x > 15 || accelerationLowSensor.x < -15){
				accelX = accelerationHighSensor.x;
			}
			else{
				accelX = accelerationLowSensor.x;
			}

			if(accelerationLowSensor.y > 15 || accelerationLowSensor.y < -15){
				accelY = accelerationHighSensor.y;
			}
			else{
				accelY = accelerationLowSensor.y;
			}

			accelZ = accelerationLowSensor.z;

			acceleration = new Point3D(accelX,accelY,accelZ);
            double accelXYZ = Math.sqrt(Math.pow(accelX, 2) + Math.pow(accelY, 2) + Math.pow(accelZ, 2));

			rotation = Sensor.PUCK_ACCELEROMETER.convertRotation(value);

			if (mTestRunning) {
                calculationMethod(acceleration, rotation);
			}
		}
	}

    /**
     * Method to calibrate and see is data can be present to the screen as a "shot".
     * @param acceleration The X, Y and Z acceleration.
     * @param rotation The angular speed
     */
    private void calculationMethod(Point3D acceleration, double rotation) {
        if (!mCalibrationDone) {
            // Calibration process
            mAccelSumX += Math.abs(acceleration.x); // Don't care about the direction.
            mAccelSumY += Math.abs(acceleration.y); // Don't care about the direction.
            mAccelSumZ += Math.abs(acceleration.z); // Don't care about the direction.

            mCalibrationDot ++;

            if (mCalibrationDot >= mCalibrationTime) {
                mCalibrationDone = true;
                mAccelSumX /= mCalibrationDot;
                mAccelSumY /= mCalibrationDot;
                mAccelSumZ /= mCalibrationDot;
            }

            if (mCalibrationDot == 1) {
                mAccelChart.getAxisLeft().setDrawGridLines(true);
                mSpeedChart.getAxisLeft().setDrawGridLines(true);
                mRotationChart.getAxisLeft().setDrawGridLines(true);
                mAccelChart.getAxisLeft().setEnabled(true);
                mSpeedChart.getAxisLeft().setEnabled(true);
                mRotationChart.getAxisLeft().setEnabled(true);
            }
            mTime = 0;
        } else {
            if (mTime % 100 == 0) {
                Log.d(TAG, "Time: " + System.currentTimeMillis());
            }
            mTime++;
            double accelX = Math.abs(acceleration.x) - mAccelSumX;
            double accelY = Math.abs(acceleration.y) - mAccelSumY;
            double accelZ = Math.abs(acceleration.z) - mAccelSumZ;
            Point3D newAcceleration = new Point3D(accelX, accelY, accelZ);
            if (mNotFilter) {
                if (mLastStage != mNotFilter) {
                    //Reset
                    mLastStage = mNotFilter;
                    mIdDatas = 2*mAccDatas.length;
                    mIterGraph = 0;
                }
                // Fill up every data and than let it unavailable for 2 sec
                if (mIdDatas < mAccDatas.length) {
                    mAccDatas[mIdDatas] = newAcceleration;
                    mRotDatas[mIdDatas] = rotation;
                } else if (mIdDatas == mAccDatas.length) {
                    mRecent[3] = mRecent[2];
                    mRecent[2] = mRecent[1];
                    mRecent[1] = mRecent[0];
                    mRecent[0] = new Shot(mAccDatas, mRotDatas, mUser);
                    populateCharts();
                    populateStatisticsFields();
                    mIterGraph = 0;
                } else {
                    mIterGraph++;
                    if (mIterGraph >= mPauseGraph) {
                        mIdDatas = 0;
                    }
                }
                mIdDatas ++;
            } else {
                if (mLastStage != mNotFilter) {
                    // Reset
                    mIterGraph = 0;
                    mLastStage = mNotFilter;
                    mSendData = true;

                }
                if (mSendData) {
                    mIterGraph++;
                    if (mIterGraph >= mPauseGraph) {
                        mIdDatas = 0;
                        mSendData = false;
                        mShotDetected = false;
                    }
                } else {
                    double accelXYZ = Math.sqrt(Math.pow(accelX, 2) + Math.pow(accelY, 2) + Math.pow(accelZ, 2));
                    if (accelXYZ > Integer.parseInt(mPeakAccTV.getText().toString())) {
                        // Case 1, Find a peak
                        mShotDetected = true;
                        mAccDatas[mIdDatas] = newAcceleration;
                        mRotDatas[mIdDatas] = rotation;
                        mIdDatas++;
                    } else if (accelXYZ > MINIMAL_NOISE_G) {
                        // Case 2 , just add
                        mAccDatas[mIdDatas] = newAcceleration;
                        mRotDatas[mIdDatas] = rotation;
                        mIdDatas++;

                    } else if (accelXYZ <= MINIMAL_NOISE_G && mShotDetected) {
                        // Case 3, Ready to send data
                        // Normally check to discard the next 1000 data for rebound issue
                        mSendData = true;
                    } else {
                        mIdDatas = 0;
                    }

                    if (mSendData) {
                        //Rebuild data...
                        Point3D[] sendAcc = new Point3D[mIdDatas];
                        double[] sendRot = new double[mIdDatas];

                        for (int i = 0; i < sendRot.length; i++) {
                            sendAcc[i] = mAccDatas[i];
                            sendRot[i] = mRotDatas[i];
                        }
                        mRecent[3] = mRecent[2];
                        mRecent[2] = mRecent[1];
                        mRecent[1] = mRecent[0];
                        mRecent[0] = new Shot(sendAcc, sendRot, mUser);
                        // 2. If trigger, than show
                        populateCharts();
                        populateStatisticsFields();
                    }
                }
            }
        }

    }

	private void onDummyChanged(Point3D[] acceleration, double[] rotation) {
        if (mNotFilter) {
            mRecent[3] = mRecent[2];
            mRecent[2] = mRecent[1];
            mRecent[1] = mRecent[0];
            mRecent[0] = new Shot(acceleration, rotation, mUser);
            populateCharts();
            populateStatisticsFields();
        } else {
            // 1. Analyse data (Simulated data, normally, only one a a time)
            boolean sendData = false;
            mShotDetected = false;
            mIdDatas = 0; // Normally continue and check if outbound
            for (int i = 0; i < rotation.length; i++) {
                double accelX = acceleration[i].x;
                double accelY = acceleration[i].y;
                double accelZ = acceleration[i].z;
                double accelXYZ = Math.sqrt(Math.pow(accelX, 2) + Math.pow(accelY, 2) + Math.pow(accelZ, 2));

                if (accelXYZ > Integer.parseInt(mPeakAccTV.getText().toString())) {
                    // Case 1, Find a peak
                    mShotDetected = true;
                    mAccDatas[mIdDatas] = acceleration[i];
                    mRotDatas[mIdDatas] = rotation[i];
                    mIdDatas++;
                } else if (accelXYZ > MINIMAL_NOISE_G) {
                    // Case 2 , just add
                    mAccDatas[mIdDatas] = acceleration[i];
                    mRotDatas[mIdDatas] = rotation[i];
                    mIdDatas++;

                } else if (accelXYZ <= MINIMAL_NOISE_G && mShotDetected) {
                    // Case 3, Ready to send data
                    // Normally check to discard the next 1000 data for rebound issue
                    sendData = true;
                    break;
                } else {
                    mIdDatas = 0;
                }
            }

            if (sendData) {
                //Rebuild data...
                Point3D[] sendAcc = new Point3D[mIdDatas];
                double[] sendRot = new double[mIdDatas];

                for (int i = 0; i < sendRot.length; i++) {
                    sendAcc[i] = mAccDatas[i];
                    sendRot[i] = mRotDatas[i];
                }

                // 2. If trigger, than show
                mRecent[3] = mRecent[2];
                mRecent[2] = mRecent[1];
                mRecent[1] = mRecent[0];
                mRecent[0] = new Shot(sendAcc, sendRot, mUser);
                populateCharts();
                populateStatisticsFields();
            } else {
                Toast.makeText(this.getActivity(), "Not parsing done", Toast.LENGTH_SHORT).show();
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


            for (int i = 0; i < mRecent[idData.get(id)].getRotations().length; i++) {
                double accelX = mRecent[idData.get(id)].getAccelerations()[i].x;
                double accelY = mRecent[idData.get(id)].getAccelerations()[i].y;
                double accelZ = mRecent[idData.get(id)].getAccelerations()[i].z;
                double accelXYZ = Math.sqrt(Math.pow(accelX, 2) + Math.pow(accelY, 2) + Math.pow(accelZ, 2));

                mTimeStep = (float) STAMP;

                mPuckSpeedXYZ = (float) Math.sqrt(Math.pow((accelX) * GRAVITY * mTimeStep, 2) +
                        Math.pow((accelY) * GRAVITY * mTimeStep, 2) + Math.pow((accelZ) * GRAVITY * mTimeStep, 2)) / 1000f + mPuckSpeedXYZ;

                //Complete recent data
                mRecent[idData.get(id)].setAccelerationXYZ(accelXYZ, i);
                mRecent[idData.get(id)].setSpeedXYZ(mPuckSpeedXYZ, i);

                addAccelEntry(i * mTimeStep + "", i, (float) accelXYZ, newSetRequired, idData.get(id), id);
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

}
