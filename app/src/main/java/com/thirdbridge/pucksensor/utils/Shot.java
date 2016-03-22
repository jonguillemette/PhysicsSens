package com.thirdbridge.pucksensor.utils;

import android.util.Pair;

import com.thirdbridge.pucksensor.models.User;
import com.thirdbridge.pucksensor.utils.ble_utils.Point3D;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 * Created by Jayson Dalpé on 2016-01-26.
 */
public class Shot {
    private static final int MAX_DATA = 1360;
    private static final int MAX_DRAFT_DATA = 108;

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
    }

    public boolean isCooked() {
        return mCooked;
    }

    public boolean isDraft() {
        return mDraft;
    }

    public Point3D[] getAccelerationsXYZ() {
        return mAccDatas;
    }

    public double[] getAccelerations() {
        return mAccTotal;
    }

    public double[] getRotations() {
        return mRotDatas;
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

    public static int getMaxData() {
        return MAX_DATA;
    }

    public static int getMaxDraftData() {
        return MAX_DRAFT_DATA;
    }


}
