package com.thirdbridge.pucksensor.ble;

import android.bluetooth.BluetoothDevice;

public class BLEDeviceInfo {
  private BluetoothDevice mDevice;
  private int mRssi;

  public BLEDeviceInfo(BluetoothDevice device, int rssi) {
    mDevice = device;
    mRssi = rssi;
  }


  public int getRssi() {
    return mRssi;
  }

  public void setRssi(int newRssi) {
    mRssi = newRssi;
  }

  public BluetoothDevice getDevice() {
    return mDevice;
  }
}