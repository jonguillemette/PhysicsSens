package com.thirdbridge.pucksensor.utils;

/**
 * Created by Christophe on 2015-10-14.
 */
public class Constants {
    public enum SelectedTest { SHOT, EVALUATION, STICK_HANDLING, FREE_ROAMING }
    public static String SELECTED_TEST = "selectedTest";
    public static String CURRENT_USER = "currentUser";
    public static String TEST_DATA = "testData";
    public static String BLE_CONNECTION_TERMINATED = "bleConnectionTerminated";
    public static String YOUTUBE_URL = "youtubeUrl";

    //Acceleration constants
    public static String TOP_ACCEL_X = "topAccelX";
    public static String TOP_ACCEL_Y = "topAccelY";
    public static String TOP_ACCEL_Z = "topAccelZ";
    public static String TOP_ACCEL_XYZ = "topAccelXYZ";
    public static String ACCEL_DATA = "accelDataX";

    //speed constants
    public static String TOP_SPEED_X = "topSpeedX";
    public static String TOP_SPEED_Y = "topSpeedY";
    public static String TOP_SPEED_Z = "topSpeedZ";
    public static String TOP_SPEED_XYZ = "topSpeedXYZ";
    public static String SPEED_DATA = "SpeedDataX";

    //Rotation constants
    public static String TOP_ROTATION = "topRotation";
    public static String ROTATION_DATA = "rotationData";

}
