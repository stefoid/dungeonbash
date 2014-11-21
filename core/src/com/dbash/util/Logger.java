package com.dbash.util;

public class Logger {

	public static boolean DEBUG = true;
	
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
