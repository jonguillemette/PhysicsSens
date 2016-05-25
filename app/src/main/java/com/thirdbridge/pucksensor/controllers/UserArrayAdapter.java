package com.thirdbridge.pucksensor.controllers;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckedTextView;

import com.thirdbridge.pucksensor.R;
import com.thirdbridge.pucksensor.models.User;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Jayson Dalpé on 2016-05-25.
 */
class UserArrayAdapter extends ArrayAdapter<User> {

    List<User> userList = new ArrayList<User>();
    Context mContext;

    public UserArrayAdapter(Context context, int textViewResourceId, List<User> objects) {
        super(context, textViewResourceId, objects);
        userList = objects;
        mContext = context;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        return getCustomView(position,convertView,parent);
    }


    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        return getCustomView(position,convertView,parent);
    }

    public View getCustomView(int position, View convertView, ViewGroup parent) {

        LayoutInflater inflater = (LayoutInflater)mContext
                .getSystemService(mContext.LAYOUT_INFLATER_SERVICE);

        View spinnerView = inflater.inflate(R.layout.spinner_dropdown_item, parent, false);

        CheckedTextView checkedTextView = (CheckedTextView) spinnerView.findViewById(R.id.spinner_dropdown_item);

        checkedTextView.setText(userList.get(position).getName());

        return spinnerView;
    }
}