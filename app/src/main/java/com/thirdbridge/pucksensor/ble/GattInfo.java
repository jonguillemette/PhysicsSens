package com.thirdbridge.pucksensor.ble;

import java.util.UUID;

public class GattInfo {
    //Use the same ID as TI spec, but no proprietary code
    public static final UUID CLIENT_CHARACTERISTIC_CONFIG = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    public static final UUID OAD_SERVICE_UUID = UUID.fromString("f000ffc0-0451-4000-b000-000000000000");
    public static final UUID CC_SERVICE_UUID = UUID.fromString("f000ccc0-0451-4000-b000-000000000000");

}
