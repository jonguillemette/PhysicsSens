package com.thirdbridge.pucksensor.controllers;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import androidx.core.content.ContextCompat;
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
import com.thirdbridge.pucksensor.adapter.ComparisonPlayerAdapter;
import com.thirdbridge.pucksensor.adapter.ComparisonShotAdapter;
import com.thirdbridge.pucksensor.models.Player;
import com.thirdbridge.pucksensor.models.ShotSpecification;
import com.thirdbridge.pucksensor.models.User;
import com.thirdbridge.pucksensor.utils.BaseFragment;
import com.thirdbridge.pucksensor.utils.Constants;
import com.thirdbridge.pucksensor.utils.IO;
import com.thirdbridge.pucksensor.utils.MathHelper;
import com.thirdbridge.pucksensor.hardware.Protocol;
import com.thirdbridge.pucksensor.hardware.Shot;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Christophe on 2015-10-14.
 * Modified by Jayson Dalpé since 2016-01-22.
 */
public class ShotStatsFragment extends BaseFragment {

	private static String TAG = ShotStatsFragment.class.getSimpleName();
    private static String FOLDER_SAVE = "Statpuck";
    private static String FOLDER_SAVE_SHOT = "Shots";
    private static String FOLDER_SAVE_ANALYSIS = "Analysis";
    private static final String LOCAL_SHOTS = "LOCAL_SHOTS";

    private static final double STAMP = 1;
    private static final double DRAFT_STAMP = 50/3;
    private static final boolean DEBUG = false;
    private static final int MINIMAL_G = 1;
    private static final String THRESHOLD_G = "THRESHOLD_G";
    private static final int MINIMAL_RELEASE_G = 2;
    private static final String THRESHOLD_RELEASE_G = "THRESHOLD_RELEASE_G";
    private static final String CHECK_ACCEL = "CHECK_ACCEL";
    private static final String CHECK_SPEED = "CHECK_SPEED";
    private static final String CHECK_ROTATION = "CHECK_ROTATION";
    private static final String CHECK_AUTOSAVE = "CHECK_AUTOSAVE";
    private static final int MINIMAL_POINTS = 5;
    private static final String POINTS_BOARD = "POINTS_BOARD";
    private static final int SHOT_WAIT_VALUE = 3;
    private static final String SHOT_WAIT = "SHOT_WAIT";

    private static final int[] GRAPH_COLOR = {Color.BLUE, Color.RED};
    private static final int[] RECENT_NAME = {R.string.recent_shot1, R.string.recent_shot2, R.string.recent_shot3, R.string.recent_shot4, R.string.recent_shot5};


    // Saving local instance
    SharedPreferences mSettings;

	private boolean mTestRunning = false;
    private boolean mAutoStart = false;
	private User mUser;
	private boolean mPreviewTest = false;
    private int[] mActualSettings = Protocol.DEFAULT.clone();

	private Button mStartStopButton;
	private Button mSaveButton;
    private EditText mHeaderET;
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
    private CheckBox mSpeedCheckB;
    private CheckBox mAngularCheckB;
    private CheckBox mAutosaveCheckB;

    // Check management
    private CheckBox[] mRecentResult;
    private Button mComparePlusBtn;
    private int mFirstCheck = 0;
    private int mSecondCheck = -1;
    private boolean mCanTouchThis = true;

    // Core
    private double[] mAccelCircularBuffer = new double[Shot.getMaxDraftData()];
    private double[] mRotCircularBuffer = new double[Shot.getMaxDraftData()];
    private int mCircularIndex = 0;
    private Shot[] mRecent = new Shot[5]; // Actual, previous shot, before preevious shot, before before previous shot, comparison
    private int mRealIndex;
    private Shot mReal;
    private long mTime = 0;

	//Accel Chart
    private LinearLayout mAccelLayout;
    private ProgressBar mAccelProgress;
	private LineChart mAccelChart;
	private TextView mTopAccelXYZTextView;
	private float[] mAccelMax = {0f, 0f};


