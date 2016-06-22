package com.thirdbridge.pucksensor.controllers;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.v4.content.ContextCompat;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Pair;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
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
import com.google.gson.Gson;
import com.thirdbridge.pucksensor.R;
import com.thirdbridge.pucksensor.models.Player;
import com.thirdbridge.pucksensor.models.ShotSpecification;
import com.thirdbridge.pucksensor.models.User;
import com.thirdbridge.pucksensor.utils.BaseFragment;
import com.thirdbridge.pucksensor.utils.Constants;
import com.thirdbridge.pucksensor.utils.IO;
import com.thirdbridge.pucksensor.utils.MathHelper;
import com.thirdbridge.pucksensor.utils.Protocol;
import com.thirdbridge.pucksensor.utils.Shot;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Christophe on 2015-10-14.
 * Modified by Jayson Dalpé since 2016-01-22.
 */
public class FreeRoamingFragment extends BaseFragment {

	private static String TAG = FreeRoamingFragment.class.getSimpleName();
    private static final String LOCAL_SHOTS = "LOCAL_SHOTS";
    private static final double DRAFT_STAMP = 50/3;
    private static final String CHECK_ACCEL = "CHECK_ACCEL";
    private static final String CHECK_SPEED = "CHECK_SPEED";
    private static final String CHECK_ROTATION = "CHECK_ROTATION";
    private static final String CHECK_AUTOSAVE = "CHECK_AUTOSAVE";
    private static final int MINIMAL_POINTS = 5;
    private static final String POINTS_BOARD = "POINTS_BOARD";

    private static final int[] GRAPH_COLOR = {Color.BLUE};


    // Saving local instance
    SharedPreferences mSettings;

	private boolean mTestRunning = false;
    private boolean mAutoStart = false;
	private boolean mPreviewTest = false;
    private int[] mActualSettings = Protocol.DEFAULT.clone();

	private Button mStartStopButton;
	private TextView mDescriptionTextView;

	//Loading screen
	private RelativeLayout mLoadingScreenRelativeLayout;

	private boolean mTestWasRun = false;

    private Thread mBackgroundThread;
    private boolean mPause = true;


    // Player comparison management
    private List<Player> mPlayers = new ArrayList<Player>();
    private List<ShotSpecification> mShotSpecs = new ArrayList<ShotSpecification>();
    private int mPlayerPosition;

    // Menu
    private CheckBox mAccCheckB;
    private CheckBox mAngularCheckB;
    private CheckBox mShowHighOnlyCheckB;
    private boolean mShotHighOnly;
    private CheckBox mShowLowOnlyCheckB;
    private boolean mShotLowOnly;


	//Accel Chart
    private LinearLayout mAccelLayout;
    private ProgressBar mAccelProgress;
	private LineChart mAccelChart;
	private TextView mTopAccelXYZTextView;
	private float[] mAccelMax = {0f, 0f};

	//Rotation Chart
    private float[] mRotationMax = {0f, 0f};
    private LinearLayout mAngularLayout;
    private ProgressBar mRotationProgress;
	private LineChart mRotationChart;
	private TextView mTopRotationTextView;
    private int mRotationDataSetIndex;

	//Calibration
	private float mTimeStep = (float)DRAFT_STAMP;
	private final double GRAVITY = 9.80665;
    private boolean mSendOnce = false;
    private boolean mCalibrationDone = false;

	//Accel Chart
	private int mAccelDataSetIndexXYZ;

	//Speed Chart
	private int mSpeedDataSetIndexXYZ;

    // Data flow
    private int mIndex = 0;
    private boolean mNewSetRequired = true;


    //Bluetooth
    private boolean mSensorReady = false;
    private boolean mSensorReadyChange = true;
    private HomeFragment.BluetoothListener mListener = new HomeFragment.BluetoothListener() {
        @Override
        public void onBluetoothCommand(byte[] values) {
            onCharacteristicChanged(values);
        }
    };

    // Popup for player and shot specification
    private PopupWindow mPlayerPopup;
    private PopupWindow mShotSpecPopup;

    // Thread running
    Runnable mRun = new Runnable() {
        @Override
        public void run() {
            while(true) {
                try {
                    Thread.sleep(1000);
                } catch (Exception e) {

                }
                if (mSensorReady != mSensorReadyChange) {
                    if (HomeFragment.getInstance().IsBluetoothReady()) {
                        mSensorReady = true;
                        activateTestButtons();
                    } else {
                        mSensorReady = false;
                        deactivateTestButtons();
                    }
                    mSensorReadyChange = mSensorReady;
                }
                if (!mTestRunning && mAutoStart) {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            start();
                        }
                    });
                }


