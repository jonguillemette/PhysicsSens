package com.thirdbridge.pucksensor.utils;

/**
 * Created by Christophe on 2015-10-15.
 */

import android.app.Application;


public class App extends Application {
    private static App instance;

    public static App get() {
        return instance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
    }
}