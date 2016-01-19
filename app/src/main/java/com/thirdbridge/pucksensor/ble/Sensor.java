package com.thirdbridge.pucksensor.ble;

//import static android.bluetooth.BluetoothGattCharacteristic.FORMAT_UINT8;

import android.bluetooth.BluetoothGattCharacteristic;

import com.thirdbridge.pucksensor.utils.ble_utils.Point2D;
import com.thirdbridge.pucksensor.utils.ble_utils.Point3D;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.UUID;

import static com.thirdbridge.pucksensor.ble.SensorDetails.UUID_ACC_CONF;
import static com.thirdbridge.pucksensor.ble.SensorDetails.UUID_ACC_DATA;
import static com.thirdbridge.pucksensor.ble.SensorDetails.UUID_ACC_SERV;
import static com.thirdbridge.pucksensor.ble.SensorDetails.UUID_PUCK_ACC_SERV;
import static com.thirdbridge.pucksensor.ble.SensorDetails.UUID_PUCK_DATA;

/**
 * This enum encapsulates the differences amongst the sensors. The differences include UUID values and how to interpret the
 * characteristic-containing-measurement.
 */
public enum Sensor {

	ACCELEROMETER(UUID_ACC_SERV, UUID_ACC_DATA, UUID_ACC_CONF) {
		@Override
		public Point3D convert(final byte[] value) {
			/*
			 * The accelerometer has the range [-2g, 2g] with unit (1/64)g.
			 *
			 * To convert from unit (1/64)g to unit g we divide by 64.
			 *
			 * (g = 9.81 m/s^2)
			 *
			 * The z value is multiplied with -1 to coincide with how we have arbitrarily defined the positive y direction. (illustrated by the apps accelerometer
			 * image)
			 */
			Integer x = (int) value[0];
			Integer y = (int) value[1];
			Integer z = (int) value[2] * -1;

			return new Point3D(x / 64.0, y / 64.0, z / 64.0);
		}
	},
	PUCK_ACCELEROMETER(UUID_PUCK_ACC_SERV, UUID_PUCK_DATA, UUID_ACC_CONF) {
		@Override
		public Point3D convertLowAccel(final byte[] value) {

			//Log.i("Sensor data", Arrays.toString(value));

			byte[] lowAccelBytesX = {value[6],value[7]};
			byte[] lowAccelBytesY = {value[8],value[9]};
			byte[] lowAccelBytesZ = {value[10],value[11]};

			ByteBuffer bufferX = ByteBuffer.wrap(lowAccelBytesX);
			ByteBuffer bufferY = ByteBuffer.wrap(lowAccelBytesY);
			ByteBuffer bufferZ = ByteBuffer.wrap(lowAccelBytesZ);

			bufferX.order(ByteOrder.LITTLE_ENDIAN);
			bufferY.order(ByteOrder.LITTLE_ENDIAN);
			bufferZ.order(ByteOrder.LITTLE_ENDIAN);

			//Shift 4 bits to the right because the data is only on 12 bits
			int lowAccelDataX = bufferX.getShort() >> 4;
			int lowAccelDataY = bufferY.getShort() >> 4;
			int lowAccelDataZ = bufferZ.getShort() >> 4;

			double lowAccelX = Math.round(convertLowAccelIntegerToG(lowAccelDataX)*100.0)/100.0;
			double lowAccelY = Math.round(convertLowAccelIntegerToG(lowAccelDataY)*100.0)/100.0;
			double lowAccelZ = Math.round(convertLowAccelIntegerToG(lowAccelDataZ)*100.0)/100.0;

			//double accelX =  convertLowAccelIntegerToG(shortSignedAtOffset(value, 6) >> 4);
			//double accelY =  convertLowAccelIntegerToG(shortSignedAtOffset(value, 8) >> 4);
			//double accelZ =  convertLowAccelIntegerToG(shortSignedAtOffset(value,10) >> 4);

			double accelZ =  convertLowAccelIntegerToG(shortSignedAtOffset(value,6) >> 4);
			//shortSignedAtOffset(value,6);

			//Log.i("sensor data","Byte buffer value: " + lowAccelX + " homeMade value: " + accelX);
//			Log.i("Sensor Data", "Short before right shift: " + String.format("%8s", Integer.toBinaryString(shortSignedAtOffset(value,10)).replace(' ','0')) + " Short after right shift: " + Integer.toBinaryString((shortSignedAtOffset(value,10) >> 4)).replace(' ', '0'));

			return new Point3D(lowAccelX, lowAccelY, lowAccelZ);
		}

		@Override
		public Point2D convertHighAccel(final byte[] value) {

			byte[] highAccelBytesX = {value[2],value[3]};
			byte[] highAccelBytesY = {value[4],value[5]};

			ByteBuffer bufferX = ByteBuffer.wrap(highAccelBytesX);
			ByteBuffer bufferY = ByteBuffer.wrap(highAccelBytesY);

			bufferX.order(ByteOrder.LITTLE_ENDIAN);
			bufferY.order(ByteOrder.LITTLE_ENDIAN);

			//Shift 4 bits to the right because the data is only on 12 bits
			int highAccelDataX = bufferX.getShort() >> 4;
			int highAccelDataY = bufferY.getShort() >> 4;

			double highAccelX = Math.round(convertHighAccelIntegerToG(highAccelDataX)*100.0)/100.0;
			double highAccelY = Math.round(convertHighAccelIntegerToG(highAccelDataY)*100.0)/100.0;

			return new Point2D(highAccelX, highAccelY);
		}

		@Override
		public double convertRotation(final byte[] value) {

			/*Integer unsignedRotation = shortUnsignedAtOffset(value, 12);
			Integer signedRotation = fromTwoComplement(unsignedRotation, 16);
			double rotation = convertGyroIntegerToDegsPerSecond(signedRotation);*/

			byte[] rotationBytes = {value[12],value[13]};

			ByteBuffer bufferRotation = ByteBuffer.wrap(rotationBytes);
			bufferRotation.order(ByteOrder.LITTLE_ENDIAN);

			int rotationData = bufferRotation.getShort() >> 4;
			double rotation = convertRotationIntegerToDegsPerSecond(rotationData);

			return rotation;
		}
	};

