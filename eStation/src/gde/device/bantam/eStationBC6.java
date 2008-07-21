package osde.device.bantam;

import java.io.FileNotFoundException;

import javax.xml.bind.JAXBException;

import osde.device.DeviceConfiguration;
import osde.device.IDevice;

/**
 * Sample device class, used as template for new device implementations
 * @author Winfried Brügmann
 */
public class eStationBC6 extends eStation implements IDevice {

	/**
	 * constructor using properties file
	 * @throws JAXBException 
	 * @throws FileNotFoundException 
	 */
	public eStationBC6(String deviceProperties) throws FileNotFoundException, JAXBException {
		super(deviceProperties);
		this.dialog = new EStationDialog(this.application.getShell(), this);
	}

	/**
	 * constructor using existing device configuration
	 * @param deviceConfig device configuration
	 */
	public eStationBC6(DeviceConfiguration deviceConfig) {
		super(deviceConfig);
		this.dialog = new EStationDialog(this.application.getShell(), this);
	}

	/**
	 * @return the dialog
	 */
	public EStationDialog getDialog() {
		return this.dialog;
	}
	
	/**
	 * convert the device bytes into raw values, no calculation will take place here, see translateValue reverseTranslateValue
	 * inactive or to be calculated data point are filled with 0 and needs to be handles after words
	 * @param points pointer to integer array to be filled with converted data
	 * @param dataBuffer byte arrax with the data to be converted
	 */
	public int[] convertDataBytes(int[] points, byte[] dataBuffer) {		
		
		// 0=Spannung 1=Strom 2=Ladung 3=Leistung 4=Energie 5=VersorgungsSpg. 
		points[0] = new Integer((((dataBuffer[35] & 0xFF)-0x80)*100 + ((dataBuffer[36] & 0xFF)-0x80))*10);  //35,36   feed-back voltage
		points[1] = new Integer((((dataBuffer[33] & 0xFF)-0x80)*100 + ((dataBuffer[34] & 0xFF)-0x80))*10);  //33,34   feed-back current : 0=0.0A,900=9.00A
		points[2] = new Integer((((dataBuffer[43] & 0xFF)-0x80)*100 + ((dataBuffer[44] & 0xFF)-0x80))*1000);  //43,44  cq_capa_dis;  : charged capacity
		points[3] = new Double((points[0] / 1000.0) * (points[1] / 1000.0) * 1000).intValue(); 							// power U*I [W]
		points[4] = new Double((points[0] / 1000.0) * (points[2] / 1000.0)).intValue();											// energy U*C [mWh]
		points[5] = new Integer((((dataBuffer[41] & 0xFF)-0x80)*100 + ((dataBuffer[42] & 0xFF)-0x80))*10);  //41,42  fd_in_12v;    : input voltage(00.00V 30.00V)
		// 6=SpannungZelle1 7=SpannungZelle2 8=SpannungZelle3 9=SpannungZelle4 10=SpannungZelle5 11=SpannungZelle6
		for (int i=0, j=0; i<points.length - 6; ++i, j+=2) {
			//log_base.info("cell " + (i+1) + " points[" + (i+6) + "]  = new Integer((((dataBuffer[" + (j+45) + "] & 0xFF)-0x80)*100 + ((dataBuffer[" + (j+46)+ "] & 0xFF)-0x80))*10);");  //45,46 CELL_420v[1];
			points[i+6]  = new Integer((((dataBuffer[j+45] & 0xFF)-0x80)*100 + ((dataBuffer[j+46] & 0xFF)-0x80))*10);  //45,46 CELL_420v[1];
		}

		return points;
	}
}
