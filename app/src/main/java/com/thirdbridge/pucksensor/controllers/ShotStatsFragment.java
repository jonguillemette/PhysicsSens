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
import java.util.Random;
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

    private static final double STAMP = 2.0; // This is the delta time between sending, need to calculate instead TODO
    private static final int MINIMAL_G = 1;


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

    // Menu
    private CheckBox mAccCheckB;
    private CheckBox mSpeedCheckB;
    private CheckBox mAngularCheckB;
    private TextView mPeakAccTV;
    private SeekBar mPeakAccSB;

    private boolean mNewSetRequired = true;

	//Accel Chart
    private LinearLayout mAccelLayout;
	private LineChart mAccelChart;
	private TextView mTopAccelXYZTextView;
	private float mAccelMax = 0f;
	private String mAccelData;
	private boolean mAccelDataVisibleX = false;
	private boolean mAccelDataVisibleY = false;
	private boolean mAccelDataVisibleZ = false;
	private boolean mCalculatedAverageOffset = false;

	//Speed Chart
    private LinearLayout mSpeedLayout;
	private LineChart mSpeedChart;
	private TextView mTopSpeedXYZTextView;
	private float mSpeedMax = 0f;
	private float mPuckSpeedXYZ = 0f;
	private float mPuckSpeedOffset = 0f;
	private String mSpeedData;
	private boolean mSpeedDataVisibleX = false;
	private boolean mSpeedDataVisibleY = false;
	private boolean mSpeedDataVisibleZ = false;
	private Button mGenerateButton;
    private Button mHackButton;

	//Rotation Chart
    private float mRotationMax = 0f;
    private LinearLayout mAngularLayout;
	private LineChart mRotationChart;
	private TextView mTopRotationTextView;
	private String mRotationData;

	//Calibration
	private int mCalibrationTime = 2000;
	private float mTimeStep = 0f;
	private final double GRAVITY = 9.80665;


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



	private void setTestVariables(){

		//Calibration time in miliseconds
		mCalibrationTime = 2000;
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
		mGenerateButton = (Button) v.findViewById(R.id.generate_button);
        mHackButton = (Button) v.findViewById(R.id.demo_start_button);

		mLoadingScreenRelativeLayout = (RelativeLayout) v.findViewById(R.id.loading_screen_relative_layout);

        // Menu
        mAccCheckB = (CheckBox) v.findViewById(R.id.show_acceleration_check);
        mSpeedCheckB = (CheckBox) v.findViewById(R.id.show_speed_check);
        mAngularCheckB = (CheckBox) v.findViewById(R.id.show_angular_check);
        mPeakAccTV = (TextView) v.findViewById(R.id.peak_acc_number);
        mPeakAccSB = (SeekBar) v.findViewById(R.id.peak_acc_seekbar);

        mPeakAccTV.setText("" + MINIMAL_G);
        mPeakAccSB.setProgress(Integer.parseInt(mPeakAccTV.getText().toString()));


        // Main structure
        mAccelLayout = (LinearLayout) v.findViewById(R.id.accel_layout);
        mSpeedLayout = (LinearLayout) v.findViewById(R.id.speed_layout);
        mAngularLayout = (LinearLayout) v.findViewById(R.id.angular_layout);

		//Accel chart
		mAccelChart = (LineChart) v.findViewById(R.id.accel_stats_chart);
		mTopAccelXYZTextView = (TextView) v.findViewById(R.id.top_accel_xyz_textview);

		// DEBUGGING DATA
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
                mAccelChart.getAxisLeft().setDrawGridLines(true);
                mSpeedChart.getAxisLeft().setDrawGridLines(true);
                mRotationChart.getAxisLeft().setDrawGridLines(true);
                mAccelChart.getAxisLeft().setEnabled(true);
                mSpeedChart.getAxisLeft().setEnabled(true);
                mRotationChart.getAxisLeft().setEnabled(true);
                int item = 400;
                Point3D[] acc = new Point3D[item];
                double[] rot = new double[item];

                for (int i=0; i<item; i++) {
                    if (i < item/2) {
                        acc[i] = new Point3D((i * mPeakAccSB.getProgress())*1f/200f, 0.0f, 0.0f);
                    } else {
                        acc[i] = new Point3D((item-i * mPeakAccSB.getProgress())*1f/200f, 0.0f, 0.0f);
                    }
                    rot[i] = Math.random()*i;
                }
                onDummyChanged(acc, rot);
			}
		});


        mPeakAccSB.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                mPeakAccTV.setText("" + progress);
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

		final PopupWindow popupWindow = inflateSaveTestPopup(container);

		mStartStopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (true/*mSensorReady*/) {

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

        showAcceleration(mAccCheckB.isChecked());
        showSpeed(mSpeedCheckB.isChecked());
        showAngularSpeed(mAngularCheckB.isChecked());

        mAccCheckB.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                showAcceleration(isChecked);
            }
        });

        mSpeedCheckB.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                showSpeed(isChecked);
            }
        });

        mAngularCheckB.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                showAngularSpeed(isChecked);
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
			mTopAccelXYZTextView.setText(MathHelper.round(mAccelMax, 2) + "g");
		}

		if (mSpeedChart != null) {
			//speed
			mTopSpeedXYZTextView.setText(MathHelper.round(mSpeedMax, 2) + " m/s");
		}

        // TODO Enable it
		if (mRotationChart != null) {
			//rotation
			mTopRotationTextView.setText(MathHelper.round(mRotationMax, 2) + " degrees/s");
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

	private void setupAccelChart() {
		mAccelChart.setNoDataTextDescription("");

		mAccelChart.setAutoScaleMinMaxEnabled(true);

		mAccelChart.getLegend().setEnabled(false);

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

			mNewSetRequired = true;

			switch (dataType) {
				case "":
					for (int k = 0; k < yEntries.size(); k++) {
						//addAccelEntry(DUMMY_DATA, xValues.get(k), 0, false);
						mNewSetRequired = false;
					}
					break;
				case "XYZ":
					for (int k = 0; k < yEntries.size(); k++) {
						addAccelEntry(XYZ_DATA, xValues.get(yEntries.get(k).getXIndex()), yEntries.get(k).getVal(), true);
						mNewSetRequired = false;
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

			if (Math.abs(yValue) > Math.abs(mAccelMax)) {
				YAxis leftAxis = mAccelChart.getAxisLeft();
				leftAxis.setAxisMaxValue(Math.abs(yValue));
				leftAxis.setAxisMinValue(0);
			}

            if (mNewSetRequired) {
                mAccelMax = 0;
                data.clearValues();
                currentLineDataSet = createSet("XYZ", ColorTemplate.getHoloBlue(), 2f, 2f, true);
                data.addDataSet(currentLineDataSet);
                mAccelDataSetIndexXYZ = data.getIndexOfDataSet(currentLineDataSet);


            }
            if (!mPreviewTest) {
                data.addEntry(new Entry(yValue, data.getXValCount()), mAccelDataSetIndexXYZ);
            } else {
                data.addEntry(new Entry(yValue, data.getXVals().indexOf(xValue)), mAccelDataSetIndexXYZ);
            }

            if (Math.abs(yValue) > Math.abs(mAccelMax)) {
                mAccelMax = Math.abs(yValue);
            }

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

			mNewSetRequired = true;

			switch (dataType) {
				case "":
					for (int k = 0; k < yEntries.size(); k++) {
						//addSpeedEntry(DUMMY_DATA, xValues.get(k), 0, false);
						mNewSetRequired = false;
					}
					break;
				case "XYZ":
					for (int k = 0; k < yEntries.size(); k++) {
						addSpeedEntry(XYZ_DATA, xValues.get(yEntries.get(k).getXIndex()), yEntries.get(k).getVal(), true);
						mNewSetRequired = false;
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

			if (Math.abs(yValue) > Math.abs(mSpeedMax)) {
				YAxis leftAxis = mSpeedChart.getAxisLeft();
				leftAxis.setAxisMaxValue(Math.abs(yValue));
				leftAxis.setAxisMinValue(0);
			}

            if (mNewSetRequired) {
                mSpeedMax = 0;
                data.clearValues();
                currentLineDataSet = createSet("XYZ", ColorTemplate.getHoloBlue(), 2f, 2f, true);
                data.addDataSet(currentLineDataSet);
                mSpeedDataSetIndexXYZ = data.getIndexOfDataSet(currentLineDataSet);

            }
            if (!mPreviewTest)
                data.addEntry(new Entry(yValue, data.getXValCount()), mSpeedDataSetIndexXYZ);
            else
                data.addEntry(new Entry(yValue, data.getXVals().indexOf(xValue)), mSpeedDataSetIndexXYZ);

            if (Math.abs(yValue) > Math.abs(mSpeedMax)) {
                mSpeedMax = Math.abs(yValue);
            }
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

            LineDataSet currentLineDataSet = null;

            if (mNewSetRequired) {
                mRotationMax = 0;
                data.clearValues();
                currentLineDataSet = createSet("Puck rotation over time (degrees/s)", ColorTemplate.getHoloBlue(), 2f, 2f, true);
                data.addDataSet(currentLineDataSet);
                mRotationDataSetIndex = data.getIndexOfDataSet(currentLineDataSet);
            }

            if (Math.abs(yValue) > Math.abs(mRotationMax)) {
                mRotationMax = yValue;
                YAxis leftAxis = mRotationChart.getAxisLeft();
                leftAxis.setAxisMaxValue(Math.abs(yValue));
                leftAxis.setAxisMinValue(0);
            }


			// add a new x-value first
			data.addXValue(xValue + " ms");
            data.addEntry(new Entry(yValue, data.getXValCount()), mRotationDataSetIndex);

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

	private void onDummyChanged(Point3D[] acceleration, double[] rotation) {
		populateCharts(acceleration, rotation);
        populateStatisticsFields();
	}

	private void populateCharts(Point3D puckAcceleration, double rotation) {
        /*
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

			mPuckSpeedXYZ = (float) Math.sqrt(Math.pow((accelX) * GRAVITY * mTimeStep, 2) +
                    Math.pow((accelY) * GRAVITY * mTimeStep, 2) + Math.pow((accelZ ) * GRAVITY * mTimeStep, 2));


			if (mPuckSpeedXYZ <= 0 || !mShowingSpeed || !mShowingAccel) {
				mPuckSpeedXYZ = 0;
				mPuckSpeedX = 0;
				mPuckSpeedY = 0;
				mPuckSpeedZ = 0;
				mPuckSpeedOffset = 0;
			}

			accelXYZ -= mAverageGravityOffset;

			int dataPrintFrequency = mPrintDataFrequency;


			if (dataCounter % dataPrintFrequency == 0) {
				addAccelEntry(DUMMY_DATA, currentTime, 0, true);
				addAccelEntry(XYZ_DATA, currentTime, (float) accelXYZ, mShowingAccel);


				mNewAccelDataSetRequired = false;

				addSpeedEntry(DUMMY_DATA, currentTime, 0, true);
				addSpeedEntry(XYZ_DATA, currentTime, mPuckSpeedXYZ, mShowingSpeed);

				mNewSpeedDataSetRequired = false;

				addRotationEntry(currentTime, (float) rotation);
			}
		}
*/
	}

    private void populateCharts(Point3D[] puckAcceleration, double[] rotation) {

        double time = 0.0f; // Use stamp

        mAccelChart.clear();
        LineData aData = new LineData();
        aData.setValueTextColor(Color.BLACK);
        mAccelChart.setData(aData);

        mSpeedChart.clear();
        LineData sData = new LineData();
        sData.setValueTextColor(Color.BLACK);
        mSpeedChart.setData(sData);

        mRotationChart.clear();
        LineData rData = new LineData();
        rData.setValueTextColor(Color.BLACK);
        mRotationChart.setData(rData);


        mNewSetRequired = true;

        mPuckSpeedXYZ = 0f;


        for (int i=0; i<rotation.length; i++) {
            double accelX = puckAcceleration[i].x;
            double accelY = puckAcceleration[i].y;
            double accelZ = puckAcceleration[i].z;
            double accelXYZ = Math.sqrt(Math.pow(accelX, 2) + Math.pow(accelY, 2) + Math.pow(accelZ, 2));

            // TODO Cancel gravity

            mTimeStep = (float)STAMP;

            mPuckSpeedXYZ = (float) Math.sqrt(Math.pow((accelX) * GRAVITY * mTimeStep, 2) +
                    Math.pow((accelY) * GRAVITY * mTimeStep, 2) + Math.pow((accelZ ) * GRAVITY * mTimeStep, 2))/1000f + mPuckSpeedXYZ;

            addAccelEntry(XYZ_DATA, i *mTimeStep+"", (float) accelXYZ, true);
            addSpeedEntry(XYZ_DATA, i * mTimeStep + "", mPuckSpeedXYZ, true);
            addRotationEntry(i * mTimeStep + "", (float) rotation[i]);

            mNewSetRequired = false;
        }
        mAccelChart.fitScreen();
        mAccelChart.notifyDataSetChanged();

        mSpeedChart.fitScreen();
        mSpeedChart.notifyDataSetChanged();

        mRotationChart.fitScreen();
        mRotationChart.notifyDataSetChanged();

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
