package com.dbash.util;

import com.badlogic.gdx.utils.JsonValue;

public class L {

	public static boolean DEBUG = true;
	
	public static boolean JARFILE = false;  // setting the config file 'standalone' stes this to true therfore
	// for anything but desktop, it will always be false.
	public static JsonValue json = null;
	
	public static boolean TESTCHARS = true;
	public static String c1 = "giant";
	public static String c2 = "red dragon";
	public static String c3 = "greater demon";
	
	public static boolean TUTORIAL_MODE = false;
	
	public static String FIRST_MONSTER = "gnome";
	
	public static boolean NO_LOAD = false;
	public static boolean NO_SAVE = false;
	public static int LEVEL = 1;
	
	public static String STRING_PREFIX = "sw_";
	public static boolean useLights = true;
	public static boolean floorShadows = false;
	public static float DARK_FACTOR = 0.17f;
	public static float SHADOW_OPACITY = 0.83f;
	public static boolean NEW_TILES = true;
	public static String HARD_ROOM_NAME = null;
	public static double NORMAL_TILE_PROB = 50.0;
	
	public static int EXTRA_XP = 0;
	public static boolean TEST_EXP = false;  // run the total XP test for a level.
	public static int TORCH_DENSITY = 8;  // higher is less dense. default is 8
	public static boolean SHOWTEXTBOXES = false;
	
	public static int TEST_STEALTH_BONUS = 0;
	public static int TEST_SKILL_BONUS = 0;
	
	public static float PPI = 100;
	public static int SCREENX = 1024;
	public static int SCREENY = 768;
	
	public static boolean SHOW_STEALTH_NUMBERS = true;
	
	public static final int STACK_LEVEL = 3;
	
	public static void log(String msg, Object... args) {
		System.out.printf(buildDebugString()+" "+msg + "\n", args);
	}
	
    private static String buildDebugString() {
        String fullClassName = Thread.currentThread().getStackTrace()[STACK_LEVEL].getClassName();
        String className = fullClassName.substring(fullClassName.lastIndexOf(".") + 1);
        String methodName = Thread.currentThread().getStackTrace()[STACK_LEVEL].getMethodName();
        int lineNumber = Thread.currentThread().getStackTrace()[STACK_LEVEL].getLineNumber();

        return (className + "." + methodName + "() [" + lineNumber+"]: ");
    }
}
