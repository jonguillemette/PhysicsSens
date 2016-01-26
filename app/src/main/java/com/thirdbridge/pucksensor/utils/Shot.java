package com.thirdbridge.pucksensor.utils;

import android.util.Log;
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
    private static final int MAX_DATA = 1000;

    private Point3D[] mAccDatas;
    private double[] mRotDatas;
    private double[] mAccXYZ;
    private double[] mSpeedXYZ;
    private String mTime;
    private double mMaxAccel = 0;
    private double mMaxSpeed = 0;
    private double mMaxRotation = 0;
    private User mUser;


    public Shot(Point3D[] acceleration, double[] rotation, User user) {
        mUser = user;
        if (acceleration.length == rotation.length) {
            int min = Math.min(rotation.length, MAX_DATA);
            mAccDatas = new Point3D[min];
            mRotDatas = new double[min];
            mAccXYZ = new double[min];
            mSpeedXYZ = new double[min];

            for (int i=0; i<min; i++) {
                mAccDatas[i] = acceleration[i];
                mRotDatas[i] = rotation[i];
            }
        } else {
            new Exception("Acceleration must be the same dimension as rotation");
        }

        DateFormat df = new SimpleDateFormat("dd_MMM_yyyy_HH.mm.ssa");
        mTime = df.format(Calendar.getInstance().getTime());
    }

    public Point3D[] getAccelerations() {
        return mAccDatas;
    }

    public double[] getRotations() {
        return mRotDatas;
    }

    public void setAccelerationXYZ(double data, int id) {
        if (id < mAccXYZ.length)
            mAccXYZ[id] = data;
    }

    public void setSpeedXYZ(double data, int id) {
        if (id < mSpeedXYZ.length)
            mSpeedXYZ[id] = data;
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

        for (int i=0; i<mAccDatas.length; i++) {
            text += mAccXYZ[i] + "," + mSpeedXYZ[i] + "," + mRotDatas[i] + "\n";
        }

        String fileName = mUser.getId() + "_" + mTime + ".csv";
        return new Pair<>(fileName, text);
    }

    public static int getMaxData() {
        return MAX_DATA;
    }
}
