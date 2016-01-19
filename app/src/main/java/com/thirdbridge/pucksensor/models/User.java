package com.thirdbridge.pucksensor.models;

/**
 * Created by Christophe on 2015-10-15.
 */
public class User {

    private String mFirstName;
    private String mLastName;
    private String mId;

    public User(String id, String firstName, String lastName){
        this.mId = id;
        this.mFirstName = firstName;
        this.mLastName = lastName;
    }

    public String getId() {
        return mId;
    }

    public String getFirstName() {
        return mFirstName;
    }

    public void setFirstName(String mFirstName) {
        this.mFirstName = mFirstName;
    }

    public String getLastName() {
        return mLastName;
    }

    public void setLastName(String mLastName) {
        this.mLastName = mLastName;
    }

}
