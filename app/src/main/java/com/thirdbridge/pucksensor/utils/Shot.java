package com.thirdbridge.pucksensor.utils;

import android.util.Pair;

import com.thirdbridge.pucksensor.models.User;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 * Created by Jayson Dalpé on 2016-01-26.
 */
public class Shot {
    private static final int MAX_DATA = 1360;
    private static final int MAX_DRAFT_DATA = 108;
    private static final int DELTA = 2; // Data display before and after the peak.

    private Point3D[] mAccDatas;
    private double[] mRotDatas;
    private double[] mAccTotal;
    private double[] mSpeedTotal;
    private String mTime;
    private double mMaxAccel = 0;
    private double mMaxSpeed = 0;
    private double mMaxRotation = 0;
    private User mUser;
    private boolean mCooked;
    private boolean mDraft = false;

    private int mMax = 0;
    private int mMin = 0;

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

    public Pair<String,String> packageFormCSV() {
        // Create a form that is easily parceable
        String text = "Player, " + mUser.getFirstName() + "," + mUser.getLastName() + "," + mUser.getId() + "\n";
        text += "Max,\n";
        text += "Acceleration, " + mMaxAccel + " g\n";
        text += "Speed, " + mMaxSpeed + " m/s\n";
        text += "Rotation, " + mMaxRotation + " degrees/s\n";
        text += "Data,\n";
        text += "Acceleration,Speed,Rotation\n";

        for (int i=0; i< mAccTotal.length; i++) {
            text += mAccTotal[i] + "," + mSpeedTotal[i] + "," + mRotDatas[i] + "\n";
        }

        String fileName = mUser.getId() + "_" + mTime + ".csv";
        return new Pair<>(fileName, text);
    }

    /**
     * Update the maximum value and the minimal one to get only the first peak.
     * IMPORTANT: Call this function one all the entry are there.
     */
    public void analyze(int threshold) {
        boolean peak = false;
        boolean direction;
        double previousValue = 0;
        int max = mMax;
        int min = mMin;
        double noise = 0.2;
        boolean noreturn = false;
        boolean minStart = false;


        for (int i=0; i<mAccTotal.length;i++) {
            if (mAccTotal[i] >= (double) noise && !minStart) {
                minStart = true;
                min= Math.max(i-DELTA, 0);
            }
            // Find threshold peak
            if (mAccTotal[i] >= (double) threshold && !peak) {
                peak = true;
            }

            if (previousValue<=mAccTotal[i]) {
                // GALLIFREY RISES!
                direction = true;
            } else {
                // SKARO RISES
                direction = false;
            }

            if (peak && !direction && !noreturn) {
                noreturn = true;
            }

            if (noreturn && mAccTotal[i]<=(double)threshold/4) {
                max = Math.min(i+DELTA,mAccTotal.length);
                break;
            }

            previousValue = mAccTotal[i];
        }
        mMax = max;
        mMin = min;
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


}