	//Speed Chart
    private LinearLayout mSpeedLayout;
    private ProgressBar mSpeedProgress;
	private LineChart mSpeedChart;
	private TextView mTopSpeedXYZTextView;
	private float[] mSpeedMax = {0f, 0f};
	private float mPuckSpeedXYZ = 0f;
	private Button mGenerateButton;
    private Button mHackButton;

	//Rotation Chart
    private float[] mRotationMax = {0f, 0f};
    private LinearLayout mAngularLayout;
    private ProgressBar mRotationProgress;
	private LineChart mRotationChart;
	private TextView mTopRotationTextView;
    private int mRotationDataSetIndex;

	//Calibration
	private float mTimeStep = 0f;
	private final double GRAVITY = 9.80665;
    private boolean mCalibrationDone = false;
    private boolean mSendOnce = false;

	//Accel Chart
	private int mAccelDataSetIndexXYZ;

	//Speed Chart
	private int mSpeedDataSetIndexXYZ;


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

    // Wait management
    private boolean mLock = false;
    private boolean mWait = false;

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

    Runnable mWaitRun = new Runnable() {
        @Override
        public void run() {
            mWait = true;
            int value = mSettings.getInt(SHOT_WAIT, SHOT_WAIT_VALUE);
            for (int i=0; i<value; i++) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            }
            mWait = false;
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
		}

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

		View v = inflater.inflate(R.layout.fragment_stats, container, false);

		mStartStopButton = (Button) v.findViewById(R.id.start_button);
		mSaveButton = (Button) v.findViewById(R.id.save_button);
        mHeaderET = (EditText) v.findViewById(R.id.header_text);
		mDescriptionTextView = (TextView) v.findViewById(R.id.stats_description_textview);
		mGenerateButton = (Button) v.findViewById(R.id.generate_button);
        mHackButton = (Button) v.findViewById(R.id.demo_start_button);

		mLoadingScreenRelativeLayout = (RelativeLayout) v.findViewById(R.id.loading_screen_relative_layout);

        // Menu
        mAccCheckB = (CheckBox) v.findViewById(R.id.show_acceleration_check);
        mSpeedCheckB = (CheckBox) v.findViewById(R.id.show_speed_check);
        mAngularCheckB = (CheckBox) v.findViewById(R.id.show_angular_check);
        mAutosaveCheckB = (CheckBox) v.findViewById(R.id.autosave);

        mSettings = getActivity().getSharedPreferences("StatPuck", 0);

        // Main structure
        mAccelLayout = (LinearLayout) v.findViewById(R.id.accel_layout);
        mSpeedLayout = (LinearLayout) v.findViewById(R.id.speed_layout);
        mAngularLayout = (LinearLayout) v.findViewById(R.id.angular_layout);

        mRecentResult = new CheckBox[5];
        mRecentResult[0] = (CheckBox) v.findViewById(R.id.recent_result1);
        mRecentResult[1] = (CheckBox) v.findViewById(R.id.recent_result2);
        mRecentResult[2] = (CheckBox) v.findViewById(R.id.recent_result3);
        mRecentResult[3] = (CheckBox) v.findViewById(R.id.recent_result4);
        mRecentResult[4] = (CheckBox) v.findViewById(R.id.recent_result5);

        mComparePlusBtn = (Button) v.findViewById(R.id.compare_btn);
        mComparePlusBtn.setText(R.string.compare_title);
        mComparePlusBtn.setVisibility(View.GONE);

