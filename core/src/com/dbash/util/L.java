package com.dbash.util;

public class L {

	public static boolean DEBUG = true;
	
	public static boolean TESTCHARS = false;
	public static String c1 = "halfling";
	public static String c2 = "acid blob";
	public static String c3 = "pit viper";
	
	public static boolean TUTORIAL_MODE = false;
	
	public static String FIRST_MONSTER = "gnome";
	
	public static boolean NO_LOAD = false;
	public static boolean NO_SAVE = true;
	public static int LEVEL = 1;
	
	public static int TORCH_DENSITY = 8;  // higher is less dense. default is 8
	public static boolean SHOWTEXTBOXES = false;
	
	public static int TEST_STEALTH_BONUS = 0;
	public static int TEST_SKILL_BONUS = 0;
	
	public static float PPI = 100;
	public static int SCREENX = 1280;
	public static int SCREENY = 720;
	
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
