package com.thirdbridge.pucksensor.hardware;

import com.thirdbridge.pucksensor.controllers.HomeFragment;

/**
 * Created by Jayson Dalpé on 2016-04-27.
 * Protocol of communication between Slave (App) and Master (Puck)
 */
public class Protocol {
    /**
     * The Slave ask the settings.
     * Return to settings mode.
     */
    public static final int SETTINGS_MODE = 1;

    /**
     * The Master send the actual settings.
     */
    public static final int SETTINGS_READ = 2;

    /**
     * The Slave send the new settings to use.
     */
    public static final int SETTINGS_NEW = 3;

    /**
     * This sequence is a data sequence (Shot mode)
     */
    public static final int DATA = 4;

    /**
     * Slave ask data (shot mode)
     * Switching into Shot mode
     */
    public static final int DATA_READY = 5;
    public static final int SHOT_MODE = 5;

    /**
     * The Master finish sending a series of data. (Shot mode)
     */
    public static final int DATA_END = 6;

    /**
     * Te Master is starting to send relevant data, no draft. (Shot mode)
     */
    public static final int DATA_START = 8;

    /**
     * The Master is sending draft data. (Shot mode)
     */
    public static final int DATA_DRAFT = 10;

    /**
     * The Slave reset an exercice. (Stick mode)
     * Swicth to Stick handling Mode
     */
    public static final int STICK_START = 11;
    public static final int STICK_MODE = 11;

    /**
     * The Master send a StickHandling data
     */
    public static final int STICK_MOMENT = 12;

    /**
     * The App want free roaming data
     */
    public static final int FREE_MODE = 13;

    /**
     * The Puck send information about the calibration.
     */
    public static final int CALIB_OUTPUT = 14;

    /**
     * The App want the master to self calibrate his axis. Everybody know that the Puck as some problem with this...
     */
    public static final int CALIB_AXIS = 15;

    // PROTOCOL DEFAULT SETTINGS
    public static final byte VALIDITY_TOKEN = 0x3D;
    public static final int[] DEFAULT = {VALIDITY_TOKEN, 0x00, 0x00, 255, 0x00, 0x01, 0x01, 0x01, 0x00, 255}; //(2G)

    public static boolean isSameMode(int mode, int cmd) {
        switch(mode) {
            case SETTINGS_MODE:
                return SETTINGS_MODE==cmd || SETTINGS_READ==cmd || SETTINGS_NEW==cmd;
            case SHOT_MODE:
                return DATA==cmd || DATA_READY==cmd || DATA_END==cmd || DATA_START==cmd || DATA_DRAFT==cmd;
            case STICK_MODE:
                return  STICK_START==cmd || STICK_MOMENT==cmd;
        }
        return false;
    }

    public static void setDefault() {
        byte[] send = new byte[20];
        send[0] = Protocol.SETTINGS_NEW;
        send[1] = 0; //Battery, don't care
        for (int i=0; i<18; i++) {
            if (i < Protocol.DEFAULT.length) {
                send[2+i] = (byte)Protocol.DEFAULT[i];
            } else {
                send[2+i] = 0;
            }
        }
        try {
            HomeFragment.getInstance().writeBLE(send);
        } catch (Exception e) {

        }
    }

    public static double[] getAccelHighShot(byte[] values) {
        // According to protocol, byte 2-19
        double[] retValue;
        retValue = new double[3];
        int value = (values[2] & 0xFF);
        value |= (values[3] & 0xFF) << 8;
        retValue[0] = ((double)value * 400)/2048;

        value = (values[8] & 0xFF);
        value |= (values[9] & 0xFF) << 8;
        retValue[1] = ((double)value * 400)/2048;

        value = (values[14] & 0xFF);
        value |= (values[15] & 0xFF) << 8;
        retValue[2] = ((double)value * 400)/2048;
        return retValue;
    }

    public static double[] getAccelLowShot(byte[] values) {
        // According to protocol, byte 2-19
        double[] retValue;
        retValue = new double[3];

        int value = (values[4] & 0xFF);
        value |= (values[5] & 0xFF) << 8;
        retValue[0] = ((double)value * 0.012);

        value = (values[10] & 0xFF);
        value |= (values[11] & 0xFF) << 8;
        retValue[1] = ((double)value * 0.012);

        value = (values[16] & 0xFF);
        value |= (values[17] & 0xFF) << 8;
        retValue[2] = ((double)value * 0.012);
        return retValue;
    }

    public static double[] getGyroShot(byte[] values) {
        // According to protocol, byte 2-19
        double[] retValue;
        retValue = new double[3];

        int value = (values[6] & 0xFF);
        value |= (values[7] & 0xFF) << 8;
        retValue[0] = ((double)value * 1.0/*0.07*/);

        value = (values[12] & 0xFF);
        value |= (values[13] & 0xFF) << 8;
        retValue[1] = ((double)value * 1.0/*0.07*/);

        value = (values[18] & 0xFF);
        value |= (values[19] & 0xFF) << 8;
        retValue[2] = ((double)value * 1.0/*0.07*/);
        return retValue;
    }

    public static double getAccelSH(byte low, byte high) {
        double retValue;
        int value = (low & 0xFF);
        value |= (high & 0xFF) << 8;
        boolean negative = false;
        boolean lowAccel = false;
        if ((value & (1<<14)) > 1) {
            value -= (1<<14);
            negative = true;
        }
        if ((value & (1<<15)) > 1) {
            value -= (1<<15);
            lowAccel = true;
        }

        if (lowAccel) {
            retValue = (double)value * 0.012;
        } else {
            retValue = ((double)value * 400)/2048;
        }

        if (negative) {
            retValue *= -1;
        }

        return retValue;
    }

    public static double getDirectionSH(byte angle) {
        return (double) angle;
    }

    public static double getRotationSH(byte low, byte high) {
        int value = (low & 0xFF);
        value |= (high & 0xFF) << 8;
        return  ((double)value * 0.07);
    }

    public static double getTickSH(byte low, byte high, double step) {
        int value = (low & 0xFF);
        value |= (high & 0xFF) << 8;

        return (double) value * step;
    }


}
