package com.thirdbridge.pucksensor.utils;

/**
 * Created by Jayson Dalpé on 2016-04-20.
 */

import android.content.Context;
import android.util.Log;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.thirdbridge.pucksensor.R;

import java.util.ArrayList;
import java.util.List;

/**
 * This Cell Organizer create UI for a TableLayout.
 */
public class CellOrganizer {
    TableLayout mTable;
    String[] mTitles;
    String mPrefixRows;
    boolean mIsIncrementPrefix;
    List<List<Double>> mDatas = new ArrayList<List<Double>>();
    int mLength = 0;
    TextView[][] mVisualDatas;
    Context mContext;



    public CellOrganizer(TableLayout uiLink, String[] columns, String rowName, boolean isIncrement) {
        mTable = uiLink;
        mTitles = columns;
        mPrefixRows = rowName;
        mIsIncrementPrefix = isIncrement;
        for (int i=0; i<mTitles.length; i++) {
            List<Double> newList = new ArrayList<> ();
            mDatas.add(newList);
        }
        mContext = mTable.getContext();
    }

    public CellOrganizer(TableLayout uiLink, String[] columns, String rowName, boolean isIncrement, int length) {
        mTable = uiLink;
        mTitles = columns;
        mPrefixRows = rowName;
        mIsIncrementPrefix = isIncrement;
        mLength = length;
        for (int i=0; i<mTitles.length; i++) {
            List<Double> newList = new ArrayList<> ();
            for (int j=0; j<mLength; j++) {
                newList.add(Double.NaN);
            }
            mDatas.add(newList);
        }
        mContext = mTable.getContext();
    }

    /**
     * Add or put an element to the lists.
     * @param columns
     * @param index
     * @param value
     * @return true if the index exists
     */
    public boolean put(int columns, int index, double value) {
        List<Double> requestList = mDatas.get(columns);
        // Check if the item index exist.
        if (requestList.size() > index) {
            requestList.set(index, value);
            return true;
        } else {
            // Add up until there
            for (int i=requestList.size(); i<index; i++) {
                requestList.add(i, Double.NaN);
            }
            requestList.add(index, value);

            return false;
        }
    }

    public void put(int columns, int index, double value, boolean autoUpdate) {
        boolean retvalue = put(columns, index, value);
        if (!autoUpdate) {
            return;
        }
        if (retvalue) {
            specificActualize(columns, index);
        } else {
            allActualize();
        }
    }

    public void clear() {
        mDatas = new ArrayList<List<Double>>();
        mTitles = null;
    }

    public void reload (String[] columns, String rowName, boolean isIncrement) {
        mTitles = columns;
        mPrefixRows = rowName;
        mIsIncrementPrefix = isIncrement;
        for (int i=0; i<mTitles.length; i++) {
            List<Double> newList = new ArrayList<> ();
            mDatas.add(newList);
        }
        mContext = mTable.getContext();
    }

    public void reload(String[] columns, String rowName, boolean isIncrement, int length) {
        mTitles = columns;
        mPrefixRows = rowName;
        mIsIncrementPrefix = isIncrement;
        mLength = length;
        for (int i=0; i<mTitles.length; i++) {
            List<Double> newList = new ArrayList<> ();
            for (int j=0; j<mLength; j++) {
                newList.add(Double.NaN);
            }
            mDatas.add(newList);
        }
        mContext = mTable.getContext();
    }

    /**
     * Actualize all the table layout.
     * Use this when adding element.
     * NOTE: Must be use in a UI thread.
     */
    public void allActualize(){
        // 1. Find the maximum number of row.
        int max = 0;
        for( List<Double> list:mDatas) {
            max = Math.max(list.size(), max);
        }

        // 2. Create headers
        TableRow header = new TableRow(mContext);
        TextView empty = new TextView(mContext);
        empty.setBackgroundResource(R.drawable.title_cell_layout);

        int width = 0;
        header.addView(empty);
        for (String title:mTitles) {
            TextView newTV = new TextView(mContext);
            newTV.setBackgroundResource(R.drawable.title_cell_layout);
            newTV.setText(title);
            header.addView(newTV);
        }
        mTable.removeAllViews();
        mTable.addView(header);
        // Create data
        mVisualDatas = new TextView[mDatas.size()][max];
        for (int j = 0; j < max; j++) {
            TableRow tableRow = new TableRow(mContext);
            TextView title = new TextView(mContext);
            title.setText(mPrefixRows + (mIsIncrementPrefix ? " " + j : ""));
            title.setBackgroundResource(R.drawable.title_cell_layout);
            tableRow.addView(title);
            for (int i=0; i<mDatas.size(); i++) {
                TextView data = new TextView(mContext);
                data.setBackgroundResource(R.drawable.cell_layout);
                try {
                    double value = mDatas.get(i).get(j);
                    String valueStr = "" + value;
                    if (!valueStr.equals("NaN")) {
                        data.setText("" + valueStr);
                    }
                } catch (Exception e) {}
                tableRow.addView(data);
                mVisualDatas[i][j] = data;
            }
            mTable.addView(tableRow);
        }
    }

    /**
     * Actualize only one case inside the layout. The index must be reachable without addLine()
     */
    public void specificActualize(int columns, int index){
        mVisualDatas[columns][index].setText("" + mDatas.get(columns).get(index));
    }
}
