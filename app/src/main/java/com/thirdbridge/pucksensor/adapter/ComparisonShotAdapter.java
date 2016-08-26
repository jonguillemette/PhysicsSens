package com.thirdbridge.pucksensor.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.thirdbridge.pucksensor.R;
import com.thirdbridge.pucksensor.models.ShotSpecification;

import java.text.DecimalFormat;
import java.util.List;

/**
 * Created by Jayson Dalpé on 2016-04-01.
 */
public class ComparisonShotAdapter extends BaseAdapter {
    private List<ShotSpecification> mShotSpecs;
    private LayoutInflater mInflater;
    private Context mContext;

    public ComparisonShotAdapter(Context context, List<ShotSpecification> shotSpecs) {
        mInflater = LayoutInflater.from(context);
        mShotSpecs = shotSpecs;
        mContext = context;
    }

    public int getCount() {
        return mShotSpecs.size();
    }

    public Object getItem(int position) {
        return mShotSpecs.get(position);
    }

    public long getItemId(int position) {
        return position;
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        ViewGroup vg;

        if (convertView != null) {
            vg = (ViewGroup) convertView;
        } else {
            vg = (ViewGroup) mInflater.inflate(R.layout.element_shot, null);
            vg.setTag(position);
        }

        ((TextView) vg.findViewById(R.id.shot_title)).setText(mShotSpecs.get(position).getName() + "\n " + mShotSpecs.get(position).getDescription());
        String info = "";
        DecimalFormat numberFormat = new DecimalFormat("#.00");
        for (int i=0; i<mShotSpecs.get(position).getNumbers().length; i++) {
            info +=numberFormat.format(mShotSpecs.get(position).getNumbers()[i]) + " " + mShotSpecs.get(position).getUnits()[i];
            if (i < mShotSpecs.get(position).getNumbers().length-1) {
                info += "\n";
            }
        }
        ((TextView) vg.findViewById(R.id.descr)).setText(info);

        return vg;
    }

}