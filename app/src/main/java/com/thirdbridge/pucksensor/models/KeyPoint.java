package com.thirdbridge.pucksensor.models;

import android.util.Pair;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Created by Jayson Dalpé on 2016-04-20.
 * Class KeyPoint: A KeyPoint is a moment of interest inside an exercise. Each KeyPoint has calibration data to let you help to mimic the full exercise.
 * For instance: A KeyPoint can be the Target acceleration at the next step.
 */
public class KeyPoint {

    enum Status {
        NOT_INIT,
        INIT,
        COMPUTED,
    };

    enum Data {
        DELTA_TIME,
        ACCELERATION_MEAN,
        ACCELERATION_MAX,
        DIRECTON_RAW,
        IS_RADIAN,
        ROTATION_DELTA,
        ACCEL_Z,
        DIRECTION,
        QUALITY_DIRECTION,
    }

    public static String getUnits(Data data, Data data2) {
        switch (data) {
            case DELTA_TIME:
                return "ms";
            case ACCELERATION_MEAN:
                return "m/s²";
            case ACCELERATION_MAX:
                return "m/s²";
            case DIRECTON_RAW:
                if (data2 == Data.IS_RADIAN) {
                    return "rad";
                } else {
                    return "degree";
                }
            case ROTATION_DELTA:
                if (data2 == Data.IS_RADIAN) {
                    return "rad/s";
                } else {
                    return "degree/s";
                }
            case ACCEL_Z:
                return "m/s²";
            case DIRECTION:
                if (data2 == Data.IS_RADIAN) {
                    return "rad";
                } else {
                    return "degree";
                }
            case QUALITY_DIRECTION:
                return "";
        }
        return "";
    }


    // Primary data
    private double mDeltaTime;
    private double mAccelerationMean;
    private double mAccelerationMax;
    private double mDirectionRaw;
    private boolean mIsRadian = false;
    private double[] mRotationDelta;
    private double mAccelZ;

    // Computed data
    private double mDirection;
    private double mQualityDirection;

    // Which data is used
    private boolean mGotDeltaTime = false;
    private boolean mGotAccelerationMean = false;
    private boolean mGotAccelerationMax = false;
    private boolean mGotDirectionRaw = false;
    private boolean mGotRotationDelta = false;
    private boolean mGotAccelZ = false;
    private boolean mGotDirection = false;
    private boolean mGotQualityDirection = false;

    private Status mStatus;

    public KeyPoint() {
        mStatus = Status.NOT_INIT;
    }

    public KeyPoint(byte[][] rawValues) {
        mStatus = Status.INIT;
        //TODO Create basic data.
    }

    public KeyPoint(JSONObject jsonPoint) {

        try {
            mDeltaTime = jsonPoint.getDouble(Keys.DELTA_TIME);
            mGotDeltaTime = true;
        } catch(Exception e) {}

        try {
            mAccelerationMean = jsonPoint.getDouble(Keys.ACCELERATION_MEAN);
            mGotAccelerationMean = true;
        } catch(Exception e) {}

        try {
            mAccelerationMax = jsonPoint.getDouble(Keys.ACCELERATION_MAX);
            mGotAccelerationMax = true;
        } catch(Exception e) {}

        try {
            mDirectionRaw = jsonPoint.getDouble(Keys.DIRECTION_RAW);
            mGotDirectionRaw = true;
        } catch(Exception e) {}

        try {
            mIsRadian = jsonPoint.getBoolean(Keys.IS_RADIAN);
        } catch(Exception e) {}

        try {
            JSONArray object = jsonPoint.getJSONArray(Keys.ROTATION_DELTA);
            mRotationDelta = new double[object.length()];
            for (int i=0; i<mRotationDelta.length; i++) {
                mRotationDelta[i] = object.getDouble(i);
            }
            mGotRotationDelta = true;
        } catch(Exception e) {}

        try {
            mAccelZ = jsonPoint.getDouble(Keys.ACCEL_Z);
            mGotAccelZ = true;
        } catch(Exception e) {}

        try {
            mDirection = jsonPoint.getDouble(Keys.DIRECTION);
            mGotDirection = true;
        } catch(Exception e) {}

        try {
            mQualityDirection = jsonPoint.getDouble(Keys.QUALITY_DIRECTION);
            mGotQualityDirection = true;
        } catch(Exception e) {}

        mStatus = Status.COMPUTED;
    }

