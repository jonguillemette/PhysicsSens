package com.thirdbridge.pucksensor.utils;

import android.util.Log;
import android.util.Pair;

import com.thirdbridge.pucksensor.models.User;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * Created by Jayson Dalpé on 2016-01-26.
 */
public class Shot {
    private static final int MAX_DATA = 1278;
    private static final int MAX_DRAFT_DATA = 108;
    private static final int DELTA = 2; // Data display before and after the peak.

    private Point3D[] mAccDatas;
    private double[] mRotDatas;
    private double[] mAccTotal;
    private double[] mSpeedTotal;
    private String mTime;
    private double mMaxAccel = 0;
    private double mMeanAccel = -1;
    private double mMaxSpeed = 0;
    private double mMaxRotation = 0;
    private User mUser;
    private boolean mCooked;
    private boolean mDraft = false;

    private int mMax = 0;
    private int mMin = 0;
    private int mReleaseTime = 0; //In number of steps

    public Shot(Point3D[] acceleration, double[] rotation, User user) {
        mUser = user;
        if (acceleration.length == rotation.length) {
            int min = Math.min(rotation.length, MAX_DATA);
            mAccDatas = new Point3D[min];
            mRotDatas = new double[min];
            mAccTotal = new double[min];
            mSpeedTotal = new double[min];

            for (int i=0; i<min; i++) {
                mAccDatas[i] = acceleration[i];
                mRotDatas[i] = rotation[i];
            }
        } else {
            new Exception("Acceleration must be the same dimension as rotation");
        }

        DateFormat df = new SimpleDateFormat("dd_MMM_yyyy_HH.mm.ssa");
        mTime = df.format(Calendar.getInstance().getTime());
        mCooked = false;
        mMax = rotation.length;
    }

    public Shot(File filename, String date) {
        depackageFormCSV(filename);
        mTime = date;
    }


    public Shot(int length, User user, boolean isDraft) {
        mUser = user;
        int min = Math.min(length, MAX_DATA);
        mRotDatas = new double[min];
        mAccTotal = new double[min];
        mSpeedTotal = new double[min];

        DateFormat df = new SimpleDateFormat("dd_MMM_yyyy_HH.mm.ssa");
        mTime = df.format(Calendar.getInstance().getTime());
        mCooked = true;

        mDraft = isDraft;
        mMax = length;
    }

    public boolean isCooked() {
        return mCooked;
    }

    public boolean isDraft() {
        return mDraft;
    }

    public Point3D[] getAccelerationsXYZ() {
        Point3D[] retValue = new Point3D[mMax-mMin];
        for(int i=0; i<mMax-mMin; i++) {
            retValue[i] = mAccDatas[mMin+i];
        }
        return retValue;
    }

    public double[] getAccelerations() {
        double[] retValue = new double[mMax-mMin];
        for(int i=0; i<mMax-mMin; i++) {
            retValue[i] = mAccTotal[mMin+i];
        }
        return retValue;
    }

    public double[] getRotations() {
        double[] retValue = new double[mMax-mMin];
        for(int i=0; i<mMax-mMin; i++) {
            retValue[i] = mRotDatas[mMin+i];
        }
        return retValue;
    }

    public void setRotation(double data, int id) {
        if (id < mRotDatas.length)
            mRotDatas[id] = data;
    }

    public void setAccelerationXYZ(double data, int id) {
        if (id < mAccTotal.length)
            mAccTotal[id] = data;
    }

    public void setSpeedXYZ(double data, int id) {
        if (id < mSpeedTotal.length)
            mSpeedTotal[id] = data;
    }

    public void setMax(double acceleration, double speed, double angular) {
        mMaxAccel = acceleration;
        mMaxSpeed = speed;
        mMaxRotation = angular;
    }

    public double[] getMax() {
        double[] values = {mMaxAccel, mMaxSpeed, mMaxRotation};
        return values;
    }

    public User getUser() {
        return mUser;
    }

    public Pair<String,String> packageFormCSV() {
        // Create a form that is easily parceable
        String text = "Player, " + mUser.getName() + "," + mUser.getId() + "\n";
        text += "Max,\n";
        text += "Acceleration, " + mMaxAccel + " g\n";
        text += "Speed, " + mMaxSpeed + " km/h\n";
        text += "Rotation, " + mMaxRotation + " degrees/s\n";
        text += "Data,\n";
        text += "Acceleration,Speed,Rotation\n";

        for (int i=0; i< mAccTotal.length; i++) {
            text += mAccTotal[i] + "," + mSpeedTotal[i] + "," + mRotDatas[i] + "\n";
        }

        String fileName = mUser.getName().replace(" ", "_") + "_" + mTime + ".csv";
        return new Pair<String,String>(fileName, text);
    }

    public void depackageFormCSV(File filename) {
        String data = IO.loadFile(filename);

        String[] datas = data.split("\n");

        String[] user = datas[0].replace("Player, ", "").split(",");
        mUser = new User(user[1], user[0]);

        mMaxAccel = Double.parseDouble(datas[2].replace("Acceleration, ", "").replace(" g", ""));
        mMaxSpeed = Double.parseDouble(datas[3].replace("Speed, ", "").replace(" km/h", "").replace(" m/s", ""));
        mMaxRotation = Double.parseDouble(datas[4].replace("Rotation, ", "").replace(" degrees/s", ""));

        int length = datas.length - 7;

        mAccTotal = new double[length];
        mSpeedTotal = new double[length];
        mRotDatas = new double[length];

        int index = 0;
        for (int i = 7; i<datas.length; i++) {
            mAccTotal[index] = Double.parseDouble(datas[i].split(",")[0]);
            mSpeedTotal[index] = Double.parseDouble(datas[i].split(",")[1]);
            mRotDatas[index] = Double.parseDouble(datas[i].split(",")[2]);
            index++;
        }

        mCooked = true;

        mDraft = false;
    }

