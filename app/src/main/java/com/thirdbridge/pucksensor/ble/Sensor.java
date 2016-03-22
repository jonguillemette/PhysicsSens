package com.thirdbridge.pucksensor.ble;


import java.util.UUID;

import static com.thirdbridge.pucksensor.ble.SensorDetails.UUID_ACC_CONF;
import static com.thirdbridge.pucksensor.ble.SensorDetails.UUID_ACC_DATA;
import static com.thirdbridge.pucksensor.ble.SensorDetails.UUID_ACC_SERV;
import static com.thirdbridge.pucksensor.ble.SensorDetails.UUID_PUCK_ACC_SERV;
import static com.thirdbridge.pucksensor.ble.SensorDetails.UUID_PUCK_DATA;

/**
 * This enum encapsulates the differences amongst the sensors. The differences include UUID values and how to interpret the
 * characteristic-containing-measurement.
 *
 * Modified by Jayson Dalpé since 2016-01-22.
 *
 */
public enum Sensor {

	ACCELEROMETER(UUID_ACC_SERV, UUID_ACC_DATA, UUID_ACC_CONF),
	PUCK_ACCELEROMETER(UUID_PUCK_ACC_SERV, UUID_PUCK_DATA, UUID_ACC_CONF);


	private final UUID service, data, config;
	private byte enableCode; // See getEnableSensorCode for explanation.
	public static final byte ENABLE_SENSOR_CODE = 1;

	/**
	 * Constructor called by all the sensors except Gyroscope
	 */
	private Sensor(UUID service, UUID data, UUID config) {
		this.service = service;
		this.data = data;
		this.config = config;
		this.enableCode = ENABLE_SENSOR_CODE; // This is the sensor enable code for all sensors except the gyroscope
	}

	public UUID getService() {
		return service;
	}

	public UUID getData() {
		return data;
	}

	public UUID getConfig() {
		return config;
	}

	public static final Sensor[] SENSOR_LIST = {PUCK_ACCELEROMETER};
}
