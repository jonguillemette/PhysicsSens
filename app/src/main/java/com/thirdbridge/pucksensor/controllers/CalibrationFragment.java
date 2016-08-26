package com.thirdbridge.pucksensor.controllers;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.thirdbridge.pucksensor.R;
import com.thirdbridge.pucksensor.utils.BaseFragment;
import com.thirdbridge.pucksensor.hardware.Calibrate;
import com.thirdbridge.pucksensor.utils.IO;
import com.thirdbridge.pucksensor.hardware.Protocol;

import java.io.File;

/**
 * Created by Christophe on 2015-10-14.
 * Modified by Jayson Dalpé since 2016-01-22.
 */
public class CalibrationFragment extends BaseFragment {

	private static String TAG = CalibrationFragment.class.getSimpleName();
    private static String FOLDER_SAVE = "Statpuck";
    private static String FOLDER_SAVE_CALIB = "Calibration";

    private static final int MINIMAL_G = 5;
    private static final String THRESHOLD_G = "THRESHOLD_G";

    private static final float MINIMAL_STICK_G = 0.25f;
    private static final String THRESHOLD_STICK_G = "STICK_THRESHOLD_G";

    private static final int MINIMAL_RELEASE_G = 2;
    private static final String THRESHOLD_RELEASE_G = "THRESHOLD_RELEASE_G";

    private static final int MINIMAL_POINTS = 5;
    private static final String POINTS_BOARD = "POINTS_BOARD";

    private static final int SHOT_WAIT_MINIMUM = 1;
    private static final int SHOT_WAIT_VALUE = 3;
    private static final String SHOT_WAIT = "SHOT_WAIT";


    // Calibration pattern
    final static long[] CALIB_TIME= {5000, 1000}; // Position time, transition time.
    final static double[] CALIB_VALUE = {0, 50, 400}; // Start, Step, Max


    // Saving local instance
    SharedPreferences mSettings;

	private boolean mTestRunning = false;
    private boolean mAutoStart = false;
	private boolean mPreviewTest = false;
    private boolean mProgressChange = false;
    private int[] mActualSettings = Protocol.DEFAULT.clone();

	private TextView mDescriptionTextView;

	//Loading screen
	private RelativeLayout mLoadingScreenRelativeLayout;

	private boolean mTestWasRun = false;

    private Thread mBackgroundThread;
    private boolean mPause = true;

    // Menu
    private TextView mPeakAccTV;
    private SeekBar mPeakAccSB;
    private ProgressBar mCalibratePB;
    private Button mCalibrateBtn;
    private Button mCalibrateCenBtn;
    private TextView mStickAccTV;
    private SeekBar mStickAccSB;
    private TextView mReleaseAccTV;
    private SeekBar mReleaseAccSB;
    private TextView mPointsBoardTV;
    private SeekBar mPointsBoardSB;
    private TextView mShotWaitTV;
    private SeekBar mShotWaitSB;


	//Calibration
	private float mTimeStep = 0f;
	private final double GRAVITY = 9.80665;
    private boolean mCalibrationDone = false;
    private boolean mSendOnce = false;


    // Calibration
    private double[] mCalibrationValue = {0,0,0};
    private int mCalibrateNb = 0;
    boolean mStartCalibration = false;
    boolean mStartCalibrationCen = false;
    Calibrate mCalibrationRoutine;
    boolean mAppIsBoss = false;

