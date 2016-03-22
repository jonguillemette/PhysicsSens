package com.thirdbridge.pucksensor.models;

/**
 * Created by Christophe on 2015-10-15.
 * Modified by Jayson Dalpé since 2016-01-26.
 */
public class User {
    public static final String SEP = ":SEP:";

    private String mFirstName;
    private String mLastName;
    private String mId;

    public User(String id, String firstName, String lastName){
        this.mId = id.trim();
        this.mFirstName = firstName.trim();
        this.mLastName = lastName.trim();
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

    public String packageForm() {
        return mFirstName + SEP + mLastName + SEP + mId;
    }

    public static User depackageForm(String data) {
        String[] elements = data.split(SEP);
        return new User(elements[2], elements[0], elements[1]);
    }

}