                if (mPause) {
                    break;
                }
            }
        }
    };

    public static FreeRoamingFragment newInstance() {
		Bundle args = new Bundle();

		FreeRoamingFragment fragment = new FreeRoamingFragment();

		return fragment;
	}


	@Override
	public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

	}

	@Override
	public void onResume() {
		Log.d(TAG, "onResume");
		super.onResume();
        HomeFragment.getInstance().addBluetoothListener(mListener);
        mPause = false;
        mBackgroundThread = new Thread(mRun);
        mBackgroundThread.start();

	}

	@Override
	public void onPause() {
        Log.d(TAG, "onPause");
		super.onPause();
        HomeFragment.getInstance().removeBluetoothListener(mListener);
        mPause = true;
        try {
            mBackgroundThread.join();
        } catch (Exception e) {

        }
	}

	@Override
	public View onCreateView(LayoutInflater inflater, final ViewGroup container, Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);

		View v = inflater.inflate(R.layout.fragment_free, container, false);

		mStartStopButton = (Button) v.findViewById(R.id.start_button);
		mDescriptionTextView = (TextView) v.findViewById(R.id.stats_description_textview);

		mLoadingScreenRelativeLayout = (RelativeLayout) v.findViewById(R.id.loading_screen_relative_layout);

        // Menu
        mAccCheckB = (CheckBox) v.findViewById(R.id.show_acceleration_check);
        mAngularCheckB = (CheckBox) v.findViewById(R.id.show_angular_check);
        mShowHighOnlyCheckB = (CheckBox) v.findViewById(R.id.show_high_check);
        mShowLowOnlyCheckB = (CheckBox) v.findViewById(R.id.show_low_check);

        mSettings = getActivity().getSharedPreferences("StatPuck", 0);

        // Main structure
        mAccelLayout = (LinearLayout) v.findViewById(R.id.accel_layout);
        mAngularLayout = (LinearLayout) v.findViewById(R.id.angular_layout);

        mStartStopButton.setVisibility(View.VISIBLE);

		//Accel chart
		mAccelChart = (LineChart) v.findViewById(R.id.accel_stats_chart);
        mAccelProgress = (ProgressBar) v.findViewById(R.id.accel_stats_progress);
        mAccelProgress.setVisibility(View.GONE);
		mTopAccelXYZTextView = (TextView) v.findViewById(R.id.top_accel_xyz_textview);

		//Rotation Chart
		mRotationChart = (LineChart) v.findViewById(R.id.rotation_stats_chart);
        mRotationProgress = (ProgressBar) v.findViewById(R.id.rotation_stats_progress);
        mRotationProgress.setVisibility(View.GONE);
		mTopRotationTextView = (TextView) v.findViewById(R.id.top_rotation_textview);

		mStartStopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                start();

            }
        });


        mAccCheckB.setChecked(mSettings.getBoolean(CHECK_ACCEL, mAccCheckB.isChecked()));
        mAngularCheckB.setChecked(mSettings.getBoolean(CHECK_ROTATION, mAngularCheckB.isChecked()));

        showAcceleration(mAccCheckB.isChecked());
        showAngularSpeed(mAngularCheckB.isChecked());

        mShowHighOnlyCheckB.setChecked(false);
        mShowLowOnlyCheckB.setChecked(true);
        mShotHighOnly = false;
        mShotLowOnly = true;

        mShowHighOnlyCheckB.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    mShotLowOnly = false;
                    mShowLowOnlyCheckB.setChecked(false);
                }
                mShotHighOnly = isChecked;
            }
        });

        mShowLowOnlyCheckB.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    mShotHighOnly = false;
                    mShowHighOnlyCheckB.setChecked(false);
                }
                mShotLowOnly = isChecked;
            }
        });

        mAccCheckB.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                showAcceleration(isChecked);
                SharedPreferences.Editor editor = mSettings.edit();
                editor.putBoolean(CHECK_ACCEL, isChecked);
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


			Handler handler = new Handler();
			handler.postDelayed(new Runnable() {
				public void run() {
					setupAccelChart();
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
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mStartStopButton != null) {
                    mStartStopButton.setVisibility(View.VISIBLE);
                }
            }
        });
	}

	private void deactivateTestButtons(){
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mStartStopButton.setEnabled(false);
            }
        });
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

            double releaseTime1 = 0;

            double releaseTime2 = 0;

			//accel
			mTopAccelXYZTextView.setText(MathHelper.round(mAccelMax[0], 2) + "g and " + MathHelper.round(mAccelMax[1], 2) + "g" + "\n" +
                    releaseTime1 + "ms and " + releaseTime2 + "ms");
            mAccelChart.notifyDataSetChanged();
            mAccelChart.setVisibleXRangeMaximum(60);
            mAccelChart.moveViewToX(mIndex - 61);
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
            mTopRotationTextView.setText(MathHelper.round(mRotationMax[0], 2) + " degrees/s and " + MathHelper.round(mRotationMax[1], 2) + " degrees/s");
            mRotationChart.notifyDataSetChanged();
            mRotationChart.setVisibleXRangeMaximum(60);
            mRotationChart.moveViewToX(mIndex - 61);
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
                currentLineDataSet = createSet("Continious", GRAPH_COLOR[id], 1f, 1f, true);
                data.addDataSet(currentLineDataSet);
                mAccelDataSetIndexXYZ = data.getIndexOfDataSet(currentLineDataSet);


            }
            data.addEntry(new Entry(yValue, xValueInt), mAccelDataSetIndexXYZ);

            if (Math.abs(yValue) > Math.abs(mAccelMax[id])) {
                mAccelMax[id] = Math.abs(yValue);
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
                currentLineDataSet = createSet("Continious", GRAPH_COLOR[id], 1f, 1f, true);
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

	private void onCharacteristicChanged(byte[] value) {
        mAutoStart = true;
        double[] accelHigh = getAccelHigh(value);
        double[] accelLow = getAccelLow(value);
        double[] gyro = getGyro(value);
        if (Protocol.isSameMode(Protocol.SHOT_MODE, value[0])) {
            double[] realAccel = new double[accelLow.length];
            for (int i = 0; i < realAccel.length; i++) {
                if (!mShotHighOnly && !mShotLowOnly) {
                    if (accelHigh[i] > accelLow[i] && accelHigh[i] > 10) {
                        realAccel[i] = accelHigh[i];
                    } else {
                        realAccel[i] = accelLow[i];
                    }
                } else if (mShotHighOnly) {
                    realAccel[i] = accelHigh[i];
                } else {
                    realAccel[i] = accelLow[i];
                }
            }
            if (mTestRunning) {
                calculationMethod(realAccel, gyro, value[0]);
            }
            //Log.i(TAG, "Check first: AH: " + accelHigh[0] + " AL: " + accelLow[0] + " Gyro: " + gyro[0]);
        } else if (Protocol.isSameMode(Protocol.SETTINGS_MODE, value[0])) {
            if (value[2] != Protocol.VALIDITY_TOKEN && !mSendOnce) {
                // Settings don't care, send default ones
                Protocol.setDefault();
                mSendOnce = true;
            }

            /*for (int i=0; i<mActualSettings.length; i++) {
                mActualSettings[i] = value[2+i];
            }*/
        }

        String val = "";
        for (int i=0; i<value.length; i++) {
            val += value[i] + ", ";
        }
        //Log.i(TAG, "Value: " + val);
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
            mRotationChart.getAxisLeft().setDrawGridLines(true);
            mAccelChart.getAxisLeft().setEnabled(true);
            mRotationChart.getAxisLeft().setEnabled(true);

            mAccelChart.clear();
            LineData aData = new LineData();
            aData.setValueTextColor(Color.BLACK);
            mAccelChart.setData(aData);

            mRotationChart.clear();
            LineData rData = new LineData();
            rData.setValueTextColor(Color.BLACK);
            mRotationChart.setData(rData);
        } else {

            if (mode == Protocol.DATA_DRAFT) {
                for (int i=0; i<acceleration.length; i++) {
                    populateCharts(acceleration[i], rotation[i]);
                }

                populateStatisticsFields();
            }
        }
    }

    private void populateCharts(double accel, double rotation) {

        // Add entry
        addAccelEntry(mIndex * mTimeStep + "", mIndex, (float) accel, mNewSetRequired, 0, 0);
        addRotationEntry(mIndex * mTimeStep + "", mIndex, (float) rotation, mNewSetRequired, 0, 0);
        mNewSetRequired = false;
        mIndex++;
    }

    private void showAcceleration(boolean show) {
        if (show) {
            mAccelLayout.setVisibility(View.VISIBLE);
        } else {
            mAccelLayout.setVisibility(View.GONE);
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
        retValue[0] = (double)value * 0.012;

        value = (values[10] & 0xFF);
        value |= (values[11] & 0xFF) << 8;
        retValue[1] = (double)value * 0.012;

        value = (values[16] & 0xFF);
        value |= (values[17] & 0xFF) << 8;
        retValue[2] = (double)value * 0.012;
        return retValue;
    }

    private double[] getGyro(byte[] values) {
        // According to protocol, byte 2-19
        double[] retValue;
        retValue = new double[3];

        int value = (values[6] & 0xFF);
        value |= (values[7] & 0xFF) << 8;
        retValue[0] = ((double)value * 0.07);

        value = (values[12] & 0xFF);
        value |= (values[13] & 0xFF) << 8;
        retValue[1] = ((double)value * 0.07);

        value = (values[18] & 0xFF);
        value |= (values[19] & 0xFF) << 8;
        retValue[2] = ((double)value * 0.07);
        return retValue;
    }




    /**
     * Start the process and initialize graphics.
     * WARNING: Must be called from a UI thread.
     */
    private void start() {
        if (mSensorReady) {
            mStartStopButton.setVisibility(View.GONE);

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
                if (mRotationChart.getLineData() != null) {
                    mRotationChart.getLineData().clearValues();
                    mRotationChart.getData().getDataSets().clear();
                    mRotationChart.getData().getXVals().clear();
                }

                mAccelChart.clear();
                mRotationChart.clear();

                setupAccelChart();
                setupRotationChart();

                mTopAccelXYZTextView.setText("");

                mTopRotationTextView.setText("");

                mStartStopButton.setText(getString(R.string.stopTest));
                mStartStopButton.setCompoundDrawablesWithIntrinsicBounds(android.R.drawable.ic_delete, 0, 0, 0);

                mTestRunning = true;
                byte[] send = {Protocol.FREE_MODE, 0x00};
                try {
                    HomeFragment.getInstance().writeBLE(send);
                } catch (Exception e) {}
            }
        }
    }
}
