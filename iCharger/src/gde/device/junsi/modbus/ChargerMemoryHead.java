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

    Copyright (c) 2020 Winfried Bruegmann
****************************************************************************************/
package gde.device.junsi.modbus;

import gde.io.DataParser;

public class ChargerMemoryHead {
	public final static int	LIST_MEM_MAX	= 32;
	//public final int[][] MEM_HEAD_DEFAULT	 = new int[][] {7,{0,1,2,3,4,5,6}};

	short							count;
	byte[]						index					= new byte[LIST_MEM_MAX];	//0-LIST_MEM_MAX
	
	/**
	 * constructor to create instance from received data
	 * @param memoryHeadBuffer filled by Modbus communication
	 */
	public ChargerMemoryHead(final byte[] memoryHeadBuffer) {
		this.count = DataParser.parse2Short(memoryHeadBuffer[0], memoryHeadBuffer[1]);
		for (int i = 0; i < this.count; ++i) {
			index[i] = memoryHeadBuffer[2 + i];
		}
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(this.getClass().getSimpleName()).append(" : \n");
		sb.append(String.format("program memory count = %d", this.count)).append("\n");
		for (int i = 0; i < this.count; ++i) {
			sb.append(index[i]).append(", ");
		}
		sb.append("\n");
		return sb.toString();
	}
	
	public byte[] getAsByteArray() {
		byte[] memHeadBuffer = new byte[size];
		memHeadBuffer[0] = (byte) (this.count & 0xFF);
		memHeadBuffer[1] = (byte) (this.count >> 8);
		for (int i = 0; i < this.count; i++) {
			memHeadBuffer[2+i] = index[i];
		}
		return memHeadBuffer;
	}
	
	final static int		size								= 17*2; //size in byte

	public static int getSize() {
		return size;
	}

	public short getCount() {
		return this.count;
	}

	public void setCount(short count) {
		this.count = count;
	}

	public byte[] getIndex() {
		return this.index;
	}
	
	public byte[] addIndexAfter(byte batTypeOrdinal) {
		byte[] updatedIndex = new byte[this.count+1];
		//find next free index, if there is any
		int n = this.count;
		int sum = n * (n + 1) / 2;
		int restSum = 0;
		for (int i = 0; i < this.count; i++) {
			restSum += this.index[i];
		}
		int nextFreeIndex = sum - restSum;
		
		for (int i=0, j=0; i < this.count+1;) {
			updatedIndex[i++] = this.index[j++];
			if (updatedIndex[i-1] == batTypeOrdinal)
				updatedIndex[i++] = (byte) nextFreeIndex;			
		}
		return updatedIndex;
	}

	public byte[] removeIndex(byte removeIndex) {
		byte[] updatedIndex = new byte[this.count+1];
		for (int i=0, j=0; i < this.count+1;) {
			if (this.index[j++] != removeIndex)
				updatedIndex[i++] = this.index[j-1];
		}
		return updatedIndex;
	}
	
	public void setIndex(byte[] index) {
		this.index = index;
	}
}
