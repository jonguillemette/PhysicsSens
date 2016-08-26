package com.thirdbridge.pucksensor.models;

import com.thirdbridge.pucksensor.hardware.Shot;

/**
 * Created by Jayson Dalpé on 2016-04-01.
 * Specification for a type of shot.
 */
public class ShotSpecification {
    private String mName;
    private String mDescription;
    private double[] mNumbers;
    private String[] mUnits;
    private String mId;
    private Shot mShot;

    /**
     * Build a shot specification.
     * @warning The array of not copy, just reference.
     * @param name
     * @param description
     * @param numbers
     * @param units
     * @param id
     */
    public ShotSpecification(String name, String description, double[] numbers, String[] units, String id) {
        mName = name;
        mDescription = description;
        mNumbers = numbers;
        mUnits = units;
        mId = id;
    }

    public ShotSpecification(String name, String description, double[] numbers, String[] units, String id, Shot shot) {
        mName = name;
        mDescription = description;
        mNumbers = numbers;
        mUnits = units;
        mId = id;
        mShot = shot;
    }

    public void setShot(Shot shot) {
        mShot = shot;
    }

    public Shot getShot() {
        return mShot;
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
