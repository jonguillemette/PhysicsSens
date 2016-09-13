package com.thirdbridge.pucksensor.hardware;

import android.util.Pair;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * Created by Jayson Dalpé on 2016-03-24.
 */
public class Calibrate {

    private String mTime;
    private List<Double[]> mData;


    public Calibrate() {
        mData = new ArrayList<Double[]>();

        DateFormat df = new SimpleDateFormat("dd_MMM_yyyy_HH.mm.ssa");
        mTime = df.format(Calendar.getInstance().getTime());
    }

    public void addEntry(double acc1, double acc2, double gyro) {
        Double[] data = new Double[3];
        data[0] = acc1;
        data[1] = acc2;
        data[2] = gyro;
        mData.add(data);
    }

    public Pair<String, String> packageFormCSV() {
        // Create a form that is easily parceable
        String text = "Calibration data, \n";
        text += "Data,\n";
        text += "Low accel,High accel,Gyro RAW\n";

        for (Double[] value: mData) {
            text += value[0] + "," + value[1] + "," + value[2] + "\n";
        }

        String fileName = "calibration_" + mTime + ".csv";
        return new Pair<String, String>(fileName, text);
    }
}