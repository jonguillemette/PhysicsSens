package com.thirdbridge.pucksensor.controllers;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.content.ContextCompat;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.utils.ColorTemplate;
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
import com.thirdbridge.pucksensor.utils.MathHelper;
import com.thirdbridge.pucksensor.utils.ble_utils.Point2D;
import com.thirdbridge.pucksensor.utils.ble_utils.Point3D;

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Created by Christophe on 2015-10-14.
 */
public class ShotStatsFragment extends BaseFragment {

	private static String TAG = ShotStatsFragment.class.getSimpleName();

	private static final int DUMMY_DATA = 0;
	private static final int XYZ_DATA = 1;
	private static final int X_DATA = 2;
	private static final int Y_DATA = 3;
	private static final int Z_DATA = 4;


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

	//popup items
	private Button mSaveTestPopupButton;
	private Button mCancelSaveTestPopupButton;
	private EditText mTestDescriptionEditText;

	private boolean mTestWasRun = false;
	private int dataCounter = 0;

	//Accel Chart
	private LineChart mAccelChart;
	private LinearLayout mAccelXStatsLinearLayout;
	private LinearLayout mAccelYStatsLinearLayout;
	private LinearLayout mAccelZStatsLinearLayout;
	private TextView mTopAccelXYZTextView;
	private TextView mTopAccelXTextView;
	private TextView mTopAccelYTextView;
	private TextView mTopAccelZTextView;
	private CheckBox mRecordIndividualAccelAxesCheckbox;
	private CheckBox mXAccelDataCheckBox;
	private CheckBox mYAccelDataCheckBox;
	private CheckBox mZAccelDataCheckBox;
	private float mTopAccelXYZ = 0f;
	private float mTopAccelX = 0f;
	private float mTopAccelY = 0f;
	private float mTopAccelZ = 0f;
	private String mAccelData;
	private boolean mAccelDataVisibleX = false;
	private boolean mAccelDataVisibleY = false;
	private boolean mAccelDataVisibleZ = false;
	private boolean mCalculatedAverageOffset = false;

	//Speed Chart
	private LineChart mSpeedChart;
	private LinearLayout mSpeedXStatsLinearLayout;
	private LinearLayout mSpeedYStatsLinearLayout;
	private LinearLayout mSpeedZStatsLinearLayout;
	private TextView mTopSpeedXYZTextView;
	private TextView mTopSpeedXTextView;
	private TextView mTopSpeedYTextView;
	private TextView mTopSpeedZTextView;
	private CheckBox mRecordIndividualSpeedAxesCheckbox;
	private CheckBox mXSpeedDataCheckBox;
	private CheckBox mYSpeedDataCheckBox;
	private CheckBox mZSpeedDataCheckBox;
	private float mTopSpeedXYZ = 0f;
	private float mTopSpeedX = 0f;
	private float mTopSpeedY = 0f;
	private float mTopSpeedZ = 0f;
	private float mPuckSpeedXYZ = 0f;
	private float mPuckSpeedX = 0f;
	private float mPuckSpeedY = 0f;
	private float mPuckSpeedZ = 0f;
	private float mPuckSpeedOffset = 0f;
	private String mSpeedData;
	private boolean mSpeedDataVisibleX = false;
	private boolean mSpeedDataVisibleY = false;
	private boolean mSpeedDataVisibleZ = false;

	//Rotation Chart
	private LineChart mRotationChart;
	private TextView mTopRotationTextView;
	private float mTopRotation = 0f;
	private String mRotationData;

	//Calibration
	private int mCalibrationTime = 2000;
	private float mTimeStep = 0f;
	private final double GRAVITY = 9.80665;
	private double mSpeedGravityOffsetRatio = 0.8f;
	private float mAverageGravityOffset = 0f;
	private int mDataCountWithoutEvent = 200;
	private double mEventDetection;
	private float mAverageAccelX = 0f;
	private float mAverageAccelY = 0f;
	private float mAverageAccelZ = 0f;

	//data visibility variables
	private boolean mShowingSpeed = false;
	private boolean mShowingAccel = false;
	private int mAccelIsGravityCounter = 0;
	private int mPrintDataFrequency = 20;
	private int mMultipleAxesDataFrequency = 10;

	//Accel Chart
	private boolean mNewAccelDataSetRequired = true;
	private int mAccelDataSetIndexXYZ;
	private int mAccelDataSetIndexX;
	private int mAccelDataSetIndexY;
	private int mAccelDataSetIndexZ;
	private List<Integer> mAccelDataSetIndexesXYZ = new ArrayList<>();
	private List<Integer> mAccelDataSetIndexesX = new ArrayList<>();
	private List<Integer> mAccelDataSetIndexesY = new ArrayList<>();
	private List<Integer> mAccelDataSetIndexesZ = new ArrayList<>();
	private Deque<Entry> mAccelEntryBufferXYZ = new ArrayDeque<>();
	private Deque<Entry> mAccelEntryBufferX = new ArrayDeque<>();
	private Deque<Entry> mAccelEntryBufferY = new ArrayDeque<>();
	private Deque<Entry> mAccelEntryBufferZ = new ArrayDeque<>();

	//Speed Chart
	private boolean mNewSpeedDataSetRequired = true;
	private int mSpeedDataSetIndexXYZ;
	private int mSpeedDataSetIndexX;
	private int mSpeedDataSetIndexY;
	private int mSpeedDataSetIndexZ;
	private List<Integer> mSpeedDataSetIndexesXYZ = new ArrayList<>();
	private List<Integer> mSpeedDataSetIndexesX = new ArrayList<>();
	private List<Integer> mSpeedDataSetIndexesY = new ArrayList<>();
	private List<Integer> mSpeedDataSetIndexesZ = new ArrayList<>();
	private Deque<Entry> mSpeedEntryBufferXYZ = new ArrayDeque<>();
	private Deque<Entry> mSpeedEntryBufferX = new ArrayDeque<>();
	private Deque<Entry> mSpeedEntryBufferY = new ArrayDeque<>();
	private Deque<Entry> mSpeedEntryBufferZ = new ArrayDeque<>();

	// BLE
	private boolean mSensorReady = false;
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



	private void setTestVariables(){

		//Calibration time in miliseconds
		mCalibrationTime = 2000;

		//To plot the speed chart, a value of (GRAVITY * TIME_STEP * RATIO) must be substracted from the calculated value
		mSpeedGravityOffsetRatio = 0.8;

		//This variable represents the necessary value (g) needed to trigger an event
		//Example:
		// if(Math.abs(calculatedAcceleration-accelerationAtRest) > mEventDetection)
		// 	{ trigger event }
		mEventDetection = 0.5;

		//After a number of event-less data, we stop recording new data until an event in triggered
		mDataCountWithoutEvent = 200;

		//Print one value out of x on the graph, increase the value of this variable for better performance
		mPrintDataFrequency = 20;

		//When the user decides to record individual axes, this variable is added to mPrintDataFrequency to enhance performance
		mMultipleAxesDataFrequency = 10;
	}



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

