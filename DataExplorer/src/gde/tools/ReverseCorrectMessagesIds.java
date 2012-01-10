/**************************************************************************************
  	This file is part of GNU DataExplorer.

    GNU DataExplorer is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    DataExplorer is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with GNU DataExplorer.  If not, see <http://www.gnu.org/licenses/>.
    
    Copyright (c) 2008,2009,2010,2011,2012 Winfried Bruegmann
****************************************************************************************/
package gde.tools;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.Properties;

/**
 * class to correct updated message IDs reverse to messages
 * @author Winfried Brügmann
 */
public class ReverseCorrectMessagesIds {

	final static String	fileHeader				= 
		"#/************************************************************************************** \n" + "#  	This file is part of GNU DataExplorer. \n"
	+ "# \n" + "#    GNU DataExplorer is free software: you can redistribute it and/or modify \n"
	+ "#    it under the terms of the GNU General Public License as published by \n"
	+ "#    the Free Software Foundation, either version 3 of the License, or \n" + "#    (at your option) any later version. \n" + "# \n"
	+ "#    DataExplorer is distributed in the hope that it will be useful, \n"
	+ "#    but WITHOUT ANY WARRANTY; without even the implied warranty of \n" + "#    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the \n"
	+ "#    GNU General Public License for more details. \n" + "# \n" + "#    You should have received a copy of the GNU General Public License \n"
	+ "#    along with GNU DataExplorer.  If not, see <http://www.gnu.org/licenses/>. \n"
	+ "#\n"
	+ "#Copyright (c) 2008,2009,2010,2011,2012 Winfried Bruegmann \n"
	+ "#****************************************************************************************/ \n"
	+ "# DataExplorer message file Locale.GERMANY Winfried Brügmann  - 22 Jan 2008 \n" + " \n" 
	+ "# GDE_MSGE0000 -> error messages \n"
	+ "# GDE_MSGW0000 -> warning messages \n" 
	+ "# GDE_MSGI0000 -> info messages \n" 
	+ "# GDE_MSGT0000 -> normal text \n"
	+ "# range 0000 to 1000 reserved for base GDE code \n" 
	+ "# range 1001 to 1099 Sample + Simulator \n" 
	+ "# range 1100 to 1199 AkkuMaster device \n" 
	+ "# range 1200 to 1299 Picolario device \n"
	+ "# range 1300 to 1399 UniLog device \n" 
	+ "# range 1400 to 1499 eStation device \n"
	+ "# range 1500 to 1599 VC800 device \n"
	+ "# range 1600 to 1699 LiPoWatch device \n" 
	+ "# range 1700 to 1799 CSV2SerialDataAdapter device \n"
	+ "# range 1800 to 1899 WStechVario device  \n"
	+ "# range 1900 to 1999 QC-Copter device  \n"
	+ "# range 2000 to 2999 GPS-Logger device  \n"
	+ "# range 2100 to 2199 NMEA-Adapter device  \n"  
	+ "# range 2200 to 2399 Ultramat/UltraDuoPlus devices  \n" 
	+ "# range 2400 to 2499 HoTTAdapter  \n"  
	+ "# range 2500 to 2599 UniLog2  \n"
	// add new supported device here
	+ " \n";

	final static String	begin_GDE_MSGE		= "# begin GDE_MSGE0000 -> error messages \n";
	final static String	end_GDE_MSGE			= "# end GDE_MSGE0000 -> error messages \n\n";
	final static String	begin_GDE_MSGW		= "# begin GDE_MSGW0000 -> warning messages \n";
	final static String	end_GDE_MSGW			= "# end GDE_MSGW0000 -> warning messages \n\n";
	final static String	begin_GDE_MSGI		= "# begin GDE_MSGI0000 -> info messages \n";
	final static String	end_GDE_MSGI			= "# end GDE_MSGI0000 -> info messages \n\n";
	final static String	begin_GDE_MSGT		= "# begin GDE_MSGT0000 -> normal text \n";
	final static String	end_GDE_MSGT			= "# end GDE_MSGT0000 -> normal text \n\n";
	final static String	skipLine					= " \n";
	final static String	range_GDE				= "# range 0000 to 1000 reserved for base GDE code \n";
	static boolean isRanageGDE = false;
	final static String range_Sample			= "# range 1001 to 1099 Sample + Simulator \n"; 
	static boolean isRanageSample = false;
	final static String	range_AkkuMaster	= "# range 1100 to 1199 AkkuMaster device \n";
	static boolean isRanageAkkuMaster = false;
	final static String	range_Picolario		= "# range 1200 to 1299 Picolario device \n";
	static boolean isRanagePicolario = false;
	final static String	range_UniLog			= "# range 1300 to 1399 UniLog device \n";
	static boolean isRanageUniLog = false;
	final static String	range_eStation		= "# range 1400 to 1499 eStation device \n";
	static boolean isRanageeStation = false;

