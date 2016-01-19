package com.thirdbridge.pucksensor.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.thirdbridge.pucksensor.database.DbSchema.ShotTestTable;
import com.thirdbridge.pucksensor.database.DbSchema.UserTable;

/**
 * Created by Christophe on 2015-10-15.
 */
public class SQLiteHelper extends SQLiteOpenHelper {

    private static final int VERSION = 1;
    private static final String DATABASE_NAME = "stice.db";

    public SQLiteHelper(Context context) {
        super(context, DATABASE_NAME, null, VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.execSQL("create table " + UserTable.NAME + "(" +
                            "_id integer primary key autoincrement, " +
                            UserTable.Cols.UUID + ", " +
                            UserTable.Cols.FIRST_NAME + ", " +
                            UserTable.Cols.LAST_NAME +
                            ")"
        );

        sqLiteDatabase.execSQL("create table " + ShotTestTable.NAME + "(" +
                        "_id integer primary key autoincrement, " +
                        ShotTestTable.Cols.UUID + ", " +
                        ShotTestTable.Cols.USERNAME + ", " +
                        ShotTestTable.Cols.DATE + ", " +
                        ShotTestTable.Cols.DESCRIPTION + ", " +
                        ShotTestTable.Cols.ACCEL_DATA + ", " +
                        ShotTestTable.Cols.SPEED_DATA + ", " +
                        ShotTestTable.Cols.ROTATION_DATA +
                        ")"
        );
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int newVersion, int oldVersion) {
        Log.w(SQLiteHelper.class.getName(),
                "Upgrading database from version " + oldVersion + " to "
                        + newVersion + ", which will destroy all old data");
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + UserTable.NAME);
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + ShotTestTable.NAME);
        onCreate(sqLiteDatabase);
    }
}