        for (int i=0; i<mRecentResult.length; i++) {
            mRecentResult[i].setText(RECENT_NAME[i]);
            mRecentResult[i].setVisibility(View.GONE);
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

        mRecentResult[4].setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (mCanTouchThis) {
                    if (isChecked) {
                        openComparisonMenu(true, true, container, null);
                        mComparePlusBtn.setVisibility(View.VISIBLE);
                    } else {
                        mComparePlusBtn.setVisibility(View.GONE);
                        checkRecent(4, isChecked);
                        mRecentResult[4].setText(RECENT_NAME[4]);
                    }
                }
            }
        });

        mStartStopButton.setVisibility(View.VISIBLE);
        mSaveButton.setVisibility(View.GONE);

        mComparePlusBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openComparisonMenu(true, true, container, null);
            }
        });

        // Pop-up management
        mPlayerPopup = inflatePlayerPopup(container,  mRecentResult[4]);
        mShotSpecPopup = inflateShotPopup(container,  mRecentResult[4]);

		//Accel chart
		mAccelChart = (LineChart) v.findViewById(R.id.accel_stats_chart);
        mAccelProgress = (ProgressBar) v.findViewById(R.id.accel_stats_progress);
        mAccelProgress.setVisibility(View.GONE);
		mTopAccelXYZTextView = (TextView) v.findViewById(R.id.top_accel_xyz_textview);


        if (DEBUG) {
            mGenerateButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                }
            });
        } else {
            mHackButton.setVisibility(View.GONE);
            mGenerateButton.setVisibility(View.GONE);
        }

		//Speed Chart
		mSpeedChart = (LineChart) v.findViewById(R.id.speed_stats_chart);
        mSpeedProgress = (ProgressBar) v.findViewById(R.id.speed_stats_progress);
        mSpeedProgress.setVisibility(View.GONE);
		mTopSpeedXYZTextView = (TextView) v.findViewById(R.id.top_speed_xyz_textview);

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

		mSaveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                save();
            }
        });

        mAccCheckB.setChecked(mSettings.getBoolean(CHECK_ACCEL, mAccCheckB.isChecked()));
        mSpeedCheckB.setChecked(mSettings.getBoolean(CHECK_SPEED, mSpeedCheckB.isChecked()));
        mAngularCheckB.setChecked(mSettings.getBoolean(CHECK_ROTATION, mAngularCheckB.isChecked()));
        mAutosaveCheckB.setChecked(mSettings.getBoolean(CHECK_AUTOSAVE, mAutosaveCheckB.isChecked()));

        showAcceleration(mAccCheckB.isChecked());
        showSpeed(mSpeedCheckB.isChecked());
        showAngularSpeed(mAngularCheckB.isChecked());

        mAutosaveCheckB.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                SharedPreferences.Editor editor = mSettings.edit();
                editor.putBoolean(CHECK_AUTOSAVE, isChecked);
                editor.commit();
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

    private void checkRecent(int id, boolean isChecked) {
        if (isChecked) {
            if (mFirstCheck == -1) {
                mFirstCheck = id;
            } else if (mSecondCheck == -1) {
                mSecondCheck = id;
            } else {
                if (mSecondCheck != id) {
                    mFirstCheck = mSecondCheck;
                    mSecondCheck = id;
                }
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
        mRecentResult[4].setChecked(false);

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
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mStartStopButton != null && mSaveButton != null) {
                    mStartStopButton.setVisibility(View.VISIBLE);
                    mSaveButton.setVisibility(View.GONE);
                }
            }
        });
	}

	private void deactivateTestButtons(){
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mStartStopButton.setEnabled(false);
                mSaveButton.setEnabled(false);
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
            if (mFirstCheck != -1 && mRecent[mFirstCheck] != null) {
                releaseTime1 = mRecent[mFirstCheck].getReleaseTime() * mTimeStep;
            }

            double releaseTime2 = 0;
            if (mSecondCheck != -1 && mRecent[mSecondCheck] != null) {
                releaseTime2 = mRecent[mSecondCheck].getReleaseTime() * mTimeStep;
            }

			//accel
			mTopAccelXYZTextView.setText(MathHelper.round(mAccelMax[0], 2) + "g and " + MathHelper.round(mAccelMax[1], 2) + "g" + "\n" +
                    releaseTime1 + "ms and " + releaseTime2 + "ms");
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
			mTopSpeedXYZTextView.setText(MathHelper.round(mSpeedMax[0], 2) + " km/h and " + MathHelper.round(mSpeedMax[1], 2) + "km/h");
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
			mTopRotationTextView.setText(MathHelper.round(mRotationMax[0], 2) + " degrees/s and " + MathHelper.round(mRotationMax[1], 2) + " degrees/s");
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

	private void onCharacteristicChanged(byte[] value) {
        mAutoStart = true;
        double[] accelHigh = Protocol.getAccelHighShot(value);
        double[] accelLow = Protocol.getAccelLowShot(value);
        double[] gyro = Protocol.getGyroShot(value);
        if (Protocol.isSameMode(Protocol.SHOT_MODE, value[0])) {
            double[] realAccel = new double[accelLow.length];
            for (int i = 0; i < realAccel.length; i++) {
                if (accelHigh[i] > accelLow[i]  && accelHigh[i] > 10) {
                    realAccel[i] = accelHigh[i];
                } else {
                    realAccel[i] = accelLow[i];
                }
            }
            if (mTestRunning) {
                calculationMethod(realAccel, accelLow, accelHigh, gyro, value[0]);
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
    private void calculationMethod(double[] acceleration, double[] lowG, double[] highG, double rotation[], byte mode) {
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

            if (mode == Protocol.DATA_DRAFT) {
                for (int i=0; i<acceleration.length; i++) {
                    // Use circular buffer
                    mAccelCircularBuffer[mCircularIndex] = acceleration[i];
                    mRotCircularBuffer[mCircularIndex] = rotation[i];
                    mCircularIndex++;
                    if (mCircularIndex >= mAccelCircularBuffer.length) {
                        mCircularIndex = 0;
                    }
                }
            } else if (mode == Protocol.DATA_START) {
                if (mWait) {
                    mLock = true;
                    return; // Don't care, I'm waiting!!
                }
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

                mReal = new Shot(Shot.getMaxRevisedData(), mUser, false);
                mRealIndex = 0;

                for (int i = 0; i < sendRot.length; i++) {
                    mRecent[0].setAccelerationXYZ(sendAcc[i], i);
                    mRecent[0].setRotation(sendRot[i], i);
                }

                for (int i=0; i<acceleration.length; i++) {
                    // Use circular buffer
                    mReal.setAccelerationXYZ(acceleration[i], i);
                    mReal.setAccelerationComposite(lowG[i], highG[i], i);
                    mReal.setRotation(rotation[i], i);
                    mRealIndex ++;
                }

                populateCharts();
                populateStatisticsFields();
                mCircularIndex = 0;
            } else if (mode == Protocol.DATA) {
                if (mLock) {
                    return;
                }
                for (int i=0; i<acceleration.length; i++) {
                    // Use circular buffer
                    mReal.setAccelerationXYZ(acceleration[i], mRealIndex+i);
                    mReal.setAccelerationComposite(lowG[i], highG[i], mRealIndex+i);
                    mReal.setRotation(rotation[i], mRealIndex+i);
                    mRealIndex ++;
                }
            } else if (mode == Protocol.DATA_END){
                if (mLock) {
                    mLock = false;
                    return;
                }
                if (!mWait) {
                    Thread t = new Thread(mWaitRun);
                    t.start();
                }
                for (int i=0; i<acceleration.length; i++) {
                    // Use circular buffer
                    mReal.setAccelerationXYZ(acceleration[i], mRealIndex+i);
                    mReal.setAccelerationComposite(lowG[i], highG[i], mRealIndex+i);
                    mReal.setRotation(rotation[i], mRealIndex+i);
                    mRealIndex ++;
                }

                mRecent[0] = mReal;
                boolean load = mRecent[0].analyze(mSettings.getInt(THRESHOLD_G, MINIMAL_G), mSettings.getInt(THRESHOLD_RELEASE_G, MINIMAL_RELEASE_G), mSettings.getInt(POINTS_BOARD, MINIMAL_POINTS));
                if (!load) {
                    Toast.makeText(getContext(), "Cannot parse graphic", Toast.LENGTH_LONG).show();
                }


                populateCharts();
                populateStatisticsFields();
                mCircularIndex = 0;
                if (mAutosaveCheckB.isChecked()) {
                    save();
                }
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
            for (int i = 0; i < mRecent[idData.get(id)].getLength(); i++) {
                if (mRecent[idData.get(id)] == null) {
                    continue;
                }
                 mTimeStep = mRecent[idData.get(id)].isDraft() ? (float) DRAFT_STAMP : (float) STAMP;

                if (mRecent[idData.get(id)].isCooked()) {
                    addAccelEntry(i * mTimeStep + "", i, (float) mRecent[idData.get(id)].getAccelerations()[i], newSetRequired, idData.get(id), id);
                } else {
                    double accelX = mRecent[idData.get(id)].getAccelerationsXYZ()[i].x;
                    double accelY = mRecent[idData.get(id)].getAccelerationsXYZ()[i].y;
                    double accelZ = mRecent[idData.get(id)].getAccelerationsXYZ()[i].z;
                    double accelXYZ = Math.sqrt(Math.pow(accelX, 2) + Math.pow(accelY, 2) + Math.pow(accelZ, 2));

                    //Complete recent data
                    mRecent[idData.get(id)].setAccelerationXYZ(accelXYZ, i);
                    addAccelEntry(i * mTimeStep + "", i, (float) accelXYZ, newSetRequired, idData.get(id), id);
                }

                // Build speed only on zone of interest
                double minOnlySpeed = mRecent[idData.get(id)].getMinOnly();
                double maxOnlySpeed = mRecent[idData.get(id)].getMaxOnly();
                if (i < minOnlySpeed) {
                    mPuckSpeedXYZ = 0;
                }
                else if (i > minOnlySpeed && i <= maxOnlySpeed)
                {
                    mPuckSpeedXYZ = (float) (mRecent[idData.get(id)].getAccelerations()[i] * GRAVITY * mTimeStep / 1000f * 3.6 + mPuckSpeedXYZ);
                }
                // Else it stay the same


                mRecent[idData.get(id)].setSpeedXYZ(mPuckSpeedXYZ, i);
                addSpeedEntry(i * mTimeStep + "", i, mPuckSpeedXYZ, newSetRequired, idData.get(id), id);
                addRotationEntry(i * mTimeStep + "", i, (float) mRecent[idData.get(id)].getRotations()[i], newSetRequired, idData.get(id), id);

                newSetRequired = false;
            }
            mRecent[idData.get(id)].setMax(mAccelMax[id], mSpeedMax[id], mRotationMax[id]);

        }

        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mRecent[0] == null) {
                    return;
                }
                if (mRecent[0].isDraft()) {
                    mAccelProgress.setVisibility(View.VISIBLE);
                    mSpeedProgress.setVisibility(View.VISIBLE);
                    mRotationProgress.setVisibility(View.VISIBLE);
                } else {
                    mAccelProgress.setVisibility(View.GONE);
                    mSpeedProgress.setVisibility(View.GONE);
                    mRotationProgress.setVisibility(View.GONE);
                }
            }
        });
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


    private void openComparisonMenu(boolean open, boolean player, ViewGroup container, String id) {
        if (open) {
            if (player) {
                setupPlayer((ListView) mPlayerPopup.getContentView().findViewById(R.id.player_list), container);
                mPlayerPopup.setFocusable(true);
                mPlayerPopup.update();
                mPlayerPopup.showAtLocation(container, Gravity.TOP, 0, 200);
            } else {
                setupShot((ListView) mShotSpecPopup.getContentView().findViewById(R.id.shot_list), id);
                mShotSpecPopup.setFocusable(true);
                mShotSpecPopup.update();
                mShotSpecPopup.showAtLocation(container, Gravity.TOP, 0, 200);
            }
        } else {
            if (player) {
                mPlayerPopup.dismiss();
            } else {
                mShotSpecPopup.dismiss();
            }
        }
    }

    private void setupShot(ListView listUI, String id) {
        //TODO query on the internet the shot of the player ID
        mShotSpecs.clear();

        if (id.equals(LOCAL_SHOTS)) {
            // Step 1: Check every file inside
            File rootsd = Environment.getExternalStorageDirectory();
            File parent = new File(rootsd.getAbsolutePath(), FOLDER_SAVE);
            File root = new File(parent.getAbsolutePath(), FOLDER_SAVE_SHOT);

            File[] files = root.listFiles();
            if (files == null) {
                return;
            }
            for (int i=0; i<files.length; i++) {
                Log.i("ALLO", "Files: " + files[i].getName());
                String date = files[i].getName().split("_", 2)[1].replace(".csv", "");
                Shot shot = new Shot(files[i], date);


                String[] units = {"g", "km/h", "degrees/s"};
                mShotSpecs.add(new ShotSpecification(shot.getUser().getName(), "Shot on " + date, shot.getMax(), units, LOCAL_SHOTS, shot));

            }

            ComparisonShotAdapter adapter = new ComparisonShotAdapter(getContext(), mShotSpecs);
            listUI.setAdapter(adapter);
            listUI.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    openComparisonMenu(false, false, null, null);


                    Shot shot = mShotSpecs.get(position).getShot();

                    boolean load = shot.analyze(mSettings.getInt(THRESHOLD_G, MINIMAL_G), mSettings.getInt(THRESHOLD_RELEASE_G, MINIMAL_RELEASE_G), mSettings.getInt(POINTS_BOARD, MINIMAL_POINTS));
                    if (!load) {
                        Toast.makeText(getContext(), "Cannot parse graphic", Toast.LENGTH_LONG).show();
                    }

                    mRecent[4] = shot;
                    mRecentResult[4].setText(RECENT_NAME[4]);
                    mRecentResult[4].setText(mRecentResult[4].getText() + mPlayers.get(mPlayerPosition).getName() + " of " + mShotSpecs.get(position).getDescription());

                    checkRecent(4, true);
                }
            });

        } else {
            double[] datas = {20, 245.3, 200.3};
            double[] datas2 = {13, 255.3, 210.3};
            String[] units = {"IntellieSportRank", "g", "km/h"};

            mShotSpecs.add(new ShotSpecification("Best rank", "Shot on 2016/04/07", datas, units, "WERTYU"));
            mShotSpecs.add(new ShotSpecification("Best speed", "Shot on 2016/04/06", datas2, units, "WERTYUI"));


            ComparisonShotAdapter adapter = new ComparisonShotAdapter(getContext(), mShotSpecs);
            listUI.setAdapter(adapter);
            listUI.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    openComparisonMenu(false, false, null, null);
                    //TODO Retrieve dot from DB
                    Log.i(TAG, mShotSpecs.get(position).getName());

                    User dataBaseUser = new User(mPlayers.get(mPlayerPosition).getId(), mPlayers.get(mPlayerPosition).getName());
                    Shot databaseShot = new Shot(Shot.getMaxData(), dataBaseUser, false);
                    for (int i = 0; i < Shot.getMaxData(); i++) {
                        if (i < Shot.getMaxData() / 4) {
                            databaseShot.setAccelerationXYZ(i, i);
                        } else if (i < Shot.getMaxData() / 2) {
                            databaseShot.setAccelerationXYZ(Shot.getMaxData() / 2 - i, i);
                        } else {
                            databaseShot.setAccelerationXYZ(i, i);
                        }
                        databaseShot.setRotation(i % 20, i);

                    }
                    boolean load = databaseShot.analyze(mSettings.getInt(THRESHOLD_G, MINIMAL_G), mSettings.getInt(THRESHOLD_RELEASE_G, MINIMAL_RELEASE_G), mSettings.getInt(POINTS_BOARD, MINIMAL_POINTS));
                    if (!load) {
                        Toast.makeText(getContext(), "Cannot parse graphic", Toast.LENGTH_LONG).show();
                    }

                    mRecent[4] = databaseShot;
                    mRecentResult[4].setText(RECENT_NAME[4]);
                    mRecentResult[4].setText(mRecentResult[4].getText() + mShotSpecs.get(position).getName() + " of " + mPlayers.get(mPlayerPosition).getName());

                    checkRecent(4, true);
                }
            });
        }



    }

    private void setupPlayer(ListView listUI, final ViewGroup container) {
        //TODO Query on the internet the list regarding to his mUser.
        mPlayers.clear();
        mPlayers.add(new Player(BitmapFactory.decodeResource(getResources(), R.drawable.save), "Local", "The shot save in this devices", LOCAL_SHOTS));
        mPlayers.add(new Player(BitmapFactory.decodeResource(getResources(), R.drawable.pksubban), "P. K. Subban", "Montreal Canadiens Defenseman", "QWT"));
        mPlayers.add(new Player(BitmapFactory.decodeResource(getResources(), R.drawable.brendangallagher), "Brendan Gallagher", "Montreal Canadiens Right wing", "QWT"));
        mPlayers.add(new Player(BitmapFactory.decodeResource(getResources(), R.drawable.maxpacioretty), "Max Pacioretty", "Montreal Canadiens Left wing", "QWT"));
        mPlayers.add(new Player(BitmapFactory.decodeResource(getResources(), R.drawable.alexgalchenyuk), "Alex Galchenyuk", "Montreal Canadiens Centerman", "QWT"));
        mPlayers.add(new Player(BitmapFactory.decodeResource(getResources(), R.drawable.daniercarr), "Daniel Carr", "Montreal Canadiens Left wing", "QWT"));
        mPlayers.add(new Player(BitmapFactory.decodeResource(getResources(), R.drawable.andreikov), "Andrei Markov", "Montreal Canadiens Defenseman", "QWT"));
        mPlayers.add(new Player(BitmapFactory.decodeResource(getResources(), R.drawable.nathanbeaulieu), "Nathan Beaulieu", "Montreal Canadiens Defenseman", "QWT"));
        mPlayers.add(new Player(BitmapFactory.decodeResource(getResources(), R.drawable.daviddesharnais), "David Desharnais", "Montreal Canadiens Centerman", "QWT"));
        mPlayers.add(new Player(BitmapFactory.decodeResource(getResources(), R.drawable.tomasplekanec), "Tomáš Plekanec", "Montreal Canadiens Centerman", "QWT"));
        ComparisonPlayerAdapter adapter = new ComparisonPlayerAdapter(getContext(), mPlayers);
        listUI.setAdapter(adapter);

        listUI.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                //TODO Query on this player:
                Log.i(TAG, "Player: " + mPlayers.get(position).getName());
                openComparisonMenu(false, true, null, null);
                openComparisonMenu(true, false, container, mPlayers.get(position).getId());
                mPlayerPosition = position;
            }
        });
    }

    /**
     * Setup the player pop-up WITHOUT any listview
     * @param container
     * @return
     */
    private PopupWindow inflatePlayerPopup(ViewGroup container, final CheckBox checkBox){

        LayoutInflater layoutInflater
                = (LayoutInflater)getController().getBaseContext()
                .getSystemService(getController().LAYOUT_INFLATER_SERVICE);

        final View popupView = layoutInflater.inflate(R.layout.select_player_popup, container, false);

        Button cancelBtn = (Button) popupView.findViewById(R.id.cancel_button);

        final PopupWindow popupWindow = new PopupWindow(
                popupView,
                500,
                ViewGroup.LayoutParams.WRAP_CONTENT);

        cancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                popupWindow.dismiss();
                checkBox.setChecked(false); //Uncheck
            }
        });

        return popupWindow;
    }

    /**
     * Setup the shot specification pop-up WITHOUT any listview
     * @param container
     * @return
     */
    private PopupWindow inflateShotPopup(final ViewGroup container, final CheckBox checkBox){

        LayoutInflater layoutInflater
                = (LayoutInflater)getController().getBaseContext()
                .getSystemService(getController().LAYOUT_INFLATER_SERVICE);

        final View popupView = layoutInflater.inflate(R.layout.select_shot_popup, container, false);

        Button cancelBtn = (Button) popupView.findViewById(R.id.cancel_button);
        Button backBtn = (Button) popupView.findViewById(R.id.back_button);

        final PopupWindow popupWindow = new PopupWindow(
                popupView,
                500,
                ViewGroup.LayoutParams.WRAP_CONTENT);

        cancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                popupWindow.dismiss();
                checkBox.setChecked(false);
            }
        });

        backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                popupWindow.dismiss();
                openComparisonMenu(true, true, container, null);
            }
        });

        return popupWindow;
    }

    /**
     * Start the process and initialize graphics.
     * WARNING: Must be called from a UI thread.
     */
    private void start() {
        if (DEBUG || mSensorReady) {
            mStartStopButton.setVisibility(View.GONE);
            mSaveButton.setVisibility(View.VISIBLE);
            for (int i=0; i<mRecentResult.length; i++) {
                mRecentResult[i].setVisibility(View.VISIBLE);
            }
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
                byte[] send = {Protocol.SHOT_MODE, 0x00};
                try {
                    HomeFragment.getInstance().writeBLE(send);
                } catch (Exception e) {}
            }
        }
    }

    private void save() {
        // Save on a known place every shot shown
        File rootsd = Environment.getExternalStorageDirectory();
        File parent = new File(rootsd.getAbsolutePath(), FOLDER_SAVE);
        File root = new File(parent.getAbsolutePath(), FOLDER_SAVE_SHOT);
        if (!parent.exists()) {
            parent.mkdirs();
        }
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
            if (mRecent[id.get(i)] == null) {
                continue;
            }
            Pair<String, String> saveData = mRecent[id.get(i)].packageFormCSV();
            File file;
            if (mHeaderET.getText().length() != 0) {
                file = new File(root, mHeaderET.getText() + "." + saveData.first);
            } else {
                file = new File(root, saveData.first);
            }
            IO.saveFile(saveData.second, file);
            Toast.makeText(getContext(), "Save " + getString(RECENT_NAME[id.get(i)]) + " in " + file, Toast.LENGTH_LONG).show();

            if (id.get(i) == 0 && mRecent[0].getMeanAccel() > 0) {
                File analysis = new File(parent.getAbsolutePath(), FOLDER_SAVE_ANALYSIS);
                if (!analysis.exists()) {
                    analysis.mkdirs();
                }
                File userFile = new File(analysis, mUser.getName() + ".csv");
                if (userFile.exists()) {
                    String data = IO.loadFile(userFile);

                    data += mRecent[0].getTime() + "," + mRecent[0].getMeanAccel() + "," + mRecent[0].getMaxAccel() + "," +  mRecent[0].getReleaseTime() + "\n";
                    IO.saveFile(data, userFile);
                } else {
                    String data = "Name:," + mUser.getName() + "\n";
                    data += "Id:," + mUser.getId() + "\n";
                    data += "Date,Mean Acceleration,Max Acceleration,Release time\n";
                    data += mRecent[0].getTime() + "," + mRecent[0].getMeanAccel() + "," + mRecent[0].getMaxAccel() + "," + mRecent[0].getReleaseTime() + "\n";
                    IO.saveFile(data, userFile);
                }

                Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                intent.setData(Uri.fromFile(userFile));
                getActivity().sendBroadcast(intent);
            }

            Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
            intent.setData(Uri.fromFile(file));
            getActivity().sendBroadcast(intent);
        }
    }


}
