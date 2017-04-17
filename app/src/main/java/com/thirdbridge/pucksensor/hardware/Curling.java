package com.thirdbridge.pucksensor.hardware;

import android.util.Log;
import android.util.Pair;

import com.thirdbridge.pucksensor.models.User;
import com.thirdbridge.pucksensor.utils.IO;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * Created by Jayson Dalpé on 2016-01-26.
 */
public class Curling {

    private List<Double> mRotDatas;
    private List<Double> mAccTotal;
    private double mTimestep;
    private String mTime;

    public Curling(double timestep) {
        mTimestep = timestep;
        DateFormat df = new SimpleDateFormat("dd_MMM_yyyy_HH.mm.ssa");
        mTime = df.format(Calendar.getInstance().getTime());
        mAccTotal = new ArrayList<>();
        mRotDatas = new ArrayList<>();
    }

    public void addAccel(double data) {
        mAccTotal.add(data);
    }

    public void addRot(double data) {
        mRotDatas.add(data);
    }

    public Pair<String,String> packageFormCSV() {
        // Create a form that is easily parceable
        String text = "Timestep, " + mTimestep + ",ms" + "\n";
        text += "Data,\n";
        text += "Acceleration,Rotation\n";

        for (int i=0; i< mAccTotal.size(); i++) {
            text += mAccTotal.get(i) + "," + mRotDatas.get(i) + "\n";
        }

        String fileName = mTime + ".csv";
        return new Pair<String,String>(fileName, text);
    }

}
