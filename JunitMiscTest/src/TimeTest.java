import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import osde.log.LogFormatter;


public class TimeTest {
	final static Logger	log	= Logger.getLogger(TimeTest.class.getName());
	static Logger				rootLogger;
	static Handler			ch	= new ConsoleHandler();
	static LogFormatter	lf	= new LogFormatter();

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		initLogger();

		final long maxCounter = 10000000;
		// Initialize the calendar object
		Calendar today = new GregorianCalendar();

		// Get the date
		int week = today.get(Calendar.DAY_OF_WEEK);
		log.log(Level.INFO, "Day of the week = " + week);
		int day = today.get(Calendar.DAY_OF_MONTH);
		log.log(Level.INFO, "day = " + day);
		int month = today.get(Calendar.MONTH);
		log.log(Level.INFO, "month = " + month);
		int year = today.get(Calendar.YEAR);
		log.log(Level.INFO, "year = " + year);

		// Get the time
		int hour = today.get(Calendar.HOUR_OF_DAY);
		log.log(Level.INFO, "hour = " + hour);
		int minute = today.get(Calendar.MINUTE);
		log.log(Level.INFO, "minute = " + minute);
		int sec = today.get(Calendar.SECOND);
		log.log(Level.INFO, "second = " + sec);

		// Time an event in a program to nanosecond precision
		long startTime = System.nanoTime();
		log.log(Level.INFO, "Start = " + startTime);
		for (long i = 0; i < maxCounter; i++) {
			long j = new Double(i - (i / 2) + (i / 3) - (i / 4) + (i / 5)).longValue();
		}
		long endTime = System.nanoTime();
		long elapsedTime = endTime - startTime;
		double millis = elapsedTime / 1.0E06;
		log.log(Level.INFO, "Elapsed Time = " + millis + " milli seconds");
		//////////////////////////////////////////////////////////////////////

		startTime = System.nanoTime();
		log.log(Level.INFO, "Start = " + startTime);
		for (long i = 0; i < maxCounter; i++) {
			long j = Double.valueOf(i - (i / 2) + (i / 3) - (i / 4) + (i / 5)).longValue();
		}
		endTime = System.nanoTime();
		elapsedTime = endTime - startTime;
		millis = elapsedTime / 1.0E06;
		log.log(Level.INFO, "Elapsed Time = " + millis + " milli seconds");
		//////////////////////////////////////////////////////////////////////

		// Time an event in a program to millisecond precision
		startTime = System.currentTimeMillis();
		log.log(Level.INFO, "Start = " + startTime);
		for (long i = 0; i < maxCounter; i++) {
			long j = new Double(i - (i / 2) + (i / 3) - (i / 4) + (i / 5)).longValue();
		}
		endTime = System.currentTimeMillis();
		elapsedTime = endTime - startTime;
		millis = elapsedTime;
		log.log(Level.INFO, "Elapsed Time = " + millis + " milli seconds");
		//////////////////////////////////////////////////////////////////////

		startTime = System.currentTimeMillis();
		log.log(Level.INFO, "Start = " + startTime);
		for (long i = 0; i < maxCounter; i++) {
			long j = Double.valueOf(i - (i / 2) + (i / 3) - (i / 4) + (i / 5)).longValue();
		}
		endTime = System.currentTimeMillis();
		elapsedTime = endTime - startTime;
		millis = elapsedTime;
		log.log(Level.INFO, "Elapsed Time = " + millis + " milli seconds");
		//////////////////////////////////////////////////////////////////////

		// Time an event in a program to millisecond precision
		
		startTime = new Date().getTime();
		log.log(Level.INFO, "Start = " + startTime);
		for (long i = 0; i < maxCounter; i++) {
			long j = new Double(i - (i / 2) + (i / 3) - (i / 4) + (i / 5)).longValue();
		}
		endTime = new Date().getTime();
		elapsedTime = endTime - startTime;
		millis = elapsedTime;
		log.log(Level.INFO, "Elapsed Time = " + millis + " milli seconds");
		//////////////////////////////////////////////////////////////////////

		startTime = new Date().getTime();
		log.log(Level.INFO, "Start = " + startTime);
		for (long i = 0; i < maxCounter; i++) {
			long j = Double.valueOf(i - (i / 2) + (i / 3) - (i / 4) + (i / 5)).longValue();
		}
		endTime = new Date().getTime();
		elapsedTime = endTime - startTime;
		millis = elapsedTime;
		log.log(Level.INFO, "Elapsed Time = " + millis + " milli seconds");
	}

	/**
	 * 
	 */
	private static void initLogger() {
		rootLogger = Logger.getLogger("");

		// clean up all handlers from outside
		Handler[] handlers = rootLogger.getHandlers();
		for (Handler handler : handlers) {
			rootLogger.removeHandler(handler);
		}
		rootLogger.setLevel(Level.INFO);
		rootLogger.addHandler(ch);
		ch.setFormatter(lf);
		ch.setLevel(Level.ALL);
	}

}
