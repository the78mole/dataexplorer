package osde.tools;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;


public class BuildMessageIds {

	final static String fileHeader = "/** \n"
		+ " *  \n"
		+ " */  \n"
		+ "package osde.messages; \n"
		+ "/** \n"
		+ " * @author Winfried Brügmann \n"
		+ " * Do not edit, MessageIds are generated from messages.properties \n"
		+ " */ \n"
		+ "public class MessageIds { \n"
		+ "\n";
	final static String fileClosing = "\n }\n";
	final static String lineHeader = "\tpublic final static String\t";
	final static String lineCenter = " = \"";
	final static String lineClosing = "\";\n";
	
	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		String inFilePath = "src/osde/messages/messages.properties";
		String outFilePath = "src/osde/messages/MessageIds.java";
		BufferedReader reader; // to read the data
		BufferedWriter writer; // to write the data
		String line;
		reader = new BufferedReader(new InputStreamReader(new FileInputStream(inFilePath), "UTF-8"));
		writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outFilePath), "UTF-8"));
		
		writer.write(fileHeader);
		while ((line = reader.readLine()) != null){
			System.out.println(line);
			if (line.trim().startsWith("#") || line.trim().length() <=1) 
				continue;
			
			System.out.println(line);
			
			String[] sections = line.trim().split("=");
			writer.write(lineHeader);
			writer.write(sections[0]);
			writer.write(lineCenter);
			writer.write(sections[0]);
			writer.write(lineClosing);
		}
		writer.write(fileClosing);
		
		writer.close();
		reader.close();
	}

}
