package com.me.dbash.desktop;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics.DisplayMode;
import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.tools.texturepacker.TexturePacker;
import com.badlogic.gdx.tools.texturepacker.TexturePacker.Settings;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.dbash.models.Character;
import com.dbash.models.Creature;
import com.dbash.util.L;
import com.me.dbash.Dbash;

import java.io.File;
import java.lang.*;
import java.util.Scanner;

@SuppressWarnings("unused")

public class DesktopLauncher {
	public static final boolean LOG = false && L.DEBUG;
	public static class MyApplicationConfiguration extends
	LwjglApplicationConfiguration {
		
		public MyApplicationConfiguration() {
			super();
			title = "Dungeon Bash";
			width = 960;
			height = 640;
		}
	}
	
	public static class MaximumApplicationConfiguration extends
		MyApplicationConfiguration {
	public MaximumApplicationConfiguration() {
		super();
		int maxWidth = 0;
		fullscreen = true;
		for (DisplayMode displayMode : LwjglApplicationConfiguration
				.getDisplayModes()) {
			if (displayMode.width > maxWidth) {
				if (LOG) L.log(displayMode.toString());
				width = displayMode.width;
				height = displayMode.height;
				maxWidth = displayMode.width;
			}
		}
	}
	}
	
	/**
	* Texture packer configuration parameter.
	* @author ph
	*
	*/
	public static class TexturePackerSettings extends Settings {
		public TexturePackerSettings() {
			maxWidth = 1024;
			maxHeight = 2048;
			combineSubdirectories = true;
			flattenPaths = true;
			paddingX = 2;
		    paddingY = 2;
		    bleed = true;
			filterMin = TextureFilter.Linear;
			filterMag = TextureFilter.Linear;
			duplicatePadding = true; // to stop gap line due to oversampling with linear filter
			useIndexes = true;
			fast = true;
		}
	}
	
	public static void main(String[] args) {
		packTextures();
		startGame(isFullScreenArgument(args) ? new MaximumApplicationConfiguration()
				: new MyApplicationConfiguration());
	}
	
	private static void packTextures() {
		String packedDir;
		String pngDir;
		if (L.JARFILE) {
			pngDir = "pngs";
			packedDir = "pngs-packed";
		} else {
			pngDir = "pngs";
			packedDir = "bin/res/dbash2/pngs-packed";
		}
		TexturePacker.process(new TexturePackerSettings(), pngDir, packedDir, "pack");
	}
	
	public static String SCREEN_WIDTH = "screen_width";
	public static String SCREEN_HEIGHT = "screen_height";
	public static String USE_LIGHTS = "lights";
	public static String LEVEL = "level";
	public static String FLOOR_SHADOWS = "floor_shadows";
	
	
	private static void startGame(LwjglApplicationConfiguration config) {
		config.width = L.SCREENX;
		config.height = L.SCREENY;

		File fl = new File("config.txt");
		String jsonString = null;
		JsonValue json = null;
		if (fl.exists() == true) {
			try {
				Scanner scanner = new Scanner(fl).useDelimiter("\\Z");
				jsonString = scanner.next();
				json = new JsonReader().parse(jsonString);
				if (json.has(SCREEN_WIDTH)) {
					config.width = json.getInt(SCREEN_WIDTH);
				}
				if (json.has(SCREEN_HEIGHT)) {
					config.height = json.getInt(SCREEN_HEIGHT);
				}
				if (json.has(USE_LIGHTS)) {
					L.useLights = json.getBoolean(USE_LIGHTS);
				}
				if (json.has(LEVEL)) {
					L.LEVEL = json.getInt(LEVEL);
				}
				if (json.has(FLOOR_SHADOWS)) {
					L.floorShadows = json.getBoolean(FLOOR_SHADOWS);
				}
			} catch (Exception e) {
			}
		}
		
		if (json == null) {
			json = new JsonValue("");
		}
		new LwjglApplication(new Dbash(0, json), config);
	}
	
	private static boolean isFullScreenArgument(String[] args) {
		return args.length>1 && "-full-screen".equals(args[1]);
	}
	
	public static void useMaximumDisplayMode(LwjglApplicationConfiguration cfg) {
	}
}
