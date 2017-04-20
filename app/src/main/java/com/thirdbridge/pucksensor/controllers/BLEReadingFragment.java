package com.thirdbridge.pucksensor.controllers;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.SystemClock;
import android.support.v4.app.ActivityCompat;
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
import com.thirdbridge.pucksensor.R;
import com.thirdbridge.pucksensor.hardware.Curling;
import com.thirdbridge.pucksensor.hardware.Protocol;
import com.thirdbridge.pucksensor.models.Player;
import com.thirdbridge.pucksensor.models.ShotSpecification;
import com.thirdbridge.pucksensor.utils.BaseFragment;
import com.thirdbridge.pucksensor.utils.IO;
import com.thirdbridge.pucksensor.utils.MathHelper;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Christophe on 2015-10-14.
 * Modified by Jayson Dalpé since 2016-01-22.
 */
public class BLEReadingFragment extends BaseFragment {

	private static String TAG = BLEReadingFragment.class.getSimpleName();
    private static final String LOCAL_SHOTS = "LOCAL_SHOTS";
    private static final double DRAFT_STAMP = 50/3;
    private static final String CHECK_ACCEL = "CHECK_ACCEL";
    private static final String CHECK_SPEED = "CHECK_SPEED";
    private static final String CHECK_ROTATION = "CHECK_ROTATION";
    private static final String CHECK_AUTOSAVE = "CHECK_AUTOSAVE";
    private static final int MINIMAL_POINTS = 5;




    // Saving local instance
    SharedPreferences mSettings;

	private boolean mTestRunning = false;
    private boolean mAutoStart = false;
	private boolean mPreviewTest = false;
    private int[] mActualSettings = Protocol.DEFAULT.clone();

	private Button mStartStopButton;
	private TextView mDescriptionTextView;

    private TextView mDataTV;

	//Loading screen
	private RelativeLayout mLoadingScreenRelativeLayout;

	private boolean mTestWasRun = false;

    private Thread mBackgroundThread;
    private boolean mPause = true;


    // Player comparison management
    private List<Player> mPlayers = new ArrayList<Player>();
    private List<ShotSpecification> mShotSpecs = new ArrayList<ShotSpecification>();
    private int mPlayerPosition;


    //Bluetooth
    private boolean mSensorReady = false;
    private boolean mSensorReadyChange = true;
    private HomeFragment.BluetoothListener mListener = new HomeFragment.BluetoothListener() {
        @Override
        public void onBluetoothCommand(byte[] values) {
            onCharacteristicChanged(values);
        }
    };

    // Magnetic time
    double mStart = SystemClock.uptimeMillis();
    double mThresLow = 1000;
    double mThresHigh = 1500;
    boolean mInHigh = false;
    boolean mIsValid = false;

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


                if (mPause) {
                    break;
                }
            }
        }
    };

    public static BLEReadingFragment newInstance() {
		Bundle args = new Bundle();

		BLEReadingFragment fragment = new BLEReadingFragment();

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

		View v = inflater.inflate(R.layout.fragment_blue_reading, container, false);

		mStartStopButton = (Button) v.findViewById(R.id.start_button);
		mDescriptionTextView = (TextView) v.findViewById(R.id.stats_description_textview);
        mDataTV = (TextView) v.findViewById(R.id.data);


		mLoadingScreenRelativeLayout = (RelativeLayout) v.findViewById(R.id.loading_screen_relative_layout);


        mSettings = getActivity().getSharedPreferences("StatPuck", 0);

        mStartStopButton.setVisibility(View.VISIBLE);

        mStartStopButton.setVisibility(View.INVISIBLE);
		mStartStopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                start();

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


	private void onCharacteristicChanged(byte[] value) {
        // TODO, check different data
        mAutoStart = true;
        double sign = 1;
        double realSign = 1;
        if (Protocol.isSameMode(Protocol.LAUNCH_MODE, value[0])) {

            double mag = Protocol.getMagneticField(value);

            String val = "Time between lines is ";

            if (!mInHigh && mag > mThresLow) {
                mInHigh = true;
            }
            else if (mInHigh && mag > mThresHigh && !mIsValid) {

                val += (SystemClock.uptimeMillis() - mStart) + " ms";
                mStart = SystemClock.uptimeMillis();

                final String uiVal = val;
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mDataTV.setText(uiVal);
                    }
                });

                mIsValid = true;
            }
            else if (mInHigh && mIsValid && mag < mThresLow)
            {
                mInHigh = false;
                mIsValid = false;
            }
            else if (mInHigh && mIsValid && mag >= mThresLow)
            {
                // Clean
            }
            else
            {
                mInHigh = false;
                mIsValid = false;
            }
            double mThresLow = 1000;
            double mThresHigh = 1500;
            boolean mInHigh = false;



            Log.i(TAG, "Value BLE reading: " + val);

        }
        else
        {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mDataTV.setText("Bad protocol");
                }
            });
        }
	}



    /**
     * Start the process and initialize graphics.
     * WARNING: Must be called from a UI thread.
     */
    private void start() {
        if (mSensorReady) {
            //mStartStopButton.setVisibility(View.GONE);

            if (mTestRunning) {
                mTestRunning = false;
                mStartStopButton.setText(getString(R.string.reset));
                mStartStopButton.setCompoundDrawablesWithIntrinsicBounds(R.drawable.reset, 0, 0, 0);
                //populateStatisticsFields();
            /*} else if (mTestWasRun) {
                mTestWasRun = false;
                getController().reloadShotStatsFragment();
                if (!getController().isBleDeviceConnected())
                    mStartStopButton.setEnabled(false);*/
            } else {
                mTestWasRun = true;


                mStartStopButton.setText(getString(R.string.stopTest));
                mStartStopButton.setCompoundDrawablesWithIntrinsicBounds(android.R.drawable.ic_delete, 0, 0, 0);

                mTestRunning = true;
                byte[] send = {Protocol.LAUNCH_MODE, 0x00};
                try {
                    HomeFragment.getInstance().writeBLE(send);
                } catch (Exception e) {}
            }
        }
    }



}
