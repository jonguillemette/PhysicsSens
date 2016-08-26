package com.thirdbridge.pucksensor.adapter;

import android.content.Context;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.thirdbridge.pucksensor.R;
import com.thirdbridge.pucksensor.models.Exercise;

import java.util.List;

/**
 * Created by Jayson Dalpé on 2016-04-20.
 */
public class LoadExerciseAdapter extends BaseAdapter {
    private List<Exercise> mExercises;
    private LayoutInflater mInflater;

    public LoadExerciseAdapter(Context context, List<Exercise> exercise) {
        mInflater = LayoutInflater.from(context);
        mExercises = exercise;
    }

    @Override
    public int getCount() {
        return mExercises.size();
    }

    @Override
    public Object getItem(int position) {
        return mExercises.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final ViewGroup vg;

        if (convertView != null) {
            vg = (ViewGroup) convertView.getTag();
        } else {
            vg = (ViewGroup) mInflater.inflate(R.layout.element_exercise, null);
            vg.setTag(vg);
        }
        ((TextView) vg.findViewById(R.id.name)).setText(mExercises.get(position).getTitle());
        ((TextView) vg.findViewById(R.id.name)).setTypeface(Typeface.DEFAULT, Typeface.BOLD_ITALIC);
        ((TextView) vg.findViewById(R.id.descr)).setText(" " + mExercises.get(position).getDescription());

        return vg;
    }
}