package com.thirdbridge.pucksensor.controllers;

import android.content.Context;
import android.graphics.Typeface;
import android.support.v4.content.ContextCompat;
import android.util.Log;
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

    public ComparisonPlayerAdapter(Context context, List<Player> players) {
        mInflater = LayoutInflater.from(context);
        mPlayers = players;
    }

    @Override
    public int getCount() {
        return mPlayers.size();
    }

    @Override
    public Object getItem(int position) {
        return mPlayers.get(position);
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
            vg = (ViewGroup) mInflater.inflate(R.layout.element_player, null);
            vg.setTag(vg);
        }
        ((TextView) vg.findViewById(R.id.name)).setText(mPlayers.get(position).getName());
        ((TextView) vg.findViewById(R.id.name)).setTypeface(Typeface.DEFAULT, Typeface.BOLD_ITALIC);
        ((TextView) vg.findViewById(R.id.descr)).setText(" " + mPlayers.get(position).getDescription());
        ((ImageView) vg.findViewById(R.id.player_image)).setImageBitmap(mPlayers.get(position).getFace());

        return vg;
    }
}