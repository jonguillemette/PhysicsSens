package com.thirdbridge.pucksensor.controllers;

import android.content.Context;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.thirdbridge.pucksensor.R;
import com.thirdbridge.pucksensor.models.Player;

import java.util.List;

/**
 * Created by Jayson Dalpé on 2016-04-01.
 */
class ComparisonPlayerAdapter extends BaseAdapter {
    private List<Player> mPlayers;
    private LayoutInflater mInflater;
    private Context mContext;

    public ComparisonPlayerAdapter(Context context, List<Player> players) {
        mInflater = LayoutInflater.from(context);
        mPlayers = players;
        mContext = context;
    }

    public int getCount() {
        return mPlayers.size();
    }

    public Object getItem(int position) {
        return mPlayers.get(position);
    }

    public long getItemId(int position) {
        return position;
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        ViewGroup vg;

        if (convertView != null) {
            vg = (ViewGroup) convertView;
        } else {
            vg = (ViewGroup) mInflater.inflate(R.layout.element_player, null);
            ((TextView) vg.findViewById(R.id.descr)).setText("Name: " + mPlayers.get(position).getName() + "\n " + mPlayers.get(position).getDescription());
            ((ImageView) vg.findViewById(R.id.player_image)).setImageBitmap(mPlayers.get(position).getFace());

            vg.setTag(position);
        }

        return vg;
    }

}