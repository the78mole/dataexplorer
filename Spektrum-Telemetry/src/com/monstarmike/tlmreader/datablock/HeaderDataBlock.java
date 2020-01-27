package com.monstarmike.tlmreader.datablock;

import com.google.common.primitives.Shorts;
import com.monstarmike.tlmreader.datablock.DataBlock.SensorGroup;

public class HeaderDataBlock extends HeaderBlock {
	boolean terminatingBlock = false;
	private short sensorType;

	public HeaderDataBlock(byte[] rawData) {
		super(rawData);
		decode(rawData);
	}

	public String getSensorTypeEnabled() {
		byte[] sensorTypeBytes = Shorts.toByteArray(sensorType);
		if (sensorTypeBytes[0x00] != sensorTypeBytes[0x01]) {
			return "invalid information! 0x4 does not equal 0x5";
		}
		return SensorGroup.fromSensorByte(sensorTypeBytes[0x00]).getName();
	}

	public boolean isTerminatingBlock() {
		return terminatingBlock;
	}

	private void decode(byte[] rawData) {
		sensorType = Shorts.fromBytes(rawData[4], rawData[5]);
	}

	@Override
	public String toString() {
		return "DataHeader; type: " + getSensorTypeEnabled();
	}

}
