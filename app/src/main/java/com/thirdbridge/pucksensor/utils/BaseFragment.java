package com.thirdbridge.pucksensor.utils;

/**
 * Created by Christophe on 2015-10-14.
 */

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import com.thirdbridge.pucksensor.activity.ContentActivity;


public abstract class BaseFragment extends Fragment
{
    private InputMethodManager mImm;

    protected ContentActivity getController()
    {
        return (ContentActivity) getActivity();
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        mImm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState)
    {
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void onResume()
    {
        super.onResume();

        hideKeyboard();

    }

    public void hideKeyboard()
    {
        if(mImm.isActive())
            mImm.hideSoftInputFromWindow(getView().getWindowToken(), 0);
    }
}
