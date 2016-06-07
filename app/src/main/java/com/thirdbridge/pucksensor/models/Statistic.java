package com.thirdbridge.pucksensor.models;

/**
 * Created by Jayson Dalpé since 2016-06-07.
 */
public class Statistic {

    public enum Stat {
        MAX_ACCEL,
        MEAN_ACCEL,
        RELEASE_TIME
    }

    public static String get(Stat stat) {
        switch (stat) {
            case MAX_ACCEL:
                return "Maximum acceleration";
            case MEAN_ACCEL:
                return "Mean acceleration";
            case RELEASE_TIME:
                return "Release time";
        }
        return "";
    }

    public static Stat[] getAll() {
        Stat[] stat = {Stat.MAX_ACCEL, Stat.MEAN_ACCEL, Stat.RELEASE_TIME};
        return stat;
    }

}
