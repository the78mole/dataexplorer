package gde.device.junsi.modbus;

import gde.io.DataParser;

public class ChargerSystem {
	final static short	MODEL_MAX						= 2;

	short								TempUnit;
	short								TempStop;
	short								TempFansOn;
	short								TempReduce;
	short								FansSpeed;
	short								FansOffDelay;
	short								LcdContraste;
	short								LightValue;
	short								BeepMode;
	short[]							BeepType						= new short[4];
	short[]							BeepEnable					= new short[4];
	short[]							BeepVOL							= new short[4];
	short								DoneBeepType;
	short								SelectAdj;
	short								Ver;
	short								SelInputSource;
	short								DCInputLowVolt;
	short								DCInputOverVolt;
	short								DCInputCurrentLimit;
	short								BatInputLowVolt;
	short								BatInputOverVolt;
	short								BatInputCurrentLimit;
	short								RegEnable;
	short								RegVoltLimit;
	short								RegCurrentLimit;
	short[]							ChargePower					= new short[MODEL_MAX];
	short[]							DischargePower			= new short[MODEL_MAX];
	short								ProPower;
	short[]							MonitorLogInterval	= new short[MODEL_MAX];
	short[]							MonitorLogSaveToSD	= new short[MODEL_MAX];

	short								ServoType;
	short								ServoUserCenter;
	short								ServoUserRate;
	short								ServoUserOpAngle;

	short								ModBusMode;
	short								ModBusAddr;

	short[]							Dump								= new short[10];

