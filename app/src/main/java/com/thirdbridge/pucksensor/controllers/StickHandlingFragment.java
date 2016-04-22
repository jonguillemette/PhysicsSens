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
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TableLayout;
import android.widget.TableRow;
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
import com.thirdbridge.pucksensor.models.Exercise;
import com.thirdbridge.pucksensor.models.Player;
import com.thirdbridge.pucksensor.models.ShotSpecification;
import com.thirdbridge.pucksensor.models.User;
import com.thirdbridge.pucksensor.utils.BaseFragment;
import com.thirdbridge.pucksensor.utils.Calibrate;
import com.thirdbridge.pucksensor.utils.CellOrganizer;
import com.thirdbridge.pucksensor.utils.Constants;
import com.thirdbridge.pucksensor.utils.IO;
import com.thirdbridge.pucksensor.utils.MathHelper;
import com.thirdbridge.pucksensor.utils.Shot;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Christophe on 2015-10-14.
 * Modified by Jayson Dalpé since 2016-01-22.
 */
public class StickHandlingFragment extends BaseFragment {

	private static String TAG = StickHandlingFragment.class.getSimpleName();
    private static String FOLDER_SAVE_SHOT = "Shots";
    private static final boolean DEBUG = true;


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
    private static final int[] RECENT_NAME = {R.string.recent_shot1, R.string.recent_shot2, R.string.recent_shot3, R.string.recent_shot4, R.string.recent_shot5};


    // Saving local instance
    SharedPreferences mSettings;

	private boolean mTestRunning = false;
    private boolean mAutoStart = false;
	private User mUser;
	private boolean mPreviewTest = false;
    private boolean mProgressChange = true;
    private int[] mActualSettings = DEFAULT.clone();

	private Button mStartStopButton;
	private Button mSaveButton;
	private TextView mDescriptionTextView;
    private ImageButton mVideobutton;
    private int mExerciseIndex = 0;

	//Loading screen
	private RelativeLayout mLoadingScreenRelativeLayout;

	private boolean mTestWasRun = false;

    private Thread mBackgroundThread;
    private boolean mPause = true;

    //UI
    Button mGenerateButton;
    Button mHackButton;
    Button mLoadExercise;
    Button mStartExercise;
    TableLayout mTable;
    CellOrganizer mCell;

    // Player comparison management
    private List<Player> mPlayers = new ArrayList<Player>();
    private List<ShotSpecification> mShotSpecs = new ArrayList<ShotSpecification>();
    private int mPlayerPosition;
    private List<Exercise> mExercises = new ArrayList<Exercise>();
    private String mJSONExercise;


    // Check management
    private CheckBox[] mRecentResult;
    private Button mComparePlusBtn;
    private int mFirstCheck = 0;
    private int mSecondCheck = -1;
    private boolean mCanTouchThis = true;

    // Popup windows
    PopupWindow mExercisePopup;
    PopupWindow mVideoPopup;


    //Bluetooth
    private boolean mSensorReady = false;
    private boolean mSensorReadyChange = true;
    private HomeFragment.BluetoothListener mListener = new HomeFragment.BluetoothListener() {
        @Override
        public void onBluetoothCommand(byte[] values) {
            onCharacteristicChanged(values);
        }
    };
    private boolean mSendOnce = false;

    // Popup for player and shot specification
    private PopupWindow mPlayerPopup;
    private PopupWindow mShotSpecPopup;