	//Low acceleration values varie from +-2048 and must be transformed to +-16g
	public double convertLowAccelIntegerToG(Integer accelerationValue){

		double maxValue = 2048.00;
		double maxGravity = 16.00;
		double calculatedValue = ((double)accelerationValue * 16)/2048;
		return calculatedValue;
	}

	//High acceleration values varie from +-2048 and must be transformed to +-400g
	public double convertHighAccelIntegerToG(Integer accelerationValue){
		return (accelerationValue * 400)/2048;
	}

	//Gyroscope values varie from +-2048 and must be transformed to +-16g
	public double convertRotationIntegerToDegsPerSecond(Integer rotationValue){
		return (rotationValue * 2000)/32767;
	}

	private static short shortSignedAtOffset(byte[] c, int offset) {
		int lowerByte = c[offset];
		int upperByte = c[offset + 1];

		//Log.i("Sensor Data", "Upper byte before shift: " + String.format("%8s", Integer.toBinaryString(upperByte & 0xff)).replace(' ', '0') + " Shifted Upper byte: " + String.format("%16s", Integer.toBinaryString((upperByte << 8) & 0xff00)).replace(' ', '0') + " Lower byte: " + String.format("%8s", Integer.toBinaryString(lowerByte & 0xFF)).replace(' ', '0') + " Combined bytes: " + String.format("%16s", Integer.toBinaryString((((upperByte << 8) & 0xff00) | lowerByte & 0xff) & 0xffff)).replace(' ', '0') + " Combined bytes right shifted: " + String.format("%16s", Integer.toBinaryString(((((upperByte << 8) & 0xff00) | lowerByte & 0xff) & 0xffff) >> 4)).replace(' ', '0') + " Short value: " + String.valueOf((short)(((upperByte << 8) | lowerByte & 0xff)  >> 4)));

		return (short)((upperByte  << 8)| lowerByte & 0xff);
	}

	/**
	 * Expects the value to contain a 2-complement number stored in the
	 * least significant bits.
	 * The contents of the most significant bits is ignored.
	 *
	 * @param value   the value to convert
	 * @param bitSize the size of the 2 complement number in the value
	 * @return the converted value
	 */
	public static int fromTwoComplement(int value, int bitSize) {
		int shift = Integer.SIZE - bitSize;
		// shift sign into position
		int result = value << shift;
		// Java right shift uses sign extension, but only works on integers or longs
		result = result >> shift;
		return result;
	}


	public void onCharacteristicChanged(BluetoothGattCharacteristic c) {
		throw new UnsupportedOperationException("Programmer error, the individual enum classes are supposed to override this method.");
	}

	public Point3D convertLowAccel(byte[] value) {
		throw new UnsupportedOperationException("Programmer error, the individual enum classes are supposed to override this method.");
	}

	public Point2D convertHighAccel(byte[] value) {
		throw new UnsupportedOperationException("Programmer error, the individual enum classes are supposed to override this method.");
	}

	public double convertRotation(byte[] value) {
		throw new UnsupportedOperationException("Programmer error, the individual enum classes are supposed to override this method.");
	}


	public Point3D convert(byte[] value) {
		throw new UnsupportedOperationException("Programmer error, the individual enum classes are supposed to override this method.");
	}

	private final UUID service, data, config;
	private byte enableCode; // See getEnableSensorCode for explanation.
	public static final byte DISABLE_SENSOR_CODE = 0;
	public static final byte ENABLE_SENSOR_CODE = 1;
	public static final byte CALIBRATE_SENSOR_CODE = 2;

	/**
	 * Constructor called by the Gyroscope because he needs a different enable
	 * code.
	 */
	private Sensor(UUID service, UUID data, UUID config, byte enableCode) {
		this.service = service;
		this.data = data;
		this.config = config;
		this.enableCode = enableCode;
	}

	/**
	 * Constructor called by all the sensors except Gyroscope
	 */
	private Sensor(UUID service, UUID data, UUID config) {
		this.service = service;
		this.data = data;
		this.config = config;
		this.enableCode = ENABLE_SENSOR_CODE; // This is the sensor enable code for all sensors except the gyroscope
	}

	/**
	 * @return the code which, when written to the configuration characteristic, turns on the sensor.
	 */
	public byte getEnableSensorCode() {
		return enableCode;
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

	public static Sensor getFromDataUuid(UUID uuid) {
		for (Sensor s : Sensor.values()) {
			if (s.getData().equals(uuid)) {
				return s;
			}
		}
		throw new RuntimeException("Programmer error, unable to find uuid.");
	}

	public static final Sensor[] SENSOR_LIST = {PUCK_ACCELEROMETER};
}
