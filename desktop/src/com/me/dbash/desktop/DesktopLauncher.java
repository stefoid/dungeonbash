package com.me.dbash.desktop;

import com.badlogic.gdx.Graphics.DisplayMode;
import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.tools.texturepacker.TexturePacker;
import com.badlogic.gdx.tools.texturepacker.TexturePacker.Settings;
import com.me.dbash.Dbash;

public class DesktopLauncher {
	
	public static class MyApplicationConfiguration extends
	LwjglApplicationConfiguration {
		
		public MyApplicationConfiguration() {
			super();
			title = "Dungeon Bash";
			width = 960;
			height = 640;
		}
	}
	
	/**
	* Full screen application configuration. 
	* 
	* @author ph
	*
	*/
	public static class MaximumApplicationConfiguration extends
		MyApplicationConfiguration {
	public MaximumApplicationConfiguration() {
		super();
		int maxWidth = 0;
		fullscreen = true;
		for (DisplayMode displayMode : LwjglApplicationConfiguration
				.getDisplayModes()) {
			if (displayMode.width > maxWidth) {
				System.out.println(displayMode);
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
		TexturePacker.process(new TexturePackerSettings(), "pngs",
				"bin/res/dbash2/pngs-packed", "pack");
	}
	
	private static void startGame(LwjglApplicationConfiguration config) {
		//config.width = 1920;
		//config.height = 1280;
		
		//config.width = 480;
		//config.height = 320;
		
		//config.width = 1280;
		//config.height = 720;
		
		//config.width = 800;
		//config.height = 480;
		
		new LwjglApplication(new Dbash(), config);
	}
	
	private static boolean isFullScreenArgument(String[] args) {
		return args.length>1 && "-full-screen".equals(args[1]);
	}
	
	public static void useMaximumDisplayMode(LwjglApplicationConfiguration cfg) {
	}
}