	//construct System data using received data array
	ChargerSystem(final byte[] readSystemData) {
		this.TempUnit = DataParser.parse2Short(readSystemData[0], readSystemData[1]);
		this.TempStop = DataParser.parse2Short(readSystemData[2], readSystemData[3]);
		this.TempFansOn = DataParser.parse2Short(readSystemData[4], readSystemData[5]);
		this.TempReduce = DataParser.parse2Short(readSystemData[6], readSystemData[7]);
		this.FansSpeed = DataParser.parse2Short(readSystemData[8], readSystemData[9]);
		this.FansOffDelay = DataParser.parse2Short(readSystemData[10], readSystemData[11]);
		this.LcdContraste = DataParser.parse2Short(readSystemData[12], readSystemData[13]);
		this.LightValue = DataParser.parse2Short(readSystemData[14], readSystemData[15]);
		this.BeepMode = DataParser.parse2Short(readSystemData[16], readSystemData[17]);
		this.BeepType[0] = DataParser.parse2Short(readSystemData[18], readSystemData[19]);
		this.BeepType[1] = DataParser.parse2Short(readSystemData[20], readSystemData[21]);
		this.BeepType[2] = DataParser.parse2Short(readSystemData[22], readSystemData[23]);
		this.BeepType[3] = DataParser.parse2Short(readSystemData[24], readSystemData[25]);
		this.BeepEnable[0] = DataParser.parse2Short(readSystemData[26], readSystemData[27]);
		this.BeepEnable[1] = DataParser.parse2Short(readSystemData[28], readSystemData[29]);
		this.BeepEnable[2] = DataParser.parse2Short(readSystemData[30], readSystemData[31]);
		this.BeepEnable[3] = DataParser.parse2Short(readSystemData[32], readSystemData[33]);
		this.BeepVOL[0] = DataParser.parse2Short(readSystemData[34], readSystemData[35]);
		this.BeepVOL[1] = DataParser.parse2Short(readSystemData[36], readSystemData[37]);
		this.BeepVOL[2] = DataParser.parse2Short(readSystemData[38], readSystemData[39]);
		this.BeepVOL[3] = DataParser.parse2Short(readSystemData[40], readSystemData[41]);
		this.DoneBeepType = DataParser.parse2Short(readSystemData[42], readSystemData[43]);
		this.SelectAdj = DataParser.parse2Short(readSystemData[44], readSystemData[45]);
		this.Ver = DataParser.parse2Short(readSystemData[46], readSystemData[47]);
		this.SelInputSource = DataParser.parse2Short(readSystemData[48], readSystemData[49]);
		this.DCInputLowVolt = DataParser.parse2Short(readSystemData[50], readSystemData[51]);
		this.DCInputOverVolt = DataParser.parse2Short(readSystemData[52], readSystemData[53]);
		this.DCInputCurrentLimit = DataParser.parse2Short(readSystemData[54], readSystemData[55]);
		this.BatInputLowVolt = DataParser.parse2Short(readSystemData[56], readSystemData[57]);
		this.BatInputOverVolt = DataParser.parse2Short(readSystemData[58], readSystemData[59]);
		this.BatInputCurrentLimit = DataParser.parse2Short(readSystemData[60], readSystemData[61]);
		this.RegEnable = DataParser.parse2Short(readSystemData[62], readSystemData[63]);
		this.RegVoltLimit = DataParser.parse2Short(readSystemData[64], readSystemData[65]);
		this.RegCurrentLimit = DataParser.parse2Short(readSystemData[66], readSystemData[67]);
		this.ChargePower[0] = DataParser.parse2Short(readSystemData[68], readSystemData[69]);
		this.ChargePower[1] = DataParser.parse2Short(readSystemData[70], readSystemData[71]);
		this.DischargePower[0] = DataParser.parse2Short(readSystemData[72], readSystemData[73]);
		this.DischargePower[1] = DataParser.parse2Short(readSystemData[74], readSystemData[75]);
		this.ProPower = DataParser.parse2Short(readSystemData[76], readSystemData[77]);
		this.MonitorLogInterval[0] = DataParser.parse2Short(readSystemData[78], readSystemData[79]);
		this.MonitorLogInterval[1] = DataParser.parse2Short(readSystemData[80], readSystemData[81]);
		this.MonitorLogSaveToSD[0] = DataParser.parse2Short(readSystemData[82], readSystemData[83]);
		this.MonitorLogSaveToSD[1] = DataParser.parse2Short(readSystemData[84], readSystemData[85]);

		this.ServoType = DataParser.parse2Short(readSystemData[86], readSystemData[87]);
		this.ServoUserCenter = DataParser.parse2Short(readSystemData[88], readSystemData[89]);
		this.ServoUserRate = DataParser.parse2Short(readSystemData[90], readSystemData[91]);
		this.ServoUserOpAngle = DataParser.parse2Short(readSystemData[92], readSystemData[93]);

		this.ModBusMode = DataParser.parse2Short(readSystemData[94], readSystemData[95]);
		this.ModBusAddr = DataParser.parse2Short(readSystemData[96], readSystemData[97]);

		this.Dump[0] = DataParser.parse2Short(readSystemData[98], readSystemData[99]);
		this.Dump[1] = DataParser.parse2Short(readSystemData[100], readSystemData[101]);
		this.Dump[2] = DataParser.parse2Short(readSystemData[102], readSystemData[103]);
		this.Dump[3] = DataParser.parse2Short(readSystemData[104], readSystemData[105]);
		this.Dump[4] = DataParser.parse2Short(readSystemData[106], readSystemData[107]);
		this.Dump[5] = DataParser.parse2Short(readSystemData[108], readSystemData[109]);
		this.Dump[6] = DataParser.parse2Short(readSystemData[110], readSystemData[111]);
		this.Dump[7] = DataParser.parse2Short(readSystemData[112], readSystemData[113]);
		this.Dump[8] = DataParser.parse2Short(readSystemData[114], readSystemData[115]);
		this.Dump[9] = DataParser.parse2Short(readSystemData[116], readSystemData[117]);
	}
	
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(this.getClass().getSimpleName()).append(":\n");
		sb.append(String.format("TempUnit \t\t= %d", TempUnit)).append("\n");
		sb.append(String.format("TempStop \t\t= %d", TempStop)).append("\n");
		sb.append(String.format("TempFansOn	\t= %d", TempFansOn)).append("\n");
		sb.append(String.format("TempReduce	\t= %d", TempReduce)).append("\n");
		sb.append(String.format("FansSpeed \t\t= %d", FansSpeed)).append("\n");
		sb.append(String.format("FansOffDelay \t\t= %d", FansOffDelay)).append("\n");
		sb.append(String.format("LcdContraste \t\t= %d", LcdContraste)).append("\n");
		sb.append(String.format("LightValue \t\t= %d", LightValue)).append("\n");
		sb.append(String.format("BeepMode \t\t= %d", BeepMode)).append("\n");
		sb.append(String.format("BeepType \t\t= [%d, %d, %d, %d]", this.BeepType[0], this.BeepType[1], this.BeepType[2], this.BeepType[3])).append("\n");
		sb.append(String.format("BeepEnable \t\t= [%d, %d, %d, %d]", this.BeepEnable[0], this.BeepEnable[1], this.BeepEnable[2], this.BeepEnable[3])).append("\n");
		sb.append(String.format("BeepVOL \t\t= [%d, %d, %d, %d]", this.BeepVOL[0], this.BeepVOL[1], this.BeepVOL[2], this.BeepVOL[3])).append("\n");
		sb.append(String.format("DoneBeepType \t\t= %d", DoneBeepType)).append("\n");
		sb.append(String.format("SelectAdj \t\t= %d", SelectAdj)).append("\n");
		sb.append(String.format("Ver \t\t\t= %d", Ver)).append("\n");
		sb.append(String.format("SelInputSource	\t= %d", SelInputSource)).append("\n");
		sb.append(String.format("DCInputLowVolt	\t= %d", DCInputLowVolt)).append("\n");
		sb.append(String.format("DCInputOverVolt \t= %d", DCInputOverVolt)).append("\n");
		sb.append(String.format("DCInputCurrentLimit \t= %d", DCInputCurrentLimit)).append("\n");
		sb.append(String.format("BatInputLowVolt \t= %d", BatInputLowVolt)).append("\n");
		sb.append(String.format("BatInputOverVolt	= %d", BatInputOverVolt)).append("\n");
		sb.append(String.format("BatInputCurrentLimit	= %d", BatInputCurrentLimit)).append("\n");
		sb.append(String.format("RegEnable \t\t= %d", RegEnable)).append("\n");
		sb.append(String.format("RegVoltLimit	\t= %d", RegVoltLimit)).append("\n");
		sb.append(String.format("RegCurrentLimit \t= %d", RegCurrentLimit)).append("\n");
		sb.append(String.format("ChargePower \t\t= [%d, %d]", this.ChargePower[0], this.ChargePower[1])).append("\n");
		sb.append(String.format("DischargePower \t\t= [%d, %d]", this.DischargePower[0], this.DischargePower[1])).append("\n");
		sb.append(String.format("ProPower \t\t= %d", BatInputLowVolt)).append("\n");
		sb.append(String.format("MonitorLogInterval \t= [%d, %d]", this.MonitorLogInterval[0], this.MonitorLogInterval[1])).append("\n");
		sb.append(String.format("MonitorLogSaveToSD \t= [%d, %d]", this.MonitorLogSaveToSD[0], this.MonitorLogSaveToSD[1])).append("\n");

