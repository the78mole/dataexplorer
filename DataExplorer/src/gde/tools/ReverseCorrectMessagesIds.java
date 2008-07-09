/**************************************************************************************
  	This file is part of OpenSerialDataExplorer.

    OpenSerialDataExplorer is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    OpenSerialDataExplorer is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with OpenSerialDataExplorer.  If not, see <http://www.gnu.org/licenses/>.
****************************************************************************************/
package osde.tools;

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
		"#/************************************************************************************** \n" + "#  	This file is part of OpenSerialDataExplorer. \n"
	+ "# \n" + "#    OpenSerialdataExplorer is free software: you can redistribute it and/or modify \n"
	+ "#    it under the terms of the GNU General Public License as published by \n"
	+ "#    the Free Software Foundation, either version 3 of the License, or \n" + "#    (at your option) any later version. \n" + "# \n"
	+ "#    OpenSerialdataExplorer is distributed in the hope that it will be useful, \n"
	+ "#    but WITHOUT ANY WARRANTY; without even the implied warranty of \n" + "#    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the \n"
	+ "#    GNU General Public License for more details. \n" + "# \n" + "#    You should have received a copy of the GNU General Public License \n"
	+ "#    along with OpenSerialdataExplorer.  If not, see <http://www.gnu.org/licenses/>. \n"
	+ "#****************************************************************************************/ \n"
	+ "# OpenSerialDataExplorer message file Locale.GERMANY Winfried Brügann  - 22 Jan 2008 \n" + " \n" + "# OSDE_MSGE0000 -> error messages \n"
	+ "# OSDE_MSGW0000 -> warning messages \n" 
	+ "# OSDE_MSGI0000 -> info messages \n" 
	+ "# OSDE_MSGT0000 -> normal text \n"
	+ "# range 0000 to 1099 reserved for base OSDE code \n" 
	+ "# range 1100 to 1199 AkkuMaster device \n" 
	+ "# range 1200 to 1299 Picolario device \n"
	+ "# range 1300 to 1399 UniLog device \n" 
	+ "# range 1400 to 1499 eStation device \n"
	// add new supported device here
	+ " \n";

	final static String	begin_OSDE_MSGE		= "# begin OSDE_MSGE0000 -> error messages \n";
	final static String	end_OSDE_MSGE			= "# end OSDE_MSGE0000 -> error messages \n\n";
	final static String	begin_OSDE_MSGW		= "# begin OSDE_MSGW0000 -> warning messages \n";
	final static String	end_OSDE_MSGW			= "# end OSDE_MSGW0000 -> warning messages \n\n";
	final static String	begin_OSDE_MSGI		= "# begin OSDE_MSGI0000 -> info messages \n";
	final static String	end_OSDE_MSGI			= "# end OSDE_MSGI0000 -> info messages \n\n";
	final static String	begin_OSDE_MSGT		= "# begin OSDE_MSGT0000 -> normal text \n";
	final static String	end_OSDE_MSGT			= "# end OSDE_MSGT0000 -> normal text \n\n";
	final static String	skipLine					= " \n";
	final static String	range_OSDE				= "# range 0000 to 1000 reserved for base OSDE code \n";
	static boolean isRanageOSDE = false;
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

		//OSDE_MSGE*
		writer.write(begin_OSDE_MSGE);
		while ((line = readerIds.readLine()) != null) {
			if (!line.trim().startsWith("public final static String") || line.trim().length() <= 1) continue;
			//System.out.println(line);

			String[] sections = line.trim().split(" |\t");
			if (!sections[4].startsWith("OSDE_MSGE")) continue;
			//System.out.println(line);
			System.out.println(sections[4] + " -> " + sections[6].substring(1, sections[6].length()-2));
			writeRangeMark(writer, calcMsgValue(sections));
			writer.write(sections[4] + "=" + msgProps.get(sections[6].substring(1, sections[6].length()-2)) + "\n");
		}
		writer.write(end_OSDE_MSGE);
		readerIds.reset();
		isRanageOSDE = isRanageAkkuMaster = isRanagePicolario = isRanageUniLog = isRanageeStation = false;

		//OSDE_MSGW*
		writer.write(begin_OSDE_MSGW);
		while ((line = readerIds.readLine()) != null) {
			if (!line.trim().startsWith("public final static String") || line.trim().length() <= 1) continue;
			//System.out.println(line);

			String[] sections = line.trim().split(" |\t");
			if (!sections[4].startsWith("OSDE_MSGW")) continue;
			//System.out.println(line);
			System.out.println(sections[4] + " -> " + sections[6].substring(1, sections[6].length()-2));
			writeRangeMark(writer, calcMsgValue(sections));
			writer.write(sections[4] + "=" + msgProps.get(sections[6].substring(1, sections[6].length()-2)) + "\n");
		}
		writer.write(end_OSDE_MSGW);
		readerIds.reset();
		isRanageOSDE = isRanageAkkuMaster = isRanagePicolario = isRanageUniLog = isRanageeStation = false;

		//OSDE_MSGI*
		writer.write(begin_OSDE_MSGI);
		while ((line = readerIds.readLine()) != null) {
			if (!line.trim().startsWith("public final static String") || line.trim().length() <= 1) continue;
			//System.out.println(line);

			String[] sections = line.trim().split(" |\t");
			if (!sections[4].startsWith("OSDE_MSGI")) continue;
			//System.out.println(line);
			System.out.println(sections[4] + " -> " + sections[6].substring(1, sections[6].length()-2));
			writeRangeMark(writer, calcMsgValue(sections));
			writer.write(sections[4] + "=" + msgProps.get(sections[6].substring(1, sections[6].length()-2)) + "\n");

		}
		writer.write(end_OSDE_MSGI);
		readerIds.reset();
		isRanageOSDE = isRanageAkkuMaster = isRanagePicolario = isRanageUniLog = isRanageeStation = false;

		//OSDE_MSGT*
		writer.write(begin_OSDE_MSGT);
		while ((line = readerIds.readLine()) != null) {
			if (!line.trim().startsWith("public final static String") || line.trim().length() <= 1) continue;
			//System.out.println(line);

			String[] sections = line.trim().split(" |\t");
			if (!sections[4].startsWith("OSDE_MSGT")) continue;
			//System.out.println(line);
			System.out.println(sections[4] + " -> " + sections[6].substring(1, sections[6].length()-2));
			writeRangeMark(writer, calcMsgValue(sections));
			writer.write(sections[4] + "=" + msgProps.get(sections[6].substring(1, sections[6].length()-2)) + "\n");
		}
		writer.write(end_OSDE_MSGT);

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
			if (!isRanageOSDE) writer.write(range_OSDE);
			isRanageOSDE = true;
		}
		else if (range > 1100 && range < 1199) {
			if (!isRanageAkkuMaster) writer.write(range_AkkuMaster);
			isRanageAkkuMaster = true;
		}
		else if (range > 1200 && range < 1299) {
			if (!isRanagePicolario) writer.write(range_Picolario);
			isRanagePicolario = true;
		}
		else if (range > 1300 && range < 1399) {
			if (!isRanageUniLog) writer.write(range_UniLog);
			isRanageUniLog = true;
		}
		else if (range > 1400 && range < 1499) {
			if (!isRanageeStation) writer.write(range_eStation);
			isRanageeStation = true;
		}
	}

}
