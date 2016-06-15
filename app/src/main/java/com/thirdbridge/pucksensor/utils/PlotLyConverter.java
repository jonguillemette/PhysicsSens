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
    private String mDefaultBar;

    //GENERAL
    private final static String SEPARATOR_ERROR_CONTAIN= "##ERROR_CONTAIN##";
    private final static String SEPARATOR_DIMEN = "##DIMEN##";

    // LINE
    private final static String SEPARATOR_X= "##X##";
    private final static String SEPARATOR_Y= "##Y##";
    private final static String SEPARATOR_ERROR= "##ERROR##";
    private final static String SEPARATOR_TYPE = "##TYPE##";

    // BAR
    private final static String SEPARATOR_X_1= "##X1##";
    private final static String SEPARATOR_NAME_1= "##NAME1##";
    private final static String SEPARATOR_Y_1 = "##Y1##";
    private final static String SEPARATOR_ERROR_1= "##ERROR1##";
    private final static String SEPARATOR_X_2 = "##X2##";
    private final static String SEPARATOR_NAME_2 = "##NAME2##";
    private final static String SEPARATOR_Y_2 = "##Y2##";
    private final static String SEPARATOR_ERROR_2 = "##ERROR2##";

    private final static String USE_ERROR = "error_y: {\n" +
            "      type: 'data',\n" +
            "      array: ##ERROR_CONTAIN##,\n" +
            "      visible: true\n" +
            "    },";

    public PlotLyConverter(Context context) {
        mContext = context;
        mDefault = IO.loadRawTextAsString(mContext, R.raw.plotly);
        mDefaultBar = IO.loadRawTextAsString(mContext, R.raw.plotly_bar);

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
        retValue = retValue.replaceAll(SEPARATOR_ERROR, USE_ERROR);
        retValue = retValue.replaceAll(SEPARATOR_ERROR_CONTAIN, errorStr);
        retValue = retValue.replaceAll(SEPARATOR_DIMEN, dimension + "");
        retValue = retValue.replaceAll(SEPARATOR_TYPE, "scatter");

        return  retValue;
    }

    public String makeLine(double[] x, double[] y, int dimension) {
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

        String retValue;
        retValue = mDefault.replaceAll(SEPARATOR_X, xStr);
        retValue = retValue.replaceAll(SEPARATOR_Y, yStr);
        retValue = retValue.replaceAll(SEPARATOR_ERROR, "");
        retValue = retValue.replaceAll(SEPARATOR_DIMEN, dimension + "");
        retValue = retValue.replaceAll(SEPARATOR_TYPE, "scatter");

        return  retValue;
    }

    public String makeTwoBar(String[] x, double[] y, double[] error, int dimension) {
        String xStr1 = "['" + x[0] + "']";
        String xStr2 = "['" + x[1] + "']";

        String yStr1 = "[" + y[0] + "]";
        String yStr2 = "[" + y[1] + "]";

        String errorStr1 = "[" + error[0] + "]";
        String errorStr2 = "[" + error[1] + "]";


        String retValue;
        retValue = mDefaultBar.replaceAll(SEPARATOR_X_1, xStr1);
        retValue = retValue.replaceAll(SEPARATOR_Y_1, yStr1);
        retValue = retValue.replaceAll(SEPARATOR_NAME_1, x[0]);
        retValue = retValue.replaceAll(SEPARATOR_ERROR_1, USE_ERROR);
        retValue = retValue.replaceAll(SEPARATOR_ERROR_CONTAIN, errorStr1);

        retValue = retValue.replaceAll(SEPARATOR_X_2, xStr2);
        retValue = retValue.replaceAll(SEPARATOR_Y_2, yStr2);
        retValue = retValue.replaceAll(SEPARATOR_NAME_2, x[1]);
        retValue = retValue.replaceAll(SEPARATOR_ERROR_2, USE_ERROR);
        retValue = retValue.replaceAll(SEPARATOR_ERROR_CONTAIN, errorStr2);
        retValue = retValue.replaceAll(SEPARATOR_DIMEN, dimension + "");

        return  retValue;
    }
}
