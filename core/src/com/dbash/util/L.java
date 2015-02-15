package com.dbash.util;

public class L {

	public static boolean DEBUG = false;
	
	public static boolean TESTCHARS = false;
	public static boolean NO_SAVE = false;
	public static int LEVEL = 1;
	
	public static float PPI = 100;
	public static int SCREENX = 1280;
	public static int SCREENY = 720;
	
	public static boolean SHOWTEXTBOXES = false;
	
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