    public KeyPoint(Object[] datas, Data[] types) {
        mStatus = Status.COMPUTED;
        for (int i=0; i<datas.length; i++) {
            switch(types[i]) {
                case DELTA_TIME:
                    mDeltaTime = (double) datas[i];
                    mGotDeltaTime = true;
                    break;
                case ACCELERATION_MEAN:
                    mAccelerationMean = (double) datas[i];
                    mGotAccelerationMean = true;
                    break;
                case ACCELERATION_MAX:
                    mAccelerationMax = (double) datas[i];
                    mGotAccelerationMax = true;
                    break;
                case DIRECTON_RAW:
                    mDirectionRaw = (double) datas[i];
                    mGotDirectionRaw = true;
                    break;
                case IS_RADIAN:
                    mIsRadian = (boolean) datas[i];
                    break;
                case ROTATION_DELTA:
                    mRotationDelta = (double[]) datas[i];
                    mGotRotationDelta = true;
                    break;
                case ACCEL_Z:
                    mAccelZ = (double) datas[i];
                    mGotAccelZ = true;
                    break;
                case DIRECTION:
                    mDirection = (double) datas[i];
                    mGotDirection = true;
                    break;
                case QUALITY_DIRECTION:
                    mQualityDirection = (double) datas[i];
                    mGotQualityDirection = true;
                    break;
            }
        }
    }

    public void compute() {
        //TODO Compute series of classic data to generated complex one.
    }

    public Status getStatus() {
        return mStatus;
    }

    public double getAccelerationMean() {
        return mAccelerationMean;
    }

    public boolean isAccelerationMax() {
        return mGotAccelerationMax;
    }

    public boolean isAccelerationMean() {
        return mGotAccelerationMean;
    }

    public double getAccelerationMax() {
        return mAccelerationMax;
    }

    public boolean isAccelZ() {
        return mGotAccelZ;
    }

    public boolean isDeltaTime() {
        return mGotDeltaTime;
    }

    public boolean isDirection() {
        return mGotDirection;
    }

    public boolean isDirectionRaw() {
        return mGotDirectionRaw;
    }

    public boolean isRadian() {
        return mIsRadian;
    }

    public boolean isQualityDirection() {
        return mGotQualityDirection;
    }

    public boolean isRotationDelta() {
        return mGotRotationDelta;
    }

    public double getAccelZ() {
        return mAccelZ;
    }

    public double getDeltaTime() {
        return mDeltaTime;
    }

    public double getDirection() {
        return mDirection;
    }

    public double getDirectionRaw() {
        return mDirectionRaw;
    }

    public double getQualityDirection() {
        return mQualityDirection;
    }

    public double[] getRotationDelta() {
        return mRotationDelta;
    }

    public void setAccelerationMax(double mAccelerationMax) {
        this.mAccelerationMax = mAccelerationMax;
    }

    public void setAccelerationMean(double mAccelerationMean) {
        this.mAccelerationMean = mAccelerationMean;
    }

    public void setDeltaTime(double mDeltaTime) {
        this.mDeltaTime = mDeltaTime;
    }

    public void setAccelZ(double mAccelZ) {
        this.mAccelZ = mAccelZ;
    }

    public void setDirection(double mDirection) {
        this.mDirection = mDirection;
    }

    public void setDirectionRaw(double mDirectionRaw) {
        this.mDirectionRaw = mDirectionRaw;
    }

    public void setGotAccelerationMax(boolean mGotAccelerationMax) {
        this.mGotAccelerationMax = mGotAccelerationMax;
    }

    public void setGotAccelerationMean(boolean mGotAccelerationMean) {
        this.mGotAccelerationMean = mGotAccelerationMean;
    }

    public void setGotAccelZ(boolean mGotAccelZ) {
        this.mGotAccelZ = mGotAccelZ;
    }

    public void setGotDeltaTime(boolean mGotDeltaTime) {
        this.mGotDeltaTime = mGotDeltaTime;
    }

    public void setGotDirection(boolean mGotDirection) {
        this.mGotDirection = mGotDirection;
    }

    public void setGotDirectionRaw(boolean mGotDirectionRaw) {
        this.mGotDirectionRaw = mGotDirectionRaw;
    }

    public void setGotQualityDirection(boolean mGotQualityDirection) {
        this.mGotQualityDirection = mGotQualityDirection;
    }

    public void setGotRotationDelta(boolean mGotRotationDelta) {
        this.mGotRotationDelta = mGotRotationDelta;
    }

    public void isRadian(boolean mIsRadian) {
        this.mIsRadian = mIsRadian;
    }

    public void setQualityDirection(double mQualityDirection) {
        this.mQualityDirection = mQualityDirection;
    }

    public void setRotationDelta(double[] mRotationDelta) {
        this.mRotationDelta = mRotationDelta;
    }

    public void setStatus(Status mStatus) {
        this.mStatus = mStatus;
    }

    private class Keys {
        public final static String DELTA_TIME = "delta_time";
        public final static String ACCELERATION_MEAN = "acceleration_mean";
        public final static String ACCELERATION_MAX = "acceleration_max";
        public final static String DIRECTION_RAW = "direction_raw";
        public final static String IS_RADIAN = "is_radian";
        public final static String ROTATION_DELTA = "rotation_delta";
        public final static String ACCEL_Z = "accel_z";
        public final static String DIRECTION = "direction";
        public final static String QUALITY_DIRECTION = "quality_direction";

    }

}