    /**
     * Update the maximum value and the minimal one to get only the first peak.
     * IMPORTANT: Call this function once all the entry are there.
     */
    public boolean analyze(int threshold, int thresholdRelease, int pointBoard) {
        mMin = 0;
        mMax = mAccTotal.length;

        mReleaseTime = 0;


        //Find min, max valuable.
        // Get release ticks

        // Phase 1, get all max:
        List<Integer> maxes = new ArrayList<Integer>();
        for (int i=2; i<mAccTotal.length -2;i++) {
            if (mAccTotal[i] - mAccTotal[i-2] > 0  && mAccTotal[i] - mAccTotal[i+2] > 0 && mAccTotal[i] >= threshold) {
                maxes.add(i);

            }
        }

        // Phase 2, keep max with good data
        List<Integer> cleanMaxes = new ArrayList<Integer>();

        for (int i=0; i<maxes.size(); i++) {
            int center = maxes.get(i);
            double[] vals = {mAccTotal[center-2], mAccTotal[center-1], mAccTotal[center], mAccTotal[center+1], mAccTotal[center+2]};
            if (vals[0] >= threshold && vals[1] >= threshold && vals[2] >= threshold && vals[3] >= threshold && vals[4] >= threshold) {
                //Woohoo
                Log.i("ALLO", "Value: " + center);
                cleanMaxes.add(center);
            }
        }


        if (cleanMaxes.size() == 0) {
            mMin = 0;
            mMax = mAccTotal.length;
            return false;
        }

        int finalId;
        if (cleanMaxes.size() == 1) {
            finalId = cleanMaxes.get(0);
        } else {
            // Get the two biggest peak
            int[] positions = {0, 0};
            double[] biggest = {0, 0};
            for (int i=0; i<cleanMaxes.size(); i++) {
                double value = mAccTotal[cleanMaxes.get(i)];

                if (positions[0]-value < 50) {
                    if (value > biggest[0]) {
                        biggest[0] = value;
                        positions[0] = cleanMaxes.get(i);
                    }
                }

                if (positions[1]-value < 50) {
                    if (value > biggest[1]) {
                        biggest[1] = value;
                        positions[1] = cleanMaxes.get(i);
                    }
                }

                if (value > biggest[0]) {
                    biggest[0] = value;
                    positions[0] = cleanMaxes.get(i);
                } else if (value > biggest[1]) {
                    biggest[1] = value;
                    positions[1] = cleanMaxes.get(i);
                }
            }
            Log.i("ALLO", "Value 2: " + positions[0] + ", " + positions[1]);

            // Determine which is good
            if (Math.abs(positions[0] - positions[1]) > getMaxData()/2) {
                if (positions[0] < positions[1]) {
                    finalId = positions[0];
                } else {
                    finalId = positions[1];
                }
            } else {
                if (biggest[0] > biggest[1]) {
                    finalId = positions[0];
                } else {
                    finalId = positions[1];
                }
            }
        }
        // The peak is set. get the minimum:
        mMin = 0;
        for (int i=finalId-1; i>=0; i-=1) {
            if ( (mAccTotal[i]-mAccTotal[i+1] > 0 && !isBetween(mAccTotal[i], mAccTotal[i+1], 10)) || mAccTotal[i] < mAccTotal[finalId]/4) {
                mMin = i;
                break;
            }
        }
        mMin = Math.max(0, mMin-pointBoard);

        // And the maximum
        mMax = 0;
        for (int i=finalId+1; i<mAccTotal.length; i+= 1) {
            if ((mAccTotal[i]-mAccTotal[i-1] > 0 && !isBetween(mAccTotal[i], mAccTotal[i+1], 10)) || mAccTotal[i] < mAccTotal[finalId]/4) {
                mMax = i;
                break;
            }
        }
        mMax = Math.min(mAccTotal.length, mMax+pointBoard);

        // Find the release time
        for (int i=mMin; i<mMax;i++) {
            if (mAccTotal[i] >= thresholdRelease) {
                mReleaseTime ++;
            }
        }

        if (mMax - mMin < 10) {
            mMin = 0;
            mMax = mAccTotal.length;
            return false;
        }

        double sum = 0;
        for (int i=mMin; i<mMax; i++) {
            sum += mAccTotal[i];
        }
        mMeanAccel = sum / (mMax-mMin);
        return true;
    }

    public double getMeanAccel() {
        return mMeanAccel;
    }

    public String getTime(){
        return mTime;
    }

    public double getMaxAccel() {
        return mMaxAccel;
    }

    public double getReleaseTime() {
        return mReleaseTime;
    }

    public int getLength() {
        return mMax-mMin;
    }

    public static int getMaxData() {
        return MAX_DATA;
    }

    public static int getMaxDraftData() {
        return MAX_DRAFT_DATA;
    }

    private static boolean isBetween(double val1, double val2, double percent) {
        return Math.min(val1, val2) / Math.max(val1, val2) >= ((100-percent))/100;
    }

    public static double[] extractMeanMax(String userData) {
        String[] elements = userData.split("\n");

        double[] retValue = new double[(elements.length-3)*4];

        for (int i=0; i<elements.length-3; i++) {
            Log.i("YOLLO", elements[i+3]);
            String[] values = elements[i+3].split(",");
            retValue[i*4 + 0] = Double.parseDouble(values[1]);
            retValue[i*4 + 1] = Double.parseDouble(values[2]);
            retValue[i*4 + 2] = Double.parseDouble(values[3]);
            retValue[i*4 + 3] = Double.parseDouble(values[4]);
        }

        return retValue;
    }

}
