package com.dbash.util;

import java.util.Random;


public class Randy {
	
	static Random rand = null;
	
	public static int getRand(int min, int max) {
		
		if (rand == null) {
			rand = new Random();
		}
		
		if (max < min) {
			max = min;
		}

	    // nextInt is normally exclusive of the top value,
	    // so add 1 to make it inclusive
	    int randomNum = rand.nextInt((max - min) + 1) + min;
	    
	    return randomNum;
	}

}


