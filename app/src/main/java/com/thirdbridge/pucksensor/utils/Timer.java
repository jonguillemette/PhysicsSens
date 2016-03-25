package com.thirdbridge.pucksensor.utils;

import android.util.Log;
import android.widget.ProgressBar;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Jayson Dalpé on 2016-03-25.
 */
public class Timer {

    private int mSeconds;
    private Thread mThread;
    private boolean mRunning = false;

    public interface  TimerCallback {
        public void onTimeOut();
    }

    private TimerCallback mListener = null;

    public Timer(int seconds, TimerCallback listener) {
        mRunning = true;
        mListener = listener;
        mSeconds = seconds;
        mThread = new Thread(new Runnable() {
            @Override
            public void run() {
                for (int i=0; i<mSeconds; i++) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    if (!mRunning) {
                        break;
                    }
                }
                if (mListener!= null && mRunning) {
                    mListener.onTimeOut();
                }
            }
        });

        mThread.start();
    }

    public void cancel() {
        mRunning = false;
    }
}