		setTestVariables();

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

		mLoadingScreenRelativeLayout = (RelativeLayout) v.findViewById(R.id.loading_screen_relative_layout);

		//Accel chart
		mAccelChart = (LineChart) v.findViewById(R.id.accel_stats_chart);
		mAccelXStatsLinearLayout = (LinearLayout) v.findViewById(R.id.accel_x_stats_box_linear_layout);
		mAccelYStatsLinearLayout = (LinearLayout) v.findViewById(R.id.accel_y_stats_box_linear_layout);
		mAccelZStatsLinearLayout = (LinearLayout) v.findViewById(R.id.accel_z_stats_box_linear_layout);
		mTopAccelXYZTextView = (TextView) v.findViewById(R.id.top_accel_xyz_textview);
		mTopAccelXTextView = (TextView) v.findViewById(R.id.top_accel_x_textview);
		mTopAccelYTextView = (TextView) v.findViewById(R.id.top_accel_y_textview);
		mTopAccelZTextView = (TextView) v.findViewById(R.id.top_accel_z_textview);
		mRecordIndividualAccelAxesCheckbox = (CheckBox) v.findViewById(R.id.accel_individual_axes_checkbox);
		mXAccelDataCheckBox = (CheckBox) v.findViewById(R.id.accel_x_checkbox);
		mYAccelDataCheckBox = (CheckBox) v.findViewById(R.id.accel_y_checkbox);
		mZAccelDataCheckBox = (CheckBox) v.findViewById(R.id.accel_z_checkbox);

		//Speed Chart
		mSpeedChart = (LineChart) v.findViewById(R.id.speed_stats_chart);
		mSpeedXStatsLinearLayout = (LinearLayout) v.findViewById(R.id.speed_x_stats_box_linear_layout);
		mSpeedYStatsLinearLayout = (LinearLayout) v.findViewById(R.id.speed_y_stats_box_linear_layout);
		mSpeedZStatsLinearLayout = (LinearLayout) v.findViewById(R.id.speed_z_stats_box_linear_layout);
		mTopSpeedXYZTextView = (TextView) v.findViewById(R.id.top_speed_xyz_textview);
		mTopSpeedXTextView = (TextView) v.findViewById(R.id.top_speed_x_textview);
		mTopSpeedYTextView = (TextView) v.findViewById(R.id.top_speed_y_textview);
		mTopSpeedZTextView = (TextView) v.findViewById(R.id.top_speed_z_textview);
		mRecordIndividualSpeedAxesCheckbox = (CheckBox) v.findViewById(R.id.speed_individual_axes_checkbox);
		mXSpeedDataCheckBox = (CheckBox) v.findViewById(R.id.speed_x_checkbox);
		mYSpeedDataCheckBox = (CheckBox) v.findViewById(R.id.speed_y_checkbox);
		mZSpeedDataCheckBox = (CheckBox) v.findViewById(R.id.speed_z_checkbox);

		//Rotation Chart
		mRotationChart = (LineChart) v.findViewById(R.id.rotation_stats_chart);
		mTopRotationTextView = (TextView) v.findViewById(R.id.top_rotation_textview);

		final PopupWindow popupWindow = inflateSaveTestPopup(container);

		mStartStopButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {

				if (mSensorReady) {

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
						mRecordIndividualAccelAxesCheckbox.setEnabled(true);
						mRecordIndividualSpeedAxesCheckbox.setEnabled(true);
						mRecordIndividualAccelAxesCheckbox.setChecked(false);
						mRecordIndividualSpeedAxesCheckbox.setChecked(false);
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

						mAverageAccelX = 0f;
						mAverageAccelY = 0f;
						mAverageAccelZ = 0f;

						mTopAccelXYZ = 0f;
						mTopAccelX = 0f;
						mTopAccelY = 0f;
						mTopAccelZ = 0f;

						mTopSpeedXYZ = 0f;
						mTopSpeedX = 0f;
						mTopSpeedY = 0f;
						mTopSpeedZ = 0f;

						mTopRotation = 0f;
						mPuckSpeedXYZ = 0f;
						mPuckSpeedX = 0f;
						mPuckSpeedY = 0f;
						mPuckSpeedZ = 0f;

						mAccelDataSetIndexesXYZ.clear();
						mAccelDataSetIndexesX.clear();
						mAccelDataSetIndexesY.clear();
						mAccelDataSetIndexesZ.clear();

						mAccelEntryBufferXYZ.clear();
						mAccelEntryBufferX.clear();
						mAccelEntryBufferY.clear();
						mAccelEntryBufferZ.clear();

						mSpeedDataSetIndexesXYZ.clear();
						mSpeedDataSetIndexesX.clear();
						mSpeedDataSetIndexesY.clear();
						mSpeedDataSetIndexesZ.clear();

						mSpeedEntryBufferXYZ.clear();
						mSpeedEntryBufferX.clear();
						mSpeedEntryBufferY.clear();
						mSpeedEntryBufferZ.clear();

						mPuckSpeedOffset = 0f;
						mAverageGravityOffset = 0f;
						mCalculatedAverageOffset = false;

						setupAccelChart();
						setupSpeedChart();
						setupRotationChart();

						mTopAccelXYZTextView.setText("");
						mTopAccelXTextView.setText("");
						mTopAccelYTextView.setText("");
						mTopAccelZTextView.setText("");

						mTopSpeedXYZTextView.setText("");
						mTopAccelXTextView.setText("");
						mTopAccelYTextView.setText("");
						mTopAccelZTextView.setText("");

						mTopRotationTextView.setText("");

						mStartStopButton.setText(getString(R.string.stopTest));
						mStartStopButton.setCompoundDrawablesWithIntrinsicBounds(android.R.drawable.ic_delete, 0, 0, 0);

						mRecordIndividualAccelAxesCheckbox.setEnabled(false);
						mRecordIndividualSpeedAxesCheckbox.setEnabled(false);

						lastDataTime = 0;
						dataCounter = 0;
						mTestRunning = true;
						mTestStartTime = System.currentTimeMillis();
					}
				}
			}
		});

		mSaveButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				if (mTestWasRun && !mTestRunning) {
					((EditText) popupWindow.getContentView().findViewById(R.id.test_description_edittext)).setText("");
					popupWindow.setFocusable(true);
					popupWindow.update();
					popupWindow.showAtLocation(container, Gravity.TOP, 0, 200);

					InputMethodManager imm = (InputMethodManager)
							getController().getSystemService(Context.INPUT_METHOD_SERVICE);
					if (imm != null) {
						imm.toggleSoftInput(InputMethodManager.SHOW_IMPLICIT, 0);
					}

				}
			}
		});

		mRecordIndividualAccelAxesCheckbox.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				if(mRecordIndividualAccelAxesCheckbox.isChecked()){
					mXAccelDataCheckBox.setVisibility(View.VISIBLE);
					mYAccelDataCheckBox.setVisibility(View.VISIBLE);
					mZAccelDataCheckBox.setVisibility(View.VISIBLE);

					mAccelXStatsLinearLayout.setVisibility(View.VISIBLE);
					mAccelYStatsLinearLayout.setVisibility(View.VISIBLE);
					mAccelZStatsLinearLayout.setVisibility(View.VISIBLE);

					mAccelDataVisibleX = false;
					mAccelDataVisibleY = false;
					mAccelDataVisibleZ = false;

					mXAccelDataCheckBox.setChecked(false);
					mYAccelDataCheckBox.setChecked(false);
					mZAccelDataCheckBox.setChecked(false);
				}
				else{
					mXAccelDataCheckBox.setVisibility(View.GONE);
					mYAccelDataCheckBox.setVisibility(View.GONE);
					mZAccelDataCheckBox.setVisibility(View.GONE);

					mAccelXStatsLinearLayout.setVisibility(View.GONE);
					mAccelYStatsLinearLayout.setVisibility(View.GONE);
					mAccelZStatsLinearLayout.setVisibility(View.GONE);
				}
			}
		});

		mXAccelDataCheckBox.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				toggleAccelChartData(X_DATA);
			}
		});

		mYAccelDataCheckBox.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				toggleAccelChartData(Y_DATA);
			}
		});

		mZAccelDataCheckBox.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				toggleAccelChartData(Z_DATA);
			}
		});

		mRecordIndividualSpeedAxesCheckbox.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				if(mRecordIndividualSpeedAxesCheckbox.isChecked()){
					mXSpeedDataCheckBox.setVisibility(View.VISIBLE);
					mYSpeedDataCheckBox.setVisibility(View.VISIBLE);
					mZSpeedDataCheckBox.setVisibility(View.VISIBLE);

					mSpeedXStatsLinearLayout.setVisibility(View.VISIBLE);
					mSpeedYStatsLinearLayout.setVisibility(View.VISIBLE);
					mSpeedZStatsLinearLayout.setVisibility(View.VISIBLE);

					mSpeedDataVisibleX = false;
					mSpeedDataVisibleY = false;
					mSpeedDataVisibleZ = false;

					mXSpeedDataCheckBox.setChecked(false);
					mYSpeedDataCheckBox.setChecked(false);
					mZSpeedDataCheckBox.setChecked(false);
				}
				else{
					mXSpeedDataCheckBox.setVisibility(View.GONE);
					mYSpeedDataCheckBox.setVisibility(View.GONE);
					mZSpeedDataCheckBox.setVisibility(View.GONE);

					mSpeedXStatsLinearLayout.setVisibility(View.GONE);
					mSpeedYStatsLinearLayout.setVisibility(View.GONE);
					mSpeedZStatsLinearLayout.setVisibility(View.GONE);
				}
			}
		});

		mXSpeedDataCheckBox.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				toggleSpeedChartData(X_DATA);
			}
		});

		mYSpeedDataCheckBox.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				toggleSpeedChartData(Y_DATA);
			}
		});

		mZSpeedDataCheckBox.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				toggleSpeedChartData(Z_DATA);
			}
		});

		return v;
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
			mRecordIndividualAccelAxesCheckbox.setVisibility(View.GONE);
			mRecordIndividualSpeedAxesCheckbox.setVisibility(View.GONE);

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
			//accel
			for (int setIndex : mAccelDataSetIndexesXYZ) {
				if (mAccelChart.getLineData().getDataSetByIndex(setIndex).getYMax() > mTopAccelXYZ) {
					mTopAccelXYZ = mAccelChart.getLineData().getDataSetByIndex(setIndex).getYMax();
				}
			}

			for (int setIndex : mAccelDataSetIndexesX) {
				if (mAccelChart.getLineData().getDataSetByIndex(setIndex).getYMax() > mTopAccelX) {
					mTopAccelXYZ = mAccelChart.getLineData().getDataSetByIndex(setIndex).getYMax();
				}
			}

			for (int setIndex : mAccelDataSetIndexesY) {
				if (mAccelChart.getLineData().getDataSetByIndex(setIndex).getYMax() > mTopAccelY) {
					mTopAccelY = mAccelChart.getLineData().getDataSetByIndex(setIndex).getYMax();
				}
			}

			for (int setIndex : mAccelDataSetIndexesZ) {
				if (mAccelChart.getLineData().getDataSetByIndex(setIndex).getYMax() > mTopAccelZ) {
					mTopAccelZ = mAccelChart.getLineData().getDataSetByIndex(setIndex).getYMax();
				}
			}


			mTopAccelXYZTextView.setText(MathHelper.round(mTopAccelXYZ, 2) + "g");
			mTopAccelXTextView.setText(MathHelper.round(mTopAccelX, 2) + "g");
			mTopAccelYTextView.setText(MathHelper.round(mTopAccelY, 2) + "g");
			mTopAccelZTextView.setText(MathHelper.round(mTopAccelZ, 2) + "g");
		}

		if (mSpeedChart != null) {
			//speed
			for (int setIndex : mSpeedDataSetIndexesXYZ) {
				if (mSpeedChart.getLineData().getDataSetByIndex(setIndex).getYMax() > mTopSpeedXYZ) {
					mTopSpeedXYZ = mSpeedChart.getLineData().getDataSetByIndex(setIndex).getYMax();
				}
			}
			for (int setIndex : mSpeedDataSetIndexesX) {
				if (mSpeedChart.getLineData().getDataSetByIndex(setIndex).getYMax() > mTopSpeedX) {
					mTopSpeedX = mSpeedChart.getLineData().getDataSetByIndex(setIndex).getYMax();
				}
			}
			for (int setIndex : mSpeedDataSetIndexesY) {
				if (mSpeedChart.getLineData().getDataSetByIndex(setIndex).getYMax() > mTopSpeedY) {
					mTopSpeedY = mSpeedChart.getLineData().getDataSetByIndex(setIndex).getYMax();
				}
			}
			for (int setIndex : mSpeedDataSetIndexesZ) {
				if (mSpeedChart.getLineData().getDataSetByIndex(setIndex).getYMax() > mTopSpeedZ) {
					mTopSpeedZ = mSpeedChart.getLineData().getDataSetByIndex(setIndex).getYMax();
				}
			}

			mTopSpeedXYZTextView.setText(MathHelper.round(mTopSpeedXYZ, 2) + " m/s");
			mTopSpeedXTextView.setText(MathHelper.round(mTopSpeedX, 2) + " m/s");
			mTopSpeedYTextView.setText(MathHelper.round(mTopSpeedY, 2) + " m/s");
			mTopSpeedZTextView.setText(MathHelper.round(mTopSpeedZ, 2) + " m/s");
		}

		if (mRotationChart != null) {
			//rotation
			if (mAccelChart.getLineData().getDataSetByIndex(0) != null)
				mTopRotation = mRotationChart.getLineData().getDataSetByIndex(0).getYMax();
			mTopRotationTextView.setText(MathHelper.round(mTopRotation, 2) + " degrees/s");
		}
	}

	private PopupWindow inflateSaveTestPopup(ViewGroup container) {

		LayoutInflater layoutInflater
				= (LayoutInflater) getController().getBaseContext()
				.getSystemService(getController().LAYOUT_INFLATER_SERVICE);

		final View popupView = layoutInflater.inflate(R.layout.save_test_popup, container, false);

		mSaveTestPopupButton = (Button) popupView.findViewById(R.id.save_test_button);
		mCancelSaveTestPopupButton = (Button) popupView.findViewById(R.id.cancel_test_button);
		mTestDescriptionEditText = (EditText) popupView.findViewById(R.id.test_description_edittext);

		final PopupWindow popupWindow = new PopupWindow(
				popupView,
				1000,
				ViewGroup.LayoutParams.WRAP_CONTENT);

		mSaveTestPopupButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				saveTest(mTestDescriptionEditText.getText().toString());
				popupWindow.dismiss();
			}
		});

		mCancelSaveTestPopupButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				popupWindow.dismiss();
			}
		});

		return popupWindow;
	}

	private void saveTest(String testDescription) {
		DateFormat df = new SimpleDateFormat("EEE, d MMM yyyy HH:mm a");
		String date = df.format(Calendar.getInstance().getTime());
		ShotTest shotTest = new ShotTest(mUser.getId(), mUser.getFirstName() + " " + mUser.getLastName(), date, testDescription, mAccelData, mSpeedData, mRotationData);
		DataManager.get().addTest(shotTest);
	}

	private void toggleAccelChartData(int chartDataTye) {
		switch (chartDataTye) {
			case X_DATA:
				if (mAccelChart != null && mAccelChart.getLineData() != null) {

					mAccelDataVisibleX = !mAccelDataVisibleX;

					boolean canToggleVisibility = true;

					for (Integer setIndex : mAccelDataSetIndexesX) {
						if (mAccelChart.getLineData().getDataSetByIndex(setIndex) == null) {
							canToggleVisibility = false;
						}
					}

					if (canToggleVisibility) {

						for (Integer setIndex : mAccelDataSetIndexesX) {
							mAccelChart.getLineData().getDataSetByIndex(setIndex).setVisible(mAccelDataVisibleX);
						}
						mAccelChart.invalidate();
					}
				}
				break;
			case Y_DATA:
				if (mAccelChart != null && mAccelChart.getLineData() != null) {

					mAccelDataVisibleY = !mAccelDataVisibleY;

					boolean canToggleVisibility = true;

					for (Integer setIndex : mAccelDataSetIndexesY) {
						if (mAccelChart.getLineData().getDataSetByIndex(setIndex) == null) {
							canToggleVisibility = false;
						}
					}

					if (canToggleVisibility) {
						for (Integer setIndex : mAccelDataSetIndexesY) {
							mAccelChart.getLineData().getDataSetByIndex(setIndex).setVisible(mAccelDataVisibleY);
						}
						mAccelChart.invalidate();
					}
				}
				break;
			case Z_DATA:
				if (mAccelChart != null && mAccelChart.getLineData() != null) {

					mAccelDataVisibleZ = !mAccelDataVisibleZ;

					boolean canToggleVisibility = true;

					for (Integer setIndex : mAccelDataSetIndexesZ) {
						if (mAccelChart.getLineData().getDataSetByIndex(setIndex) == null) {
							canToggleVisibility = false;
						}
					}

					if (canToggleVisibility) {

						for (Integer setIndex : mAccelDataSetIndexesZ) {
							mAccelChart.getLineData().getDataSetByIndex(setIndex).setVisible(mAccelDataVisibleZ);
						}
						mAccelChart.invalidate();
					}
				}
				break;
		}
	}

	private void toggleSpeedChartData(int chartDataTye) {
		switch (chartDataTye) {
			case X_DATA:
				if (mSpeedChart != null && mSpeedChart.getLineData() != null) {

					mSpeedDataVisibleX = !mSpeedDataVisibleX;

					boolean canToggleVisibility = true;

					for (Integer setIndex : mSpeedDataSetIndexesX) {
						if (mSpeedChart.getLineData().getDataSetByIndex(setIndex) == null) {
							canToggleVisibility = false;
						}
					}

					if (canToggleVisibility) {
						for (Integer setIndex : mSpeedDataSetIndexesX) {
							mSpeedChart.getLineData().getDataSetByIndex(setIndex).setVisible(mSpeedDataVisibleX);
						}
						mSpeedChart.invalidate();
					}
				}
				break;
			case Y_DATA:
				if (mSpeedChart != null && mSpeedChart.getLineData() != null) {

					mSpeedDataVisibleY = !mSpeedDataVisibleY;

					boolean canToggleVisibility = true;

					for (Integer setIndex : mSpeedDataSetIndexesY) {
						if (mSpeedChart.getLineData().getDataSetByIndex(setIndex) == null) {
							canToggleVisibility = false;
						}
					}

					if (canToggleVisibility) {

						for (Integer setIndex : mSpeedDataSetIndexesY) {
							mSpeedChart.getLineData().getDataSetByIndex(setIndex).setVisible(mSpeedDataVisibleY);
						}
						mSpeedChart.invalidate();
					}
				}
				break;
			case Z_DATA:
				if (mSpeedChart != null && mSpeedChart.getLineData() != null) {

					mSpeedDataVisibleZ = !mSpeedDataVisibleZ;

					boolean canToggleVisibility = true;

					for (Integer setIndex : mSpeedDataSetIndexesZ) {
						if (mSpeedChart.getLineData().getDataSetByIndex(setIndex) == null) {
							canToggleVisibility = false;
						}
					}

					if (canToggleVisibility) {

						for (Integer setIndex : mSpeedDataSetIndexesZ) {
							mSpeedChart.getLineData().getDataSetByIndex(setIndex).setVisible(mSpeedDataVisibleZ);
						}
						mSpeedChart.invalidate();
					}
				}
				break;
		}
	}

	private void setupAccelChart() {
		mAccelChart.setNoDataTextDescription("");

		mAccelChart.setAutoScaleMinMaxEnabled(true);

		mAccelChart.getLegend().setEnabled(false);

		// enable touch gestures
		mAccelChart.setTouchEnabled(true);

		// enable scaling and dragging
		mAccelChart.setDragEnabled(true);
		mAccelChart.setScaleEnabled(true);
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

		LineData shotTestAccelData = new Gson().fromJson(mShotTest.getAccelData(), LineData.class);

		for (int i = 0; i < shotTestAccelData.getDataSetCount(); i++) {

			List<Entry> yEntries = shotTestAccelData.getDataSetByIndex(i).getYVals();
			List<String> xValues = shotTestAccelData.getXVals();
			String dataType = shotTestAccelData.getDataSetByIndex(i).getLabel();

			mNewAccelDataSetRequired = true;

			switch (dataType) {
				case "":
					for (int k = 0; k < yEntries.size(); k++) {
						//addAccelEntry(DUMMY_DATA, xValues.get(k), 0, false);
						mNewAccelDataSetRequired = false;
					}
					break;
				case "XYZ":
					for (int k = 0; k < yEntries.size(); k++) {
						addAccelEntry(XYZ_DATA, xValues.get(yEntries.get(k).getXIndex()), yEntries.get(k).getVal(), true);
						mNewAccelDataSetRequired = false;
					}
					break;
				case "X":
					mXAccelDataCheckBox.setVisibility(View.VISIBLE);
					mAccelXStatsLinearLayout.setVisibility(View.VISIBLE);
					for (int k = 0; k < yEntries.size(); k++) {
						addAccelEntry(X_DATA, xValues.get(yEntries.get(k).getXIndex()), yEntries.get(k).getVal(), true);
						mNewAccelDataSetRequired = false;
					}
					break;
				case "Y":
					mYAccelDataCheckBox.setVisibility(View.VISIBLE);
					mAccelYStatsLinearLayout.setVisibility(View.VISIBLE);
					for (int k = 0; k < yEntries.size(); k++) {
						addAccelEntry(Y_DATA, xValues.get(yEntries.get(k).getXIndex()), yEntries.get(k).getVal(), true);
						mNewAccelDataSetRequired = false;
					}
					break;
				case "Z":
					mZAccelDataCheckBox.setVisibility(View.VISIBLE);
					mAccelZStatsLinearLayout.setVisibility(View.VISIBLE);
					for (int k = 0; k < yEntries.size(); k++) {
						addAccelEntry(Z_DATA, xValues.get(yEntries.get(k).getXIndex()), yEntries.get(k).getVal(), true);
						mNewAccelDataSetRequired = false;
					}
					break;
				default:
					break;
			}
		}

	}

	private void addAccelEntry(int dataType, String xValue, float yValue, boolean showEntry) {

		LineData data = mAccelChart.getData();

		if (data != null) {

			LineDataSet currentLineDataSet = null;

			// add a new x-value first
			if (mPreviewTest) {
				if (!data.getXVals().contains(xValue)) {
					data.addXValue(xValue);
				}
			} else {
				if (!data.getXVals().contains(xValue + " ms"))
					data.addXValue(xValue + " ms");
			}

			if (Math.abs(yValue) > Math.abs(mTopAccelXYZ) && Math.abs(yValue) > Math.abs(mTopAccelX) && Math.abs(yValue) > Math.abs(mTopAccelY) && Math.abs(yValue) > Math.abs(mTopAccelZ)) {
				YAxis leftAxis = mAccelChart.getAxisLeft();
				leftAxis.setAxisMaxValue(Math.abs(yValue));
				leftAxis.setAxisMinValue(-Math.abs(yValue));
			}

			switch (dataType) {
				case DUMMY_DATA:
					currentLineDataSet = data.getDataSetByIndex(DUMMY_DATA);
					if (currentLineDataSet == null) {
						currentLineDataSet = createSet("", ColorTemplate.getHoloBlue(), 0f, 0f, true);
						data.addDataSet(currentLineDataSet);
						currentLineDataSet.setVisible(false);
					}
					data.addEntry(new Entry(yValue, data.getXValCount()), dataType);
					break;
				case XYZ_DATA:
					if (showEntry) {
						if (mNewAccelDataSetRequired) {
							currentLineDataSet = createSet("XYZ", ColorTemplate.getHoloBlue(), 2f, 2f, true);
							data.addDataSet(currentLineDataSet);
							mAccelDataSetIndexXYZ = data.getIndexOfDataSet(currentLineDataSet);
							mAccelDataSetIndexesXYZ.add(mAccelDataSetIndexXYZ);

							int accelBufferSize = mAccelEntryBufferXYZ.size();

							for (int i = 0; i < accelBufferSize; i++) {
								data.addEntry(mAccelEntryBufferXYZ.getLast(), mAccelDataSetIndexXYZ);
								mAccelEntryBufferXYZ.removeLast();
							}

						}
						if (!mPreviewTest)
							data.addEntry(new Entry(yValue, data.getXValCount()), mAccelDataSetIndexXYZ);
						else
							data.addEntry(new Entry(yValue, data.getXVals().indexOf(xValue)), mAccelDataSetIndexXYZ);
					} else {
						if (mAccelEntryBufferXYZ.size() >= 5) {
							mAccelEntryBufferXYZ.removeLast();
						}
						mAccelEntryBufferXYZ.push(new Entry(yValue, data.getXValCount()));
					}

					if (Math.abs(yValue) > Math.abs(mTopAccelXYZ)) {
						mTopAccelXYZ = Math.abs(yValue);
					}
					break;
				case X_DATA:
					if (showEntry) {
						if (mNewAccelDataSetRequired) {
							currentLineDataSet = createSet("X", Color.RED, 1f, 1.5f, true);
							data.addDataSet(currentLineDataSet);
							mAccelDataSetIndexX = data.getIndexOfDataSet(currentLineDataSet);
							mAccelDataSetIndexesX.add(mAccelDataSetIndexX);

							if (!mAccelDataVisibleX) {
								data.getDataSetByIndex(mAccelDataSetIndexX).setVisible(false);
								mAccelChart.invalidate();
							}

							int accelBufferSize = mAccelEntryBufferX.size();

							for (int i = 0; i < accelBufferSize; i++) {
								data.addEntry(mAccelEntryBufferX.getLast(), mAccelDataSetIndexX);
								mAccelEntryBufferX.removeLast();
							}

						}
						if (!mPreviewTest)
							data.addEntry(new Entry(yValue, data.getXValCount()), mAccelDataSetIndexX);
						else
							data.addEntry(new Entry(yValue, data.getXVals().indexOf(xValue)), mAccelDataSetIndexX);
					} else {
						if (mAccelEntryBufferX.size() >= 5) {
							mAccelEntryBufferX.removeLast();
						}
						mAccelEntryBufferX.push(new Entry(yValue, data.getXValCount()));
					}
					if (Math.abs(yValue) > Math.abs(mTopAccelX)) {
						mTopAccelX = yValue;
					}
					break;
				case Y_DATA:
					if (showEntry) {
						if (mNewAccelDataSetRequired) {
							currentLineDataSet = createSet("Y", Color.GREEN, 1f, 1.5f, true);
							data.addDataSet(currentLineDataSet);
							mAccelDataSetIndexY = data.getIndexOfDataSet(currentLineDataSet);
							mAccelDataSetIndexesY.add(mAccelDataSetIndexY);

							if (!mAccelDataVisibleY) {
								data.getDataSetByIndex(mAccelDataSetIndexY).setVisible(false);
								mAccelChart.invalidate();
							}

							int accelBufferSize = mAccelEntryBufferY.size();

							for (int i = 0; i < accelBufferSize; i++) {
								data.addEntry(mAccelEntryBufferY.getLast(), mAccelDataSetIndexY);
								mAccelEntryBufferY.removeLast();
							}

						}
						if (!mPreviewTest)
							data.addEntry(new Entry(yValue, data.getXValCount()), mAccelDataSetIndexY);
						else
							data.addEntry(new Entry(yValue, data.getXVals().indexOf(xValue)), mAccelDataSetIndexY);
					} else {
						if (mAccelEntryBufferY.size() >= 5) {
							mAccelEntryBufferY.removeLast();
						}
						mAccelEntryBufferY.push(new Entry(yValue, data.getXValCount()));
					}
					if (Math.abs(yValue) > Math.abs(mTopAccelY)) {
						mTopAccelY = yValue;
					}
					break;
				case Z_DATA:
					if (showEntry) {
						if (mNewAccelDataSetRequired) {
							currentLineDataSet = createSet("Z", Color.MAGENTA, 1f, 1.5f, true);
							data.addDataSet(currentLineDataSet);
							mAccelDataSetIndexZ = data.getIndexOfDataSet(currentLineDataSet);
							mAccelDataSetIndexesZ.add(mAccelDataSetIndexZ);

							if (!mAccelDataVisibleZ) {
								data.getDataSetByIndex(mAccelDataSetIndexZ).setVisible(false);
								mAccelChart.invalidate();
							}

							int accelBufferSize = mAccelEntryBufferZ.size();

							for (int i = 0; i < accelBufferSize; i++) {
								data.addEntry(mAccelEntryBufferZ.getLast(), mAccelDataSetIndexZ);
								mAccelEntryBufferZ.removeLast();
							}
						}
						if (!mPreviewTest)
							data.addEntry(new Entry(yValue, data.getXValCount()), mAccelDataSetIndexZ);
						else
							data.addEntry(new Entry(yValue, data.getXVals().indexOf(xValue)), mAccelDataSetIndexZ);
					} else {
						if (mAccelEntryBufferZ.size() >= 5) {
							mAccelEntryBufferZ.removeLast();
						}
						mAccelEntryBufferZ.push(new Entry(yValue, data.getXValCount()));
					}
					if (Math.abs(yValue) > Math.abs(mTopAccelZ)) {
						mTopAccelZ = yValue;
					}
					break;
			}

			// let the chart know it's data has changed
			mAccelChart.notifyDataSetChanged();

			// limit the number of visible entries
			mAccelChart.setVisibleXRangeMaximum(60);

			// move to the latest entry
			if(!mPreviewTest)
				mAccelChart.moveViewToX(data.getXValCount() - 61);
		}
	}


	private void setupSpeedChart() {
		mSpeedChart.setNoDataTextDescription("");

		mSpeedChart.setAutoScaleMinMaxEnabled(true);

		mSpeedChart.getLegend().setEnabled(false);

		// enable touch gestures
		mSpeedChart.setTouchEnabled(true);

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

			mNewSpeedDataSetRequired = true;

			switch (dataType) {
				case "":
					for (int k = 0; k < yEntries.size(); k++) {
						//addSpeedEntry(DUMMY_DATA, xValues.get(k), 0, false);
						mNewSpeedDataSetRequired = false;
					}
					break;
				case "XYZ":
					for (int k = 0; k < yEntries.size(); k++) {
						addSpeedEntry(XYZ_DATA, xValues.get(yEntries.get(k).getXIndex()), yEntries.get(k).getVal(), true);
						mNewSpeedDataSetRequired = false;
					}
					break;
				case "X":
					mXSpeedDataCheckBox.setVisibility(View.VISIBLE);
					mSpeedXStatsLinearLayout.setVisibility(View.VISIBLE);
					for (int k = 0; k < yEntries.size(); k++) {
						addSpeedEntry(X_DATA, xValues.get(yEntries.get(k).getXIndex()), yEntries.get(k).getVal(), true);
						mNewSpeedDataSetRequired = false;
					}
					break;
				case "Y":
					mYSpeedDataCheckBox.setVisibility(View.VISIBLE);
					mSpeedYStatsLinearLayout.setVisibility(View.VISIBLE);
					for (int k = 0; k < yEntries.size(); k++) {
						addSpeedEntry(Y_DATA, xValues.get(yEntries.get(k).getXIndex()), yEntries.get(k).getVal(), true);
						mNewSpeedDataSetRequired = false;
					}
					break;
				case "Z":
					mZSpeedDataCheckBox.setVisibility(View.VISIBLE);
					mSpeedZStatsLinearLayout.setVisibility(View.VISIBLE);
					for (int k = 0; k < yEntries.size(); k++) {
						addSpeedEntry(Z_DATA, xValues.get(yEntries.get(k).getXIndex()), yEntries.get(k).getVal(), true);
						mNewSpeedDataSetRequired = false;
					}
					break;
				default:
					break;
			}
		}

		dismissLoadingScreen();
	}

	private void addSpeedEntry(int dataType, String xValue, float yValue, boolean showEntry) {

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

			if (Math.abs(yValue) > Math.abs(mTopSpeedXYZ) && Math.abs(yValue) > Math.abs(mTopSpeedX) && Math.abs(yValue) > Math.abs(mTopSpeedY) && dataType != Z_DATA) {
				YAxis leftAxis = mSpeedChart.getAxisLeft();
				leftAxis.setAxisMaxValue(Math.abs(yValue));
				leftAxis.setAxisMinValue(-Math.abs(yValue));
			}

			switch (dataType) {
				case DUMMY_DATA:
					currentLineDataSet = data.getDataSetByIndex(DUMMY_DATA);
					if (currentLineDataSet == null) {
						currentLineDataSet = createSet("", ColorTemplate.getHoloBlue(), 0f, 0f, true);
						data.addDataSet(currentLineDataSet);
						currentLineDataSet.setVisible(false);
					}
					data.addEntry(new Entry(yValue, data.getXValCount()), dataType);
					break;
				case XYZ_DATA:
					if (showEntry) {
						if (mNewSpeedDataSetRequired) {
							currentLineDataSet = createSet("XYZ", ColorTemplate.getHoloBlue(), 2f, 2f, true);
							data.addDataSet(currentLineDataSet);
							mSpeedDataSetIndexXYZ = data.getIndexOfDataSet(currentLineDataSet);
							mSpeedDataSetIndexesXYZ.add(mSpeedDataSetIndexXYZ);

							int speedBufferSize = mSpeedEntryBufferXYZ.size();

							for (int i = 0; i < speedBufferSize; i++) {
								data.addEntry(mSpeedEntryBufferXYZ.getLast(), mSpeedDataSetIndexXYZ);
								mSpeedEntryBufferXYZ.removeLast();
							}
						}
						if (!mPreviewTest)
							data.addEntry(new Entry(yValue, data.getXValCount()), mSpeedDataSetIndexXYZ);
						else
							data.addEntry(new Entry(yValue, data.getXVals().indexOf(xValue)), mSpeedDataSetIndexXYZ);
					} else {
						if (mSpeedEntryBufferXYZ.size() >= 5) {
							mSpeedEntryBufferXYZ.removeLast();
						}
						mSpeedEntryBufferXYZ.push(new Entry(yValue, data.getXValCount()));
					}

					if (Math.abs(yValue) > Math.abs(mTopSpeedXYZ)) {
						mTopSpeedXYZ = Math.abs(yValue);
					}
					break;
				case X_DATA:
					if (showEntry) {
						if (mNewSpeedDataSetRequired) {
							currentLineDataSet = createSet("X", Color.RED, 1f, 1.5f, true);
							data.addDataSet(currentLineDataSet);
							mSpeedDataSetIndexX = data.getIndexOfDataSet(currentLineDataSet);
							mSpeedDataSetIndexesX.add(mSpeedDataSetIndexX);

							if (!mSpeedDataVisibleX) {
								data.getDataSetByIndex(mSpeedDataSetIndexX).setVisible(false);
								mSpeedChart.invalidate();
							}

							int speedBufferSize = mSpeedEntryBufferX.size();

							for (int i = 0; i < speedBufferSize; i++) {
								data.addEntry(mSpeedEntryBufferX.getLast(), mSpeedDataSetIndexX);
								mSpeedEntryBufferX.removeLast();
							}
						}
						if (!mPreviewTest)
							data.addEntry(new Entry(yValue, data.getXValCount()), mSpeedDataSetIndexX);
						else
							data.addEntry(new Entry(yValue, data.getXVals().indexOf(xValue)), mSpeedDataSetIndexX);
					} else {
						if (mSpeedEntryBufferX.size() >= 5) {
							mSpeedEntryBufferX.removeLast();
						}
						mSpeedEntryBufferX.push(new Entry(yValue, data.getXValCount()));
					}
					if (Math.abs(yValue) > Math.abs(mTopSpeedX)) {
						mTopSpeedX = yValue;
					}
					break;
				case Y_DATA:
					if (showEntry) {
						if (mNewSpeedDataSetRequired) {
							currentLineDataSet = createSet("Y", Color.GREEN, 1f, 1.5f, true);
							data.addDataSet(currentLineDataSet);
							mSpeedDataSetIndexY = data.getIndexOfDataSet(currentLineDataSet);
							mSpeedDataSetIndexesY.add(mSpeedDataSetIndexY);

							if (!mSpeedDataVisibleY) {
								data.getDataSetByIndex(mSpeedDataSetIndexY).setVisible(false);
								mSpeedChart.invalidate();
							}

							int speedBufferSize = mSpeedEntryBufferY.size();

							for (int i = 0; i < speedBufferSize; i++) {
								data.addEntry(mSpeedEntryBufferY.getLast(), mSpeedDataSetIndexY);
								mSpeedEntryBufferY.removeLast();
							}
						}
						if (!mPreviewTest)
							data.addEntry(new Entry(yValue, data.getXValCount()), mSpeedDataSetIndexY);
						else
							data.addEntry(new Entry(yValue, data.getXVals().indexOf(xValue)), mSpeedDataSetIndexY);
					} else {
						if (mSpeedEntryBufferY.size() >= 5) {
							mSpeedEntryBufferY.removeLast();
						}
						mSpeedEntryBufferY.push(new Entry(yValue, data.getXValCount()));
					}
					if (Math.abs(yValue) > Math.abs(mTopSpeedY)) {
						mTopSpeedY = yValue;
					}
					break;
				case Z_DATA:
					if (showEntry) {
						if (mNewSpeedDataSetRequired) {
							currentLineDataSet = createSet("Z", Color.MAGENTA, 1f, 1.5f, true);
							data.addDataSet(currentLineDataSet);
							mSpeedDataSetIndexZ = data.getIndexOfDataSet(currentLineDataSet);
							mSpeedDataSetIndexesZ.add(mSpeedDataSetIndexZ);

							if (!mSpeedDataVisibleZ) {
								data.getDataSetByIndex(mSpeedDataSetIndexZ).setVisible(false);
								mSpeedChart.invalidate();
							}

							int speedBufferSize = mSpeedEntryBufferZ.size();

							for (int i = 0; i < speedBufferSize; i++) {
								data.addEntry(mSpeedEntryBufferZ.getLast(), mSpeedDataSetIndexZ);
								mSpeedEntryBufferZ.removeLast();
							}
						}
						if (!mPreviewTest)
							data.addEntry(new Entry(yValue, data.getXValCount()), mSpeedDataSetIndexZ);
						else
							data.addEntry(new Entry(yValue, data.getXVals().indexOf(xValue)), mSpeedDataSetIndexZ);
					} else {
						if (mSpeedEntryBufferZ.size() >= 5) {
							mSpeedEntryBufferZ.removeLast();
						}
						mSpeedEntryBufferZ.push(new Entry(yValue, data.getXValCount()));
					}
					if (Math.abs(yValue) > Math.abs(mTopSpeedZ)) {
						mTopSpeedZ = yValue;
					}
					break;
			}

			// let the chart know it's data has changed
			mSpeedChart.notifyDataSetChanged();

			// limit the number of visible entries
			mSpeedChart.setVisibleXRangeMaximum(60);

			// move to the latest entry
			if(!mPreviewTest)
				mSpeedChart.moveViewToX(data.getXValCount() - 61);
		}
	}

	private void setupRotationChart() {
		mRotationChart.setNoDataTextDescription("");

		mRotationChart.setAutoScaleMinMaxEnabled(true);

		// enable touch gestures
		mRotationChart.setTouchEnabled(true);

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

			for (int k = 0; k < yEntries.size(); k++) {
				addRotationEntry(xValues.get(k), yEntries.get(k).getVal());
			}
		}
	}

	private void addRotationEntry(String xValue, float yValue) {

		LineData data = mRotationChart.getData();

		if (data != null) {

			LineDataSet rotationSet = data.getDataSetByIndex(0);

			if (rotationSet == null) {
				rotationSet = createSet("Puck rotation over time (degrees/s)", ColorTemplate.getHoloBlue(), 2f, 3f, true);
				data.addDataSet(rotationSet);
			}

			if (Math.abs(yValue) > Math.abs(mTopRotation)) {
				mTopRotation = yValue;
			}

			// add a new x-value first
			data.addXValue(xValue + " ms");
			data.addEntry(new Entry(yValue, rotationSet.getEntryCount()), 0);

			// let the chart know it's data has changed
			mRotationChart.notifyDataSetChanged();

			// limit the number of visible entries
			mRotationChart.setVisibleXRangeMaximum(60);
			// mChart.setVisibleYRange(30, AxisDependency.LEFT);

			// move to the latest entry
			if(!mPreviewTest)
				mRotationChart.moveViewToX(data.getXValCount() - 61);
		}
	}


	private LineDataSet createSet(String setTitle, int lineColor, float lineWidth, float circleSize, boolean fullLine) {
		LineDataSet set = new LineDataSet(null, setTitle);
		set.setAxisDependency(YAxis.AxisDependency.LEFT);
		set.setColor(lineColor);
		set.setCircleColor(Color.BLACK);
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
	}

	private void onCharacteristicChanged(String uuidStr, byte[] value) {
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

			rotation = Sensor.PUCK_ACCELEROMETER.convertRotation(value);

			if (mTestRunning) {
				populateCharts(acceleration, rotation);
			}
		}
	}

	private void populateCharts(Point3D puckAcceleration, double rotation) {

		dataCounter++;

		String currentTime = String.valueOf(System.currentTimeMillis() - mTestStartTime);

		if (lastDataTime != -1) {
			lastDataTime = Float.valueOf(currentTime);
		} else {
			lastDataTime = mTestStartTime;
		}

		double accelX = puckAcceleration.x;
		double accelY = puckAcceleration.y;
		double accelZ = puckAcceleration.z;
		double accelXYZ = Math.sqrt(Math.pow(accelX, 2) + Math.pow(accelY, 2) + Math.pow(accelZ, 2));

		//Calibrate the puck
		if ((System.currentTimeMillis() - mTestStartTime) < mCalibrationTime) {
			if (mAverageGravityOffset == 0) {
				mAccelChart.setNoDataText("Calibrating sensors...");
				mAccelChart.invalidate();
				mSpeedChart.setNoDataText("Calibrating sensors...");
				mSpeedChart.invalidate();
				mRotationChart.setNoDataText("Calibration sensors...");
				mRotationChart.invalidate();
			}
			mAverageGravityOffset += accelXYZ;
			mAverageAccelX += accelX;
			mAverageAccelY += accelY;
			mAverageAccelZ += accelZ;
		} else {
			if (!mCalculatedAverageOffset) {
				mAverageGravityOffset /= dataCounter;
				mAverageAccelX /= dataCounter;
				mAverageAccelY /= dataCounter;
				mAverageAccelZ /= dataCounter;

				mTimeStep = (float) ((System.currentTimeMillis() - mTestStartTime) / (dataCounter * 1000.0));
				mPuckSpeedOffset = 0f;
				mCalculatedAverageOffset = true;

				mAccelChart.getAxisLeft().setDrawGridLines(true);
				mSpeedChart.getAxisLeft().setDrawGridLines(true);
				mRotationChart.getAxisLeft().setDrawGridLines(true);
				mAccelChart.getAxisLeft().setEnabled(true);
				mSpeedChart.getAxisLeft().setEnabled(true);
				mRotationChart.getAxisLeft().setEnabled(true);

				Log.i("Average Data", "Found average gravity contribution: " + mAverageGravityOffset + " and average X accel: " + mAverageAccelX + " and average Y accel:" + mAverageAccelY + " and average Z accel:" + mAverageAccelZ);
				Log.i("Average Data", "Got :" + String.valueOf(dataCounter) + " data in: " + String.valueOf(mCalibrationTime) + "ms");
				Log.i("Average Data", "Average time step " + String.valueOf(mTimeStep));
			}

			boolean eventHasOccured = Math.abs(accelX-mAverageAccelX) > mEventDetection || Math.abs(accelY-mAverageAccelY) > mEventDetection || Math.abs(accelZ-mAverageAccelZ) > mEventDetection;

			if(!eventHasOccured){
				mAccelIsGravityCounter++;
				if (mAccelIsGravityCounter > mDataCountWithoutEvent) {
					mShowingSpeed = false;
					mShowingAccel = false;
					mNewSpeedDataSetRequired = true;
					mNewAccelDataSetRequired = true;
				}
			}
			else {
				mShowingSpeed = true;
				mShowingAccel = true;
				mAccelIsGravityCounter = 0;
			}

			mPuckSpeedOffset += (mSpeedGravityOffsetRatio*GRAVITY * mTimeStep);

			mPuckSpeedX += (accelX) * GRAVITY * mTimeStep;
			mPuckSpeedY += (accelY) * GRAVITY * mTimeStep;
			mPuckSpeedZ += (accelZ ) * GRAVITY * mTimeStep;

			mPuckSpeedXYZ = (float) Math.sqrt(Math.pow(mPuckSpeedX, 2) + Math.pow(mPuckSpeedY, 2) + Math.pow(mPuckSpeedZ, 2));
			mPuckSpeedXYZ -= mPuckSpeedOffset;

			if (mPuckSpeedXYZ <= 0 || !mShowingSpeed || !mShowingAccel) {
				mPuckSpeedXYZ = 0;
				mPuckSpeedX = 0;
				mPuckSpeedY = 0;
				mPuckSpeedZ = 0;
				mPuckSpeedOffset = 0;
			}

			accelXYZ -= mAverageGravityOffset;

			int dataPrintFrequency = mPrintDataFrequency;

			if(mRecordIndividualAccelAxesCheckbox.isChecked())
				dataPrintFrequency += mMultipleAxesDataFrequency;
			if(mRecordIndividualSpeedAxesCheckbox.isChecked())
				dataPrintFrequency += mMultipleAxesDataFrequency;

			if (dataCounter % dataPrintFrequency == 0) {
				addAccelEntry(DUMMY_DATA, currentTime, 0, true);
				addAccelEntry(XYZ_DATA, currentTime, (float) accelXYZ, mShowingAccel);

				if(mRecordIndividualAccelAxesCheckbox.isChecked()) {
					addAccelEntry(X_DATA, currentTime, (float) accelX, mShowingAccel);
					addAccelEntry(Y_DATA, currentTime, (float) accelY, mShowingAccel);
					addAccelEntry(Z_DATA, currentTime, (float) accelZ, mShowingAccel);
				}

				mNewAccelDataSetRequired = false;

				addSpeedEntry(DUMMY_DATA, currentTime, 0, true);
				addSpeedEntry(XYZ_DATA, currentTime, mPuckSpeedXYZ, mShowingSpeed);

				if(mRecordIndividualSpeedAxesCheckbox.isChecked()) {
					addSpeedEntry(X_DATA, currentTime, mPuckSpeedX, mShowingSpeed);
					addSpeedEntry(Y_DATA, currentTime, mPuckSpeedY, mShowingSpeed);
					addSpeedEntry(Z_DATA, currentTime, mPuckSpeedZ, mShowingSpeed);
				}

				mNewSpeedDataSetRequired = false;

				addRotationEntry(currentTime, (float) rotation);
			}
		}

	}
}
