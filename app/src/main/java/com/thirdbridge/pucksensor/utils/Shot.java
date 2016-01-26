package com.thirdbridge.pucksensor.utils;

import com.thirdbridge.pucksensor.utils.ble_utils.Point3D;

/**
 * Created by Jayson Dalpé on 2016-01-26.
 */
public class Shot {
    private static final int MAX_DATA = 1000;

    private Point3D[] mAccDatas;
    private double[] mRotDatas;

    public Shot(Point3D[] acceleration, double[] rotation) {
        if (acceleration.length == rotation.length) {
            int min = Math.min(rotation.length, MAX_DATA);
            mAccDatas = new Point3D[min];
            mRotDatas = new double[min];

            for (int i=0; i<min; i++) {
                mAccDatas[i] = acceleration[i];
                mRotDatas[i] = rotation[i];
            }
        } else {
            new Exception("Acceleration must be the same dimension as rotation");
        }
    }

    public Point3D[] getAccelerations() {
        return mAccDatas;
    }

    public double[] getRotations() {
        return mRotDatas;
    }

    public static int getMax() {
        return MAX_DATA;
    }
}
