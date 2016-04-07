package com.thirdbridge.pucksensor.models;

/**
 * Created by Christophe on 2015-10-15.
 * Modified by Jayson Dalpé since 2016-01-26.
 */
public class User {
    public static final String SEP = ":SEP:";

    private String mName;
    private String mId;

    public User(String id, String firstName, String lastName){
        this.mId = id.trim();
        this.mName = firstName.trim() + " " + lastName.trim();
    }

    public User(String id, String fullName){
        this.mId = id.trim();
        this.mName = fullName.trim();
    }

    public String getId() {
        return mId;
    }

    public String getName() {
        return mName;
    }

    public void setName(String name) {
        this.mName = name;
    }

    public String packageForm() {
        return mName + SEP + mId;
    }

    public static User depackageForm(String data) {
        String[] elements = data.split(SEP);
        return new User(elements[1], elements[0]);
    }

}
