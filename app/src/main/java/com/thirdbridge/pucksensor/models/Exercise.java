package com.thirdbridge.pucksensor.models;

import android.util.Pair;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * Created by Jayson Dalpé on 2016-04-20.
 *
 * Exercise Class. A package to handle KeyPoints of an exercise and his headers.
 */
public class Exercise {
    private static final String JSON_KEYPOINTS = "key_points";
    private static final String JSON_KEYNOTES = "key_notes";

    private String mTitle;
    private User mUser = null;
    private String mTime;
    private String mDescription;
    private String mId;
    private String mVideo;
    private String[] mKeyNotes;
    private List<KeyPoint> mKeypoints;
    public double totalTime;
    public double accelerationMax;
    public double accelerationMean;
    public double totalTouchTime;
    public double totalFlightTime;

    private double mLeftOver;

    public Exercise(String id, String title, String description, String video) {
        mId = id;
        mTitle = title;
        mDescription = description;
        mVideo = video;
        mKeypoints = new ArrayList<KeyPoint>();

        DateFormat df = new SimpleDateFormat("dd_MMM_yyyy_HH.mm.ssa");
        mTime = df.format(Calendar.getInstance().getTime());
    }

    public Exercise(String id, String video, User user) {
        mId = id;
        mUser = user;
        mTitle = "No exercice";
        mDescription = "";
        mVideo = video;
        mKeypoints = new ArrayList<KeyPoint>();

        DateFormat df = new SimpleDateFormat("dd_MMM_yyyy_HH.mm.ssa");
        mTime = df.format(Calendar.getInstance().getTime());
    }

    // Live action
    public boolean addKeypoints(KeyPoint point) {
        /*mKeypoints.add(point);
        return false;
        */
        if (mKeypoints.size() >= 1) {
            KeyPoint.Interact interact = mKeypoints.get(mKeypoints.size() - 1).interact(point, mLeftOver);
            switch (interact) {
                case NEW:
                    point.deltaFlyingTime += mLeftOver;
                    mKeypoints.add(point);
                    mLeftOver = 0;
                    return false;
                case TAKE_FIRST:
                    mLeftOver += point.deltaFlyingTime + point.deltaTime;
                    return true;
                case TAKE_SECOND:
                    return true;
            }
        } else {
            mKeypoints.add(point);
        }
        return false;
    }

    public void compute() {
        // Get all the general data with those keypoints.

        totalTime = 0;
        accelerationMax = 0;
        accelerationMean = 0;
        totalTouchTime = 0;
        totalFlightTime = 0;

        for (int i=0; i<mKeypoints.size(); i++) {
            // Step 1, compute them!
            KeyPoint kp =  mKeypoints.get(i);
            kp.compute();
            totalTouchTime += kp.deltaTime;
            totalFlightTime += kp.deltaFlyingTime;
            totalTime += kp.deltaTime + kp.deltaFlyingTime;
            accelerationMax = Math.max(accelerationMax, kp.accelerationMax);
            accelerationMean += kp.accelerationMean;
        }
        accelerationMean /= mKeypoints.size();
    }

    //Load form data
    public void loadInformation(String jsonData) {
        JSONObject  jsonRootObject = null;
        try {
            jsonRootObject = new JSONObject(jsonData);
            JSONArray jsonArray = jsonRootObject.optJSONArray(JSON_KEYPOINTS);
            mKeypoints = new ArrayList<KeyPoint>();
            for (int i=0; i<jsonArray.length(); i++) {
                mKeypoints.add(new KeyPoint(jsonArray.getJSONObject(i)));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        try {
            jsonRootObject = new JSONObject(jsonData);
            JSONArray jsonArray = jsonRootObject.optJSONArray(JSON_KEYNOTES);
            mKeyNotes = new String[jsonArray.length()];
            for (int i=0; i<jsonArray.length(); i++) {
                mKeyNotes[i] = jsonArray.getString(i);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String[] getKeyNotes() {
        return mKeyNotes;
    }

    public String getTitle() {
        return mTitle;
    }

    public String getDescription() {
        return mDescription;
    }

    public String getVideo() {
        return mVideo;
    }

    public List<KeyPoint> getKeyPoints() {
        return mKeypoints;
    }

    public KeyPoint getKeyPoint(int index) {
        if (index == -1) {
            return mKeypoints.get(mKeypoints.size()-1);
        }
        return mKeypoints.get(index);
    }

    public Pair<String,String> packageFormCSV() {
        String retValue = "";
        if (mUser != null) {
            retValue = "Player," + mUser.getName() + ", " + mUser.getId() + "\n";
        }
        retValue += "Id," + mId + "\n";
        retValue += "Title," + mTitle + "\n";
        retValue += "Description," + mDescription + "\n";
        retValue += "\n";
        retValue += "Stats: \n";
        retValue += "Total time," + totalTime + "\n";
        retValue += "Total flying time," + totalFlightTime + "\n";
        retValue += "Total touch time," + totalTouchTime + "\n";
        retValue += "Acceleration mean," + accelerationMean + "\n";
        retValue += "Acceleration max," + accelerationMax + "\n";
        retValue += "KeyPoints:\n";
        if (mKeypoints.size() >= 1) {
            retValue += mKeypoints.get(0).packageTitleFormCSV() + "\n";
            for (int i = 0; i < mKeypoints.size(); i++) {
                retValue += mKeypoints.get(i).packageFormCSV() + "\n";
            }
        }

        String fileName = mUser.getName().replace(" ", ".") + "_" + mTime + ".csv";
        return new Pair<String,String>(fileName, retValue);
    }
}