	// add new supported device here

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		String srcFilePath = args[0]; //"src/osde/messages/messages.properties";
		String inFilePathIds = args[1]; //"src/osde/messages/MessageIds.java";
		String outFilePath = args[2]; //"src/osde/messages/messages_.properties";
		BufferedReader readerIds; // to read the ID data
		BufferedWriter writer; // to write the data
		Properties msgProps = new Properties();
		String line;
		msgProps.load(new InputStreamReader(new FileInputStream(srcFilePath), "ISO-8859-1"));
		readerIds = new BufferedReader(new InputStreamReader(new FileInputStream(inFilePathIds), "UTF-8"));
		writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outFilePath), "ISO-8859-1"));
		writer.write(fileHeader);
		writer.write(skipLine);
		readerIds.mark(50000);

		//GDE_MSGE*
		writer.write(begin_GDE_MSGE);
		while ((line = readerIds.readLine()) != null) {
			if (!line.trim().startsWith("public final static String") || line.trim().length() <= 1) continue;
			//System.out.println(line);

			String[] sections = line.trim().split(" |\t");
			if (!sections[4].startsWith("GDE_MSGE")) continue;
			//System.out.println(line);
			System.out.println(sections[4] + " -> " + sections[6].substring(1, sections[6].length()-2));
			writeRangeMark(writer, calcMsgValue(sections));
			writer.write(sections[4] + "=" + msgProps.get(sections[6].substring(1, sections[6].length()-2)) + "\n");
		}
		writer.write(end_GDE_MSGE);
		readerIds.reset();
		isRanageGDE = isRanageAkkuMaster = isRanagePicolario = isRanageUniLog = isRanageeStation = false;

		//GDE_MSGW*
		writer.write(begin_GDE_MSGW);
		while ((line = readerIds.readLine()) != null) {
			if (!line.trim().startsWith("public final static String") || line.trim().length() <= 1) continue;
			//System.out.println(line);

			String[] sections = line.trim().split(" |\t");
			if (!sections[4].startsWith("GDE_MSGW")) continue;
			//System.out.println(line);
			System.out.println(sections[4] + " -> " + sections[6].substring(1, sections[6].length()-2));
			writeRangeMark(writer, calcMsgValue(sections));
			writer.write(sections[4] + "=" + msgProps.get(sections[6].substring(1, sections[6].length()-2)) + "\n");
		}
		writer.write(end_GDE_MSGW);
		readerIds.reset();
		isRanageGDE = isRanageAkkuMaster = isRanagePicolario = isRanageUniLog = isRanageeStation = false;

		//GDE_MSGI*
		writer.write(begin_GDE_MSGI);
		while ((line = readerIds.readLine()) != null) {
			if (!line.trim().startsWith("public final static String") || line.trim().length() <= 1) continue;
			//System.out.println(line);

			String[] sections = line.trim().split(" |\t");
			if (!sections[4].startsWith("GDE_MSGI")) continue;
			//System.out.println(line);
			System.out.println(sections[4] + " -> " + sections[6].substring(1, sections[6].length()-2));
			writeRangeMark(writer, calcMsgValue(sections));
			writer.write(sections[4] + "=" + msgProps.get(sections[6].substring(1, sections[6].length()-2)) + "\n");

		}
		writer.write(end_GDE_MSGI);
		readerIds.reset();
		isRanageGDE = isRanageAkkuMaster = isRanagePicolario = isRanageUniLog = isRanageeStation = false;

		//GDE_MSGT*
		writer.write(begin_GDE_MSGT);
		while ((line = readerIds.readLine()) != null) {
			if (!line.trim().startsWith("public final static String") || line.trim().length() <= 1) continue;
			//System.out.println(line);

			String[] sections = line.trim().split(" |\t");
			if (!sections[4].startsWith("GDE_MSGT")) continue;
			//System.out.println(line);
			System.out.println(sections[4] + " -> " + sections[6].substring(1, sections[6].length()-2));
			writeRangeMark(writer, calcMsgValue(sections));
			writer.write(sections[4] + "=" + msgProps.get(sections[6].substring(1, sections[6].length()-2)) + "\n");
		}
		writer.write(end_GDE_MSGT);

		//		writer.write(fileClosing);

		writer.close();
		readerIds.close();
	}

	/**
	 * @param sections
	 * @return
	 */
	static Integer calcMsgValue(String[] sections) {
		return new Integer(sections[6].substring(10, sections[6].length()-2));
	}

	/**
	 * @param writer
	 * @param range
	 * @throws IOException
	 */
	private static void writeRangeMark(BufferedWriter writer, int range) throws IOException {
		if (range < 1000) {
			if (!isRanageGDE) writer.write(range_GDE);
			isRanageGDE = true;
		}
		else if (range >= 1001 && range < 1099) {
			if (!isRanageSample) writer.write(range_Sample);
			isRanageSample = true;
		}
		else if (range >= 1100 && range < 1199) {
			if (!isRanageAkkuMaster) writer.write(range_AkkuMaster);
			isRanageAkkuMaster = true;
		}
		else if (range >= 1200 && range < 1299) {
			if (!isRanagePicolario) writer.write(range_Picolario);
			isRanagePicolario = true;
		}
		else if (range >= 1300 && range < 1399) {
			if (!isRanageUniLog) writer.write(range_UniLog);
			isRanageUniLog = true;
		}
		else if (range >= 1400 && range < 1499) {
			if (!isRanageeStation) writer.write(range_eStation);
			isRanageeStation = true;
		}
	}

}