		sb.append(String.format("ServoType \t\t= %d", ServoType)).append("\n");
		sb.append(String.format("ServoUserCenter \t= %d", ServoUserCenter)).append("\n");
		sb.append(String.format("ServoUserRate \t\t= %d", ServoUserRate)).append("\n");
		sb.append(String.format("ServoUserOpAngle \t= %d", ServoUserOpAngle)).append("\n");

		sb.append(String.format("ModBusMode \t\t= %d", ModBusMode)).append("\n");
		sb.append(String.format("ModBusAddr \t\t= %d", ModBusAddr)).append("\n");

		sb.append(String.format("Dump \t\t\t= [%d, %d, %d, %d ,%d, %d, %d, %d, %d, %d]", this.Dump[0], this.Dump[1], this.Dump[2], this.Dump[3], this.Dump[4], this.Dump[5], this.Dump[6], this.Dump[7], this.Dump[8], this.Dump[9])).append("\n");
		return sb.toString();
	}
	
	final static int		size								= 59*2; //size in byte

	public static int getSize() {
		return size;
	}

	public short getTempUnit() {
		return TempUnit;
	}

	public void setTempUnit(short tempUnit) {
		TempUnit = tempUnit;
	}

	public short getTempStop() {
		return TempStop;
	}

	public void setTempStop(short tempStop) {
		TempStop = tempStop;
	}

	public short getTempFansOn() {
		return TempFansOn;
	}

	public void setTempFansOn(short tempFansOn) {
		TempFansOn = tempFansOn;
	}

	public short getTempReduce() {
		return TempReduce;
	}

	public void setTempReduce(short tempReduce) {
		TempReduce = tempReduce;
	}

	public short getFansSpeed() {
		return FansSpeed;
	}

	public void setFansSpeed(short fansSpeed) {
		FansSpeed = fansSpeed;
	}

	public short getFansOffDelay() {
		return FansOffDelay;
	}

	public void setFansOffDelay(short fansOffDelay) {
		FansOffDelay = fansOffDelay;
	}

	public short getLcdContraste() {
		return LcdContraste;
	}

	public void setLcdContraste(short lcdContraste) {
		LcdContraste = lcdContraste;
	}

	public short getLightValue() {
		return LightValue;
	}

	public void setLightValue(short lightValue) {
		LightValue = lightValue;
	}

	public short getBeepMode() {
		return BeepMode;
	}

	public void setBeepMode(short beepMode) {
		BeepMode = beepMode;
	}

	public short[] getBeepType() {
		return BeepType;
	}

	public void setBeepType(short[] beepType) {
		BeepType = beepType;
	}

	public short[] getBeepEnable() {
		return BeepEnable;
	}

	public void setBeepEnable(short[] beepEnable) {
		BeepEnable = beepEnable;
	}

	public short[] getBeepVOL() {
		return BeepVOL;
	}

	public void setBeepVOL(short[] beepVOL) {
		BeepVOL = beepVOL;
	}

	public short getDoneBeepType() {
		return DoneBeepType;
	}

	public void setDoneBeepType(short doneBeepType) {
		DoneBeepType = doneBeepType;
	}

	public short getSelectAdj() {
		return SelectAdj;
	}

	public void setSelectAdj(short selectAdj) {
		SelectAdj = selectAdj;
	}

	public short getVer() {
		return Ver;
	}

	public void setVer(short ver) {
		Ver = ver;
	}

	public short getSelInputSource() {
		return SelInputSource;
	}

	public void setSelInputSource(short selInputSource) {
		SelInputSource = selInputSource;
	}

	public short getDCInputLowVolt() {
		return DCInputLowVolt;
	}

	public void setDCInputLowVolt(short dCInputLowVolt) {
		DCInputLowVolt = dCInputLowVolt;
	}

	public short getDCInputOverVolt() {
		return DCInputOverVolt;
	}

	public void setDCInputOverVolt(short dCInputOverVolt) {
		DCInputOverVolt = dCInputOverVolt;
	}

	public short getDCInputCurrentLimit() {
		return DCInputCurrentLimit;
	}

	public void setDCInputCurrentLimit(short dCInputCurrentLimit) {
		DCInputCurrentLimit = dCInputCurrentLimit;
	}

	public short getBatInputLowVolt() {
		return BatInputLowVolt;
	}

	public void setBatInputLowVolt(short batInputLowVolt) {
		BatInputLowVolt = batInputLowVolt;
	}

	public short getBatInputOverVolt() {
		return BatInputOverVolt;
	}

	public void setBatInputOverVolt(short batInputOverVolt) {
		BatInputOverVolt = batInputOverVolt;
	}

	public short getBatInputCurrentLimit() {
		return BatInputCurrentLimit;
	}

	public void setBatInputCurrentLimit(short batInputCurrentLimit) {
		BatInputCurrentLimit = batInputCurrentLimit;
	}

	public short getRegEnable() {
		return RegEnable;
	}

	public void setRegEnable(short regEnable) {
		RegEnable = regEnable;
	}

	public short getRegVoltLimit() {
		return RegVoltLimit;
	}

	public void setRegVoltLimit(short regVoltLimit) {
		RegVoltLimit = regVoltLimit;
	}

	public short getRegCurrentLimit() {
		return RegCurrentLimit;
	}

	public void setRegCurrentLimit(short regCurrentLimit) {
		RegCurrentLimit = regCurrentLimit;
	}

	public short[] getChargePower() {
		return ChargePower;
	}

	public void setChargePower(short[] chargePower) {
		ChargePower = chargePower;
	}

	public short[] getDischargePower() {
		return DischargePower;
	}

	public void setDischargePower(short[] dischargePower) {
		DischargePower = dischargePower;
	}

	public short getProPower() {
		return ProPower;
	}

	public void setProPower(short proPower) {
		ProPower = proPower;
	}

	public short[] getMonitorLogInterval() {
		return MonitorLogInterval;
	}

	public void setMonitorLogInterval(short[] monitorLogInterval) {
		MonitorLogInterval = monitorLogInterval;
	}

	public short[] getMonitorLogSaveToSD() {
		return MonitorLogSaveToSD;
	}

	public void setMonitorLogSaveToSD(short[] monitorLogSaveToSD) {
		MonitorLogSaveToSD = monitorLogSaveToSD;
	}

	public short getServoType() {
		return ServoType;
	}

	public void setServoType(short servoType) {
		ServoType = servoType;
	}

	public short getServoUserCenter() {
		return ServoUserCenter;
	}

	public void setServoUserCenter(short servoUserCenter) {
		ServoUserCenter = servoUserCenter;
	}

	public short getServoUserRate() {
		return ServoUserRate;
	}

	public void setServoUserRate(short servoUserRate) {
		ServoUserRate = servoUserRate;
	}

	public short getServoUserOpAngle() {
		return ServoUserOpAngle;
	}

	public void setServoUserOpAngle(short servoUserOpAngle) {
		ServoUserOpAngle = servoUserOpAngle;
	}

	public short getModBusMode() {
		return ModBusMode;
	}

	public void setModBusMode(short modBusMode) {
		ModBusMode = modBusMode;
	}

	public short getModBusAddr() {
		return ModBusAddr;
	}

	public void setModBusAddr(short modBusAddr) {
		ModBusAddr = modBusAddr;
	}

	public short[] getDump() {
		return Dump;
	}

	public void setDump(short[] dump) {
		Dump = dump;
	}

}
