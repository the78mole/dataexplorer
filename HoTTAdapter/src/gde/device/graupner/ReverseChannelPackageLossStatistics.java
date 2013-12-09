package gde.device.graupner;

import java.util.Locale;
import java.util.Vector;

public class ReverseChannelPackageLossStatistics extends Vector<Integer> {
	private static final long	serialVersionUID	= -1434896150654661385L;

	int												minValue					= 0;
	int												maxValue					= 0;
	double										avgValue					= 0;
	double										sigmaValue				= 0;

	/**
	 * add value and detect min/max value
	 */
	@Override
	public synchronized boolean add(Integer value) {

		if (super.size() == 0) {
			this.minValue = this.maxValue = value;
		}
		else {
			if (value > this.maxValue)
				this.maxValue = value;
			else if (value < this.minValue) this.minValue = value;
		}

		return super.add(value);
	}

	/**
	 * calculates the avgValue
	 */
	public synchronized double getAvgValue() {
		synchronized (this) {
			if (this.size() >= 2) {
				long sum = 0;
				int zeroCount = 0;
				for (Integer xi : this) {
					if (xi != 0) {
						sum += xi;
					}
					else {
						zeroCount++;
					}
				}
				this.avgValue = (this.size() - zeroCount) != 0 ? Long.valueOf(sum / (this.size() - zeroCount)).intValue() : 0;
			}
		}
		return this.avgValue;
	}

	/**
	 * calculates the sigmaValue 
	 */
	public synchronized double getSigmaValue() {
		synchronized (this) {
			if (super.size() >= 2) {
				double average = this.getAvgValue() / 1000.0;
				double sumPoweredValues = 0;
				for (Integer xi : this) {
					sumPoweredValues += Math.pow(xi / 1000.0 - average, 2);
				}
				this.sigmaValue = Double.valueOf(Math.sqrt(sumPoweredValues / (this.size() - 1)) * 1000).intValue();
			}
		}
		return this.sigmaValue;
	}

	/**
	 * @return the minValue
	 */
	public synchronized int getMinValue() {
		return minValue;
	}

	/**
	 * @return the maxValue
	 */
	public synchronized int getMaxValue() {
		return maxValue;
	}
	
	/**
	 * @return the statistics as formated string
	 */
	public String getStatistics() {
		return String.format(Locale.getDefault(), "min=%.2f sec; max=%.2f sec; avg=%.2f sec; sigma=%.2f sec", this.getMinValue()/100.0, this.getMaxValue()/100.0, this.getAvgValue()/100.0, this.getSigmaValue()/100.0);
	}
}
