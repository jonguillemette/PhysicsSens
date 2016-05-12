package com.thirdbridge.pucksensor.models;

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

    public enum Data {
        ACCELERATION_INIT_X,
        ACCELERATION_INIT_Y,
        ACCELERATION_END_X,
        ACCELERATION_END_Y,
        DELTA_TIME,
        DELTA_FLYING_TIME,
        ACCELERATION_MEAN,
        ACCELERATION_MAX,
        DIRECTION_RAW_START,
        DIRECTION_RAW_END,
        IS_RADIAN,
        ROTATION_DELTA,
        ACCEL_Z,
        DIRECTION_START,
        DIRECTION_END,
        QUALITY_DIRECTION,
    }

    public static String getUnits(Data data, Data data2) {
        switch (data) {
            case ACCELERATION_INIT_X:
                return "g";
            case ACCELERATION_INIT_Y:
                return "g";
            case ACCELERATION_END_X:
                return "g";
            case ACCELERATION_END_Y:
                return "g";
            case DELTA_TIME:
                return "ms";
            case DELTA_FLYING_TIME:
                return "ms";
            case ACCELERATION_MEAN:
                return "g";
            case ACCELERATION_MAX:
                return "g";
            case DIRECTION_RAW_START:
                if (data2 == Data.IS_RADIAN) {
                    return "rad";
                } else {
                    return "degree";
                }
            case DIRECTION_RAW_END:
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
                return "g";
            case DIRECTION_START:
                if (data2 == Data.IS_RADIAN) {
                    return "rad";
                } else {
                    return "degree";
                }
            case DIRECTION_END:
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
    public double accelerationInitX;
    public double accelerationInitY;
    public double accelerationEndX;
    public double accelerationEndY;
    public double deltaTime;
    public double deltaFlyingTime;
    public double accelerationMean;
    public double accelerationMax;
    public double directionRawStart;
    public double directionRawEnd;
    public boolean isRadian = false;
    public double rotationDelta;
    public double accelZ;

    // Computed data
    public double directionStart;
    public double directionEnd;
    public double qualityDirection;

    // Which data is used
    public boolean gotAccelerationInitX = false;
    public boolean gotAccelerationInitY = false;
    public boolean gotAccelerationEndX = false;
    public boolean gotAccelerationEndY = false;
    public boolean gotDeltaTime = false;
    public boolean gotDeltaFlyingTime = false;
    public boolean gotAccelerationMean = false;
    public boolean gotAccelerationMax = false;
    public boolean gotDirectionRawStart = false;
    public boolean gotDirectionRawEnd = false;
    public boolean gotRotationDelta = false;
    public boolean gotAccelZ = false;
    public boolean gotDirectionStart = false;
    public boolean gotDirectionEnd = false;
    public boolean gotQualityDirection = false;

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
            accelerationInitX = jsonPoint.getDouble(Keys.ACCELERATION_INIT_X);
            gotAccelerationInitX = true;
        } catch(Exception e) {}

        try {
            accelerationInitY = jsonPoint.getDouble(Keys.ACCELERATION_INIT_Y);
            gotAccelerationInitY = true;
        } catch(Exception e) {}

        try {
            accelerationEndX = jsonPoint.getDouble(Keys.ACCELERATION_END_X);
            gotAccelerationEndX = true;
        } catch(Exception e) {}

        try {
            accelerationEndY = jsonPoint.getDouble(Keys.ACCELERATION_END_Y);
            gotAccelerationEndY = true;
        } catch(Exception e) {}

        try {
            deltaTime = jsonPoint.getDouble(Keys.DELTA_TIME);
            gotDeltaTime = true;
        } catch(Exception e) {}

        try {
            deltaFlyingTime = jsonPoint.getDouble(Keys.DELTA_FLYING_TIME);
            gotDeltaFlyingTime = true;
        } catch(Exception e) {}

        try {
            accelerationMean = jsonPoint.getDouble(Keys.ACCELERATION_MEAN);
            gotAccelerationMean = true;
        } catch(Exception e) {}

        try {
            accelerationMax = jsonPoint.getDouble(Keys.ACCELERATION_MAX);
            gotAccelerationMax = true;
        } catch(Exception e) {}

        try {
            directionRawStart = jsonPoint.getDouble(Keys.DIRECTION_RAW_START);
            gotDirectionRawStart = true;
        } catch(Exception e) {}

        try {
            directionRawEnd = jsonPoint.getDouble(Keys.DIRECTION_RAW_END);
            gotDirectionRawEnd = true;
        } catch(Exception e) {}

        try {
            isRadian = jsonPoint.getBoolean(Keys.IS_RADIAN);
        } catch(Exception e) {}

        try {
            rotationDelta = jsonPoint.getDouble(Keys.ROTATION_DELTA);
            gotRotationDelta = true;
        } catch(Exception e) {}

        try {
            accelZ = jsonPoint.getDouble(Keys.ACCEL_Z);
            gotAccelZ = true;
        } catch(Exception e) {}

        try {
            directionStart = jsonPoint.getDouble(Keys.DIRECTION_START);
            gotDirectionStart = true;
        } catch(Exception e) {}

        try {
            directionEnd = jsonPoint.getDouble(Keys.DIRECTION_END);
            gotDirectionEnd = true;
        } catch(Exception e) {}

        try {
            qualityDirection = jsonPoint.getDouble(Keys.QUALITY_DIRECTION);
            gotQualityDirection = true;
        } catch(Exception e) {}

        mStatus = Status.COMPUTED;
    }

    public KeyPoint(Object[] datas, Data[] types) {
        mStatus = Status.COMPUTED;
        for (int i=0; i<datas.length; i++) {
            switch(types[i]) {
                case ACCELERATION_INIT_X:
                    accelerationInitX = (double) datas[i];
                    gotAccelerationInitX = true;
                    break;
                case ACCELERATION_INIT_Y:
                    accelerationInitY = (double) datas[i];
                    gotAccelerationInitY = true;
                    break;
                case ACCELERATION_END_X:
                    accelerationEndX = (double) datas[i];
                    gotAccelerationEndX = true;
                    break;
                case ACCELERATION_END_Y:
                    accelerationEndY = (double) datas[i];
                    gotAccelerationEndY = true;
                    break;
                case DELTA_TIME:
                    deltaTime = (double) datas[i];
                    gotDeltaTime = true;
                    break;
                case DELTA_FLYING_TIME:
                    deltaFlyingTime = (double) datas[i];
                    gotDeltaFlyingTime = true;
                    break;
                case ACCELERATION_MEAN:
                    accelerationMean = (double) datas[i];
                    gotAccelerationMean = true;
                    break;
                case ACCELERATION_MAX:
                    accelerationMax = (double) datas[i];
                    gotAccelerationMax = true;
                    break;
                case DIRECTION_RAW_START:
                    directionRawStart = (double) datas[i];
                    gotDirectionRawStart = true;
                    break;
                case DIRECTION_RAW_END:
                    directionRawEnd = (double) datas[i];
                    gotDirectionRawEnd = true;
                    break;
                case IS_RADIAN:
                    isRadian = (boolean) datas[i];
                    break;
                case ROTATION_DELTA:
                    rotationDelta = (double) datas[i];
                    gotRotationDelta = true;
                    break;
                case ACCEL_Z:
                    accelZ = (double) datas[i];
                    gotAccelZ = true;
                    break;
                case DIRECTION_START:
                    directionStart = (double) datas[i];
                    gotDirectionStart = true;
                    break;
                case DIRECTION_END:
                    directionEnd = (double) datas[i];
                    gotDirectionEnd = true;
                    break;
                case QUALITY_DIRECTION:
                    qualityDirection = (double) datas[i];
                    gotQualityDirection = true;
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


    public void setStatus(Status mStatus) {
        this.mStatus = mStatus;
    }

    private class Keys {
        public final static String ACCELERATION_INIT_X = "acceleration_init_x";
        public final static String ACCELERATION_INIT_Y = "acceleration_init_y";
        public final static String ACCELERATION_END_X = "acceleration_end_x";
        public final static String ACCELERATION_END_Y = "acceleration_end_y";
        public final static String DELTA_TIME = "delta_time";
        public final static String DELTA_FLYING_TIME = "delta_flyig_time";
        public final static String ACCELERATION_MEAN = "acceleration_mean";
        public final static String ACCELERATION_MAX = "acceleration_max";
        public final static String DIRECTION_RAW_START = "direction_raw_start";
        public final static String DIRECTION_RAW_END = "direction_raw_end";
        public final static String IS_RADIAN = "is_radian";
        public final static String ROTATION_DELTA = "rotation_delta";
        public final static String ACCEL_Z = "accel_z";
        public final static String DIRECTION_START = "direction_start";
        public final static String DIRECTION_END = "direction_end";
        public final static String QUALITY_DIRECTION = "quality_direction";

    }

}
