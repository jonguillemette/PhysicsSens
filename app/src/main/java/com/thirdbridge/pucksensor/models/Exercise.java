package com.thirdbridge.pucksensor.models;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
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
    private String mDescription;
    private String mId;
    private String mVideo;
    private String[] mKeyNotes;
    private List<KeyPoint> mKeypoints;
    private double mTotalTime;
    private double mAccelerationMax;
    private double mAccelerationMean;
    private double mTotalTouchTime;
    private double mTotalFlightTime;

    public Exercise(String id, String title, String description, String video) {
        mId = id;
        mTitle = title;
        mDescription = description;
        mVideo = video;
        mKeypoints = new ArrayList<>();
    }

    // Live action
    public void addKeypoints(KeyPoint point) {
        mKeypoints.add(point);
    }

    //Load form data
    public void loadInformation(String jsonData) {
        JSONObject  jsonRootObject = null;
        try {
            jsonRootObject = new JSONObject(jsonData);
            JSONArray jsonArray = jsonRootObject.optJSONArray(JSON_KEYPOINTS);
            mKeypoints = new ArrayList<>();
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
        return mKeypoints.get(index);
    }
}
