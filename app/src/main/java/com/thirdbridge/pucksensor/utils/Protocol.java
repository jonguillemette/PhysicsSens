package com.thirdbridge.pucksensor.utils;

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
}
