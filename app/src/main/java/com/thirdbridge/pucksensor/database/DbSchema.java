package com.thirdbridge.pucksensor.database;

/**
 * Created by Christophe on 2015-10-15.
 */
public class DbSchema {

    public static final class UserTable{
        public static final String NAME = "users";

        public static final class Cols{
            public static final String UUID = "uuid";
            public static final String FIRST_NAME = "firstname";
            public static final String LAST_NAME = "lastname";
        }
    }

    public static final class ShotTestTable{
        public static final String NAME = "shotTests";

        public static final class Cols{
            public static final String UUID = "uuid";
            public static final String USERNAME = "username";
            public static final String DATE = "date";
            public static final String DESCRIPTION = "description";
            public static String ACCEL_DATA = "accelData";
            public static String SPEED_DATA = "SpeedDataX";
            public static String ROTATION_DATA = "rotationData";
        }
    }
}
