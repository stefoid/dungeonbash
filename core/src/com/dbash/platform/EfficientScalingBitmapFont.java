package com.dbash.platform;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.g2d.BitmapFont;

public class EfficientScalingBitmapFont extends BitmapFont {
	public EfficientScalingBitmapFont (FileHandle fontFile, boolean flip) {
		super(fontFile, flip);
	}
	
	@Override
	public void setScale(float x, float y) {
		if (x != getScaleX() || y != getScaleY()) {
			super.setScale(x, y);
		}
	}
}
