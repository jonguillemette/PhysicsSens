package com.thirdbridge.pucksensor.database;

import android.database.Cursor;

import com.thirdbridge.pucksensor.database.DbSchema.ShotTestTable;
import com.thirdbridge.pucksensor.database.DbSchema.UserTable;
import com.thirdbridge.pucksensor.models.ShotTest;
import com.thirdbridge.pucksensor.models.User;
import com.thirdbridge.pucksensor.utils.Constants;

import java.util.HashMap;

/**
 * Created by Christophe on 2015-10-15.
 */
public class SticeCursorWrapper extends android.database.CursorWrapper {

    public SticeCursorWrapper(Cursor cursor) {
        super(cursor);
    }

    public User getUser(){
        String uuid = getString(getColumnIndex(UserTable.Cols.UUID));
        String firstName = getString(getColumnIndex(UserTable.Cols.FIRST_NAME));
        String lastName = getString(getColumnIndex(UserTable.Cols.LAST_NAME));

        return new User(uuid,firstName,lastName);
    }

    public ShotTest getShotTest(){
        String uuid = getString(getColumnIndex(ShotTestTable.Cols.UUID));
        String username = getString(getColumnIndex(ShotTestTable.Cols.USERNAME));
        String date = getString(getColumnIndex(ShotTestTable.Cols.DATE));
        String description = getString(getColumnIndex(ShotTestTable.Cols.DESCRIPTION));

        //Acceleration
        String accelData = getString(getColumnIndex(ShotTestTable.Cols.ACCEL_DATA));

        //Speed
        String speedData = getString(getColumnIndex(ShotTestTable.Cols.SPEED_DATA));

        //Rotation
        String rotationData = getString(getColumnIndex(ShotTestTable.Cols.ROTATION_DATA));

        return new ShotTest(uuid,username,date,description,accelData,speedData,rotationData);

    }
}
