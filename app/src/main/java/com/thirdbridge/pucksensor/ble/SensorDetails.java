/**************************************************************************************************
 * Filename:       SensorTag.java
 * Revised:        $Date: 2013-08-30 11:44:31 +0200 (fr, 30 aug 2013) $
 * Revision:       $Revision: 27454 $
 * <p/>
 * Copyright 2013 Texas Instruments Incorporated. All rights reserved.
 * <p/>
 * IMPORTANT: Your use of this Software is limited to those specific rights
 * granted under the terms of a software license agreement between the user
 * who downloaded the software, his/her employer (which must be your employer)
 * and Texas Instruments Incorporated (the "License").  You may not use this
 * Software unless you agree to abide by the terms of the License.
 * The License limits your use, and you acknowledge, that the Software may not be
 * modified, copied or distributed unless used solely and exclusively in conjunction
 * with a Texas Instruments Bluetooth device. Other than for the foregoing purpose,
 * you may not use, reproduce, copy, prepare derivative works of, modify, distribute,
 * perform, display or sell this Software and/or its documentation for any purpose.
 * <p/>
 * YOU FURTHER ACKNOWLEDGE AND AGREE THAT THE SOFTWARE AND DOCUMENTATION ARE
 * PROVIDED �AS IS" WITHOUT WARRANTY OF ANY KIND, EITHER EXPRESS OR IMPLIED,
 * INCLUDING WITHOUT LIMITATION, ANY WARRANTY OF MERCHANTABILITY, TITLE,
 * NON-INFRINGEMENT AND FITNESS FOR A PARTICULAR PURPOSE. IN NO EVENT SHALL
 * TEXAS INSTRUMENTS OR ITS LICENSORS BE LIABLE OR OBLIGATED UNDER CONTRACT,
 * NEGLIGENCE, STRICT LIABILITY, CONTRIBUTION, BREACH OF WARRANTY, OR OTHER
 * LEGAL EQUITABLE THEORY ANY DIRECT OR INDIRECT DAMAGES OR EXPENSES
 * INCLUDING BUT NOT LIMITED TO ANY INCIDENTAL, SPECIAL, INDIRECT, PUNITIVE
 * OR CONSEQUENTIAL DAMAGES, LOST PROFITS OR LOST DATA, COST OF PROCUREMENT
 * OF SUBSTITUTE GOODS, TECHNOLOGY, SERVICES, OR ANY CLAIMS BY THIRD PARTIES
 * (INCLUDING BUT NOT LIMITED TO ANY DEFENSE THEREOF), OR OTHER SIMILAR COSTS.
 * <p/>
 * Should you have any questions regarding your right to use this Software,
 * contact Texas Instruments Incorporated at www.TI.com
 **************************************************************************************************/
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
		UUID_PUCK_CONF = null;
}