    //Bluetooth
    private boolean mSensorReady = false;
    private boolean mSensorReadyChange = true;
    private HomeFragment.BluetoothListener mListener = new HomeFragment.BluetoothListener() {
        @Override
        public void onBluetoothCommand(byte[] values) {
            onCharacteristicChanged(values);
        }
    };


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
                    } else {
                        mSensorReady = false;
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

                Log.i("ALLO", "Value: " + mProgressChange);
                if (mProgressChange && mSettings != null) {
                    Log.i("ALLO", "Value: enter");
                    long value = mSettings.getInt(THRESHOLD_G, MINIMAL_G);

                    float stick_value = mSettings.getFloat(THRESHOLD_STICK_G, MINIMAL_STICK_G);

                    byte[] send = new byte[20];
                    send[0] = Protocol.SETTINGS_NEW;
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

                    stick_value = stick_value * 2048 / 16;
                    value = (int) stick_value;
                    mActualSettings[8] = (int) (value & 255);
                    mActualSettings[9] = (int) (value/256 & 255);

                    Log.i(TAG, "Value: " + value);
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
                        HomeFragment.getInstance().writeBLE(send);
                        mProgressChange = false;
                        mAppIsBoss = false;
                    } catch(Exception e) {
                    }
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

            byte[] send = {Protocol.CALIB_AXIS, 0x00};

            // The puck is working by itself, the most idiot thing to do is to touch it actually..
            // validate by freeroaming mode after.

            try {
                HomeFragment.getInstance().writeBLE(send);
                Thread.sleep(2050);
            } catch (Exception e) {}

            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mCalibrateBtn.setVisibility(View.VISIBLE);
                    mCalibratePB.setVisibility(View.GONE);
                    mCalibrateCenBtn.setEnabled(true);
                }
            });
        }
    };

    Runnable mStartCalibrationCenRunnable = new Runnable() {
        @Override
        public void run() {

            int[] saveValue = {mActualSettings[5], mActualSettings[6], mActualSettings[7]};
            mActualSettings[5] = 0;
            mActualSettings[6] = 0;
            mActualSettings[7] = 0;

            int actualProgress = mSettings.getInt(THRESHOLD_G, MINIMAL_G);

            SharedPreferences.Editor editor = mSettings.edit();
            editor.putInt(THRESHOLD_G, 4000);
            editor.commit();

            byte[] send = {Protocol.FREE_MODE, 0x00};
            try {
                HomeFragment.getInstance().writeBLE(send);
                Thread.sleep(1050);
            } catch (Exception e) {}

            mProgressChange = true;

            try {
                // Sleep fo 1sec (setting thread delay) + 50 ms (IC max delay for settings)
                Thread.sleep(2000);
            } catch (Exception e) {}


            // Initialise calibration cen
            mCalibrationRoutine = new Calibrate();

            for (int i=(int)CALIB_VALUE[0]; i<=CALIB_VALUE[2]; i+= (int)CALIB_VALUE[1]) {
                mCalibrationValue[0] = (double) i;
                mStartCalibrationCen = true;
                try {
                    Thread.sleep(CALIB_TIME[0]);
                } catch (Exception e) {}
                mStartCalibrationCen = false;
                try {
                    Thread.sleep(CALIB_TIME[1]);
                } catch (Exception e) {}
            }

            // Gather result and save them
            File rootsd = Environment.getExternalStorageDirectory();
            File parent = new File(rootsd.getAbsolutePath(), FOLDER_SAVE);
            File root = new File(parent.getAbsolutePath(), FOLDER_SAVE_CALIB);
            if (!parent.exists()) {
                parent.mkdirs();
            }
            if (!root.exists()) {
                root.mkdirs();
            }
            Pair<String, String> saveData = mCalibrationRoutine.packageFormCSV();
            final File file = new File(root, saveData.first);
            IO.saveFile(saveData.second, file);
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getContext(), "Save calibration in " + file.getName(), Toast.LENGTH_LONG).show();
                }
            });

            Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
            intent.setData(Uri.fromFile(file));
            getActivity().sendBroadcast(intent);

            mAppIsBoss = true;
            mActualSettings[5] = saveValue[0];
            mActualSettings[6] = saveValue[1];
            mActualSettings[7] = saveValue[2];
            editor.putInt(THRESHOLD_G, actualProgress);
            editor.commit();

            byte[] send2 = {Protocol.SETTINGS_MODE, 0x00};
            try {
                HomeFragment.getInstance().writeBLE(send2);
                Thread.sleep(1050);
            } catch (Exception e) {}


            mProgressChange = true;


            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mCalibrateCenBtn.setVisibility(View.VISIBLE);
                    mCalibratePB.setVisibility(View.GONE);
                    mCalibrateBtn.setEnabled(true);
                }
            });
        }
    };


    public static CalibrationFragment newInstance() {
		CalibrationFragment fragment = new CalibrationFragment();
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

		View v = inflater.inflate(R.layout.fragment_calibration, container, false);

		mDescriptionTextView = (TextView) v.findViewById(R.id.stats_description_textview);
        mPeakAccTV = (TextView) v.findViewById(R.id.peak_acc_number);
        mPeakAccSB = (SeekBar) v.findViewById(R.id.peak_acc_seekbar);
        mCalibrateBtn = (Button) v.findViewById(R.id.calibrate_button);
        mCalibratePB = (ProgressBar) v.findViewById(R.id.calibrate_progress);
        mCalibrateCenBtn = (Button) v.findViewById(R.id.calibrate_centrifuge_button);
        mStickAccTV = (TextView) v.findViewById(R.id.stick_acc_number);
        mStickAccSB = (SeekBar) v.findViewById(R.id.stick_acc_seekbar);
        mReleaseAccTV = (TextView) v.findViewById(R.id.release_acc_number);
        mReleaseAccSB = (SeekBar) v.findViewById(R.id.release_acc_seekbar);
        mPointsBoardTV = (TextView) v.findViewById(R.id.points_board_number);
        mPointsBoardSB = (SeekBar) v.findViewById(R.id.points_board_seekbar);
        mShotWaitTV = (TextView) v.findViewById(R.id.shot_wait_number);
        mShotWaitSB = (SeekBar) v.findViewById(R.id.shot_wait_seekbar);

		mLoadingScreenRelativeLayout = (RelativeLayout) v.findViewById(R.id.loading_screen_relative_layout);



        mSettings = getActivity().getSharedPreferences("StatPuck", 0);

        String value = "" + (mSettings.getInt(THRESHOLD_G, MINIMAL_G));
        mPeakAccTV.setText(value);
        mPeakAccSB.setProgress(Integer.parseInt(mPeakAccTV.getText().toString()) - MINIMAL_G);

        value = "" + (mSettings.getFloat(THRESHOLD_STICK_G, MINIMAL_STICK_G));
        mStickAccTV.setText(value);
        mStickAccSB.setProgress((int) ((mSettings.getFloat(THRESHOLD_STICK_G, MINIMAL_STICK_G) - 0.25f) * 4f));

        value = "" + (mSettings.getInt(THRESHOLD_RELEASE_G, MINIMAL_RELEASE_G));
        mReleaseAccTV.setText(value);
        mReleaseAccSB.setProgress((int) ((mSettings.getInt(THRESHOLD_RELEASE_G, MINIMAL_RELEASE_G) - MINIMAL_RELEASE_G) * 0.5f));

        value = "" + (mSettings.getInt(POINTS_BOARD, MINIMAL_POINTS));
        mPointsBoardTV.setText(value);
        mPointsBoardSB.setProgress((int) ((mSettings.getInt(POINTS_BOARD, MINIMAL_POINTS) - MINIMAL_POINTS) * 0.5f));

        value = "" + (mSettings.getInt(SHOT_WAIT, SHOT_WAIT_VALUE));
        mShotWaitTV.setText(value + " s");
        mShotWaitSB.setProgress(mSettings.getInt(SHOT_WAIT, SHOT_WAIT_VALUE)-SHOT_WAIT_MINIMUM);

        mCalibratePB.setVisibility(View.GONE);


        mCalibrateBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCalibratePB.setVisibility(View.VISIBLE);
                mCalibrateBtn.setVisibility(View.GONE);
                mCalibrateCenBtn.setEnabled(false);

                Thread t = new Thread(mStartCalibrationRunnable);
                t.start();
            }
        });

        mCalibrateCenBtn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                mCalibrateBtn.setEnabled(false);
                mCalibratePB.setVisibility(View.VISIBLE);
                mCalibrateCenBtn.setVisibility(View.GONE);
                Thread t = new Thread(mStartCalibrationCenRunnable);
                t.start();
            }
        });




        mPeakAccSB.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                mPeakAccTV.setText("" + (progress + MINIMAL_G));
                SharedPreferences.Editor editor = mSettings.edit();
                editor.putInt(THRESHOLD_G, progress + MINIMAL_G);
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

        mStickAccSB.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float value = (float)progress*0.25f + 0.25f;
                mStickAccTV.setText("" + value);
                SharedPreferences.Editor editor = mSettings.edit();
                editor.putFloat(THRESHOLD_STICK_G, value);
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

        mReleaseAccSB.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int value = progress*2 + MINIMAL_RELEASE_G;
                mReleaseAccTV.setText("" + value);
                SharedPreferences.Editor editor = mSettings.edit();
                editor.putInt(THRESHOLD_RELEASE_G, value);
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

        mPointsBoardSB.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int value = (int)((double)progress*2) + MINIMAL_POINTS;
                mPointsBoardTV.setText("" + value);
                SharedPreferences.Editor editor = mSettings.edit();
                editor.putInt(POINTS_BOARD, value);
                editor.commit();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        mShotWaitSB.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int value = (int)((double)progress) + SHOT_WAIT_MINIMUM;
                mShotWaitTV.setText(value + " s");
                SharedPreferences.Editor editor = mSettings.edit();
                editor.putInt(SHOT_WAIT, value);
                editor.commit();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

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


		}

	}



	private void onCharacteristicChanged(byte[] value) {
        mAutoStart = true;
        double[] accelHigh = Protocol.getAccelHighShot(value);
        double[] accelLow = Protocol.getAccelLowShot(value);
        double[] gyro = Protocol.getGyroShot(value);
        if (value[0] == Protocol.DATA_DRAFT) {
            // Calibration process
            if (mStartCalibration) {
                mCalibrationValue[0] += (double) (short) value[2];
                mCalibrationValue[1] += (double) (short) value[4];
                mCalibrationValue[2] += (double) (short) value[6];
                mCalibrateNb++;

                mCalibrationValue[0] += (double) (short) value[8];
                mCalibrationValue[1] += (double) (short) value[10];
                mCalibrationValue[2] += (double) (short) value[12];
                mCalibrateNb++;

                mCalibrationValue[0] += (double) (short) value[14];
                mCalibrationValue[1] += (double) (short) value[16];
                mCalibrationValue[2] += (double) (short) value[18];
                mCalibrateNb++;

            // Calibration with cetrifuge
            } else if (mStartCalibrationCen) { // Delete DATA_START, ... when calibrating forces
                for (int i = 0; i < accelHigh.length; i++) {
                    mCalibrationRoutine.addEntry(accelHigh[i], mCalibrationValue[0]);
                }

            }
            //Log.i(TAG, "Check first: AH: " + accelHigh[0] + " AL: " + accelLow[0] + " Gyro: " + gyro[0]);
        } else {
            if (value[2] != Protocol.VALIDITY_TOKEN && !mSendOnce) {
                // Settings don't care, send default ones
                byte[] send = new byte[20];
                send[0] = Protocol.SETTINGS_NEW;
                send[1] = 0; //Battery, don't care
                for (int i=0; i<18; i++) {
                    if (i < Protocol.DEFAULT.length) {
                        send[2+i] = (byte)mActualSettings[i];
                    } else {
                        send[2+i] = 0;
                    }
                }
                try {
                    HomeFragment.getInstance().writeBLE(send);
                    mSendOnce = true;
                } catch (Exception e) {

                }
            }

            /*if (!mAppIsBoss) {
                for (int i = 0; i < mActualSettings.length; i++) {
                    mActualSettings[i] = value[2 + i];
                }
            }*/
        }

        String val = "";
        for (int i=0; i<value.length; i++) {
            val += value[i] + ", ";
        }
        Log.i(TAG, "Value: " + val);
	}



    /**
     * Start the process and initialize graphics.
     * WARNING: Must be called from a UI thread.
     */
    private void start() {
        if (mSensorReady) {
            mCalibrateBtn.setVisibility(View.VISIBLE);

            if (mTestRunning) {
                mTestRunning = false;
            } else if (mTestWasRun) {
                mTestWasRun = false;
                getController().reloadShotStatsFragment();
            } else {
                mTestWasRun = true;
                mTestRunning = true;

            }
            byte[] send = {Protocol.SETTINGS_MODE, 0x00};
            try {
                HomeFragment.getInstance().writeBLE(send);
                Thread.sleep(100);
                mProgressChange = true;
            } catch (Exception e) {}


        }
    }


}
