package com.dbash.util;

public class SequenceNumber {

	private static int sequenceNumber = 0;
	
	public static final int getCurrent() {
		return sequenceNumber;
	}
	
	public static final int getNext() {
		return sequenceNumber++;
	}
	
}
