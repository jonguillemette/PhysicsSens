package com.thirdbridge.pucksensor.controllers;

import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.thirdbridge.pucksensor.R;

/**
 * A placeholder fragment containing a simple view.
 */
public class SplashFragment extends Fragment {

    public static SplashFragment newInstance(){
        SplashFragment fragment = new SplashFragment();
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_splash, container, false);
    }
}
