package com.thirdbridge.pucksensor.models;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by Jayson Dalpé on 2016-04-20.
 *
 * Exercise Class. A package to handle KeyPoints of an exercise and his headers.
 */
public class Exercise {
    private static final String JSON_KEYPOINTS = "key_points";

    private String mTitle;
    private String mDescription;
    private String mId;
    private KeyPoint[] mKeypoints;
    private double mTotalTime;
    private double mAccelerationMax;
    private double mAccelerationMean;
    private double mTotalTouchTime;
    private double mTotalFlightTime;

    public Exercise(String id, String title, String description) {
        mId = id;
        mTitle = title;
        mDescription = description;
    }

    public void loadInformation(String jsonData) {
        JSONObject  jsonRootObject = null;
        try {
            jsonRootObject = new JSONObject(jsonData);
            JSONArray jsonArray = jsonRootObject.optJSONArray(JSON_KEYPOINTS);
            mKeypoints = new KeyPoint[jsonArray.length()];
            for (int i=0; i<jsonArray.length(); i++) {
                mKeypoints[i] = new KeyPoint(jsonArray.getJSONObject(i));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public String getTitle() {
        return mTitle;
    }

    public String getDescription() {
        return mDescription;
    }
}
