package com.dbash.util;

import com.badlogic.gdx.utils.JsonValue;

public class L {

	public static boolean DEBUG = false;
	
	public static boolean JARFILE = true;
	public static JsonValue json = null;
	
	public static boolean TESTCHARS = false;
	public static String c1 = "greater demon";
	public static String c2 = "greater demon";
	public static String c3 = "greater demon";
	
	public static boolean TUTORIAL_MODE = false;
	
	public static String FIRST_MONSTER = "gnome";
	
	public static boolean NO_LOAD = false;
	public static boolean NO_SAVE = false;
	public static int LEVEL = 1;
	
	public static boolean useLights = true;
	public static boolean floorShadows = true;
	public static int DARK_PERCENTAGE = 100;
	public static boolean NEW_TILES = false;
	
	public static int EXTRA_XP = 0;
	public static boolean TEST_EXP = false;  // run the total XP test for a level.
	public static int TORCH_DENSITY = 8;  // higher is less dense. default is 8
	public static boolean SHOWTEXTBOXES = false;
	
	public static int TEST_STEALTH_BONUS = 0;
	public static int TEST_SKILL_BONUS = 0;
	
	public static float PPI = 100;
	public static int SCREENX = 1024;
	public static int SCREENY = 768;
	
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
