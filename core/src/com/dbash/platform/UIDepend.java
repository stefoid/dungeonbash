package com.dbash.platform;

import java.util.ArrayList;

import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.dbash.presenters.root.OverlayQueues;

// This is here to wrap up dependencies on the gdx libraries
// The Presenters should have no such dependencies.  But they will be required to pass this
// down to Views.
// Only Views need to look inside to get what they need to talk to libgdx.
public class UIDepend {

	public SpriteManager spriteManager;
	public CameraViewPort cameraViewPort;
	public ArrayList<SmoothBitmapFont> defaultFonts;
	public ArrayList<SmoothBitmapFont> numericalFonts;
	public BitmapFont numberFont;
	public SizeCalculator sizeCalculator;
	public Audio audio;
	public AssetManager assetManager;
	public OverlayQueues overlayQueues;
	
	public UIDepend() {
		
	}
	
	public UIDepend(UIDepend u) {
		spriteManager = u.spriteManager;
		cameraViewPort  = u.cameraViewPort;
		defaultFonts = u.defaultFonts;
		numericalFonts = u.numericalFonts;
		numberFont = u.numberFont;
		sizeCalculator = u.sizeCalculator;
		audio = u.audio;
		assetManager = u.assetManager;
		overlayQueues = u.overlayQueues;
	}
}
