package com.dbash.util;

import java.util.Random;


public class Randy {
	
	public static final int getRand(int min, int max) {

	    // Usually this can be a field rather than a method variable
	    Random rand = new Random();
	    int dif = max-min;
	    if (dif<0) {
	    	dif = 0;
	    }
	    // nextInt is normally exclusive of the top value,
	    // so add 1 to make it inclusive
	    int randomNum = rand.nextInt(dif + 1) + min;

	    return randomNum;
	}

}
