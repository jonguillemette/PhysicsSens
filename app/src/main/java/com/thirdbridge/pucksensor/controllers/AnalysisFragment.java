package com.thirdbridge.pucksensor.controllers;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Pair;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.thirdbridge.pucksensor.R;
import com.thirdbridge.pucksensor.models.User;
import com.thirdbridge.pucksensor.utils.BaseFragment;
import com.thirdbridge.pucksensor.utils.Calibrate;
import com.thirdbridge.pucksensor.utils.Constants;
import com.thirdbridge.pucksensor.utils.IO;
import com.thirdbridge.pucksensor.utils.Protocol;

import java.io.File;
import java.lang.reflect.Array;
import java.util.ArrayList;

/**
 * Created by Christophe on 2015-10-14.
 * Modified by Jayson Dalpé since 2016-01-22.
 */
public class AnalysisFragment extends BaseFragment {

	private static String TAG = AnalysisFragment.class.getSimpleName();
    private static String FOLDER_SAVE = "Statpuck";
    private static String FOLDER_SAVE_ANALYSIS = "Analysis";


    // Saving local instance
    SharedPreferences mSettings;

	private boolean mPreviewTest = false;

	private TextView mDescriptionTextView;

	//Loading screen
	private RelativeLayout mLoadingScreenRelativeLayout;


    // Menu
    private TextView mTimeEvolutionTV;
	private TextView mMeanAccelTV;
	private WebView mMeanAccelWV;
	private TextView mMaxAccelTV;
	private WebView mMaxAccelWV;
	private TextView mCompareTV;
	private Spinner mCompareSpinner;
	private WebView mCompareWB;
	private UserArrayAdapter mSpinnerAdapter;

	// User management
    User mUser;
	private ArrayList<User> mUsers;


    public static AnalysisFragment newInstance(User user) {
        Bundle args = new Bundle();
        args.putString(Constants.CURRENT_USER, new Gson().toJson(user, User.class));

        AnalysisFragment fragment = new AnalysisFragment();
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
	}

	@Override
	public void onPause() {
        Log.d(TAG, "onPause");
		super.onPause();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, final ViewGroup container, Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);

		View v = inflater.inflate(R.layout.fragment_analysis, container, false);

		mDescriptionTextView = (TextView) v.findViewById(R.id.stats_description_textview);
        mTimeEvolutionTV = (TextView) v.findViewById(R.id.time_evolution_label);
		mMeanAccelTV = (TextView) v.findViewById(R.id.mean_accel_label);
		mMeanAccelWV = (WebView) v.findViewById(R.id.mean_accel_webview);
		mMaxAccelTV = (TextView) v.findViewById(R.id.max_accel_label);
		mMaxAccelWV = (WebView) v.findViewById(R.id.max_accel_webview);
		mCompareTV = (TextView) v.findViewById(R.id.compare_label);
		mCompareSpinner = (Spinner) v.findViewById(R.id.compare_spinner);
		mCompareWB = (WebView) v.findViewById(R.id.compare_webview);

		mLoadingScreenRelativeLayout = (RelativeLayout) v.findViewById(R.id.loading_screen_relative_layout);

        mSettings = getActivity().getSharedPreferences("StatPuck", 0);

		Log.i(TAG, "width " + getActivity().findViewById(android.R.id.content).getWidth());
		Log.i(TAG, "height " + getActivity().findViewById(android.R.id.content).getHeight());

		float ratio = getActivity().findViewById(android.R.id.content).getWidth();
		LinearLayout.LayoutParams webViewLayout = new LinearLayout.LayoutParams((int)(ratio * 0.45), (int)(ratio * 0.45));
		webViewLayout.setMargins(20, 20, 20, 20);
        webViewLayout.gravity = Gravity.CENTER;

		mMeanAccelWV.setLayoutParams(webViewLayout);
		mMaxAccelWV.setLayoutParams(webViewLayout);
		mCompareWB.setLayoutParams(webViewLayout);

        populateSpinner();
        mCompareWB.setVisibility(View.GONE);

        loadData();

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

    private void loadData() {
        File rootsd = Environment.getExternalStorageDirectory();
        File parent = new File(rootsd.getAbsolutePath(), FOLDER_SAVE);
        File analysis = new File(parent.getAbsolutePath(), FOLDER_SAVE_ANALYSIS);
        if (!analysis.exists()) {
            analysis.mkdirs();
        }
        File userFile = new File(analysis, mUser.getName() + ".csv");
        if (userFile.exists()) {
            String data = IO.loadFile(userFile);

        } else {
            Toast.makeText(getContext(), "No shot save for " + mUser.getName(), Toast.LENGTH_LONG).show();
            getController().gotoHome();
        }
    }

	private void populateSpinner(){
		mUsers = new ArrayList<User>();

		File root = new File(this.getContext().getFilesDir(), "users");
		if (!root.exists()) {
			root.mkdirs();
			return;
		}

		File[] users = root.listFiles();
		for(int i=0; i<users.length; i++){
			User newUser = User.depackageForm(IO.loadFile(users[i]));
            if (!newUser.getId().equals(mUser.getId())) {
                mUsers.add(newUser);
            }
		}

		mSpinnerAdapter = new UserArrayAdapter(getController(), R.layout.spinner_dropdown_item, mUsers);
		mCompareSpinner.setAdapter(mSpinnerAdapter);
	}
}
