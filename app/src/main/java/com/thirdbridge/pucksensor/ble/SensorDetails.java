package com.thirdbridge.pucksensor.ble;

import java.util.UUID;

import static java.util.UUID.fromString;

public class SensorDetails {

	public final static UUID
		//Sensor tag
		UUID_ACC_SERV = fromString("f000aa10-0451-4000-b000-000000000000"),
		UUID_ACC_DATA = fromString("f000aa11-0451-4000-b000-000000000000"),
		UUID_ACC_CONF = fromString("f000aa12-0451-4000-b000-000000000000"),

		//Puck sensor
		UUID_PUCK_ACC_SERV = fromString("00002000-0000-1000-8000-00805f9b34fb"),
		UUID_PUCK_DATA = fromString("00002e00-0000-1000-8000-00805f9b34fb"),
		UUID_PUCK_WRITE = fromString("00002e01-0000-1000-8000-00805f9b34fb");
}
