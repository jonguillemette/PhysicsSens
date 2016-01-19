package com.thirdbridge.pucksensor.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.thirdbridge.pucksensor.database.DbSchema.ShotTestTable;
import com.thirdbridge.pucksensor.database.DbSchema.UserTable;
import com.thirdbridge.pucksensor.models.ShotTest;
import com.thirdbridge.pucksensor.models.User;
import com.thirdbridge.pucksensor.utils.App;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Christophe on 2015-10-15.
 */
public class DataManager {

    private static DataManager sDataManager;

    private SQLiteDatabase mDatabase;

    public static DataManager get(){
        return sDataManager != null ? sDataManager : new DataManager(App.get());
    }

    private DataManager(Context context){
        mDatabase = new SQLiteHelper(context)
                .getWritableDatabase();
    }

    public void addUser(User user){
        ContentValues values = getUserContentValues(user);
        mDatabase.insert(UserTable.NAME, null, values);
    }

    private static ContentValues getUserContentValues(User user){
        ContentValues values = new ContentValues();
        values.put(UserTable.Cols.UUID, user.getId());
        values.put(UserTable.Cols.FIRST_NAME, user.getFirstName());
        values.put(UserTable.Cols.LAST_NAME, user.getLastName());

        return values;
    }

    public List<User> getUsers(){
        List<User> users = new ArrayList<>();

        SticeCursorWrapper cursor = queryUsers(null, null);

        try{
            cursor.moveToFirst();
            while(!cursor.isAfterLast()){
                users.add(cursor.getUser());
                cursor.moveToNext();
            }
        }
        finally {
            cursor.close();
            mDatabase.close();
        }

        return users;
    }

    private SticeCursorWrapper queryUsers(String whereClause, String[] whereArgs){
        Cursor cursor = mDatabase.query(
                UserTable.NAME,
                null,
                whereClause,
                whereArgs,
                null,
                null,
                null
        );

        return new SticeCursorWrapper(cursor);
    }

    public void addTest(ShotTest shotTest){
        ContentValues values = getShotTestContentValues(shotTest);
        mDatabase.insert(ShotTestTable.NAME, null, values);
    }

    private static ContentValues getShotTestContentValues(ShotTest shotTest){
        ContentValues values = new ContentValues();
        values.put(ShotTestTable.Cols.UUID, shotTest.getId());
        values.put(ShotTestTable.Cols.USERNAME, shotTest.getUsername());
        values.put(ShotTestTable.Cols.DATE, shotTest.getDate());
        values.put(ShotTestTable.Cols.DESCRIPTION, shotTest.getDescription());

        //Acceleration
        values.put(ShotTestTable.Cols.ACCEL_DATA, shotTest.getAccelData());

        //Speed
        values.put(ShotTestTable.Cols.SPEED_DATA, shotTest.getSpeedData());

        //Rotation
        values.put(ShotTestTable.Cols.ROTATION_DATA, shotTest.getRotationData());

        return values;
    }

    public List<ShotTest> getShotTests(){
        List<ShotTest> shotTests = new ArrayList<>();

        SticeCursorWrapper cursor = queryShotTests(null, null);

        try{
            cursor.moveToFirst();
            while(!cursor.isAfterLast()){
                shotTests.add(cursor.getShotTest());
                cursor.moveToNext();
            }
        }
        finally {
            cursor.close();
            mDatabase.close();
        }

        return shotTests;
    }

    private SticeCursorWrapper queryShotTests(String whereClause, String[] whereArgs){
        Cursor cursor = mDatabase.query(
                ShotTestTable.NAME,
                null,
                whereClause,
                whereArgs,
                null,
                null,
                null
        );

        return new SticeCursorWrapper(cursor);
    }
}
