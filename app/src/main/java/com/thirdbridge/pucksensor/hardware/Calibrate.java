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
    private List<Pair<Double, Double>> mData;


    public Calibrate() {
        mData = new ArrayList<Pair<Double, Double>>();

        DateFormat df = new SimpleDateFormat("dd_MMM_yyyy_HH.mm.ssa");
        mTime = df.format(Calendar.getInstance().getTime());
    }

    public void addEntry(double acc, double ask) {
        mData.add(new Pair(acc, ask));
    }

    public Pair<String, String> packageFormCSV() {
        // Create a form that is easily parceable
        String text = "Calibration data, \n";
        text += "Data,\n";
        text += "Acceleration,Ask\n";

        for (Pair<Double, Double> value: mData) {
            text += value.first + "," + value.second + "\n";
        }

        String fileName = "calibration_" + mTime + ".csv";
        return new Pair<String, String>(fileName, text);
    }
}