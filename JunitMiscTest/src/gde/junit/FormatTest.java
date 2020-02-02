package gde.junit;
/**************************************************************************************
  	This file is part of GNU DataExplorer.

    GNU DataExplorer is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    GNU DataExplorer is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with GNU DataExplorer.  If not, see <https://www.gnu.org/licenses/>.
    
    Copyright (c) 2008,2009,2010,2011,2012,2013,2014,2015,2016,2017,2018,2019,2020 Winfried Bruegmann
****************************************************************************************/
import java.util.Locale;

public class FormatTest {

	/**
	 * @param args
	 */
	public static void main(String[] args) {

		Double zahl = 234.5657687;
		System.out.println(String.format("%.2f", zahl));
		System.out.println(String.format(Locale.ENGLISH, "%.2f", zahl));
		System.out.println(String.format(Locale.US, "%.2f", zahl));
		System.out.println(String.format(Locale.GERMAN, "%.2f", zahl));
		System.out.println(String.format(Locale.GERMANY, "%.2f", zahl));
		System.out.println(""+(((int)(100.0*zahl))/100.0));
	}

}
