package com.thirdbridge.pucksensor.utils;

import android.content.Context;
import android.util.Log;

import com.thirdbridge.pucksensor.R;

/**
 * Created by Jay on 2016-05-25.
 */
public class PlotLyConverter {

    private Context mContext;
    private String mDefault;
    private final static String SEPARATOR_X= "##X##";
    private final static String SEPARATOR_Y= "##Y##";
    private final static String SEPARATOR_ERROR= "##ERROR##";
    private final static String SEPARATOR_DIMEN = "##DIMEN##";
    private final static String SEPARATOR_TYPE = "##TYPE##";

    public PlotLyConverter(Context context) {
        mContext = context;
        mDefault = IO.loadRawTextAsString(mContext, R.raw.plotly);

        Log.i("YOLLO", mDefault);
    }

    public String makeLine(double[] x, double[] y, double[] error, int dimension) {
        String xStr = "[";
        for (int i=0; i<x.length; i++) {
            if (i ==  x.length -1) {
               xStr +=  x[i] + "]";
            } else {
                xStr += x[i] + ", ";
            }
        }
        String yStr = "[";
        for (int i=0; i<y.length; i++) {
            if (i ==  y.length -1) {
                yStr +=  y[i] + "]";
            } else {
                yStr += y[i] + ", ";
            }
        }

        String errorStr = "[";
        for (int i=0; i<error.length; i++) {
            if (i ==  error.length -1) {
                errorStr +=  error[i] + "]";
            } else {
                errorStr += error[i] + ", ";
            }
        }
        String retValue;
        retValue = mDefault.replaceAll(SEPARATOR_X, xStr);
        retValue = retValue.replaceAll(SEPARATOR_Y, yStr);
        retValue = retValue.replaceAll(SEPARATOR_ERROR, errorStr);
        retValue = retValue.replaceAll(SEPARATOR_DIMEN, dimension + "");
        retValue = retValue.replaceAll(SEPARATOR_TYPE, "scatter");

        return  retValue;
    }

    public String makeBar(String[] x, double[] y, double[] error, int dimension) {
        String xStr = "[";
        for (int i=0; i<x.length; i++) {
            if (i ==  x.length -1) {
                xStr +=  "'" + x[i] + "']";
            } else {
                xStr += "'" + x[i] + "', ";
            }
        }
        String yStr = "[";
        for (int i=0; i<y.length; i++) {
            if (i ==  y.length -1) {
                yStr +=  y[i] + "]";
            } else {
                yStr += y[i] + ", ";
            }
        }

        String errorStr = "[";
        for (int i=0; i<error.length; i++) {
            if (i ==  error.length -1) {
                errorStr +=  error[i] + "]";
            } else {
                errorStr += error[i] + ", ";
            }
        }
        String retValue;
        retValue = mDefault.replaceAll(SEPARATOR_X, xStr);
        retValue = retValue.replaceAll(SEPARATOR_Y, yStr);
        retValue = retValue.replaceAll(SEPARATOR_ERROR, errorStr);
        retValue = retValue.replaceAll(SEPARATOR_DIMEN, dimension + "");
        retValue = retValue.replaceAll(SEPARATOR_TYPE, "bar");

        return  retValue;
    }
}
