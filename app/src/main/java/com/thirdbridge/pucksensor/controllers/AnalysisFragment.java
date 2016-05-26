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
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.AdapterView;
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
import com.thirdbridge.pucksensor.utils.PlotLyConverter;
import com.thirdbridge.pucksensor.utils.Protocol;
import com.thirdbridge.pucksensor.utils.Shot;

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
    private int mWidthWebView;

    private PlotLyConverter mConverter;

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

        mConverter = new PlotLyConverter(getContext());

		Log.i(TAG, "width " + getActivity().findViewById(android.R.id.content).getWidth());
		Log.i(TAG, "height " + getActivity().findViewById(android.R.id.content).getHeight());

		float ratio = getActivity().findViewById(android.R.id.content).getWidth();
        mWidthWebView = (int)(ratio * 0.45);
		LinearLayout.LayoutParams webViewLayout = new LinearLayout.LayoutParams(mWidthWebView, mWidthWebView);
		webViewLayout.setMargins(20, 20, 20, 20);
        webViewLayout.gravity = Gravity.CENTER;

		mMeanAccelWV.setLayoutParams(webViewLayout);
		mMaxAccelWV.setLayoutParams(webViewLayout);
		mCompareWB.setLayoutParams(webViewLayout);

        populateSpinner();
        mCompareWB.setVisibility(View.GONE);

        mCompareSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                updateCompare(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        if (mCompareSpinner.getSelectedItemPosition() >= 0) {
            updateCompare(mCompareSpinner.getSelectedItemPosition());
        }

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

    private void updateCompare(int position) {

        File rootsd = Environment.getExternalStorageDirectory();
        final File parent = new File(rootsd.getAbsolutePath(), FOLDER_SAVE);
        File analysis = new File(parent.getAbsolutePath(), FOLDER_SAVE_ANALYSIS);
        if (!analysis.exists()) {
            analysis.mkdirs();
        }
        final File userFile1 = new File(analysis, mUser.getName() + ".csv");
        final File userFile2 = new File(analysis, mUsers.get(position).getName() + ".csv");
        if (userFile1.exists() && userFile2.exists()) {
            mCompareWB.setVisibility(View.VISIBLE);
            String data1 = IO.loadFile(userFile1);
            String data2 = IO.loadFile(userFile2);

            WebSettings wSettings1;
            wSettings1 = mCompareWB.getSettings();
            wSettings1.setJavaScriptEnabled(true);
            File file = new File(parent, "web3.html");

            double[] elements1 = Shot.extractMeanMax(data1);
            double[] elements2 = Shot.extractMeanMax(data2);
            String[] x = {"Mean " + mUser.getName(), "Mean " + mUsers.get(position).getName(),
                          "Max " + mUser.getName(), "Max " + mUsers.get(position).getName()};
            double mean1 = 0;
            double mean1Error = 0;
            double mean2 = 0;
            double mean2Error = 0;
            double max1 = 0;
            double max1Error = 0;
            double max2 = 0;
            double max2Error = 0;

            for (int i=0; i<elements1.length/4; i++) {
                mean1 += elements1[i*4 + 0];
                mean1Error += elements1[i*4 + 1];
                max1 += elements1[i*4 + 2];
                max1Error += elements1[i*4 + 3];
            }

            for (int i=0; i<elements2.length/4; i++) {
                mean2 += elements2[i*4 + 0];
                mean2Error += elements2[i*4 + 1];
                max2 += elements2[i*4 + 2];
                max2Error += elements2[i*4 + 3];
            }
            double sum1 = elements1.length/4;
            double sum2 = elements2.length/4;

            double[] data = {mean1/sum1, mean2/sum2, max1/sum1, max2/sum2};
            double[] error = {mean1Error/sum1, mean2Error/sum2, max1Error/sum1, max2Error/sum2};

            IO.saveFile(mConverter.makeBar(x, data, error, mWidthWebView), file);
            mCompareWB.loadUrl("file:///storage/emulated/0/Statpuck/web3.html");

            Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
            intent.setData(Uri.fromFile(file));
            getActivity().sendBroadcast(intent);

        } else {
            mCompareWB.setVisibility(View.GONE);
            Toast.makeText(getContext(), "No user file to compare", Toast.LENGTH_LONG).show();
        }
    }

    private void loadData() {
        File rootsd = Environment.getExternalStorageDirectory();
        final File parent = new File(rootsd.getAbsolutePath(), FOLDER_SAVE);
        File analysis = new File(parent.getAbsolutePath(), FOLDER_SAVE_ANALYSIS);
        if (!analysis.exists()) {
            analysis.mkdirs();
        }
        final File userFile = new File(analysis, mUser.getName() + ".csv");
        if (userFile.exists()) {
            String data = IO.loadFile(userFile);

            // Web Mean
            WebSettings wSettings1;
            wSettings1 = mMeanAccelWV.getSettings();
            wSettings1.setJavaScriptEnabled(true);
            File file = new File(parent, "web1.html");

            double[] elements = Shot.extractMeanMax(data);
            double[] x = new double[elements.length/4];
            double[] y = new double[elements.length/4];
            double[] error = new double[elements.length/4];

            double sum = 0;
            double mean;
            double sd;
            double se;
            for (int i=0; i<elements.length/4; i++) {
                x[i] = i;
                y[i] = elements[i*4 + 0];
                error[i] = elements[i*4 + 1];
            }

            IO.saveFile(mConverter.makeLine(x, y, error, mWidthWebView), file);
            mMeanAccelWV.loadUrl("file:///storage/emulated/0/Statpuck/web1.html");

            Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
            intent.setData(Uri.fromFile(file));
            getActivity().sendBroadcast(intent);

            // Web Max
            WebSettings wSettings2;
            wSettings2 = mMaxAccelWV.getSettings();
            wSettings2.setJavaScriptEnabled(true);
            file = new File(parent, "web2.html");

            elements = Shot.extractMeanMax(data);
            x = new double[elements.length/4];
            y = new double[elements.length/4];
            error = new double[elements.length/4];

            sum = 0;
            for (int i=0; i<elements.length/4; i++) {
                x[i] = i;
                y[i] = elements[i*4 + 2];
                error[i] = elements[i*4 + 3];
            }

            IO.saveFile(mConverter.makeLine(x, y, error, mWidthWebView), file);
            mMaxAccelWV.loadUrl("file:///storage/emulated/0/Statpuck/web2.html");

            intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
            intent.setData(Uri.fromFile(file));
            getActivity().sendBroadcast(intent);
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
