package com.thirdbridge.pucksensor.controllers;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.thirdbridge.pucksensor.R;
import com.thirdbridge.pucksensor.adapter.StatArrayAdapter;
import com.thirdbridge.pucksensor.adapter.UserArrayAdapter;
import com.thirdbridge.pucksensor.models.Statistic;
import com.thirdbridge.pucksensor.models.User;
import com.thirdbridge.pucksensor.utils.BaseFragment;
import com.thirdbridge.pucksensor.utils.Constants;
import com.thirdbridge.pucksensor.utils.IO;
import com.thirdbridge.pucksensor.web.PlotLyConverter;
import com.thirdbridge.pucksensor.hardware.Shot;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

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
    private TextView mCompareStatTV;
    private TextView mStandardErrorTV;
    private WebView mStandardErrorWV;
    private TextView mReleaseTimeTV;
    private WebView mReleaseTimeWB;
	private Spinner mCompareSpinner;
    private Spinner mCompareStatSpinner;
	private WebView mCompareWB;
	private UserArrayAdapter mSpinnerAdapter;
    private StatArrayAdapter mSpinnerStatAdapter;
    private int mWidthWebView;

    private int mCompare;
    private int mCompareStat = 0;

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
        mCompareStatTV = (TextView) v.findViewById(R.id.compare_stat_label);
		mCompareSpinner = (Spinner) v.findViewById(R.id.compare_spinner);
        mCompareStatSpinner = (Spinner) v.findViewById(R.id.compare_stat_spinner);
		mCompareWB = (WebView) v.findViewById(R.id.compare_webview);
        mStandardErrorTV = (TextView) v.findViewById(R.id.standard_error_label);
        mStandardErrorWV = (WebView) v.findViewById(R.id.standard_error_webview);
        mReleaseTimeTV = (TextView) v.findViewById(R.id.release_time_label);
        mReleaseTimeWB = (WebView) v.findViewById(R.id.release_time_webview);

		mLoadingScreenRelativeLayout = (RelativeLayout) v.findViewById(R.id.loading_screen_relative_layout);

        mTimeEvolutionTV.setVisibility(View.GONE);
        mMeanAccelTV.setVisibility(View.GONE);
        mMeanAccelWV.setVisibility(View.GONE);
        mMaxAccelTV.setVisibility(View.GONE);
        mMaxAccelWV.setVisibility(View.GONE);
        mStandardErrorTV.setVisibility(View.GONE);
        mStandardErrorWV.setVisibility(View.GONE);
        mReleaseTimeTV.setVisibility(View.GONE);
        mReleaseTimeWB.setVisibility(View.GONE);

        mSettings = getActivity().getSharedPreferences("StatPuck", 0);

        mConverter = new PlotLyConverter(getContext());

		Log.i(TAG, "width " + getActivity().findViewById(android.R.id.content).getWidth());
		Log.i(TAG, "height " + getActivity().findViewById(android.R.id.content).getHeight());

		float ratio = getActivity().findViewById(android.R.id.content).getWidth();
        mWidthWebView = (int)(ratio * 0.45);
		LinearLayout.LayoutParams webViewLayout = new LinearLayout.LayoutParams(mWidthWebView*2, mWidthWebView);
		webViewLayout.setMargins(20, 20, 20, 20);
        webViewLayout.gravity = Gravity.CENTER;

		mMeanAccelWV.setLayoutParams(webViewLayout);
		mMaxAccelWV.setLayoutParams(webViewLayout);
		mCompareWB.setLayoutParams(webViewLayout);

        populateSpinners();
        mCompareWB.setVisibility(View.GONE);

        mCompareSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                mCompare = position;
                updateCompare(mCompare, mCompareStat);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        mCompareStatSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                mCompareStat = position;
                updateCompare(mCompare, mCompareStat);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });


        if (mCompareSpinner.getSelectedItemPosition() >= 0) {
            updateCompare(mCompareSpinner.getSelectedItemPosition(), mCompareStat);
        }

        //TODO Enable this after demo
        //loadData();

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

    private void updateCompare(int position, int position2) {

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
            File file = new File(parent, "web03.html");

            double[] elements1 = Shot.extractMeanMax(data1);
            double[] elements2 = Shot.extractMeanMax(data2);
            String[] x = {mUser.getName(), mUsers.get(position).getName()};

            double[] max1 = new double[elements1.length/3];
            double[] max2 = new double[elements2.length/3];

            double[] mean1 = new double[elements1.length/3];
            double[] mean2 = new double[elements2.length/3];

            double[] release1 = new double[elements1.length/3];
            double[] release2 = new double[elements2.length/3];

            for (int i=0;i<elements1.length/3; i++) {
                max1[i] = elements1[i*3 + 1];
                mean1[i] = elements1[i*3 + 0];
                release1[i] = elements1[i*3 + 2];
            }

            for (int i=0;i<elements2.length/3; i++) {
                max2[i] = elements2[i*3 + 1];
                mean2[i] = elements2[i*3 + 0];
                release2[i] = elements2[i*3 + 2];
            }

            double[] data = new double[2];
            double[] error = new double[2];

            double[] dataY1;
            double[] dataY2;

            switch (Statistic.getAll()[position2]) {
                case MAX_ACCEL:
                    dataY1 = max1;
                    dataY2 = max2;
                    break;
                case MEAN_ACCEL:
                    dataY1 = mean1;
                    dataY2 = mean2;
                    break;
                case RELEASE_TIME:
                default:
                    dataY1 = release1;
                    dataY2 = release2;
                    break;
            }

            double mean = 0;
            double ecart = 0;

            for (int i=0; i<dataY1.length; i++) {
                mean += dataY1[i];
            }
            mean /= dataY1.length;

            for (int i=0;i<dataY1.length; i++) {
                ecart += Math.pow(dataY1[i] - mean,2);
            }
            ecart /= dataY1.length;
            ecart = Math.sqrt(ecart);

            data[0] = mean;
            error[0] = ecart / Math.sqrt(dataY1.length);

            mean = 0;
            ecart = 0;

            for (int i=0; i<dataY2.length; i++) {
                mean += dataY2[i];
            }
            mean /= dataY2.length;

            for (int i=0;i<dataY2.length; i++) {
                ecart += Math.pow(dataY2[i] - mean,2);
            }
            ecart /= dataY2.length;
            ecart = Math.sqrt(ecart);

            data[1] = mean;
            error[1] = ecart / Math.sqrt(dataY2.length);

            IO.saveFile(mConverter.makeTwoBar(x, data, error, (int)((float)mWidthWebView * 0.9f)), file);
            mCompareWB.loadUrl("file:///storage/emulated/0/Statpuck/web03.html");

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
            double[] x = new double[elements.length/3];
            double[] y = new double[elements.length/3];
            double[] releasetime = new double[elements.length/3];

            for (int i=0; i<elements.length/3; i++) {
                x[i] = i;
                y[i] = elements[i*3 + 0];
                releasetime[i] = elements[i*3 + 2];
            }

            IO.saveFile(mConverter.makeLine(x, y, mWidthWebView), file);
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
            x = new double[elements.length/3];
            y = new double[elements.length/3];

            for (int i=0; i<elements.length/3; i++) {
                x[i] = i;
                y[i] = elements[i*3 + 1];
            }

            IO.saveFile(mConverter.makeLine(x, y, mWidthWebView), file);
            mMaxAccelWV.loadUrl("file:///storage/emulated/0/Statpuck/web2.html");

            intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
            intent.setData(Uri.fromFile(file));
            getActivity().sendBroadcast(intent);
        } else {
            Toast.makeText(getContext(), "No shot save for " + mUser.getName(), Toast.LENGTH_LONG).show();
            getController().gotoHome();
        }
    }

	private void populateSpinners(){
		mUsers = new ArrayList<User>();
        Statistic.Stat[] stats = Statistic.getAll();
        List<Statistic.Stat> statsList = new ArrayList<Statistic.Stat>();
        for (int i=0; i<stats.length; i++) {
            statsList.add(stats[i]);
        }

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

        mSpinnerStatAdapter = new StatArrayAdapter(getController(), R.layout.spinner_dropdown_item, statsList);
        mCompareStatSpinner.setAdapter(mSpinnerStatAdapter);
	}

}
