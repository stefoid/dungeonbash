package com.dbash.platform;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture.TextureFilter;

public class SmoothBitmapFont extends EfficientScalingBitmapFont {
	public SmoothBitmapFont(int size) {
		super(Gdx.files.internal("res/dbash2/toontime" + size +".fnt"), false);
		getRegion().getTexture().setFilter(TextureFilter.Linear, TextureFilter.Linear);
	}
}