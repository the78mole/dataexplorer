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
    
    Copyright (c) 2010 Winfried Bruegmann
****************************************************************************************/
package gde.utils;

import java.util.logging.Logger;

/**
 * simple thread implementation to wait during a main thread without using existing locks
 * use synchronous -> new WaitTimer(500).run(); 
 */
public class WaitTimer extends Thread {
	final static String 				$CLASS_NAME 			= WaitTimer.class.getName();
	final static Logger					log								= Logger.getLogger($CLASS_NAME);
	
	long delayTimeMilliSecinds = 1000;
	
	static WaitTimer instance = null;
	
	public static WaitTimer getInstance() {
		if (instance == null) {
			instance = new WaitTimer();
		}
		return instance;
	}
	
	/**
	 * needs to specify sleep time while calling sleep
	 * initially set wait time to 1000 ms
	 */
	private WaitTimer() {
		super("waitTimer");
	}

	public void run() {
		try {
			WaitTimer.sleep(this.delayTimeMilliSecinds);
		}
		catch (InterruptedException e) {
		}
	}
	
	/**
	 * synchronous wait for the given time
	 * @param milliSeconds
	 */
	public void delay(long milliSeconds) {
		this.delayTimeMilliSecinds = milliSeconds;
		//this.start();
		try {
			WaitTimer.sleep(this.delayTimeMilliSecinds);
			//this.join();
		}
		catch (InterruptedException e) {
		}
	}
}
