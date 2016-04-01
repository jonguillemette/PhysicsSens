package com.thirdbridge.pucksensor.models;

import android.graphics.Bitmap;

/**
 * Created by Jayson Dalpé on 2016-04-01.
 * Specification for a type of shot.
 */
public class ShotSpecification {
    private String mName;
    private String mDescription;
    private double[] mNumbers;
    private String[] mUnits;

    /**
     * Build a shot specification.
     * @warning The array of not copy, just reference.
     * @param name
     * @param description
     * @param numbers
     * @param units
     */
    public ShotSpecification(String name, String description, double[] numbers, String[] units) {
        mName = name;
        mDescription = description;
        mNumbers = numbers;
        mUnits = units;
    }


    public String getName() {
        return mName;
    }

    public void setName(String mName) {
        this.mName = mName;
    }

    public String getDescription() {
        return mDescription;
    }

    public void setDescription(String mDescription) {
        this.mDescription = mDescription;
    }

    public double[] getNumbers() {
        return mNumbers;
    }

    public void setNumbers(double[] numbers) {
        this.mNumbers = numbers;
    }

    public String[] getUnits() {
        return mUnits;
    }

    public void setUnits(String[] units) {
        this.mUnits = units;
    }
}
