package com.me.dbash.android;

import java.io.File;
import java.util.Scanner;

import android.os.Bundle;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.android.AndroidApplication;
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.crashlytics.android.Crashlytics;
import com.dbash.presenters.dungeon.DungeonAreaPresenter;
import com.dbash.util.L;
//import com.crashlytics.android.Crashlytics;
import com.me.dbash.Dbash;

public class AndroidLauncher extends AndroidApplication {
	@Override
	protected void onCreate (Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Crashlytics.start(this);
		AndroidApplicationConfiguration config = new AndroidApplicationConfiguration();
		initialize(new Dbash(0, new com.badlogic.gdx.utils.JsonValue("")), config);
	}
	
}
