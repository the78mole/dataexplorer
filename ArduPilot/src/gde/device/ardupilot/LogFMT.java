package gde.device.ardupilot;

import java.util.ArrayList;
import java.util.List;

import gde.GDE;

/**
 * Log entry formating
 * GPS, BIBcLLeeEe, Status,Time,NSats,HDop,Lat,Lng,RelAlt,Alt,Spd,GCrs
 */
public class LogFMT {
	String name;
	List<String> fmts;
	List<String> measurementNames;
	List<String> symbols;
	List<String> units;
	List<Double> mults;
	
	public LogFMT(final String fmtEntry, final String separator) {
		String[] entries = fmtEntry.split(", ");
		this.name = entries[3];

		this.measurementNames = new ArrayList<>();
		for (String string : entries[5].split(separator)) {
			this.measurementNames.add(String.format("%s_%s", this.name, string));
		}
		
		this.fmts = new ArrayList<>();
		for (int i = 0; i < this.measurementNames.size(); i++) {
			String measurementName = this.measurementNames.get(i).contains(GDE.STRING_UNDER_BAR) ? this.measurementNames.get(i).split(GDE.STRING_UNDER_BAR)[1] : this.measurementNames.get(i);
			switch (ArduPilot.LogEntry.getLogEntry(this.name)) {
			case ACC:
				this.fmts.add(ArduPilot.ACC.getFmt(measurementName));
				break;
			case ARSP:
				this.fmts.add(ArduPilot.ARSP.getFmt(measurementName));
				break;
			case BARO:
				this.fmts.add(ArduPilot.BARO.getFmt(measurementName));
				break;
			case CURR:
				this.fmts.add(ArduPilot.CURR.getFmt(measurementName));
				break;
			case CURR_CELL:
				this.fmts.add(ArduPilot.CURR_CELL.getFmt(measurementName));
				break;
			case ESC:
				this.fmts.add(ArduPilot.ESC.getFmt(measurementName));
				break;
			case GPA:
				this.fmts.add(ArduPilot.GPA.getFmt(measurementName));
				break;
			case GPS:
				this.fmts.add(ArduPilot.GPS.getFmt(measurementName));
				break;
			case GYR:
				this.fmts.add(ArduPilot.GYR.getFmt(measurementName));
				break;
			case IMT:
				this.fmts.add(ArduPilot.IMT.getFmt(measurementName));
				break;
			case IMU:
				this.fmts.add(ArduPilot.IMU.getFmt(measurementName));
				break;
			case ISBD:
				this.fmts.add(ArduPilot.ISBD.getFmt(measurementName));
				break;
			case ISBH:
				this.fmts.add(ArduPilot.ISBH.getFmt(measurementName));
				break;
			case MAG:
				this.fmts.add(ArduPilot.MAG.getFmt(measurementName));
				break;
			case PID:
				this.fmts.add(ArduPilot.PID.getFmt(measurementName));
				break;
			case QUAT:
				this.fmts.add(ArduPilot.QUAT.getFmt(measurementName));
				break;
			default:
				this.fmts.add(GDE.STRING_EMPTY);
			}
		}

		this.symbols = new ArrayList<>();
		for (int i = 0; i < this.measurementNames.size(); i++) {
			String measurementName = this.measurementNames.get(i).contains(GDE.STRING_UNDER_BAR) ? this.measurementNames.get(i).split(GDE.STRING_UNDER_BAR)[1] : this.measurementNames.get(i);
			switch (ArduPilot.LogEntry.getLogEntry(this.name)) {
			case ACC:
				this.symbols.add(ArduPilot.ACC.getSymbol(measurementName));
				break;
			case ARSP:
				this.symbols.add(ArduPilot.ARSP.getSymbol(measurementName));
				break;
			case BARO:
				this.symbols.add(ArduPilot.BARO.getSymbol(measurementName));
				break;
			case CURR:
				this.symbols.add(ArduPilot.CURR.getSymbol(measurementName));
				break;
			case CURR_CELL:
				this.symbols.add(ArduPilot.CURR_CELL.getSymbol(measurementName));
				break;
			case ESC:
				this.symbols.add(ArduPilot.ESC.getSymbol(measurementName));
				break;
			case GPA:
				this.symbols.add(ArduPilot.GPA.getSymbol(measurementName));
				break;
			case GPS:
				this.symbols.add(ArduPilot.GPS.getSymbol(measurementName));
				break;
			case GYR:
				this.symbols.add(ArduPilot.GYR.getSymbol(measurementName));
				break;
			case IMT:
				this.symbols.add(ArduPilot.IMT.getSymbol(measurementName));
				break;
			case IMU:
				this.symbols.add(ArduPilot.IMU.getSymbol(measurementName));
				break;
			case ISBD:
				this.symbols.add(ArduPilot.ISBD.getSymbol(measurementName));
				break;
			case ISBH:
				this.symbols.add(ArduPilot.ISBH.getSymbol(measurementName));
				break;
			case MAG:
				this.symbols.add(ArduPilot.MAG.getSymbol(measurementName));
				break;
			case PID:
				this.symbols.add(ArduPilot.PID.getSymbol(measurementName));
				break;
			case QUAT:
				this.symbols.add(ArduPilot.QUAT.getSymbol(measurementName));
				break;
			default:
				this.symbols.add(GDE.STRING_EMPTY);
			}
		}

		this.units = new ArrayList<>();
		for (int i = 0; i < this.measurementNames.size(); i++) {
			String measurementName = this.measurementNames.get(i).contains(GDE.STRING_UNDER_BAR) ? this.measurementNames.get(i).split(GDE.STRING_UNDER_BAR)[1] : this.measurementNames.get(i);
			switch (ArduPilot.LogEntry.getLogEntry(this.name)) {
			case ACC:
				this.units.add(ArduPilot.ACC.getUnit(measurementName));
				break;
			case ARSP:
				this.units.add(ArduPilot.ARSP.getUnit(measurementName));
				break;
			case BARO:
				this.units.add(ArduPilot.BARO.getUnit(measurementName));
				break;
			case CURR:
				this.units.add(ArduPilot.CURR.getUnit(measurementName));
				break;
			case CURR_CELL:
				this.units.add(ArduPilot.CURR_CELL.getUnit(measurementName));
				break;
			case ESC:
				this.units.add(ArduPilot.ESC.getUnit(measurementName));
				break;
			case GPA:
				this.units.add(ArduPilot.GPA.getUnit(measurementName));
				break;
			case GPS:
				this.units.add(ArduPilot.GPS.getUnit(measurementName));
				break;
			case GYR:
				this.units.add(ArduPilot.GYR.getUnit(measurementName));
				break;
			case IMT:
				this.units.add(ArduPilot.IMT.getUnit(measurementName));
				break;
			case IMU:
				this.units.add(ArduPilot.IMU.getUnit(measurementName));
				break;
			case ISBD:
				this.units.add(ArduPilot.ISBD.getUnit(measurementName));
				break;
			case ISBH:
				this.units.add(ArduPilot.ISBH.getUnit(measurementName));
				break;
			case MAG:
				this.units.add(ArduPilot.MAG.getUnit(measurementName));
				break;
			case PID:
				this.units.add(ArduPilot.PID.getUnit(measurementName));
				break;
			case QUAT:
				this.units.add(ArduPilot.QUAT.getUnit(measurementName));
				break;
			default:
				this.units.add(GDE.STRING_EMPTY);
			}
		}

		this.mults = new ArrayList<>();
		for (int i = 0; i < this.measurementNames.size(); i++) {
			switch (ArduPilot.LogEntry.getLogEntry(this.name)) {
			case ACC:
				this.mults.add(ArduPilot.ACC.getMults(this.measurementNames.get(i).split(GDE.STRING_UNDER_BAR)[1]));
				break;
			case ARSP:
				this.mults.add(ArduPilot.ARSP.getMults(this.measurementNames.get(i).split(GDE.STRING_UNDER_BAR)[1]));
				break;
			case BARO:
				this.mults.add(ArduPilot.BARO.getMults(this.measurementNames.get(i).split(GDE.STRING_UNDER_BAR)[1]));
				break;
			case CURR:
				this.mults.add(ArduPilot.CURR.getMults(this.measurementNames.get(i).split(GDE.STRING_UNDER_BAR)[1]));
				break;
			case CURR_CELL:
				this.mults.add(ArduPilot.CURR_CELL.getMults(this.measurementNames.get(i).split(GDE.STRING_UNDER_BAR)[1]));
				break;
			case ESC:
				this.mults.add(ArduPilot.ESC.getMults(this.measurementNames.get(i).split(GDE.STRING_UNDER_BAR)[1]));
				break;
			case GPA:
				this.mults.add(ArduPilot.GPA.getMults(this.measurementNames.get(i).split(GDE.STRING_UNDER_BAR)[1]));
				break;
			case GPS:
				this.mults.add(ArduPilot.GPS.getMults(this.measurementNames.get(i).split(GDE.STRING_UNDER_BAR)[1]));
				break;
			case GYR:
				this.mults.add(ArduPilot.GYR.getMults(this.measurementNames.get(i).split(GDE.STRING_UNDER_BAR)[1]));
				break;
			case IMT:
				this.mults.add(ArduPilot.IMT.getMults(this.measurementNames.get(i).split(GDE.STRING_UNDER_BAR)[1]));
				break;
			case IMU:
				this.mults.add(ArduPilot.IMU.getMults(this.measurementNames.get(i).split(GDE.STRING_UNDER_BAR)[1]));
				break;
			case ISBD:
				this.mults.add(ArduPilot.ISBD.getMults(this.measurementNames.get(i).split(GDE.STRING_UNDER_BAR)[1]));
				break;
			case ISBH:
				this.mults.add(ArduPilot.ISBH.getMults(this.measurementNames.get(i).split(GDE.STRING_UNDER_BAR)[1]));
				break;
			case MAG:
				this.mults.add(ArduPilot.MAG.getMults(this.measurementNames.get(i).split(GDE.STRING_UNDER_BAR)[1]));
				break;
			case PID:
				this.mults.add(ArduPilot.PID.getMults(this.measurementNames.get(i).split(GDE.STRING_UNDER_BAR)[1]));
				break;
			case QUAT:
				this.mults.add(ArduPilot.QUAT.getMults(this.measurementNames.get(i).split(GDE.STRING_UNDER_BAR)[1]));
				break;
			default:
				this.mults.add(1.0);
			}
		}
	}
	
	public String getFmt(int index) {
		return this.fmts.get(index);
	}
	
	public List<String> getMeasurementNames() {
		return this.measurementNames;
	}
	
	public List<String> getSymbols() {
		return this.symbols;
	}
	
	public List<String> getUnits() {
		return this.units;
	}
	
	public List<Double> getMults() {
		return this.mults;
	}
}
