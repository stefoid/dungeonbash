package com.me.dbash;

import org.robovm.apple.foundation.NSAutoreleasePool;
import org.robovm.apple.uikit.UIApplication;
import org.robovm.apple.uikit.UIDevice;
import org.robovm.bindings.crashlytics.Crashlytics;

import com.badlogic.gdx.backends.iosrobovm.IOSApplication;
import com.badlogic.gdx.backends.iosrobovm.IOSApplicationConfiguration;

public class IOSLauncher extends IOSApplication.Delegate {
    @Override
    protected IOSApplication createApplication() {
        IOSApplicationConfiguration config = new IOSApplicationConfiguration();
        config.orientationLandscape = true;
        config.orientationPortrait = false;
        String systemVersion = UIDevice.getCurrentDevice().getSystemVersion();
        int version = Character.getNumericValue(systemVersion.charAt(0));
        return new IOSApplication(new Dbash(version), config);
    }

	@Override
	public void didFinishLaunching (UIApplication application) {
		@SuppressWarnings("unused")
		Crashlytics crashlytics = Crashlytics.start("2fae8bedad1e84766db4bddb94031c22de93804d");
	}
    
    public static void main(String[] argv) {
        NSAutoreleasePool pool = new NSAutoreleasePool();
        UIApplication.main(argv, null, IOSLauncher.class);
        pool.close();
    }
}