    // Thread running
    Runnable mRun = new Runnable() {
        @Override
        public void run() {
            while(true) {
                if (!mTestRunning && mAutoStart) {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            start();
                        }
                    });
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



    public static StickHandlingFragment newInstance(User user) {
		Bundle args = new Bundle();
		args.putString(Constants.CURRENT_USER, new Gson().toJson(user, User.class));

		StickHandlingFragment fragment = new StickHandlingFragment();
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

		View v = inflater.inflate(R.layout.fragment_stick, container, false);

		mStartStopButton = (Button) v.findViewById(R.id.start_button);
		mSaveButton = (Button) v.findViewById(R.id.save_button);
		mDescriptionTextView = (TextView) v.findViewById(R.id.stats_description_textview);
		mGenerateButton = (Button) v.findViewById(R.id.generate_button);
        mHackButton = (Button) v.findViewById(R.id.demo_start_button);
        mLoadExercise = (Button) v.findViewById(R.id.load_exercice_button);
        mStartExercise = (Button) v.findViewById(R.id.start_exercice_button);
        mTable = (TableLayout) v.findViewById(R.id.table_layout);
        mVideobutton = (ImageButton) v.findViewById(R.id.video_button);


		mLoadingScreenRelativeLayout = (RelativeLayout) v.findViewById(R.id.loading_screen_relative_layout);

        // Menu

        mSettings = getActivity().getSharedPreferences("StatPuck", 0);


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
                        //TODO openComparisonMenu(true, true, container, null);
                        mComparePlusBtn.setVisibility(View.VISIBLE);
                    } else {
                        mComparePlusBtn.setVisibility(View.GONE);
                        checkRecent(4, isChecked);
                        mRecentResult[4].setText(RECENT_NAME[4]);
                    }
                }
            }
        });

        mVideobutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });

        mLoadExercise.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openExerciseMenu(true, container);
            }
        });

        mStartExercise.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i(TAG, mJSONExercise);
                //mExercises.get(mExerciseIndex).loadInformation(mJSONExercise);
            }
        });

        mStartStopButton.setVisibility(View.VISIBLE);
        mSaveButton.setVisibility(View.GONE);

        mComparePlusBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //openComparisonMenu(true, true, container, null);
            }
        });

        // TODO Pop-up management
        mExercisePopup = inflateExercisePopup(container);


        if (DEBUG) {
            mGenerateButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String titles[] = {"Test", "Karabomga", "Pizza"};
                    if (mCell == null) {
                        mCell = new CellOrganizer(mTable, titles, "Key Points", true, 10);
                        mCell.allActualize();
                    } else {
                        mCell.clear();
                        mCell.reload(titles, "Banana", false, 75);
                        mCell.allActualize();
                    }
                }
            });

            mHackButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mCell.put(1, 11, 10, true);
                }
            });
        } else {
            mHackButton.setVisibility(View.GONE);
            mGenerateButton.setVisibility(View.GONE);
        }


		mStartStopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                start();

            }
        });

		mSaveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //TODO Save other format
                /*
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
                    if (mRecent[id.get(i)] == null) {
                        continue;
                    }
                    Pair<String, String> saveData = mRecent[id.get(i)].packageFormCSV();
                    File file = new File(root, saveData.first);
                    IO.saveFile(saveData.second, file);
                    Toast.makeText(getContext(), "Save " + getString(RECENT_NAME[id.get(i)]) + " in " + file, Toast.LENGTH_LONG).show();

                    Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                    intent.setData(Uri.fromFile(file));
                    getActivity().sendBroadcast(intent);
                }
                */
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
			Handler handler = new Handler();
			handler.postDelayed(new Runnable() {
                public void run() {
                    // TODO Some setup
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


	private void onCharacteristicChanged(byte[] value) {
        if (value[0] != SETTINGS_READ) {

            //Log.i(TAG, "Check first: AH: " + accelHigh[0] + " AL: " + accelLow[0] + " Gyro: " + gyro[0]);
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
                    HomeFragment.getInstance().writeBLE(send);
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
        //Log.i(TAG, "Value: " + val);
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
            } else if (mTestWasRun) {
                mTestWasRun = false;
                getController().reloadStickHandFragment();
                if (!getController().isBleDeviceConnected())
                    mStartStopButton.setEnabled(false);
            } else {
                mTestWasRun = true;
                //TODO Start BLE code

                mStartStopButton.setText(getString(R.string.stopTest));
                mStartStopButton.setCompoundDrawablesWithIntrinsicBounds(android.R.drawable.ic_delete, 0, 0, 0);

                mTestRunning = true;
                byte[] send = {DATA_READY, 0x00};
                try {
                    HomeFragment.getInstance().writeBLE(send);
                } catch (Exception e) {}
            }
        }
    }

    private PopupWindow inflateExercisePopup(ViewGroup container){

        LayoutInflater layoutInflater
                = (LayoutInflater)getController().getBaseContext()
                .getSystemService(getController().LAYOUT_INFLATER_SERVICE);

        final View popupView = layoutInflater.inflate(R.layout.select_exercise_popup, container, false);

        Button cancelBtn = (Button) popupView.findViewById(R.id.cancel_button);

        final PopupWindow popupWindow = new PopupWindow(
                popupView,
                500,
                500);

        cancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                popupWindow.dismiss();
            }
        });

        return popupWindow;
    }

    private void openExerciseMenu(boolean open, ViewGroup container) {
        if (open) {
            setupExercise((ListView) mExercisePopup.getContentView().findViewById(R.id.exercise_list), container);
            mExercisePopup.setFocusable(true);
            mExercisePopup.update();
            mExercisePopup.showAtLocation(container, Gravity.TOP, 0, 200);
        } else {
            mExercisePopup.dismiss();
        }
    }

    private void setupExercise(ListView listUI, final ViewGroup container) {
        //TODO Query on the internet
        mExercises.clear();
        mExercises.add(new Exercise("QWERTY", "Cones exercise", "Pass the puck between cones."));
        mExercises.add(new Exercise("QWERTY", "Glove & Infinite symbol", "Place gloves on ice and create a infinite symbole over the two gloves."));

        LoadExerciseAdapter adapter = new LoadExerciseAdapter(getContext(), mExercises);
        listUI.setAdapter(adapter);

        listUI.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                //TODO Query on this player:
                Log.i(TAG, "Exercise: " + mExercises.get(position).getTitle());

                mJSONExercise = IO.loadAssetTextAsString(getContext(), "dummy.json");
                openExerciseMenu(false, null);
                mStartExercise.setText(R.string.start_exercice);
                mStartExercise.setText(mStartExercise.getText() + ": " + mExercises.get(position).getTitle());
                mExerciseIndex = position;
            }
        });
    }


}
