package com.dbash.platform;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture.TextureFilter;

public class SmallSmoothBitmapFont extends EfficientScalingBitmapFont {
	public SmallSmoothBitmapFont(int fontSize) {
		super(Gdx.files.internal("res/dbash2/toontime"+fontSize+".fnt"), false);
		getRegion().getTexture().setFilter(TextureFilter.Linear, TextureFilter.Linear);
	}
}