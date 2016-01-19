package com.thirdbridge.pucksensor.models;

import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.google.gson.Gson;
import com.thirdbridge.pucksensor.utils.MathHelper;

import java.util.List;

/**
 * Created by Christophe on 2015-10-14.
 */
public class ShotTest {

    private String mId;
    private String mUsername;
    private String mDate;
    private String mDescription;

    private String mAccelData;
    private String mSpeedData;
    private String mRotationData;

    public ShotTest(String id, String username, String date, String description, String accelData, String speedData, String rotationData){
        mId = id;
        mUsername = username;
        mDate = date;
        mDescription = description;

        this.mAccelData = accelData;
		this.mSpeedData = speedData;
		this.mRotationData = rotationData;


    }

    public String getId(){ return mId; }

    public String getUsername() { return mUsername; }

    public String getDate() { return mDate; }

    public String getDescription(){ return mDescription; }

    public String getAccelData(){ return mAccelData; }

	public String getSpeedData(){ return mSpeedData; }

    public String getRotationData(){ return mRotationData; }

    public String getTopSpeed(){

        float topSpeed= 0f;

        LineData shotTestSpeedData = new Gson().fromJson(mSpeedData, LineData.class);

        for (int i = 0; i < shotTestSpeedData.getDataSetCount(); i++) {

            if(shotTestSpeedData.getDataSetByIndex(i).getLabel().equals("XYZ")) {

                List<Entry> yEntries = shotTestSpeedData.getDataSetByIndex(i).getYVals();

                for(Entry entry: yEntries){
                    if(entry.getVal() > topSpeed){
                        topSpeed = entry.getVal();
                    }
                }
            }
        }

        topSpeed = MathHelper.round(topSpeed,2);

        return String.valueOf(topSpeed);
    }


    public String getTopRotation(){

        float topRotation= 0f;

        LineData shotTestRotationData = new Gson().fromJson(mRotationData, LineData.class);

        List<Entry> yEntries = shotTestRotationData.getDataSetByIndex(0).getYVals();

        for(Entry entry: yEntries){
            if(entry.getVal() > topRotation){
                topRotation = entry.getVal();
            }
        }

        topRotation = MathHelper.round(topRotation,2);

        return String.valueOf(topRotation);
    }


}
