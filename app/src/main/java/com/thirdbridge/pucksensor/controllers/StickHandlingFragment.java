package com.thirdbridge.pucksensor.controllers;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
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
import com.thirdbridge.pucksensor.utils.Protocol;
import com.thirdbridge.pucksensor.utils.Shot;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Christophe on 2015-10-14.
 * Modified by Jayson Dalpé since 2016-01-22.
 */
public class StickHandlingFragment extends BaseFragment {

	private static String TAG = StickHandlingFragment.class.getSimpleName();
    private static String FOLDER_SAVE_SHOT = "Statpuck";
    private static final boolean DEBUG = true;

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
	private TextView mDescriptionTextView;
    private ImageButton mVideobutton;
    private int mExerciseIndex = 0;
    private TextView mKeyExerciseTV;

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

    // Beta tested autoincroment
    int mIncrement = 0;

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
        mKeyExerciseTV = (TextView) v.findViewById(R.id.key_exercise);

        mVideobutton.setVisibility(View.GONE);
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
                getController().gotoYoutube(mExercises.get(mExerciseIndex).getVideo());
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
                Log.i(TAG, "Acceleration: " + mExercises.get(mExerciseIndex).getKeyPoint(0).getAccelerationMean());
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

                    if (mCell == null) {
                        String titles[] = new String[20];
                        for (int i=0; i<titles.length; i++) {
                            titles[i] = "Data " + i;
                        }
                        mCell = new CellOrganizer(mTable, titles, "Key Points", true, 1);
                        mCell.allActualize();
                    }
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
        mAutoStart = true;
        if (Protocol.isSameMode(Protocol.STICK_MODE, value[0])) {
            // TODO OnChanged
            if (value[2] == 1) {
                for (int i=0; i<10; i++) {
                    mCell.put(i, mIncrement, value[2+i], false);
                }
            } else {
                for (int i=0; i<10; i++) {
                    mCell.put(i+10, mIncrement, value[2+i], false);
                }
                mIncrement ++;
                mCell.allActualize();
            }



        } else if (Protocol.isSameMode(Protocol.SETTINGS_MODE, value[0])) {
            if (value[2] != Protocol.VALIDITY_TOKEN && !mSendOnce) {
                Protocol.setDefault();
                mSendOnce = true;
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

                mStartStopButton.setVisibility(View.GONE);
                mSaveButton.setVisibility(View.VISIBLE);

                mTestRunning = true;
                byte[] send = {Protocol.STICK_START, 0x00};
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

        mExercises.add(new Exercise("QWERTY", "Cones exercise", "Pass the puck between cones.", "btPJPFnesV4"));
        mExercises.add(new Exercise("QWERTY", "Glove & Infinite symbol", "Place gloves on ice and create a infinite symbole over the two gloves.","NJMkSD1PUQA"));

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

                mExercises.get(mExerciseIndex).loadInformation(mJSONExercise);
                String text = "";
                if (mExercises.get(mExerciseIndex).getKeyNotes() != null) {
                    for (int i = 0; i < mExercises.get(mExerciseIndex).getKeyNotes().length; i++) {
                        text += mExercises.get(mExerciseIndex).getKeyNotes()[i];
                        if (i < mExercises.get(mExerciseIndex).getKeyNotes().length - 1) {
                            text += "\n";
                        }
                    }
                }
                mKeyExerciseTV.setText(text);

                // Update images:
                loadImageFromYoutube(mExercises.get(position).getVideo(), mVideobutton);
            }
        });
    }

    public void loadImageFromYoutube(final String video, final ImageButton img) {
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    URL url = null;
                    url = new URL("http://img.youtube.com/vi/" + video + "/mqdefault.jpg");
                    final Bitmap bmp = BitmapFactory.decodeStream(url.openConnection().getInputStream());
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            img.setImageBitmap(bmp);
                            img.setVisibility(View.VISIBLE);
                        }
                    });

                } catch (MalformedURLException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        t.start();

    }


}
