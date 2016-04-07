package com.thirdbridge.pucksensor.models;

import android.graphics.Bitmap;

/**
 * Created by Jayson Dalpé on 2016-04-01.
 * Player model for the query
 */
public class Player {
    private Bitmap mFace;
    private String mName;
    private String mDescription;
    private String mId;

    public Player(Bitmap face, String name, String description, String id) {
        mFace = face;
        mName = name;
        mDescription = description;
        mId = id;
    }


    public Bitmap getFace() {
        return mFace;
    }

    public void setFace(Bitmap mFace) {
        this.mFace = mFace;
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

    public void setId(String mId) {
        this.mId = mId;
    }

    public String getId() {
        return this.mId;
    }
}
