package com.thirdbridge.pucksensor.activity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import androidx.fragment.app.Fragment;

import com.thirdbridge.pucksensor.controllers.SplashFragment;
import com.thirdbridge.pucksensor.utils.SingleFragmentActivity;

public class SplashActivity extends SingleFragmentActivity {

    private boolean mBleSupported = true;

    @Override
    protected Fragment createFragment() {
        return SplashFragment.newInstance();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Runnable afterLoadingRunnable = new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(SplashActivity.this, ContentActivity.class);
                startActivity(intent);
            }
        };

        Handler h = new Handler();
        h.postDelayed(afterLoadingRunnable, 1500);




    }
}
