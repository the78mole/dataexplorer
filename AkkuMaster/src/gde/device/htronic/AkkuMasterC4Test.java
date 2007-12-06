package osde.device.htronic;

import gnu.io.SerialPort;

import java.util.Timer;
import java.util.TimerTask;

import osde.config.DeviceConfiguration;
import osde.device.DeviceSerialPort;


public class AkkuMasterC4Test {

	static AkkuMasterC4SerialPort	akkuMaster;
	static int										counter	= 0;

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		DeviceConfiguration deviceConfig;
		try {
			deviceConfig = new DeviceConfiguration("c:/Documents and Settings/brueg/Application Data/OpenSerialDataExploroer/Devices/Htronic Akkumaster C4.ini", true);
			akkuMaster = new AkkuMasterC4SerialPort(deviceConfig, null);
			DeviceSerialPort.listConfiguredSerialPorts();
			SerialPort serialPort = akkuMaster.open();

			akkuMaster.print(akkuMaster.getVersion());
			System.out.println();

			int delay = 10; // delay for 10 msec.
			int period = 10000; // repeat every 10 sec.
			Timer timer = new Timer();

			TimerTask timerTask = new TimerTask() {
				public void run() {
					try {
						akkuMaster.print(akkuMaster.getData(AkkuMasterC4SerialPort.channel_1, 0, null));
						
//						System.out.println("Kanal 1");
//						akkuMaster.print(akkuMaster.getConfiguration(AkkuMasterC4SerialPort.channel_1));
//						akkuMaster.print(akkuMaster.getAdjustedValues(AkkuMasterC4SerialPort.channel_1));
//						akkuMaster.print(akkuMaster.getMeasuredValues(AkkuMasterC4SerialPort.channel_1));
//						System.out.println();
//
//						System.out.println("Kanal 2");
//						akkuMaster.print(akkuMaster.getConfiguration(AkkuMasterC4SerialPort.channel_2));
//						akkuMaster.print(akkuMaster.getAdjustedValues(AkkuMasterC4SerialPort.channel_2));
//						akkuMaster.print(akkuMaster.getMeasuredValues(AkkuMasterC4SerialPort.channel_2));
//						System.out.println();
//
//						System.out.println("Kanal 3");
//						akkuMaster.print(akkuMaster.getConfiguration(AkkuMasterC4SerialPort.channel_3));
//						akkuMaster.print(akkuMaster.getAdjustedValues(AkkuMasterC4SerialPort.channel_3));
//						akkuMaster.print(akkuMaster.getMeasuredValues(AkkuMasterC4SerialPort.channel_3));
//						System.out.println();
//
//						System.out.println("Kanal 4");
//						akkuMaster.print(akkuMaster.getConfiguration(AkkuMasterC4SerialPort.channel_4));
//						akkuMaster.print(akkuMaster.getAdjustedValues(AkkuMasterC4SerialPort.channel_4));
//						akkuMaster.print(akkuMaster.getMeasuredValues(AkkuMasterC4SerialPort.channel_4));
//
//						System.out.println("counter = " + counter++);
					}
					catch (Exception e) {
						e.printStackTrace();
					}
				}
			};

			timer.scheduleAtFixedRate(timerTask, delay, period);

			Thread.sleep(30000);
			timerTask.cancel();
			timer.cancel();
			serialPort.close();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		System.out.println("fertig !");
	}
}
